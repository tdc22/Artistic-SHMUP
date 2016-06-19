package objects;

import java.util.ArrayList;
import java.util.List;

import math.QuatMath;
import physics.PhysicsShapeCreator;
import quaternion.Quaternionf;
import shape.Cylinder;
import vector.Vector3f;

public class Tower extends Cylinder implements Shootable, Damageable {
	RigidBody3 body;
	boolean isShooting = true;
	List<Cannon> cannons;
	Vector3f towerfront;
	int health;

	public Tower(float x, float y, float z) {
		super(x, y, z, 1, 1, 36);
		body = new RigidBody3(PhysicsShapeCreator.create(this));
		body.setMass(0);
		body.setInertia(new Quaternionf());
		body.setRestitution(0);
		body.setLinearFactor(new Vector3f(0, 0, 0));
		body.setAngularFactor(new Vector3f(0, 0, 0));

		cannons = new ArrayList<Cannon>();
	}

	public boolean isShooting() {
		return isShooting;
	}

	public void setShooting(boolean shooting) {
		isShooting = shooting;
	}

	public void tickShoot(int delta) {
		for (Cannon cannon : cannons)
			cannon.tickShoot(delta);
	}

	public void shoot() {
		towerfront = QuatMath.transform(this.getRotation(), front);
		for (Cannon cannon : cannons)
			cannon.shoot();
	}

	public void addCannon(Cannon cannon) {
		cannons.add(cannon);
	}

	@Override
	public RigidBody3 getBody() {
		return body;
	}

	@Override
	public void setHealth(int health) {
		this.health = health;
	}

	@Override
	public int getHealth() {
		return health;
	}

	@Override
	public void damage(int damage) {
		health -= damage;
	}

}