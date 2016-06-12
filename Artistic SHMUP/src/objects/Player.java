package objects;

import physics.PhysicsShapeCreator;
import quaternion.Quaternionf;
import shape.Box;
import vector.Vector3f;

public class Player extends Box {
	RigidBody3 playerbody;
	final float acceleration = 20f;
	final float maxspeed = 20f;
	final float maxspeedSqared = maxspeed * maxspeed;

	public Player(float x, float y, float z) {
		super(x, y, z, 1, 1, 1);
		playerbody = new RigidBody3(PhysicsShapeCreator.create(this));
		playerbody.setMass(1);
		playerbody.setInertia(new Quaternionf());
		playerbody.setRestitution(0);
		playerbody.setLinearFactor(new Vector3f(1, 0, 1));
		playerbody.setAngularFactor(new Vector3f(0, 0, 0));
	}

	public RigidBody3 getBody() {
		return playerbody;
	}

	public float getAcceleration() {
		return acceleration;
	}

	public float getMaxSpeed() {
		return maxspeed;
	}

	public float getMaxSpeedSquared() {
		return maxspeedSqared;
	}
}