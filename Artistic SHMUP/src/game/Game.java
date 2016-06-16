package game;

import java.util.ArrayList;
import java.util.List;

import broadphase.SAP;
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
import manifold.SimpleManifoldManager;
import narrowphase.EPA;
import narrowphase.GJK;
import objects.Camera2;
import objects.CollisionShape3;
import objects.Player;
import objects.RigidBody3;
import objects.ShapedObject3;
import objects.Shot;
import objects.StandardCannon;
import physics.PhysicsDebug;
import physics.PhysicsShapeCreator;
import physics.PhysicsSpace;
import positionalcorrection.ProjectionCorrection;
import quaternion.Complexf;
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
import utils.ProjectionHelper;
import vector.Vector2f;
import vector.Vector3f;
import vector.Vector4f;

public class Game extends StandardGame {
	Debugger debugger;
	PhysicsDebug physicsdebug;
	InputEvent eventUp, eventDown, eventLeft, eventRight, eventShoot;
	PhysicsSpace space;
	Player player;
	Shader defaultshader;
	RigidBody3 testrb;

	final float mouseSensitivity = -0.1f; // negative sensitivity = not inverted

	final Vector3f front = new Vector3f(0, 0, 1);
	Vector3f playerfront = new Vector3f();
	Vector2f move = new Vector2f();
	Complexf playerrotation = new Complexf();
	final Vector3f cameraOffset = new Vector3f(0, 10, 10);
	Vector3f transformedCameraOffset = new Vector3f();

	final int levelsizeX = 150;
	final int levelsizeZ = 150;
	final int splashResolution = 2048;

	boolean last0 = true;
	FramebufferObject newSplashFramebuffer, splashFramebuffer0, splashFramebuffer1;
	Box ground;

	Shader newSplashShader, splashQuadShader, splashGroundShader, levelObjectShader;
	Texture[] splashtextures;
	PostProcessingShader combinationShader;

	Sphere shotgeometry;
	CollisionShape3 shotcollisionshape;
	List<Shot> shots;
	List<Shader> shotColorShaders;

	@Override
	public void init() {
		initDisplay(new GLDisplay(), new DisplayMode(800, 600, "Artful SHMUP", false), new PixelFormat(),
				new VideoSettings(), new NullSoundEnvironment());
		display.bindMouse();
		cam.setFlyCam(false);
		setRendered(true, false, true);
		layer2d.setProjectionMatrix(ProjectionHelper.ortho(0, levelsizeX, levelsizeZ, 0, -1, 1));

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
		Shader bluecolorshader = new Shader(colorshaderprogram);
		bluecolorshader.addArgument("u_color", new Vector4f(0, 0, 1, 1));
		addShader(bluecolorshader);

		Font font = FontLoader.loadFont("res/fonts/DejaVuSans.ttf");
		debugger = new Debugger(inputs, defaultshader, defaultshaderInterface, font, cam);
		physicsdebug = new PhysicsDebug(inputs, font, space, defaultshader);

		newSplashFramebuffer = new FramebufferObject(layer2d, splashResolution, splashResolution, 0,
				new Camera2(new Vector2f(0, 0)));
		splashFramebuffer0 = new FramebufferObject(layer2d, splashResolution, splashResolution, 0,
				new Camera2(new Vector2f(0, 0)));
		splashFramebuffer1 = new FramebufferObject(layer2d, splashResolution, splashResolution, 0,
				new Camera2(new Vector2f(0, 0)));

		int textureshaderID = ShaderLoader.loadShaderFromFile("res/shaders/textureshader.vert",
				"res/shaders/textureshader.frag");

		splashGroundShader = new Shader(textureshaderID);
		splashGroundShader.addArgument("u_texture", splashFramebuffer0.getColorTexture());
		addShader(splashGroundShader);

		levelObjectShader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/levelobjectshader.vert",
				"res/shaders/levelobjectshader.frag"));
		levelObjectShader.addArgument("u_texture", splashFramebuffer0.getColorTexture());
		levelObjectShader.addArgument("u_levelsizeX", levelsizeX);
		levelObjectShader.addArgument("u_levelsizeZ", levelsizeZ);
		addShader(levelObjectShader);

		splashtextures = new Texture[2];
		splashtextures[0] = new Texture(TextureLoader.loadTexture("res/textures/splash1.png"));
		splashtextures[1] = new Texture(TextureLoader.loadTexture("res/textures/testGreenSplash.png"));

		newSplashShader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/splashshader.vert", "res/shaders/splashshader.frag"));
		newSplashShader.addArgument("u_texture", splashtextures[0]);
		newSplashShader.addArgument("u_color", new Vector4f(0, 1, 0, 1));
		addShader2d(newSplashShader);

		Shader combShader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/combinationshader.vert",
				"res/shaders/combinationshader.frag"));
		combShader.addArgument("u_texture", splashFramebuffer0.getColorTexture());
		combShader.addArgument("u_depthTexture", splashFramebuffer0.getDepthTexture());
		combShader.addArgument("u_splashTexture", newSplashFramebuffer.getColorTexture());
		combinationShader = new PostProcessingShader(combShader, 1);
		combinationShader.getShader().addObject(screen);

		float halflevelsizeX = levelsizeX / 2f;
		float halflevelsizeZ = levelsizeZ / 2f;
		ground = new Box(halflevelsizeX, -2f, halflevelsizeZ, halflevelsizeX, 1, halflevelsizeZ);
		ground.setRenderHints(false, true, false);
		RigidBody3 rbground = new RigidBody3(PhysicsShapeCreator.create(ground));
		rbground.setMass(0);
		space.addRigidBody(ground, rbground);
		splashGroundShader.addObject(ground);

		generateLevel(150);

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

		player = new Player(halflevelsizeX, 0, halflevelsizeZ);
		space.addRigidBody(player, player.getBody());
		bluecolorshader.addObject(player);

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
		splashFramebuffer0.updateTexture();
		newSplashShader.removeObject(a);
		a.delete();
	}

	private void addStaticBox(float x, float y, float z, float width, float height, float depth) {
		Box box = new Box(x, y, z, width, height, depth);
		box.setRenderHints(false, false, false);
		RigidBody3 rb = new RigidBody3(PhysicsShapeCreator.create(box));
		space.addRigidBody(box, rb);
		levelObjectShader.addObject(box);
	}

	public void generateLevel(int numboxes) {
		for (int i = 0; i < numboxes; i++) {
			int sizeX = 1;
			int sizeZ = 1;
			addStaticBox((int) (sizeX + (Math.random() * (levelsizeX - 2 * sizeX))), 0,
					(int) (sizeZ + (Math.random() * (levelsizeZ - 2 * sizeZ))), sizeX, 1, sizeZ);
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
		Input inputKeyUp = new Input(Input.KEYBOARD_EVENT, "W", KeyInput.KEY_DOWN);
		Input inputKeyDown = new Input(Input.KEYBOARD_EVENT, "S", KeyInput.KEY_DOWN);
		Input inputKeyLeft = new Input(Input.KEYBOARD_EVENT, "A", KeyInput.KEY_DOWN);
		Input inputKeyRight = new Input(Input.KEYBOARD_EVENT, "D", KeyInput.KEY_DOWN);
		Input inputMouseLeft = new Input(Input.MOUSE_EVENT, "Left", MouseInput.MOUSE_BUTTON_DOWN);

		eventUp = new InputEvent("Up", inputKeyUp);
		eventDown = new InputEvent("Down", inputKeyDown);
		eventLeft = new InputEvent("Left", inputKeyLeft);
		eventRight = new InputEvent("Right", inputKeyRight);
		eventShoot = new InputEvent("Shoot", inputMouseLeft);

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

		player.tickShoot(delta);
		if (eventShoot.isActive()) {
			player.shoot();
		}

		move.transform(playerrotation);
		if (move.lengthSquared() > 0) {
			move.normalize();
			move.scale(player.getAcceleration());
			player.getBody().applyCentralForce(new Vector3f(move.x, 0, move.y)); // TODO:
																					// add
																					// missing
																					// setter
																					// to
																					// rigidbody-methods
		}
		if (player.getBody().getLinearVelocity().lengthSquared() > player.getMaxSpeedSquared()) {
			player.getBody().getLinearVelocity().setLength(player.getMaxSpeed());
		}
		// player.getBody().setLinearVelocity(move.x,
		// player.getBody().getLinearVelocity().y, move.y);

		space.update(delta);
		physicsdebug.update();

		for (int i = 0; i < shots.size(); i++) {
			Shot shot = shots.get(i);
			if (space.hasCollision(shot.getBody())) {
				System.out.println("Hit!");

				Quad a = new Quad(shot.getTranslation().x, shot.getTranslation().z, 2f, 2f);
				a.setRenderHints(false, true, false);
				a.rotate((float) (Math.random() * 360));
				newSplashShader.addObject(a);
				newSplashShader.setArgument("u_texture", splashtextures[(int) (Math.random() * splashtextures.length)]);
				newSplashShader.setArgument("u_color", shot.getShotShader().getArgument("u_color"));

				shots.remove(i);
				deleteShot(shot);

				newSplashFramebuffer.updateTexture();

				newSplashShader.removeObject(a);
				a.delete();

				combinationShader.apply(splashFramebuffer0, splashFramebuffer1);
				splashFramebuffer1.copyTo(splashFramebuffer0);

				i--;
			} else {
				if (shot.getTranslation().y < -20) {
					shots.remove(i);
					deleteShot(shot);
				}
			}
		}
		System.out.println(space.getObjects().size());
		// System.out.println(space.hasOverlap(testrb) + "; " +
		// space.hasCollision(testrb) + "; " + testrb.getTranslation() + "; " +
		// testrb.getRotation());

		cam.translateTo(player.getTranslation());
		cam.translate(transformedCameraOffset);
		cam.rotateTo((float) Math.toDegrees(playerrotation.angle()), -45);
		cam.update(delta);
	}

	private void deleteShot(Shot shot) {
		shot.getShotShader().removeObject(shot);
		shot.deleteData();
		space.removeRigidBody(shot, shot.getBody());
		space.removeCollisionFilter(player.getBody(), shot.getBody());
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
