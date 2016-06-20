package objects;

import shader.Shader;

public interface Damageable {
	public void setHealth(int health);

	public int getHealth();

	public void damage(int damage);

	public RigidBody3 getBody();

	public Shootable getShooter();

	public Shader getShader();

	public ShapedObject3 getShapedObject();
}
