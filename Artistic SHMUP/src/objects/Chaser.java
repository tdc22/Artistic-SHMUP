package objects;

import collisionshape.EllipsoidShape;
import math.VecMath;
import quaternion.Quaternionf;
import shader.Shader;
import shape.Sphere;
import sound.Sound;
import vector.Vector3f;

public class Chaser extends Sphere implements Damageable, Enemy {
	RigidBody3 body;
	int maxhealth, health, healthbarID;
	Shader shader, blinkshader, colorshader;
	final Vector3f up = new Vector3f(0, 1, 0);
	int explosionTimer = 0;
	boolean isBlinkColor = false;
	Sound beepsound;

	final float acceleration = 20f;
	final float maxspeed = 15f;
	final float maxspeedSquared = maxspeed * maxspeed;
	final float maxChaseDistanceSquared = 50 * 50;
	final float explosionRange = 10;
	final float explosionRangeSquared = explosionRange * explosionRange;
	final float explosionTriggerDistanceSquared = 6 * 6;
	final int maxExplosionTimer = 3000;
	final int baseDamage = 50;
	final float baseKnockback = 20;

	// sin(x^3)
	public Chaser(float x, float y, float z, Shader shader, Shader blinkshader, Shader colorshader, int healthbarID,
			int starthealth, Sound beepsound) {
		super(x, y, z, 0.3f, 36, 36);
		body = new RigidBody3(new EllipsoidShape(x, y, z, 1, 1, 1));
		body.scale(3);
		body.setMass(1);
		body.setInertia(new Quaternionf());
		body.setRestitution(0);
		body.setLinearFactor(new Vector3f(1, 0, 1));
		body.setAngularFactor(new Vector3f(0, 1, 0));

		this.shader = shader;
		this.blinkshader = blinkshader;
		this.colorshader = colorshader;
		maxhealth = starthealth;
		health = maxhealth;
		this.healthbarID = healthbarID;
		this.beepsound = beepsound;
		beepsound.setSourcePositionRelative(false);
	}

	public void update(int delta, Player player) {
		Vector3f tp = VecMath.subtraction(player.getTranslation(), getTranslation());
		double distanceSquared = tp.lengthSquared();
		if (distanceSquared <= maxChaseDistanceSquared) {
			if (distanceSquared <= explosionTriggerDistanceSquared) {
				explosionTimer += delta;
				float blinktimerhelper = explosionTimer / 700f;
				float blinktimer = (float) Math.sin(blinktimerhelper * blinktimerhelper * blinktimerhelper);
				if (blinktimer > 0) {
					if (!isBlinkColor) {
						shader.removeObject(this);
						blinkshader.addObject(this);
						isBlinkColor = true;
						beepsound.setPitch(2f + explosionTimer / (float) maxExplosionTimer);
						beepsound.play();
					}
				} else {
					if (isBlinkColor) {
						blinkshader.removeObject(this);
						shader.addObject(this);
						isBlinkColor = false;
					}
				}
			} else {
				explosionTimer = 0;
				if (isBlinkColor) {
					blinkshader.removeObject(this);
					shader.addObject(this);
					isBlinkColor = false;
				}
			}
			tp.normalize();
			tp.scale(acceleration);
			body.applyCentralForce(tp);
		}
		if (getBody().getLinearVelocity().lengthSquared() > maxspeedSquared) {
			getBody().getLinearVelocity().setLength(maxspeed);
		}
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
	public Shootable getShooter() {
		return null;
	}

	public Shader getColorShader() {
		return colorshader;
	}

	public boolean hasExploded() {
		return explosionTimer >= maxExplosionTimer;
	}

	@Override
	public Damageable getDamageable() {
		return this;
	}

	public float getExplosionRange() {
		return explosionRange;
	}

	public float getExplosionRangeSquared() {
		return explosionRangeSquared;
	}

	public int getBaseDamage() {
		return baseDamage;
	}

	public float getBaseKnockback() {
		return baseKnockback;
	}

	@Override
	public void updateSoundPosition() {
		beepsound.setSourcePosition(getTranslation());
	}
}