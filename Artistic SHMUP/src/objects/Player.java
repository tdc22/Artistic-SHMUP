package objects;

import shader.Shader;

public class Player extends Ship {
	final float acceleration = 20f;
	final float maxspeed = 20f;
	final float maxspeedSqared = maxspeed * maxspeed;

	public Player(float x, float y, float z, Shader shader) {
		super(x, y, z, 1, 1, 1, shader);
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