package objects;

import quaternion.Quaternionf;
import vector.Vector3f;

public interface Shootable {

	final Vector3f front = new Vector3f(0, 0, 1);

	public Vector3f getTranslation();

	public Quaternionf getRotation();

	public RigidBody3 getBody();

	public boolean isShooting();

	public void setShooting(boolean shooting);

	public void tickShoot(int delta);

	public void shoot();

	public void addCannon(Cannon cannon);
}
