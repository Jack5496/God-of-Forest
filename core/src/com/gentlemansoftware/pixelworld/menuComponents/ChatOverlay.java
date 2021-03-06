package com.gentlemansoftware.pixelworld.menuComponents;

import java.util.List;

import com.gentlemansoftware.easyGameNetwork.EasyGameNetwork;
import com.gentlemansoftware.pixelworld.game.CameraControllerInterface;
import com.gentlemansoftware.pixelworld.helper.Rectangle;
import com.gentlemansoftware.pixelworld.inputs.GamePad;
import com.gentlemansoftware.pixelworld.menu.MenuHandler;
import com.gentlemansoftware.pixelworld.simplemenu.SimpleMenuComponent;

public class ChatOverlay implements SimpleMenuComponent {

	private MenuHandler handler;

	public ChatOverlay(MenuHandler handler) {
		this.handler = handler;
	}

	@Override
	public int render(CameraControllerInterface display, int ypos) {
		EasyGameNetwork network = handler.user.network;
		if (network != null) {
			List<Object> logMessages = network.getLogMessages();
			ypos = display.getHeight() - 128;
			for (Object objAr : logMessages) {
				String message = (String) objAr;
				display.drawInformationRightAlignedAtPos(display.getWidth(), ypos, message);
				ypos -= 15;
			}

		}

		return 0;
	}

	@Override
	public boolean update(GamePad gamepad) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void select() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActive(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Rectangle getTouchRegion() {
		// TODO Auto-generated method stub
		return null;
	}

}
