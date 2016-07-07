package game;

import vector.Vector3f;

public interface WindowContent {
	public void init();

	public void update(int delta);

	public void render();

	public void renderInterface();

	public void setActive(boolean active);

	public void delete();

	public boolean isActive();

	public Vector3f getPlayerColor();

	public boolean isBackgroundWhite();
}
