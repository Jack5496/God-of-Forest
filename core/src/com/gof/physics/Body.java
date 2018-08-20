package com.gof.physics;

import com.gof.game.Main;
import com.gof.world.MapTile;

public class Body implements Comparable<Body> {
	
	protected Position position;
	Speed velocity;
	Position acceleration;
	
	MapTile referrsTo;

	public Body(Position position, Speed velocity, Position acceleration) {
		this.position = position.cpy();
		setVelocity(velocity);
		setAcceleration(acceleration);
	}
	
	public Body(){
		this(new Position(),new Speed(),new Position());
	}

	public Body(Position position) {
		this(position, new Speed(), new Position());
	}

	public Body(Position position, Speed velocity) {
		this(position, velocity, new Position());
	}

	public Position getPosition() {
		return position.cpy();
	}
	
	public Body setPosition(int x, int y){
		return setPosition(new Position(x,y));
	}
	
	public Body setPosition(Body b){
		return setPosition(b.position);
	}

	protected Body setPosition(Position newpos) {
		this.position = newpos.cpy();
		
		return this;
	}
	
	
	
	/**
	 * One Second are 60 steps
	 * @param deltaTime
	 */
	public void calcPhysicStep(int steps) {
		Position vel = velocity.calcStep(steps);
		setPosition(getPosition().addAndSet(vel));
	}

	public float getValueOfVector(Position vec) {
		return vec.heightCompareLength();
	}

	public Body setVelocity(Speed velocity) {
		this.velocity = velocity.cpy();
		return this;
	}

	public Speed getVelocity() {
		return this.velocity.cpy();
	}

	public Body setAcceleration(Position acceleration) {
		this.acceleration = acceleration.cpy();
		return this;
	}

	@Override
	public int compareTo(Body o) {
		return this.position.compareTo(o.position);
	}

}
