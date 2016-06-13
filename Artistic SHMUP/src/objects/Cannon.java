package objects;

import physics.PhysicsSpace;
import vector.Vector3f;

public abstract class Cannon {
	PhysicsSpace space;
	Ship ship;
	Vector3f relativetranslation;

	public Cannon(PhysicsSpace space, Ship ship, Vector3f relativetranslation) {
		this.space = space;
		this.ship = ship;
		this.relativetranslation = relativetranslation;
	}

	public abstract void tickShoot(int delta);
}
