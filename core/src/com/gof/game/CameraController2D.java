package com.gof.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.gof.inputs.Mouse;
import com.gof.entitys.Entity;
import com.gof.entitys.EntityHostileType;
import com.gof.entitys.Human;
import com.gof.physics.Body;
import com.gof.physics.Direction;
import com.gof.physics.Position;
import com.gof.physics.PositionComperator;
import com.gof.profiles.User;
import com.gof.shaders.ShaddowShader;
import com.gof.world.MapTile;
import com.gof.world.TileWorld;

import com.gof.items.AbstractItem;
import com.gof.items.Item;

public class CameraController2D implements CameraControllerInterface {

	public Body camera;

	private FrameBuffer fbo;
	private FrameBuffer fbUI;

	public SpriteBatch fboBatch;
	public BitmapFont font;

	public int width;
	public int height;

	public static int zoomLevel = 0;
	public static int zoomLevelmin = -2;
	public static int zoomLevelmax = 3;
	
	Direction cameraDirection = Direction.NORTH;

	public GlyphLayout layout;

	private Entity track;

	private User user;

	public CameraController2D(User localUser, int width, int height) {
		resize(width, height);
		this.user = localUser;
		camera.setPosition(0, 0);

		font = new BitmapFont();
		font.setColor(Color.BLACK);
		layout = new GlyphLayout();
	}

	public void resize(int width, int height) {
		setScreenSize(width, height);
		initCamera();
		initFrameBuffer();
	}

	private void setScreenSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	private void initCamera() {
		camera = new Body();
	}

	private void initFrameBuffer() {
		fbo = new FrameBuffer(Format.RGBA8888, width, height, true);
		fbo.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		fbUI = new FrameBuffer(Format.RGBA8888, width, height, true);
		fbUI.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		if (fboBatch != null)
			fboBatch.dispose();
		fboBatch = new SpriteBatch();
	}

	public void renderTileWorldToFrameBuffer() {
		fbo.begin();

		Gdx.gl.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		fboBatch.begin();

		boolean gameActive = this.user.menuHandler.activMenu == this.user.menuHandler.ingameMenu;
		Color save = fboBatch.getColor().cpy();

		if (!gameActive) {
			float intensivity = 0.8f;
			Color shaddow = new Color(new Color(intensivity, intensivity, intensivity, 1));
			fboBatch.setColor(shaddow);
		}

		TileWorld world = user.activGameWorld;
		if (world != null) {
			world.deactivateAllChunks();
			world.activateChunk(world.getChunkGlobalPos(camera.getPosition().x, camera.getPosition().y));
			List<MapTile> area = getAreaToDraw(world);
			Collections.sort(area, new PositionComperator(this.cameraDirection));
			drawGround(area, world);
			drawNatureShaddow(area, world);
			drawNatureAndEntitys(area);
		}

		fboBatch.setColor(save);

		fboBatch.end();
		fbo.end();
	}

	public Direction getCameraDirection() {
		return this.cameraDirection;
	}

	public List<MapTile> getAreaToDraw(TileWorld world) {
		if (track != null) {
			camera.setPosition(track);
		}

		List<MapTile> area = new ArrayList<MapTile>();

		int xcenter = camera.getPosition().x;
		int ycenter = camera.getPosition().y;

		// int safetytiles = 7;
		int safetytiles = 9;

		int breite = this.width / scaleZoom(MapTile.tileWidth) + safetytiles;
		int hoehe = this.height / scaleZoom(MapTile.tileHeight) + safetytiles;

		for (int a = -hoehe + 1; a < hoehe; a++) {
			for (int b = -breite + 1; b < breite; b++) {
				if ((b & 1) != (a & 1))
					continue;
				int x = (a + b) / 2;
				int y = (a - b) / 2;
				MapTile m = world.getMapTileFromGlobalPos(xcenter + x, ycenter + y);
				if (m != null) {
					area.add(m);
				}
			}
		}

		numerator = getZoomLevelScaleFactorNumerator();
		denumerator = getZoomLevelScaleFactorDenumerator();

		return area;
	}

	public int scaleZoom(int orginalPixel) {
		return orginalPixel * numerator / denumerator;
	}

	int numerator = getZoomLevelScaleFactorNumerator();
	int denumerator = getZoomLevelScaleFactorDenumerator();

	private void drawNatureShaddow(List<MapTile> area, TileWorld world) {
		int tileWidth = scaleZoom(MapTile.tileWidth);
		int tileHeight = scaleZoom(MapTile.tileHeight);
		int tileWidthHalf = tileWidth / 2;
		int tileHeightHalf = tileHeight / 2;

		int shaddowRotation = world.time.getShaddowAngle();

		fboBatch.setShader(new ShaddowShader());
		Color save = fboBatch.getColor().cpy();

		float intense = world.time.getLightIntense();
		float shaddowLength = world.time.getShaddowLength();
		Color shaddow = new Color(new Color(0, 0, 0, intense));
		fboBatch.setColor(shaddow);

		for (MapTile tile : area) {
			Sprite nature = tile.getNatureTexture();
			if (nature != null) {

				nature.setScale(1, shaddowLength);
				drawTileSprite(nature, tile.getGlobalPosition(), tileWidthHalf, tileHeightHalf,
						shaddowRotation, tile.height);
			}
		}

		fboBatch.setShader(null);
		fboBatch.setColor(save);
	}

	private void drawNatureAndEntitys(List<MapTile> area) {
		int tileWidth = scaleZoom(MapTile.tileWidth);
		int tileHeight = scaleZoom(MapTile.tileHeight);
		int tileWidthHalf = tileWidth / 2;
		int tileHeightHalf = tileHeight / 2;

		for (MapTile tile : area) {
			for (Entity e : tile.entitys) {
				for (Sprite s : e.getSprite()) {
					drawOnGround(s, tile, e.getPosition(),tileWidthHalf, tileHeightHalf);
				}
			}
			
			Sprite nature = tile.getNatureTexture();
			drawOnGround(nature, tile, tile.getGlobalPosition(), tileWidthHalf, tileHeightHalf);
			
		}
	}

	private void drawOnGround(Sprite sprite, MapTile tile,Position globalPos, int tileWidthHalf, int tileHeightHalf) {

		Color save = fboBatch.getColor();

		drawTileSprite(sprite, globalPos, tileWidthHalf, tileHeightHalf, tile.getRotation(),tile.height);

		if (tile.isInShaddow()) {
			fboBatch.setColor(save);
		}

		fboBatch.setColor(save);
	}

	private void drawGround(List<MapTile> area, TileWorld world) {
		int tileWidth = scaleZoom(MapTile.tileWidth);
		int tileHeight = scaleZoom(MapTile.tileHeight);
		int tileWidthHalf = tileWidth / 2;
		int tileHeightHalf = tileHeight / 2;

		Mouse m = Main.getInstance().inputHandler.keyboardHandler.mouse;

		MapTile mouseTile = null;

		if (m != null) {
			Position globalPos = getGlobalPosFromScreenPos(m.getX(), this.height - m.getY());
			mouseTile = world.getMapTileFromGlobalPos(globalPos.x, globalPos.y);
		}

		for (MapTile tile : area) {
			Sprite sprite = tile.getMaterialSprite();
			Color save = fboBatch.getColor();

			// if (tile.isInShaddow()) {
			// fboBatch.setColor(save.cpy().add(-0.5f, -0.5f, -0.5f, 0));
			// }

			drawTileSprite(sprite, tile.getGlobalPosition(), tileWidthHalf, tileHeightHalf,
					tile.getRotation(),tile.height);

			if (tile.isInShaddow()) {
				fboBatch.setColor(save);
			}

			fboBatch.setColor(save);

		}

	}

	private void drawTileSprite(Sprite sprite, Position globalPos, int tileWidthHalf, int tileHeightHalf,
			int rotation, int heightDifference) {
		if (sprite == null) {
			return;
		}
		int[] xy = globalPosToScreenPos(globalPos, tileHeightHalf, tileWidthHalf, sprite);

		sprite.setPosition(xy[0], xy[1]+scaleZoom(heightDifference));

		// sprite.setOrigin(tileWidthHalf, tileHeightHalf);
		sprite.setOrigin(scaleZoom(90), scaleZoom(240 - 145));
		sprite.setSize(sprite.getWidth() * numerator / denumerator, sprite.getHeight() * numerator / denumerator);
		sprite.setRotation(rotation);

		drawSprite(sprite);
		if (this.user.profile.showDebugInformationCoordinatesOnMapTiles) {
			drawInformationAtPos(xy[0] + 20, xy[1] + tileHeightHalf + 80, globalPos.x + "/" + globalPos.y);
		}
	}

	public void drawSprite(Sprite sprite) {
		fboBatch.draw(sprite, sprite.getX(), sprite.getY(), sprite.getOriginX(), sprite.getOriginY(), sprite.getWidth(),
				sprite.getHeight(), sprite.getScaleX(), sprite.getScaleY(), sprite.getRotation());
	}

	private int getZoomLevelScaleFactorDenumerator() {
		double amount = Math.pow(2, zoomLevel);
		if (amount < 1) {
			return 1;
		}

		return (int) amount;
	}

	private int getZoomLevelScaleFactorNumerator() {
		double amount = Math.pow(2, zoomLevel);
		if (amount < 1) {
			return (int) (1.0 / amount);
		}

		return 1;
	}

	private int[] globalPosToScreenPos(Position globalPos, int tileHeightHalf, int tileWidthHalf, Sprite sprite) {
		int[] screenPos = new int[2];

		int globalY = globalPos.y;
		int globalX = globalPos.x;
		int globalYFrac = globalPos.yFraction;
		int globalXFrac = globalPos.xFraction;
		
		int relativeY = (globalY - camera.getPosition().y);
		int relativeX = (globalX - camera.getPosition().x);

		int oldYF = scaleZoom(globalYFrac-camera.getPosition().yFraction);
		int oldXF = scaleZoom(globalXFrac-camera.getPosition().xFraction);

		int spriteCorrection = scaleZoom(-sprite.getRegionWidth() / 2);
		int widthCorrection = this.width / 2;
		int tileCorrection = scaleZoom(-MapTile.tileHeight);
		int heightCorrection = this.height / 2;

		int xPosMultXPart = 1;
		int xPosMultYPart = -1;
		int yPosMultXPart = 1;
		int yPosMultYPart = 1;

		int extraXCorrection = 0;
		int extraYCorrection = 0;

		if (this.cameraDirection == Direction.NORTH) {
			xPosMultXPart = 1;
			xPosMultYPart = -1;
			yPosMultXPart = 1;
			yPosMultYPart = 1;
		}
		if (this.cameraDirection == Direction.EAST) {
			xPosMultXPart = -1;
			xPosMultYPart = -1;
			yPosMultXPart = 1;
			yPosMultYPart = -1;
			extraYCorrection = +1;
		}
		if (this.cameraDirection == Direction.SOUTH) {
			xPosMultXPart = -1;
			xPosMultYPart = 1;
			yPosMultXPart = -1;
			yPosMultYPart = -1;
			extraXCorrection = +1;
			extraYCorrection = +1;
		}
		if (this.cameraDirection == Direction.WEST) {
			xPosMultXPart = 1;
			xPosMultYPart = 1;
			yPosMultXPart = -1;
			yPosMultYPart = 1;
			extraXCorrection = +1;
		}

		relativeX += extraXCorrection;
		relativeY += extraYCorrection;

		int relativeXYForX = (xPosMultXPart * relativeX + xPosMultYPart * relativeY);
		int fractionCorrectionX = (xPosMultXPart * oldXF / 2 + xPosMultYPart * oldYF);
		screenPos[0] = relativeXYForX * tileWidthHalf + spriteCorrection + widthCorrection + fractionCorrectionX;

		int relativeXYForY = (yPosMultXPart * relativeX + yPosMultYPart * relativeY);
		int fractionCorrectionY = (yPosMultXPart * oldXF / 4 + yPosMultYPart * oldYF / 2);
		screenPos[1] = relativeXYForY * tileHeightHalf + tileCorrection + heightCorrection + fractionCorrectionY;

		return screenPos;
	}

	public Position getGlobalPosFromScreenPos(int screenX, int screenY) {
		int oldYF = scaleZoom(-camera.getPosition().yFraction);
		int oldXF = scaleZoom(-camera.getPosition().xFraction);

		int fractionCorrectionX = oldXF / 2 - oldYF;
		int fractionCorrectionY = oldXF / 4 + oldYF / 2;

		screenX -= fractionCorrectionX;
		screenY -= fractionCorrectionY;

		screenX -= this.width / 2;
		screenY -= this.height / 2;

		boolean xNegative = screenX < 0 ? true : false;
		boolean yNegative = screenY < 0 ? true : false;

		if (xNegative) {
			screenX *= -1;
		}
		if (yNegative) {
			screenY *= -1;
		}

		Sprite mouse = new Sprite(ResourceLoader.getInstance().getTile("mouseMatter"));

		int spriteCorrection = scaleZoom(mouse.getRegionWidth() / 2);
		screenX += spriteCorrection;

		int regionX = screenX / scaleZoom(mouse.getRegionWidth());
		int regionY = screenY / scaleZoom(mouse.getRegionHeight()) * 2;

		int mouseMapX = screenX % scaleZoom(mouse.getRegionWidth());
		int mouseMapY = screenY % scaleZoom(mouse.getRegionHeight()) * 2;

		if (xNegative) {
			regionX *= -1;
		}
		if (yNegative) {
			regionY = -1 * regionY;
		}

		if (yNegative) {
			regionY -= 2;
		}

		int[] region = getRegionDXFromMouseMap(mouseMapX, mouseMapY, scaleZoom(mouse.getRegionWidth()),
				scaleZoom(mouse.getRegionHeight() * 2), scaleZoom(mouse.getRegionWidth() / 2), xNegative, yNegative);

		if (xNegative) {
			// region[0]*=-1;
			// region[1]*=-1;
		}
		if (yNegative) {
		}

		int xCorrection = regionX + regionY / 2 + region[0];
		int yCorrection = -regionX + regionY / 2 + region[1];

		if (this.cameraDirection == Direction.NORTH) {

		}
		if (this.cameraDirection == Direction.EAST) {
			int helper = xCorrection;
			xCorrection = yCorrection;
			yCorrection = -helper;

		}
		if (this.cameraDirection == Direction.SOUTH) {
			xCorrection = -xCorrection;
			yCorrection = -yCorrection;
		}
		if (this.cameraDirection == Direction.WEST) {
			int helper = xCorrection;
			xCorrection = -yCorrection;
			yCorrection = helper;
		}

		int globalX = camera.getPosition().x + xCorrection;
		int globalY = camera.getPosition().y + yCorrection;

		Position globalPos = new Position(globalX, globalY);

		return globalPos;
	}

	/**
	 * Return int[dx,dy]
	 * 
	 * @param mouseMapX
	 * @param mouseMapY
	 * @param mouseMapXMax
	 * @param mouseMapYMax
	 * @param maxDistance
	 * @return
	 */
	private int[] getRegionDXFromMouseMap(int mouseMapX, int mouseMapY, int mouseMapXMax, int mouseMapYMax,
			int maxDistance, boolean xNegative, boolean yNegative) {
		int[] back = { 0, 0 };

		int yTopD = (mouseMapY - mouseMapYMax) * -1;
		int xRightD = (mouseMapX - mouseMapXMax) * -1;

		/*
		 * https://www.gamedev.net/resources/_/technical/game-programming/
		 * isometric-n-hexagonal-maps-part-i-r747
		 */

		if (mouseMapX + mouseMapY <= maxDistance) { /* Gr�ner Bereich */

			if (xNegative && yNegative) {
				back[0] = 1;
			} else if (xNegative) {
				back[1] = -1;
			} else if (yNegative) {
				back[1] = 1;
			} else {
				back[0] = -1;
			}
		} else if (xRightD + mouseMapY <= maxDistance) { /* Blauer Bereich */
			if (xNegative && yNegative) {
				back[1] = 1;
			} else if (xNegative) {
				back[0] = -1;
			} else if (yNegative) {
				back[0] = 1;
			} else {
				back[1] = -1;
			}
		}

		else if (mouseMapX + yTopD <= maxDistance) { /* Roter Bereich */

			if (xNegative && yNegative) {
				back[1] = -1;
			} else if (xNegative) {
				back[0] = 1;
			} else if (yNegative) {
				back[0] = -1;
			} else {
				back[1] = 1;
			}
		} else if (xRightD + yTopD <= maxDistance) { /* Gelber Bereich */
			if (xNegative && yNegative) { /* Hier Gr�ner */
				back[0] = -1;
			} else /* ende Gr�ner Bereich */
			if (xNegative) {
				back[1] = 1;
			} else if (yNegative) {
				back[1] = -1;
			} else {
				back[0] = 1;
			}
		} else {
			// Main.log(getClass(), "Drin");
		}

		return back;
	}

	static int line;

	public void renderToInformationBuffer() {
		fbo.begin();

		Gdx.gl.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());

		// nicht clearen
		// Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT |
		// GL20.GL_DEPTH_BUFFER_BIT);

		fboBatch.enableBlending();
		fboBatch.begin();

		// int x = (int) camera.position.x;
		// int y = (int) camera.position.y;

		line = 1;
		font.setColor(Color.YELLOW);
		drawInformationLine("FPS: " + Gdx.graphics.getFramesPerSecond());
		drawInformationLine("Player: " + Main.getInstance().userHandler.getUserNumber(this.user));
		drawInformationLine("GamePad: " + this.user.gamepad.toString());
		drawInformationLine("Zoom: " + zoomLevel);
		drawInformationLine("CameraDirection: " + this.cameraDirection);

		TileWorld world = this.user.activGameWorld;
		if (world != null) {
			drawTileWorldInformations(world);
		}

		Mouse m = Main.getInstance().inputHandler.keyboardHandler.mouse;

		if (m != null) {
			drawInformationLine("Mouse: " + getGlobalPosFromScreenPos(m.getX(), this.height - m.getY()).toString());
		}
		font.setColor(Color.BLACK);

		fboBatch.end();
		fbo.end();
	}

	private void drawTileWorldInformations(TileWorld world) {
		float light = world.time.getLightIntense();
		drawInformationLine(
				"Days: " + world.time.getDays() + " - Time: " + world.time.getTicks() + " - Light: " + light);
		drawInformationLine("ShaddowAngle: " + world.time.getShaddowAngle());

		if (track != null) {
			Position bodyPos = track.getPosition();
			drawInformationLine("Body Position: " + bodyPos.x + ":" + bodyPos.xFraction + "|" + bodyPos.y + ":"
					+ bodyPos.yFraction);
			MapTile standOn = world.getMapTileFromGlobalPos((int) bodyPos.x, (int) bodyPos.y);
			drawInformationLine("Body Chunk: " + standOn.chunk.x + "|" + standOn.chunk.y);
			drawInformationLine("Stand On: " + standOn.material.getName());
			if (standOn.nature != null) {
				drawInformationLine("Nature: " + standOn.nature.getName());
			}
		}
	}

	private void drawInformationLine(String s) {
		float z = font.getLineHeight();
		drawInformationAtPos(10, height - line * z, s);
		line++;
	}

	private void drawInformationAtPos(float x, float y, String s) {
		float z = font.getLineHeight();
		font.draw(fboBatch, s, x, y - 0.5f * z);
	}

	public void renderGUI() {
		fbUI.begin();

		Gdx.gl.glViewport(0, 0, fbUI.getWidth(), fbUI.getHeight());

		// nicht clearen

		fboBatch.enableBlending();
		fboBatch.begin();

		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT |
		// GL20.GL_DEPTH_BUFFER_BIT);

		drawMenu();
		drawMouseIcon();

		fboBatch.end();
		fbUI.end();
	}

	public void drawMenu() {
		this.user.menuHandler.renderActivMenu(this);
	}

	public int drawSpriteAndSubtractYpos(Sprite s, int xpos, int ypos) {
		ypos -= s.getRegionHeight();
		s.setPosition(xpos, ypos);
		drawSprite(s);
		return ypos;
	}

	private void drawMouseIcon() {
		Mouse m = Main.getInstance().inputHandler.keyboardHandler.mouse;

		Sprite hand = new Sprite(ResourceLoader.getInstance().getGUI("hand_select"));

		if (m != null) {
			hand.setPosition(m.getX() - hand.getRegionWidth() / 2, this.height - m.getY() - hand.getRegionHeight() / 2);
			fboBatch.draw(hand, hand.getX(), hand.getY(), hand.getOriginX(), hand.getOriginY(), hand.getWidth(),
					hand.getHeight(), hand.getScaleX(), hand.getScaleY(), hand.getRotation());
		}
	}

	public void renderToScreen() {
		fboBatch.begin();
		fboBatch.draw(fbo.getColorBufferTexture(), 0, 0, width, height, 0, 0, 1, 1);
		fboBatch.draw(fbUI.getColorBufferTexture(), 0, 0, width, height, 0, 0, 1, 1);
		fboBatch.end();
	}

	public void distanceIncrease() {
		changeDistance(1);
	}

	public void distanceDecrease() {
		changeDistance(-1);
	}

	public void dispose() {
		fbo.dispose();
		fboBatch.dispose();
	}

	public void changeDistance(int amount) {
		zoomLevel += amount;
		if (zoomLevel < zoomLevelmin)
			zoomLevel = zoomLevelmin;
		if (zoomLevel > zoomLevelmax)
			zoomLevel = zoomLevelmax;
	}

	public void setTrack(Entity body) {
		this.track = body;
	}

	@Override
	public void render() {
		// TODO Auto-generated method stub
		renderTileWorldToFrameBuffer();
		if (this.user.profile.showDebugInformationSide) {
			renderToInformationBuffer();
		}
		renderGUI();
		renderToScreen();
	}

	@Override
	public void rotateCamera(float deg) {
		// TODO Auto-generated method stub
		this.cameraDirection = Direction.rotateDirection(cameraDirection, 90);
	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return this.width;
	}

	@Override
	public int getHeigth() {
		// TODO Auto-generated method stub
		return this.height;
	}

	@Override
	public BitmapFont getFont() {
		// TODO Auto-generated method stub
		return this.font;
	}

	@Override
	public GlyphLayout getLayout() {
		// TODO Auto-generated method stub
		return this.layout;
	}

	@Override
	public SpriteBatch getSpriteBatch() {
		// TODO Auto-generated method stub
		return this.fboBatch;
	}
}