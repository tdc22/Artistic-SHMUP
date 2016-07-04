package objects;

import java.util.ArrayList;
import java.util.List;

import math.QuatMath;
import math.VecMath;
import quaternion.Quaternionf;
import shader.Shader;
import vector.Vector3f;

public class Ship extends GameObject3 implements Shootable, Damageable, LateUpdateable {
	ShapedObject3 shape;
	ShapedObject3 headshape;
	RigidBody3 body, head;
	List<Cannon> cannons;
	boolean isShooting = false;
	Vector3f shipfront;
	int maxhealth, health, healthbarID;
	float rotationoffset;
	Shader shader;

	public Ship(float x, float y, float z, float halfsizeX, float halfsizeY, float halfsizeZ, Shader shader,
			int healthbarID, ShapedObject3 shape, RigidBody3 body, ShapedObject3 headshape, RigidBody3 headbody) {
		super(x, y, z);
		this.shape = shape;
		this.body = body;
		body.setMass(1);
		body.setInertia(new Quaternionf());
		body.setRestitution(0);
		body.setLinearFactor(new Vector3f(1, 0, 1));
		body.setAngularFactor(new Vector3f(0, 0, 0));

		this.headshape = headshape;
		this.head = headbody;
		head.setMass(1);
		head.setInertia(new Quaternionf());
		head.setRestitution(0);
		head.setLinearFactor(new Vector3f(1, 0, 1));
		head.setAngularFactor(new Vector3f(0, 0, 0));

		this.shader = shader;
		maxhealth = 100;
		health = maxhealth;
		cannons = new ArrayList<Cannon>();
		this.healthbarID = healthbarID;
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
		return shape;
	}

	public ShapedObject3 getHeadShapedObject() {
		return headshape;
	}

	public RigidBody3 getHeadBody() {
		return head;
	}

	@Override
	public int getHealthbarID() {
		return healthbarID;
	}

	@Override
	public int getMaxHealth() {
		return maxhealth;
	}

	@Override
	public void setHealthbarID(int healthbarID) {
		this.healthbarID = healthbarID;
	}

	final Vector3f yAxis = new Vector3f(0, 1, 0);

	@Override
	public void lateUpdate(int delta) {
		Vector3f force = VecMath.subtraction(getTranslation(), head.getTranslation());
		force.scale(force.length() + 10);
		force.y = 0;
		head.setLinearVelocity(force);
		headshape.translateTo(head.getTranslation());

		rotationoffset += delta * 0.1;
		if (rotationoffset >= 360)
			rotationoffset %= 360;
		shape.translateTo(getTranslation().x,
				getTranslation().y + (float) Math.sin(Math.toRadians(rotationoffset)) * 0.3f + 0.3f,
				getTranslation().z);
		Quaternionf rot = new Quaternionf(getRotation());
		rot.rotate(rotationoffset, yAxis);
		shape.rotateTo(rot);
	}
}
