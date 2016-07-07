package objects;

import shader.Shader;
import vector.Vector3f;

public interface Damageable {
	public void setHealth(int health);

	public int getHealth();

	public int getMaxHealth();

	public void damage(int damage);

	public RigidBody3 getBody();

	public Shootable getShooter();

	public Shader getShader();

	public ShapedObject3 getShapedObject();

	public int getHealthbarID();

	public void setHealthbarID(int healthbarID);

	public Vector3f getTranslation();
}
