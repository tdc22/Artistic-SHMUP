package objects;

import java.util.ArrayList;
import java.util.List;

import math.QuatMath;
import physics.PhysicsShapeCreator;
import quaternion.Quaternionf;
import shader.Shader;
import shape.Box;
import vector.Vector3f;

public class Ship extends Box implements Shootable, Damageable {
	RigidBody3 body;
	List<Cannon> cannons;
	boolean isShooting = false;
	Vector3f shipfront;
	int health;
	Shader shader;

	public Ship(float x, float y, float z, float halfsizeX, float halfsizeY, float halfsizeZ, Shader shader) {
		super(x, y, z, halfsizeX, halfsizeY, halfsizeZ);
		body = new RigidBody3(PhysicsShapeCreator.create(this));
		body.setMass(1);
		body.setInertia(new Quaternionf());
		body.setRestitution(0);
		body.setLinearFactor(new Vector3f(1, 0, 1));
		body.setAngularFactor(new Vector3f(0, 0, 0));

		this.shader = shader;
		health = 100;
		cannons = new ArrayList<Cannon>();
	}

	public RigidBody3 getBody() {
		return body;
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

	@Override
	public Shootable getShooter() {
		return this;
	}

	@Override
	public Shader getShader() {
		return shader;
	}

	@Override
	public ShapedObject3 getShapedObject() {
		return this;
	}
}
