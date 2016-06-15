package objects;

import java.util.ArrayList;
import java.util.List;

import math.QuatMath;
import physics.PhysicsShapeCreator;
import quaternion.Quaternionf;
import shape.Box;
import vector.Vector3f;

public class Ship extends Box {
	RigidBody3 body;
	List<Cannon> cannons;
	boolean isShooting = false;
	final Vector3f front = new Vector3f(0, 0, 1);
	Vector3f shipfront;

	public Ship(float x, float y, float z, float halfsizeX, float halfsizeY, float halfsizeZ) {
		super(x, y, z, halfsizeX, halfsizeY, halfsizeZ);
		body = new RigidBody3(PhysicsShapeCreator.create(this));
		body.setMass(1);
		body.setInertia(new Quaternionf());
		body.setRestitution(0);
		body.setLinearFactor(new Vector3f(1, 0, 1));
		body.setAngularFactor(new Vector3f(0, 0, 0));

		cannons = new ArrayList<Cannon>();
	}

	public RigidBody3 getBody() {
		return body;
	}

	public void setShooting(boolean shooting) {
		isShooting = shooting;
	}

	public void tickShoot(int delta) {
		for (Cannon cannon : cannons)
			cannon.tickShoot(delta);
	}

	public void shoot() {
		shipfront = QuatMath.transform(this.getRotation(), front);
		for (Cannon cannon : cannons)
			cannon.shoot();
	}

	public Vector3f getShipFront() {
		return shipfront;
	}

	public void addCannon(Cannon cannon) {
		cannons.add(cannon);
	}
}
