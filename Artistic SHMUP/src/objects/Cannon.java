package objects;

import physics.PhysicsSpace;
import vector.Vector3f;

public abstract class Cannon {
	PhysicsSpace space;
	Shootable shooter;
	Vector3f relativetranslation, relativedirection;

	public Cannon(PhysicsSpace space, Shootable shooter, Vector3f relativetranslation, Vector3f relativedirection) {
		this.space = space;
		this.shooter = shooter;
		this.relativetranslation = relativetranslation;
		this.relativedirection = relativedirection;
	}

	public abstract void tickShoot(int delta);

	public abstract void shoot();
}
