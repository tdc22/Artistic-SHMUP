package objects;

public interface Enemy {
	public void update(int delta, Player player);

	public Damageable getDamageable();
}
