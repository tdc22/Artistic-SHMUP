package objects;

public interface Enemy {
	public void update(int delta, Player player);

	public void updateSoundPosition();

	public Damageable getDamageable();
}
