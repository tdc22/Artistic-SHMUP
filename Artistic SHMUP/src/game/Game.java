package game;

import java.util.ArrayList;
import java.util.List;

import broadphase.SAP;
import collisionshape.BoxShape;
import curves.BezierCurve3;
import curves.SquadCurve3;
import display.DisplayMode;
import display.GLDisplay;
import display.PixelFormat;
import display.VideoSettings;
import gui.Font;
import input.Input;
import input.InputEvent;
import input.KeyInput;
import input.MouseInput;
import integration.VerletIntegration;
import loader.FontLoader;
import loader.ShaderLoader;
import loader.TextureLoader;
import manifold.CollisionManifold;
import manifold.SimpleManifoldManager;
import narrowphase.EPA;
import narrowphase.GJK;
import objects.Camera2;
import objects.CollisionShape3;
import objects.Damageable;
import objects.Enemy;
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
import quaternion.Quaternionf;
import resolution.SimpleLinearImpulseResolution;
import shader.PostProcessingShader;
import shader.Shader;
import shape.Box;
import shape.MarchingSquaresGenerator;
import shape.Sphere;
import shape2d.Quad;
import sound.NullSoundEnvironment;
import texture.FramebufferObject;
import texture.Texture;
import utils.Debugger;
import utils.DefaultValues;
import utils.ProjectionHelper;
import vector.Vector2f;
import vector.Vector3f;
import vector.Vector4f;

public class Game extends StandardGame {
	Debugger debugger;
	PhysicsDebug physicsdebug;
	InputEvent eventEsc, eventUp, eventDown, eventLeft, eventRight, eventShoot;
	PhysicsSpace space;
	Player player;
	Shader defaultshader;
	RigidBody3 testrb;
	boolean isPaused = false;

	final float mouseSensitivity = -0.1f; // negative sensitivity = not inverted

	final Vector3f front = new Vector3f(0, 0, 1);
	final Vector3f zero = new Vector3f(0, 0, 0);
	Vector3f playerfront = new Vector3f();
	Vector2f move = new Vector2f();
	Complexf playerrotation = new Complexf();
	final Vector3f cameraOffset = new Vector3f(0, 10, 10);
	Vector3f transformedCameraOffset = new Vector3f();

	final int levelsizeX = 160; // mod 2 = 0 !!!
	final int levelsizeZ = 160;
	final int splashResolution = 512; // power of 2 !!!
	final int splashSubdivision = 4;

	BoxShape groundshape;

	final int healthbarHalfSizeX = 100;
	final int healthbarHalfSizeY = 10;
	final int healthbarMargin = 10;
	final int healthbarBorder = 1;

	boolean playerAlive = true;
	float deathtimer = 0;

	Shader newSplashShader, blackcolorshader;
	Texture[] splashtextures;

	Shader[][] splashGroundShaders, splashObjectShaders;
	FramebufferObject[][] splashGroundFramebuffers;
	FramebufferObject[][] splashGroundFramebufferHelpers;
	FramebufferObject[][] newSplashFramebuffer;
	PostProcessingShader[][] splashCombinationShaders;
	boolean[][] splashGroundFramebufferFirst;
	Texture[][] currentSplashTextures;
	Box[][] groundboxes;

	Quad healthbar;
	BezierCurve3 deathcamCurve;
	SquadCurve3 deathcamRotationCurve;
	SimpleParticleSystem lifebars;
	final Vector2f enemyLifebarSize = new Vector2f(1, 0.3);

	Sphere shotgeometry;
	CollisionShape3 shotcollisionshape;
	List<Shootable> shooters;
	List<Damageable> targets;
	List<Enemy> enemies;
	List<Shot> shots;
	List<Shader> shotColorShaders;

	@Override
	public void init() {
		initDisplay(new GLDisplay(),
				new DisplayMode(DefaultValues.DEFAULT_DISPLAY_POSITION_X, 20, 1280, 720, "Artful SHMUP", false,
						DefaultValues.DEFAULT_DISPLAY_RESIZEABLE, DefaultValues.DEFAULT_DISPLAY_FULLSCREEN),
				new PixelFormat(), new VideoSettings(1280, 720), new NullSoundEnvironment());
		display.bindMouse();
		cam.setFlyCam(false);
		setRendered(true, false, true);
		layer2d.setProjectionMatrix(ProjectionHelper.ortho(0, levelsizeX / (float) splashSubdivision,
				levelsizeZ / (float) splashSubdivision, 0, -1, 1));

		space = new PhysicsSpace(new VerletIntegration(), new SAP(), new GJK(new EPA()),
				new SimpleLinearImpulseResolution(), new ProjectionCorrection(0.01f),
				new SimpleManifoldManager<Vector3f>());
		space.setGlobalGravitation(new Vector3f(0, -10, 0));

		int defaultshaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		defaultshader = new Shader(defaultshaderID);
		addShader(defaultshader);
		Shader defaultshaderInterface = new Shader(defaultshaderID);
		addShaderInterface(defaultshaderInterface);

		int colorshaderprogram = ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert",
				"res/shaders/colorshader.frag");
		Shader playercolorshader = new Shader(colorshaderprogram);
		playercolorshader.addArgument("u_color", new Vector4f(0.75, 0.75, 0.75, 1));
		addShader(playercolorshader);
		blackcolorshader = new Shader(colorshaderprogram);
		blackcolorshader.addArgument("u_color", new Vector4f(0, 0, 0, 1));
		addShader(blackcolorshader);
		Shader redcolorshaderInterface = new Shader(colorshaderprogram);
		redcolorshaderInterface.addArgument("u_color", new Vector4f(1, 0, 0, 1));
		addShaderInterface(redcolorshaderInterface);

		Font font = FontLoader.loadFont("res/fonts/DejaVuSans.ttf");
		debugger = new Debugger(inputs, defaultshader, defaultshaderInterface, font, cam);
		physicsdebug = new PhysicsDebug(inputs, font, space, defaultshader);

		int textureshaderID = ShaderLoader.loadShaderFromFile("res/shaders/textureshader.vert",
				"res/shaders/textureshader.frag");

		Shader frameShader = new Shader(textureshaderID);
		frameShader.addArgument("u_texture", new Texture(TextureLoader.loadTexture("res/textures/wood1.png")));
		addShader(frameShader);

		splashtextures = new Texture[5];
		splashtextures[0] = new Texture(TextureLoader.loadTexture("res/textures/splash1.png"));
		splashtextures[1] = new Texture(TextureLoader.loadTexture("res/textures/splash2.png"));
		splashtextures[2] = new Texture(TextureLoader.loadTexture("res/textures/splash3.png"));
		splashtextures[3] = new Texture(TextureLoader.loadTexture("res/textures/splash4.png"));
		splashtextures[4] = new Texture(TextureLoader.loadTexture("res/textures/splash5.png"));

		newSplashShader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/splashshader.vert", "res/shaders/splashshader.frag"));
		newSplashShader.addArgument("u_texture", splashtextures[0]);
		newSplashShader.addArgument("u_color", new Vector4f(0, 1, 0, 1));
		addShader2d(newSplashShader);

		Shader healthbarshader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/healthbarshader.vert",
				"res/shaders/healthbarshader.frag"));
		addShader(healthbarshader);

		Quad healthbarBackground = new Quad(healthbarMargin + healthbarBorder + healthbarHalfSizeX,
				healthbarMargin + healthbarBorder + healthbarHalfSizeY, healthbarBorder + healthbarHalfSizeX,
				healthbarBorder + healthbarHalfSizeY);
		defaultshaderInterface.addObject(healthbarBackground);
		healthbar = new Quad(healthbarMargin + healthbarBorder + healthbarHalfSizeX,
				healthbarMargin + healthbarBorder + healthbarHalfSizeY, healthbarHalfSizeX, healthbarHalfSizeY);
		redcolorshaderInterface.addObject(healthbar);

		lifebars = new SimpleParticleSystem(new Vector3f(), cam, false);
		lifebars.getParticleObject().setRenderHints(true, true, false);
		healthbarshader.addObject(lifebars);

		float halflevelsizeX = levelsizeX / 2f;
		float halflevelsizeZ = levelsizeZ / 2f;
		groundshape = new BoxShape(halflevelsizeX, -2f, halflevelsizeZ, halflevelsizeX, 1, halflevelsizeZ);
		RigidBody3 rbground = new RigidBody3(groundshape);
		rbground.translate(halflevelsizeX, -2f, halflevelsizeZ);
		rbground.setMass(0);
		space.addRigidBody(rbground);

		splashGroundFramebuffers = new FramebufferObject[splashSubdivision][splashSubdivision];
		splashGroundFramebufferHelpers = new FramebufferObject[splashSubdivision][splashSubdivision];
		newSplashFramebuffer = new FramebufferObject[splashSubdivision][splashSubdivision];
		splashGroundFramebufferFirst = new boolean[splashSubdivision][splashSubdivision];
		splashGroundShaders = new Shader[splashSubdivision][splashSubdivision];
		splashObjectShaders = new Shader[splashSubdivision][splashSubdivision];
		splashCombinationShaders = new PostProcessingShader[splashSubdivision][splashSubdivision];
		currentSplashTextures = new Texture[splashSubdivision][splashSubdivision];
		float halfboxsizeX = levelsizeX / (float) (splashSubdivision * 2);
		float halfboxsizeZ = levelsizeZ / (float) (splashSubdivision * 2);
		System.out.println("hbs " + 2 * halfboxsizeX + "; " + 2 * halfboxsizeZ);
		groundboxes = new Box[splashSubdivision][splashSubdivision];

		for (int x = 0; x < splashSubdivision; x++) {
			for (int z = 0; z < splashSubdivision; z++) {
				Vector2f campos = new Vector2f(2 * halfboxsizeX * x, 2 * halfboxsizeZ * z);
				FramebufferObject groundfbo = new FramebufferObject(layer2d, splashResolution, splashResolution, 0,
						new Camera2(campos));
				splashGroundFramebuffers[x][z] = groundfbo;
				splashGroundFramebufferHelpers[x][z] = new FramebufferObject(layer2d, splashResolution,
						splashResolution, 0, new Camera2(campos));
				newSplashFramebuffer[x][z] = new FramebufferObject(layer2d, splashResolution, splashResolution, 0,
						new Camera2(campos));
				splashGroundFramebufferFirst[x][z] = true;

				currentSplashTextures[x][z] = new Texture(groundfbo.getColorTextureID());

				Shader levelObjectShader = new Shader(ShaderLoader.loadShaderFromFile(
						"res/shaders/levelobjectshader.vert", "res/shaders/levelobjectshader.frag"));
				levelObjectShader.addArgument("u_texture", currentSplashTextures[x][z]);
				levelObjectShader.addArgument("u_groundblocksizeX", (int) (2 * halfboxsizeX));
				levelObjectShader.addArgument("u_groundblocksizeZ", (int) (2 * halfboxsizeZ));
				addShader(levelObjectShader);
				splashObjectShaders[x][z] = levelObjectShader;

				Shader combShader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/combinationshader.vert",
						"res/shaders/combinationshader.frag"));
				combShader.addArgument("u_texture", currentSplashTextures[x][z]);
				combShader.addArgument("u_depthTexture", splashGroundFramebuffers[x][z].getDepthTexture());
				combShader.addArgument("u_splashTexture", newSplashFramebuffer[x][z].getColorTexture());
				PostProcessingShader combinationShader = new PostProcessingShader(combShader, 1);
				combinationShader.getShader().addObject(screen);
				splashCombinationShaders[x][z] = combinationShader;

				Shader groundshader = new Shader(textureshaderID);
				groundshader.addArgument("u_texture", currentSplashTextures[x][z]);
				addShader(groundshader);
				splashGroundShaders[x][z] = groundshader;

				Box groundbox = new Box(halfboxsizeX + (2 * halfboxsizeX * x), -2f,
						halfboxsizeZ + (2 * halfboxsizeZ * z), halfboxsizeX, 1, halfboxsizeZ);
				groundbox.setRenderHints(false, true, false);
				groundshader.addObject(groundbox);
			}
		}

		addStaticBox(halflevelsizeX, 0, 0.5f, halflevelsizeX, 1, 0.5f);
		addStaticBox(halflevelsizeX, 0, levelsizeZ - 0.5f, halflevelsizeX, 1, 0.5f);
		addStaticBox(0.5f, 0, halflevelsizeZ, 0.5f, 1, halflevelsizeZ);
		addStaticBox(levelsizeX - 0.5f, 0, halflevelsizeZ, 0.5f, 1, halflevelsizeZ);

		Box frame1 = new Box(halflevelsizeX, 0, -3, halflevelsizeX + 6, 1, 3);
		Box frame2 = new Box(halflevelsizeX, 0, levelsizeZ + 3, halflevelsizeX + 6, 1, 3);
		Box frame3 = new Box(-3, 0, halflevelsizeZ, halflevelsizeZ, 1, 3);
		Box frame4 = new Box(levelsizeX + 3, 0, halflevelsizeZ, halflevelsizeZ, 1, 3);
		frame3.rotate(0, 90, 0);
		frame4.rotate(0, 90, 0);
		frame1.setRenderHints(false, true, false);
		frame2.setRenderHints(false, true, false);
		frame3.setRenderHints(false, true, false);
		frame4.setRenderHints(false, true, false);
		frameShader.addObject(frame1);
		frameShader.addObject(frame2);
		frameShader.addObject(frame3);
		frameShader.addObject(frame4);

		shooters = new ArrayList<Shootable>();
		targets = new ArrayList<Damageable>();
		enemies = new ArrayList<Enemy>();
		shotgeometry = new Sphere(0, 0, 0, 0.2f, 36, 36);
		shotcollisionshape = PhysicsShapeCreator.create(shotgeometry);

		shots = new ArrayList<Shot>();
		shotColorShaders = new ArrayList<Shader>();
		shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(1, 0, 0, 1)));
		shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(0, 1, 0, 1)));
		shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(0, 0, 1, 1)));
		shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(1, 1, 0, 1)));
		shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(1, 0, 1, 1)));
		shotColorShaders.add(new Shader(colorshaderprogram, "u_color", new Vector4f(0, 1, 1, 1)));
		for (Shader s : shotColorShaders) {
			addShader(s);
		}

		generateLevel(100, 10);

		player = new Player(halflevelsizeX, 0, halflevelsizeZ, playercolorshader);
		space.addRigidBody(player, player.getBody());
		playercolorshader.addObject(player);
		shooters.add(player);
		targets.add(player);

		player.addCannon(new StandardCannon(this, space, player, new Vector3f(0, 0, -1), new Vector3f(0, 0, 1),
				shotColorShaders.get(0), shotgeometry, shotcollisionshape));
		player.addCannon(new StandardCannon(this, space, player, new Vector3f(0.8f, 0, -1),
				new Vector3f(-0.4472136, 0, 0.8944272), shotColorShaders.get(1), shotgeometry, shotcollisionshape));
		player.addCannon(new StandardCannon(this, space, player, new Vector3f(-0.8f, 0, -1),
				new Vector3f(0.4472136, 0, 0.8944272), shotColorShaders.get(2), shotgeometry, shotcollisionshape));

		setupInputs();
		updatePlayerRotationVariables();

		Quad a = new Quad(halflevelsizeX, halflevelsizeZ, halflevelsizeX, halflevelsizeZ);
		a.setRenderHints(false, true, false);
		newSplashShader.addObject(a);
		newSplashShader.setArgument("u_texture", new Texture(TextureLoader.loadTexture("res/textures/whitePixel.png")));
		newSplashShader.setArgument("u_color", new Vector4f(1, 1, 1, 1));
		for (FramebufferObject[] fbos : splashGroundFramebuffers) {
			for (FramebufferObject fbo : fbos) {
				fbo.updateTexture();
			}
		}
		newSplashShader.removeObject(a);
		a.delete();
	}

	private void addStaticBox(float x, float y, float z, float width, float height, float depth) {
		Box box = new Box(x, y, z, width, height, depth);
		box.setRenderHints(false, false, false);
		RigidBody3 rb = new RigidBody3(PhysicsShapeCreator.create(box));
		space.addRigidBody(box, rb);
		splashObjectShaders[calculateSplashGridX((int) x)][calculateSplashGridZ((int) z)].addObject(box);
	}

	private void addTower(float x, float y, float z) {
		Tower tower = new Tower(x, y, z, blackcolorshader, lifebars.getParticleObject().getVertices().size() / 4);
		space.addRigidBody(tower, tower.getBody());
		tower.addCannon(new StandardCannon(this, space, tower, new Vector3f(0, 0, -1), new Vector3f(0, 0, 1),
				shotColorShaders.get((int) (Math.random() * shotColorShaders.size())), shotgeometry,
				shotcollisionshape));
		blackcolorshader.addObject(tower);
		shooters.add(tower);
		targets.add(tower);
		enemies.add(tower);
		lifebars.addParticle(
				new Vector3f(tower.getTranslation().x, tower.getTranslation().y + 2, tower.getTranslation().z), zero,
				enemyLifebarSize, 1000);
	}

	public void generateLevel(int numboxes, int numtowers) {
		int gridsizeX = levelsizeX / 2 - 1;
		int gridsizeZ = levelsizeZ / 2 - 1;
		boolean[][] levelgrid = new boolean[gridsizeX][gridsizeZ];

		for (int x = 0; x < gridsizeX; x++) {
			for (int z = 0; z < gridsizeZ; z++) {
				levelgrid[x][z] = false;
			}
		}
		levelgrid[gridsizeX / 2][gridsizeZ / 2] = true; // player

		int sizeX = 1;
		int sizeZ = 1;
		for (int i = 0; i < numboxes; i++) {
			int posX = (int) (Math.random() * gridsizeX);
			int posZ = (int) (Math.random() * gridsizeZ);
			if (!levelgrid[posX][posZ]) {
				levelgrid[posX][posZ] = true;
				addStaticBox(posX * 2 + 2, 0, posZ * 2 + 2, sizeX, 1, sizeZ);
			} else {
				i--;
			}
		}
		for (int i = 0; i < numtowers; i++) {
			int posX = (int) (Math.random() * gridsizeX);
			int posZ = (int) (Math.random() * gridsizeZ);
			if (!levelgrid[posX][posZ]) {
				levelgrid[posX][posZ] = true;
				addTower(posX * 2 + 2, 0, posZ * 2 + 2);
			} else {
				i--;
			}
		}

		boolean[][] testarray = { { false, false, false, false, false }, { false, false, true, false, false },
				{ false, true, true, true, false }, { false, false, true, false, false },
				{ false, false, false, false, false }, };
		ShapedObject3 testobject = MarchingSquaresGenerator.generate(testarray, 1f);
		testrb = new RigidBody3(PhysicsShapeCreator.createHull(testobject));
		testrb.setMass(0);
		space.addRigidBody(testobject, testrb);
		testobject.translateTo(-0, 0, -6);
		defaultshader.addObject(testobject);
	}

	public void setupInputs() {
		inputs.getInputEvents().remove("game_close");

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

		inputs.addEvent(eventEsc);
		inputs.addEvent(eventUp);
		inputs.addEvent(eventDown);
		inputs.addEvent(eventLeft);
		inputs.addEvent(eventRight);
		inputs.addEvent(eventShoot);
	}

	@Override
	public void update(int delta) {
		debugger.update(fps, 0, 0);

		move.set(0, 0);
		if (eventEsc.isActive()) {
			isPaused = !isPaused;
			if (isPaused) {
				display.unbindMouse();
			} else {
				display.bindMouse();
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

			float mouseX = inputs.getMouseX();
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
						damaged.damage(damage);
						if (damaged.getHealthbarID() == -1) {
							healthbar.scaleTo(damaged.getHealth() / 100f, 1);
							healthbar.translate(damage / 100f * -healthbarHalfSizeX, 0);
							if (damaged.getHealth() <= 0) {
								int halfLevelSizeX = levelsizeX / 2;
								int halfLevelSizeZ = levelsizeZ / 2;
								Vector3f b = new Vector3f(cam.getTranslation());
								b.scale(0.4f);
								Vector3f d = new Vector3f(halfLevelSizeX, 100, halfLevelSizeZ);
								Vector3f c = new Vector3f(halfLevelSizeX, 40, halfLevelSizeZ);
								deathcamCurve = new BezierCurve3(cam.getTranslation(), cam.getTranslation(), c, d);
								Quaternionf lookdown = new Quaternionf();
								lookdown.rotate(-90, new Vector3f(1, 0, 0));
								deathcamRotationCurve = new SquadCurve3(cam.getRotation(), cam.getRotation(), lookdown,
										lookdown);
								playerAlive = false;
							}
						} else {
							lifebars.removeParticle(damaged.getHealthbarID());
							damaged.setHealthbarID(lifebars.addParticle(
									new Vector3f(damaged.getTranslation().x, damaged.getTranslation().y + 2,
											damaged.getTranslation().z),
									zero, enemyLifebarSize,
									(int) (damaged.getHealth() / (float) damaged.getMaxHealth() * 1000)));
						}
						if (damaged.getHealth() <= 0) {
							targets.remove(damaged);
							shooters.remove(damaged.getShooter());
							space.removeRigidBody(damaged.getShapedObject(), damaged.getBody());
							damaged.getShader().removeObject(damaged.getShapedObject());
							damaged.getShapedObject().delete();
							if (damaged.getHealthbarID() == -1) {

							} else {
								lifebars.removeParticle(damaged.getHealthbarID());
							}
						}
					}

					float splashsize = 1.5f + (float) (Math.random() * 1.5f);
					Quad a = new Quad(shot.getTranslation().x, shot.getTranslation().z, splashsize, splashsize);
					a.setRenderHints(false, true, false);
					a.rotate((float) (Math.random() * 360));
					newSplashShader.addObject(a);
					newSplashShader.setArgument("u_texture",
							splashtextures[(int) (Math.random() * splashtextures.length)]);
					newSplashShader.setArgument("u_color", shot.getShotShader().getArgument("u_color"));

					shots.remove(i);
					deleteShot(shot);

					for (Vector2f grids : getAffectedSplashGrids(a)) {
						int x = (int) grids.x;
						int z = (int) grids.y;
						if (x >= 0 && z >= 0) {
							newSplashFramebuffer[x][z].updateTexture();
							splashGround(x, z);
						}
					}
					newSplashShader.removeObject(a);

					a.delete();
					i--;
				} else {
					if (shot.getTranslation().y < -20) {
						shots.remove(i);
						deleteShot(shot);
					}
				}
			}

			if (playerAlive) {
				cam.translateTo(player.getTranslation());
				cam.translate(transformedCameraOffset);
				cam.rotateTo((float) Math.toDegrees(playerrotation.angle()), -45);
			} else {
				if (deathtimer < 1)
					deathtimer += delta * 0.0002;
				else
					deathtimer = 1;
				cam.translateTo(deathcamCurve.getPoint(deathtimer));
				cam.rotateTo(deathcamRotationCurve.getRotation(deathtimer));
			}
			lifebars.updateParticles(0, 1000);
		}
	}

	private int calculateSplashGridX(int x) {
		return x / (int) (levelsizeX / (float) splashSubdivision);
	}

	private int calculateSplashGridZ(int z) {
		return z / (int) (levelsizeZ / (float) splashSubdivision);
	}

	List<Vector2f> affectedSplashGrids = new ArrayList<Vector2f>();

	private List<Vector2f> getAffectedSplashGrids(Quad quad) {
		affectedSplashGrids.clear();

		float diag = (float) Math.sqrt(quad.getSize().x * quad.getSize().x + quad.getSize().y * quad.getSize().y);

		int minX = calculateSplashGridX((int) Math.floor(quad.getTranslation().getX() - diag));
		int minZ = calculateSplashGridZ((int) Math.floor(quad.getTranslation().getY() - diag));
		int maxX = calculateSplashGridX((int) Math.ceil(quad.getTranslation().getX() + diag));
		int maxZ = calculateSplashGridZ((int) Math.ceil(quad.getTranslation().getY() + diag));

		if (minX >= splashSubdivision)
			minX--;
		if (minZ >= splashSubdivision)
			minZ--;
		if (maxX >= splashSubdivision)
			maxX--;
		if (maxZ >= splashSubdivision)
			maxZ--;

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
		if (splashGroundFramebufferFirst[x][z]) {
			splashCombinationShaders[x][z].apply(splashGroundFramebuffers[x][z], splashGroundFramebufferHelpers[x][z]);
			currentSplashTextures[x][z].setTextureID(splashGroundFramebufferHelpers[x][z].getColorTextureID());
		} else {
			splashCombinationShaders[x][z].apply(splashGroundFramebufferHelpers[x][z], splashGroundFramebuffers[x][z]);
			currentSplashTextures[x][z].setTextureID(splashGroundFramebuffers[x][z].getColorTextureID());
		}
		splashGroundFramebufferFirst[x][z] = !splashGroundFramebufferFirst[x][z];
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
		debugger.begin();
		render3dLayer();
		physicsdebug.render3d();
	}

	@Override
	public void render2d() {

	}

	@Override
	public void renderInterface() {
		renderInterfaceLayer();
		debugger.end();
	}
}