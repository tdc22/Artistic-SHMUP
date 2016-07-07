package game;

import java.util.ArrayList;
import java.util.List;

import broadphase.SAP;
import collisionshape.BoxShape;
import input.Input;
import input.InputEvent;
import input.KeyInput;
import input.MouseInput;
import integration.VerletIntegration;
import loader.FontLoader;
import loader.ModelLoader;
import loader.ShaderLoader;
import loader.SoundLoader;
import loader.TextureLoader;
import manifold.CollisionManifold;
import manifold.SimpleManifoldManager;
import math.VecMath;
import narrowphase.EPA;
import narrowphase.GJK;
import objects.Chaser;
import objects.CollisionShape3;
import objects.Damageable;
import objects.Enemy;
import objects.LateUpdateable;
import objects.Player;
import objects.RigidBody;
import objects.RigidBody3;
import objects.ShapedObject3;
import objects.Shootable;
import objects.Shot;
import objects.StandardCannon;
import objects.Tower;
import particle.SimpleParticleSystem;
import physics.PhysicsDebug;
import physics.PhysicsShapeCreator;
import physics.PhysicsSpace;
import positionalcorrection.ProjectionCorrection;
import quaternion.Complexf;
import resolution.SimpleLinearImpulseResolution;
import shader.Shader;
import shape.Box;
import shape.Sphere;
import shape2d.Quad;
import shapedata.CylinderData;
import shapedata.SphereData;
import sound.ALSound;
import sound.Sound;
import texture.Texture;
import vector.Vector2f;
import vector.Vector3f;
import vector.Vector4f;

public class Game implements WindowContent {
	MainWindow game;
	PhysicsDebug physicsdebug;
	InputEvent eventEsc, eventUp, eventDown, eventLeft, eventRight, eventShoot;
	PhysicsSpace space;
	Player player;
	Vector3f playerspawn;
	Shader defaultshader, playershader, defaultshaderInterface, redcolorshaderInterface, healthbarshader;
	boolean isPaused = false;

	final float mouseSensitivity = -0.1f; // negative sensitivity = not inverted

	final Vector3f front = new Vector3f(0, 0, 1);
	final Vector3f zero = new Vector3f(0, 0, 0);
	Vector3f playerfront = new Vector3f();
	Vector2f move = new Vector2f();
	Complexf playerrotation = new Complexf();
	final Vector3f cameraOffset = new Vector3f(0, 10, 10);
	Vector3f transformedCameraOffset = new Vector3f();

	final int levelsizeX; // mod 2 = 0 !!!
	final int levelsizeZ;
	final int halflevelsizeX;
	final int halflevelsizeZ;
	final int gridsizeX;
	final int gridsizeZ;
	Vector3f playercolor;
	boolean whitebackground;

	boolean[][] levelgrid;
	BoxShape groundshape;

	final int healthbarHalfSizeX = 100;
	final int healthbarHalfSizeY = 10;
	final int healthbarMargin = 10;
	final int healthbarBorder = 1;

	boolean isEndless;
	int spawntimer, maxspawntimer, spawntowers, spawnchasers;
	boolean isActive = true;

	Shader blackcolorshader, whitecolorshader, playercolorshader;
	Texture[] splashtextures;
	ALSound beepsound;
	ALSound[] splashsounds;

	Quad healthbar;
	SimpleParticleSystem lifebars;
	final Vector2f enemyLifebarSize = new Vector2f(1, 0.3);
	final Vector3f up = new Vector3f(0, 1, 0);

	Sphere shotgeometry;
	CollisionShape3 shotcollisionshape;
	List<Shootable> shooters;
	List<Damageable> targets;
	List<Enemy> enemies;
	List<Enemy> movingEnemies;
	List<LateUpdateable> lateupdates;
	List<Shot> shots;
	List<Chaser> chasers;
	Shader playerShotShader;
	List<Shader> shotColorShaders;
	List<Box> levelgeometry;

	public Game(MainWindow game, int levelsizeX, int levelsizeZ, Vector3f playercolor, boolean whitebackground,
			boolean isEndless) {
		this.game = game;
		this.levelsizeX = levelsizeX;
		this.levelsizeZ = levelsizeZ;
		halflevelsizeX = levelsizeX / 2;
		halflevelsizeZ = levelsizeZ / 2;
		gridsizeX = levelsizeX / 2 - 2;
		gridsizeZ = levelsizeZ / 2 - 2;
		this.playercolor = playercolor;
		this.whitebackground = whitebackground;
		this.isEndless = isEndless;
		if (isEndless) {
			spawntimer = 0;
			maxspawntimer = 12000;
			spawntowers = 1;
			spawnchasers = 0;
		}
	}

	@Override
	public void init() {
		game.display.bindMouse();

		space = new PhysicsSpace(new VerletIntegration(), new SAP(), new GJK(new EPA()),
				new SimpleLinearImpulseResolution(), new ProjectionCorrection(0.01f),
				new SimpleManifoldManager<Vector3f>());
		space.setGlobalGravitation(new Vector3f(0, -10, 0));

		int defaultshaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		defaultshader = new Shader(defaultshaderID);
		game.addShader(defaultshader);
		defaultshaderInterface = new Shader(defaultshaderID);
		game.addShaderInterface(defaultshaderInterface);

		int colorshaderprogram = ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert",
				"res/shaders/colorshader.frag");
		playercolorshader = new Shader(colorshaderprogram);
		playercolorshader.addArgument("u_color", new Vector4f(0.75, 0.75, 0.75, 1));
		game.addShader(playercolorshader);
		blackcolorshader = new Shader(colorshaderprogram);
		blackcolorshader.addArgument("u_color", new Vector4f(0, 0, 0, 1));
		game.addShader(blackcolorshader);
		whitecolorshader = new Shader(colorshaderprogram);
		whitecolorshader.addArgument("u_color", new Vector4f(1, 1, 1, 1));
		game.addShader(whitecolorshader);
		redcolorshaderInterface = new Shader(colorshaderprogram);
		redcolorshaderInterface.addArgument("u_color", new Vector4f(1, 0, 0, 1));
		game.addShaderInterface(redcolorshaderInterface);

		physicsdebug = new PhysicsDebug(game.inputs, FontLoader.loadFont("res/fonts/DejaVuSans.ttf"), space,
				defaultshader);

		splashtextures = new Texture[5];
		splashtextures[0] = new Texture(TextureLoader.loadTexture("res/textures/splash1.png"));
		splashtextures[1] = new Texture(TextureLoader.loadTexture("res/textures/splash2.png"));
		splashtextures[2] = new Texture(TextureLoader.loadTexture("res/textures/splash3.png"));
		splashtextures[3] = new Texture(TextureLoader.loadTexture("res/textures/splash4.png"));
		splashtextures[4] = new Texture(TextureLoader.loadTexture("res/textures/splash5.png"));

		beepsound = new ALSound(SoundLoader.loadSound("res/sounds/beep.ogg"));
		splashsounds = new ALSound[7];
		splashsounds[0] = new ALSound(SoundLoader.loadSound("res/sounds/splash1.ogg"));
		splashsounds[1] = new ALSound(SoundLoader.loadSound("res/sounds/splash2.ogg"));
		splashsounds[2] = new ALSound(SoundLoader.loadSound("res/sounds/splash3.ogg"));
		splashsounds[3] = new ALSound(SoundLoader.loadSound("res/sounds/splash4.ogg"));
		splashsounds[4] = new ALSound(SoundLoader.loadSound("res/sounds/splash5.ogg"));
		splashsounds[5] = new ALSound(SoundLoader.loadSound("res/sounds/splash6.ogg"));
		splashsounds[6] = new ALSound(SoundLoader.loadSound("res/sounds/splash7.ogg"));
		for (Sound sound : splashsounds) {
			sound.setSourcePositionRelative(false);
		}

		healthbarshader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/healthbarshader.vert",
				"res/shaders/healthbarshader.frag"));
		game.addShader(healthbarshader);

		Quad healthbarBackground = new Quad(healthbarMargin + healthbarBorder + healthbarHalfSizeX,
				healthbarMargin + healthbarBorder + healthbarHalfSizeY, healthbarBorder + healthbarHalfSizeX,
				healthbarBorder + healthbarHalfSizeY);
		defaultshaderInterface.addObject(healthbarBackground);
		healthbar = new Quad(healthbarMargin + healthbarBorder + healthbarHalfSizeX,
				healthbarMargin + healthbarBorder + healthbarHalfSizeY, healthbarHalfSizeX, healthbarHalfSizeY);
		redcolorshaderInterface.addObject(healthbar);

		lifebars = new SimpleParticleSystem(new Vector3f(), game.cam, false);
		lifebars.getParticleObject().setRenderHints(true, true, false);
		healthbarshader.addObject(lifebars);

		groundshape = new BoxShape(halflevelsizeX, -2f, halflevelsizeZ, halflevelsizeX, 1, halflevelsizeZ);
		RigidBody3 rbground = new RigidBody3(groundshape);
		rbground.translate(halflevelsizeX, -2f, halflevelsizeZ);
		rbground.setMass(0);
		space.addRigidBody(rbground);

		float halfboxsizeX = levelsizeX / (float) (game.splashSubdivision * 2);
		float halfboxsizeZ = levelsizeZ / (float) (game.splashSubdivision * 2);

		float delta = 0.01f;
		game.addStaticBox(halfboxsizeX, 0, 1f, halfboxsizeX - delta, 1, 1f, space);
		game.addStaticBox(1f, 0, halfboxsizeZ + 1f, 1f, 1, halfboxsizeZ - 1f - delta, space);
		float boxsizeX = halfboxsizeX * 2;
		float boxsizeZ = halfboxsizeZ * 2;
		for (int i = 1; i < game.splashSubdivision - 1; i++) {
			game.addStaticBox(i * boxsizeX + halfboxsizeX, 0, 1f, halfboxsizeX - delta, 1, 1f, space);
			game.addStaticBox(1f, 0, i * boxsizeZ + halfboxsizeZ, 1f, 1, halfboxsizeZ - delta, space);
			game.addStaticBox(i * boxsizeX + halfboxsizeX, 0, levelsizeZ - 1f, halfboxsizeX - delta, 1, 1f - delta,
					space);
			game.addStaticBox(levelsizeX - 1f, 0, i * boxsizeZ + halfboxsizeZ, 1f - delta, 1, halfboxsizeZ - delta,
					space);
		}
		if (game.splashSubdivision > 1) {
			game.addStaticBox(levelsizeX - halfboxsizeX, 0, 1f, halfboxsizeX - delta, 1, 1f, space);
			game.addStaticBox(1f, 0, levelsizeZ - halfboxsizeZ, 1f, 1, halfboxsizeZ - delta, space);
			game.addStaticBox(halfboxsizeX + 1f, 0, levelsizeZ - 1f, halfboxsizeX - 1f - delta, 1, 1f - delta, space);
			game.addStaticBox(levelsizeX - 1f, 0, halfboxsizeZ + 1f, 1f - delta, 1, halfboxsizeZ - 1f - delta, space);
			game.addStaticBox(levelsizeX - halfboxsizeX, 0, levelsizeZ - 1f, halfboxsizeX - delta, 1, 1f - delta,
					space);
			game.addStaticBox(levelsizeX - 1f, 0, levelsizeZ - halfboxsizeZ - 1f, 1f - delta, 1,
					halfboxsizeZ - 1f - delta, space);
		}

		shooters = new ArrayList<Shootable>();
		targets = new ArrayList<Damageable>();
		enemies = new ArrayList<Enemy>();
		movingEnemies = new ArrayList<Enemy>();
		lateupdates = new ArrayList<LateUpdateable>();
		levelgeometry = new ArrayList<Box>();
		chasers = new ArrayList<Chaser>();
		shotgeometry = new Sphere(0, 0, 0, 0.2f, 36, 36);
		shotcollisionshape = PhysicsShapeCreator.create(shotgeometry);

		shots = new ArrayList<Shot>();
		shotColorShaders = new ArrayList<Shader>();
		if (playercolor.x == 1 && playercolor.y == 0 && playercolor.z == 0) {
			playerShotShader = new Shader(colorshaderprogram, "u_color", new Vector4f(1, 0, 0, 1));
		} else {
			shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(1, 0, 0, 1)));
		}
		if (playercolor.x == 0 && playercolor.y == 1 && playercolor.z == 0) {
			playerShotShader = new Shader(colorshaderprogram, "u_color", new Vector4f(0, 1, 0, 1));
		} else {
			shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(0, 1, 0, 1)));
		}
		if (playercolor.x == 0 && playercolor.y == 0 && playercolor.z == 1) {
			playerShotShader = new Shader(colorshaderprogram, "u_color", new Vector4f(0, 0, 1, 1));
		} else {
			shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(0, 0, 1, 1)));
		}
		if (playercolor.x == 1 && playercolor.y == 1 && playercolor.z == 0) {
			playerShotShader = new Shader(colorshaderprogram, "u_color", new Vector4f(1, 1, 0, 1));
		} else {
			shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(1, 1, 0, 1)));
		}
		if (playercolor.x == 1 && playercolor.y == 0 && playercolor.z == 1) {
			playerShotShader = new Shader(colorshaderprogram, "u_color", new Vector4f(1, 0, 1, 1));
		} else {
			shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(1, 0, 1, 1)));
		}
		if (playercolor.x == 0 && playercolor.y == 1 && playercolor.z == 1) {
			playerShotShader = new Shader(colorshaderprogram, "u_color", new Vector4f(0, 1, 1, 1));
		} else {
			shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(0, 1, 1, 1)));
		}
		game.addShader(playerShotShader);
		for (Shader s : shotColorShaders) {
			game.addShader(s);
		}

		playershader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/phongshader.vert", "res/shaders/phongshader.frag"));
		playershader.addArgumentName("u_lightpos");
		playershader.addArgument(new Vector3f(0, 0, 10));
		playershader.addArgumentName("u_ambient");
		playershader.addArgument(new Vector3f(0.2f, 0.2f, 0.2f));
		playershader.addArgumentName("u_diffuse");
		playershader.addArgument(new Vector3f(0.5f, 0.5f, 0.5f));
		playershader.addArgumentName("u_shininess");
		playershader.addArgument(10f);
		game.addShader(playershader);

		if (isEndless) {
			generateLevel(100, 1, 0);
		} else {
			generateLevel(100, 10, 5);
		}

		float xzscale = 1.3f;
		playerspawn = new Vector3f(halflevelsizeX, 0, halflevelsizeZ);
		ShapedObject3 bodyshape = ModelLoader.load("res/models/playerbase.obj");
		bodyshape.translateTo(playerspawn.x, 0, playerspawn.z);
		bodyshape.scale(1, 0.8f, 1);
		player = new Player(playerspawn.x, playerspawn.y, playerspawn.z, playershader, bodyshape,
				new RigidBody3(PhysicsShapeCreator.create(new CylinderData(0, 0, 0, xzscale * 0.6f, 1))),
				new Sphere(playerspawn.x, 1f, playerspawn.z, 0.3f, 32, 32),
				new RigidBody3(PhysicsShapeCreator.create(new SphereData(0, 0, 0, 0.3f))));
		player.scale(xzscale, 1, xzscale);
		space.addRigidBody(player, player.getBody());
		space.addRigidBody(player.getHeadShapedObject(), player.getHeadBody());
		space.addCollisionFilter(player.getBody(), player.getHeadBody());
		player.getShapedObject().setRenderHints(false, false, true);
		player.getHeadShapedObject().setRenderHints(false, false, true);
		playershader.addObject(player.getShapedObject());
		playershader.addObject(player.getHeadShapedObject());
		shooters.add(player);
		targets.add(player);
		lateupdates.add(player);

		player.addCannon(new StandardCannon(this, space, player, new Vector3f(0, 0, -1), new Vector3f(0, 0, 1),
				playerShotShader, shotgeometry, shotcollisionshape));
		/*
		 * player.addCannon(new StandardCannon(this, space, player, new
		 * Vector3f(0.8f, 0, -1), new Vector3f(-0.4472136, 0, 0.8944272),
		 * shotColorShaders.get(1), shotgeometry, shotcollisionshape));
		 * player.addCannon(new StandardCannon(this, space, player, new
		 * Vector3f(-0.8f, 0, -1), new Vector3f(0.4472136, 0, 0.8944272),
		 * shotColorShaders.get(2), shotgeometry, shotcollisionshape));
		 */

		setupInputs();
		updatePlayerRotationVariables();
	}

	public void spawnWave() {
		spawntowers++;
		spawnchasers++;
		spawnTowers(spawntowers);
		// spawnChasers(spawnchasers);
	}

	private void addTower(float x, float y, float z) {
		System.out.println("Added " + lifebars.getParticleList().size() + "; " + x + "; " + z);
		Tower tower = new Tower(x, y, z, blackcolorshader, lifebars.getParticleList().size(), 50);
		space.addRigidBody(tower, tower.getBody());
		tower.addCannon(new StandardCannon(this, space, tower, new Vector3f(0, 0, -1), new Vector3f(0, 0, 1),
				getRandomShotColorShader(), shotgeometry, shotcollisionshape));
		blackcolorshader.addObject(tower);
		shooters.add(tower);
		targets.add(tower);
		enemies.add(tower);
		lifebars.addParticle(
				new Vector3f(tower.getTranslation().x, tower.getTranslation().y + 2, tower.getTranslation().z), zero,
				enemyLifebarSize, 1000);
	}

	private void addChaser(float x, float y, float z) {
		Shader colorshader = getRandomShotColorShader();
		Chaser chaser = new Chaser(x, y, z, blackcolorshader, colorshader, colorshader,
				lifebars.getParticleList().size(), 70, new ALSound(beepsound.getSoundBufferHandle()));
		space.addRigidBody(chaser, chaser.getBody());
		blackcolorshader.addObject(chaser);
		targets.add(chaser);
		enemies.add(chaser);
		movingEnemies.add(chaser);
		chasers.add(chaser);
		lifebars.addParticle(
				new Vector3f(chaser.getTranslation().x, chaser.getTranslation().y + 2, chaser.getTranslation().z), zero,
				enemyLifebarSize, 1000);
	}

	public Shader getRandomShotColorShader() {
		return shotColorShaders.get((int) (Math.random() * shotColorShaders.size()));
	}

	public void generateLevel(int numboxes, int numtowers, int numchasers) {
		levelgrid = new boolean[gridsizeX][gridsizeZ];

		for (int x = 0; x < gridsizeX; x++) {
			for (int z = 0; z < gridsizeZ; z++) {
				levelgrid[x][z] = false;
			}
		}
		levelgrid[gridsizeX / 2][gridsizeZ / 2] = true; // player

		float sizeX = 0.999f;
		float sizeZ = 0.999f;
		for (int i = 0; i < numboxes; i++) {
			int posX = (int) (Math.random() * gridsizeX);
			int posZ = (int) (Math.random() * gridsizeZ);
			if (!levelgrid[posX][posZ]) {
				levelgrid[posX][posZ] = true;
				levelgeometry.add(game.addStaticBox(posX * 2 + 3, 0, posZ * 2 + 3, sizeX, 1, sizeZ, space));
			} else {
				i--;
			}
		}
		spawnTowers(numtowers);
		spawnChasers(numchasers);
	}

	private void spawnTowers(int numtowers) {
		for (int i = 0; i < numtowers; i++) {
			int posX = (int) (Math.random() * gridsizeX);
			int posZ = (int) (Math.random() * gridsizeZ);
			if (!levelgrid[posX][posZ]) {
				addTower(posX * 2 + 3, 0, posZ * 2 + 3);
			} else {
				i--;
			}
		}
	}

	private void spawnChasers(int numchasers) {
		for (int i = 0; i < numchasers; i++) {
			int posX = (int) (Math.random() * gridsizeX);
			int posZ = (int) (Math.random() * gridsizeZ);
			if (!levelgrid[posX][posZ]) {
				addChaser(posX * 2 + 3, 0, posZ * 2 + 3);
			} else {
				i--;
			}
		}
	}

	public void setupInputs() {
		game.inputs.getInputEvents().remove("game_close");

		Input inputKeyEscape = new Input(Input.KEYBOARD_EVENT, "Escape", KeyInput.KEY_PRESSED);
		Input inputKeyUp = new Input(Input.KEYBOARD_EVENT, "W", KeyInput.KEY_DOWN);
		Input inputKeyDown = new Input(Input.KEYBOARD_EVENT, "S", KeyInput.KEY_DOWN);
		Input inputKeyLeft = new Input(Input.KEYBOARD_EVENT, "A", KeyInput.KEY_DOWN);
		Input inputKeyRight = new Input(Input.KEYBOARD_EVENT, "D", KeyInput.KEY_DOWN);
		Input inputMouseLeft = new Input(Input.MOUSE_EVENT, "Left", MouseInput.MOUSE_BUTTON_DOWN);

		eventEsc = new InputEvent("Escape", inputKeyEscape);
		eventUp = new InputEvent("Up", inputKeyUp);
		eventDown = new InputEvent("Down", inputKeyDown);
		eventLeft = new InputEvent("Left", inputKeyLeft);
		eventRight = new InputEvent("Right", inputKeyRight);
		eventShoot = new InputEvent("Shoot", inputMouseLeft);

		game.inputs.addEvent(eventEsc);
		game.inputs.addEvent(eventUp);
		game.inputs.addEvent(eventDown);
		game.inputs.addEvent(eventLeft);
		game.inputs.addEvent(eventRight);
		game.inputs.addEvent(eventShoot);
	}

	@Override
	public void update(int delta) {
		move.set(0, 0);
		if (eventEsc.isActive()) {
			isPaused = !isPaused;
			if (isPaused) {
				game.display.unbindMouse();
			} else {
				game.display.bindMouse();
			}
		}
		if (!isPaused) {
			if (eventUp.isActive()) {
				move.y -= 1;
			}
			if (eventDown.isActive()) {
				move.y += 1;
			}
			if (eventLeft.isActive()) {
				move.x -= 1;
			}
			if (eventRight.isActive()) {
				move.x += 1;
			}

			float mouseX = game.inputs.getMouseX();
			if (mouseX > 0 || mouseX < 0) {
				player.getBody().rotate(0, mouseX * mouseSensitivity, 0);
				updatePlayerRotationVariables();
			}

			player.setShooting(eventShoot.isActive());
			for (Shootable shooter : shooters) {
				shooter.tickShoot(delta);
				if (shooter.isShooting()) {
					shooter.shoot();
				}
			}

			move.transform(playerrotation);
			if (move.lengthSquared() > 0) {
				move.normalize();
				move.scale(player.getAcceleration());
				if (move.y < 0) {
					move.scale(2);
				}
				player.getBody().applyCentralForce(new Vector3f(move.x, 0, move.y)); // TODO:
																						// add
																						// missing
																						// setter
																						// to
																						// rigidbody-methods
			}
			if (player.getBody().getLinearVelocity().lengthSquared() > 0) {
				if (player.getBody().getLinearVelocity().lengthSquared() > player.getMaxSpeedSquared()) {
					player.getBody().getLinearVelocity().setLength(player.getMaxSpeed());
				}
				Vector3f drag = new Vector3f(player.getBody().getLinearVelocity());
				drag.scale(1f);
				drag.negate();
				player.getBody().applyCentralForce(drag);
			}

			for (Enemy enemy : enemies) {
				enemy.update(delta, player);
			}

			space.update(delta);
			physicsdebug.update();
			game.soundEnvironment.setListenerPosition(player.getTranslation());
			game.soundEnvironment.setListenerOrientation(up, playerfront);

			for (LateUpdateable lateupdate : lateupdates) {
				lateupdate.lateUpdate(delta);
			}

			for (int i = 0; i < shots.size(); i++) {
				Shot shot = shots.get(i);
				CollisionManifold<Vector3f> manifold = space.getFirstCollisionManifold(shot.getBody());
				if (manifold != null) {
					RigidBody<Vector3f, ?, ?, ?> other = (manifold.getObjects().getFirst().equals(shot.getBody()))
							? manifold.getObjects().getSecond() : manifold.getObjects().getFirst();
					Damageable damaged = null;
					for (Damageable dmg : targets) {
						if (other.equals(dmg.getBody())) {
							damaged = dmg;
							break;
						}
					}
					if (damaged != null) {
						int damage = 10;
						applyDamage(damaged, damage);
					}

					float splashsize = 1.5f + (float) (Math.random() * 1.5f);
					createSplash(shot.getTranslation().x, shot.getTranslation().z, splashsize,
							(Vector4f) shot.getShotShader().getArgument("u_color"));
					playRandomSplashSound(shot.getTranslation());

					shots.remove(i);
					deleteShot(shot);
					i--;
				} else {
					if (shot.getTranslation().y < -20) {
						shots.remove(i);
						deleteShot(shot);
					}
				}
			}

			for (Enemy enemy : movingEnemies) {
				Damageable damageable = enemy.getDamageable();
				lifebars.getParticle(damageable.getHealthbarID()).getPosition().set(damageable.getTranslation().x,
						damageable.getTranslation().y + 2, damageable.getTranslation().z);
				enemy.updateSoundPosition();
			}

			for (int i = chasers.size() - 1; i >= 0; i--) {
				if (i < chasers.size()) {
					Chaser chaser = chasers.get(i);
					if (chaser.hasExploded()) {
						createSplash(chaser.getTranslation().x, chaser.getTranslation().z, chaser.getExplosionRange(),
								(Vector4f) chaser.getColorShader().getArgument("u_color"));
						for (int a = 0; a < targets.size(); a++) {
							Damageable damageable = targets.get(a);
							if (!damageable.equals(chaser)) {
								Vector3f dist = VecMath.subtraction(damageable.getTranslation(),
										chaser.getTranslation());
								double lenSquared = dist.lengthSquared();
								if (lenSquared < chaser.getExplosionRangeSquared()) {
									float damagefactor = (float) (1 - lenSquared / chaser.getExplosionRangeSquared());
									applyDamage(damageable, (int) (chaser.getBaseDamage() * damagefactor));
									dist.normalize();
									dist.scale(chaser.getBaseKnockback() * damagefactor);
									damageable.getBody().applyCentralImpulse(dist);
								}
							}
						}
						playRandomSplashSound(chaser.getTranslation());
						removeDamageable(chaser);
					}
				}
			}

			game.cam.translateTo(player.getTranslation());
			game.cam.translate(transformedCameraOffset);
			game.cam.rotateTo((float) Math.toDegrees(playerrotation.angle()), -45);
			lifebars.updateParticles(0, 1000);
		}
	}

	public void playRandomSplashSound(Vector3f position) {
		Sound sound = splashsounds[(int) (Math.random() * splashsounds.length)];
		sound.setSourcePosition(position);
		sound.play();
	}

	public void applyDamage(Damageable damaged, int damage) {
		damaged.damage(damage);
		if (damaged.getHealthbarID() == -1) {
			healthbar.scaleTo(damaged.getHealth() / 100f, 1);
			healthbar.translate(damage / 100f * -healthbarHalfSizeX, 0);
		} else {
			System.out.println("Updated " + damaged.getHealthbarID());
			lifebars.getParticle(damaged.getHealthbarID())
					.setLifetime((int) (damaged.getHealth() / (float) damaged.getMaxHealth() * 1000));
		}
		if (damaged.getHealth() <= 0) {
			removeDamageable(damaged);
		}
	}

	public void createSplash(float splashX, float splashZ, float splashsize, Vector4f color) {
		Quad a = new Quad(splashX, splashZ, splashsize, splashsize);
		a.setRenderHints(false, true, false);
		a.rotate((float) (Math.random() * 360));
		game.newSplashShader.addObject(a);
		game.newSplashShader.setArgument("u_texture", splashtextures[(int) (Math.random() * splashtextures.length)]);
		game.newSplashShader.setArgument("u_color", color);

		for (Vector2f grids : getAffectedSplashGrids(a)) {
			int x = (int) grids.x;
			int z = (int) grids.y;
			if (x >= 0 && z >= 0 && x < game.splashSubdivision && z < game.splashSubdivision) {
				game.newSplashFramebuffer[x][z].updateTexture();
				splashGround(x, z);
			}
		}
		game.newSplashShader.removeObject(a);

		a.delete();
	}

	private void removeDamageable(Damageable damageable) {
		targets.remove(damageable);
		if (damageable.getHealthbarID() != -1) {
			enemies.remove(damageable);
			if (chasers.contains(damageable)) {
				chasers.remove(damageable);
				movingEnemies.remove(damageable);
			}
		}
		if (damageable.getShooter() != null)
			shooters.remove(damageable.getShooter());
		space.removeRigidBody(damageable.getShapedObject(), damageable.getBody());
		damageable.getShader().removeObject(damageable.getShapedObject());
		damageable.getShapedObject().delete();
		if (damageable.getHealthbarID() == -1) {
			exitGame();
		} else {
			System.out.println("Removed " + damageable.getHealthbarID());
			lifebars.removeParticle(damageable.getHealthbarID());
			if (enemies.isEmpty()) {
				if (isEndless) {
					spawnWave();
				} else {
					exitGame();
				}
			}
		}
	}

	public void exitGame() {
		isActive = false;
		removeAllEnemies();
		game.zoomOut(halflevelsizeX, halflevelsizeZ);
	}

	private void removeAllEnemies() {
		for (Damageable damaged : targets) {
			if (damaged.getShooter() != null)
				shooters.remove(damaged.getShooter());
			space.removeRigidBody(damaged.getShapedObject(), damaged.getBody());
			damaged.getShader().removeObject(damaged.getShapedObject());
			damaged.getShapedObject().delete();
			System.out.println("Removed " + damaged.getHealthbarID());
			if (damaged.getHealthbarID() != -1)
				lifebars.removeParticle(damaged.getHealthbarID());
		}
		targets.clear();
		enemies.clear();
		chasers.clear();
		movingEnemies.clear();
	}

	List<Vector2f> affectedSplashGrids = new ArrayList<Vector2f>();

	private List<Vector2f> getAffectedSplashGrids(Quad quad) {
		affectedSplashGrids.clear();

		float diag = (float) Math.sqrt(quad.getSize().x * quad.getSize().x + quad.getSize().y * quad.getSize().y);

		int minX = game.calculateSplashGridX((int) Math.floor(quad.getTranslation().getX() - diag));
		int minZ = game.calculateSplashGridZ((int) Math.floor(quad.getTranslation().getY() - diag));
		int maxX = game.calculateSplashGridX((int) Math.ceil(quad.getTranslation().getX() + diag));
		int maxZ = game.calculateSplashGridZ((int) Math.ceil(quad.getTranslation().getY() + diag));

		affectedSplashGrids.add(new Vector2f(minX, minZ));
		if (maxX > minX) {
			affectedSplashGrids.add(new Vector2f(maxX, minZ));
		}
		if (maxZ > minZ) {
			affectedSplashGrids.add(new Vector2f(minX, maxZ));
		}
		if (maxX > minX && maxZ > minZ) {
			affectedSplashGrids.add(new Vector2f(maxX, maxZ));
		}

		return affectedSplashGrids;
	}

	private void splashGround(int x, int z) {
		if (game.splashGroundFramebufferFirst[x][z]) {
			game.splashCombinationShaders[x][z].apply(game.splashGroundFramebuffers[x][z],
					game.splashGroundFramebufferHelpers[x][z]);
			game.currentSplashTextures[x][z]
					.setTextureID(game.splashGroundFramebufferHelpers[x][z].getColorTextureID());
		} else {
			game.splashCombinationShaders[x][z].apply(game.splashGroundFramebufferHelpers[x][z],
					game.splashGroundFramebuffers[x][z]);
			game.currentSplashTextures[x][z].setTextureID(game.splashGroundFramebuffers[x][z].getColorTextureID());
		}
		game.splashGroundFramebufferFirst[x][z] = !game.splashGroundFramebufferFirst[x][z];
	}

	private void deleteShot(Shot shot) {
		shot.getShotShader().removeObject(shot);
		shot.deleteData();
		space.removeRigidBody(shot, shot.getBody());
		space.removeCollisionFilter(shot.getOwner(), shot.getBody());
	}

	public void addShot(Shot shot) {
		shots.add(shot);
	}

	public void updatePlayerRotationVariables() {
		playerfront.set(front);
		playerfront.transform(player.getRotation());
		playerrotation.set(playerfront.z, playerfront.x);
		transformedCameraOffset.set(cameraOffset);
		transformedCameraOffset.transform(player.getRotation());
	}

	@Override
	public void render() {
		game.render3dLayer();
		physicsdebug.render3d();
	}

	@Override
	public void renderInterface() {
		game.renderInterfaceLayer();
	}

	@Override
	public void delete() {
		game.getShader().remove(defaultshader);
		game.getShader().remove(blackcolorshader);
		game.getShader().remove(whitecolorshader);
		game.getShader().remove(playercolorshader);
		game.getShader().remove(playershader);
		game.getShader().remove(healthbarshader);
		game.getShaderInterface().remove(defaultshaderInterface);
		game.getShaderInterface().remove(redcolorshaderInterface);
		defaultshader.delete();
		blackcolorshader.delete();
		whitecolorshader.delete();
		playercolorshader.delete();
		playershader.delete();
		healthbarshader.delete();
		defaultshaderInterface.delete();
		redcolorshaderInterface.delete();

		game.getShader().remove(playerShotShader);
		playerShotShader.delete();
		for (Shader s : shotColorShaders) {
			game.getShader().remove(s);
			s.delete();
		}
		shotColorShaders.clear();

		for (Box b : levelgeometry) {
			game.splashObjectShaders[game.calculateSplashGridX((int) b.getTranslation().x)][game
					.calculateSplashGridZ((int) b.getTranslation().z)].removeObject(b);
			b.delete();
		}
		levelgeometry.clear();

		game.inputs.removeEvent(eventEsc);
		game.inputs.removeEvent(eventUp);
		game.inputs.removeEvent(eventDown);
		game.inputs.removeEvent(eventLeft);
		game.inputs.removeEvent(eventRight);
		game.inputs.removeEvent(eventShoot);
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void setActive(boolean active) {
		isActive = active;
	}

	@Override
	public Vector3f getPlayerColor() {
		return playercolor;
	}

	@Override
	public boolean isBackgroundWhite() {
		return whitebackground;
	}
}