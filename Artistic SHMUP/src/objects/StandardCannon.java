package objects;

import game.Game;
import physics.PhysicsSpace;
import shader.Shader;
import vector.Vector3f;

public class StandardCannon extends Cannon {
	Game game;
	Shader shotshader;
	ShapedObject3 shotgeometry;
	CollisionShape3 shotcollisionshape;
	int lastshot;
	final int minTimeBetweenShoot = 150;
	boolean canShoot = true;

	public StandardCannon(Game game, PhysicsSpace space, Shootable shooter, Vector3f relativetranslation,
			Vector3f relativedirection, Shader shotshader, ShapedObject3 shotgeometry,
			CollisionShape3 shotcollisionshape) {
		super(space, shooter, relativetranslation, relativedirection);
		this.game = game;
		this.shotshader = shotshader;
		this.shotgeometry = shotgeometry;
		this.shotcollisionshape = shotcollisionshape;
	}

	@Override
	public void tickShoot(int delta) {
		if (!canShoot) {
			lastshot += delta;
			if (lastshot >= minTimeBetweenShoot) {
				lastshot %= minTimeBetweenShoot;
				canShoot = true;
			}
		}
	}

	@Override
	public void shoot() {
		if (canShoot) {
			Vector3f spawnposition = new Vector3f(relativetranslation);
			spawnposition.transform(shooter.getRotation());
			spawnposition.translate(shooter.getTranslationForCannons());
			Vector3f initialdirection = new Vector3f(relativedirection);
			initialdirection.transform(shooter.getRotation());
			Shot shot = new Shot(spawnposition, shotgeometry, shotcollisionshape, initialdirection, shotshader,
					shooter.getBody());
			space.addRigidBody(shot, shot.getBody());
			space.addCollisionFilter(shooter.getBody(), shot.getBody());
			shotshader.addObject(shot);
			game.addShot(shot);

			canShoot = false;
			lastshot = 0;
		}
	}
}
