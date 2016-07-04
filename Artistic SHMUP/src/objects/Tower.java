package objects;

import java.util.ArrayList;
import java.util.List;

import math.QuatMath;
import math.VecMath;
import physics.PhysicsShapeCreator;
import quaternion.Quaternionf;
import shader.Shader;
import shape.Cylinder;
import vector.Vector3f;

public class Tower extends Cylinder implements Shootable, Damageable, Enemy {
	RigidBody3 body;
	boolean isShooting = true;
	List<Cannon> cannons;
	Vector3f towerfront;
	int maxhealth, health, healthbarID;
	Shader shader;
	Vector3f translationForCannons;
	final Vector3f up = new Vector3f(0, 1, 0);

	public Tower(float x, float y, float z, Shader shader, int healthbarID, int starthealth) {
		super(x, y, z, 1, 1, 36);
		body = new RigidBody3(PhysicsShapeCreator.create(this));
		body.setMass(1);
		body.setInertia(new Quaternionf());
		body.setRestitution(0);
		body.setLinearFactor(new Vector3f(0, 0, 0));
		body.setAngularFactor(new Vector3f(0, 1, 0));

		towerfront = QuatMath.transform(this.getRotation(), front);
		this.shader = shader;
		maxhealth = starthealth;
		health = maxhealth;
		cannons = new ArrayList<Cannon>();
		this.healthbarID = healthbarID;

		translationForCannons = new Vector3f(getTranslation().x, getTranslation().y + 1, getTranslation().z);
	}

	public boolean isShooting() {
		return isShooting;
	}

	public void setShooting(boolean shooting) {
		isShooting = shooting;
	}

	public void update(int delta, Player player) {
		Vector3f tp = VecMath.subtraction(player.getTranslation(), getTranslation());
		tp.normalize();
		Vector3f side = VecMath.crossproduct(towerfront, up);
		body.applyTorqueImpulse(new Vector3f(0, VecMath.dotproduct(side, tp), 0));
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

	@Override
	public Vector3f getTranslationForCannons() {
		return translationForCannons;
	}

}