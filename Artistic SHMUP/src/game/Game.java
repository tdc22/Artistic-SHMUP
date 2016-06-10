package game;

import java.awt.Color;

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
import manifold.SimpleManifoldManager;
import math.VecMath;
import narrowphase.EPA;
import narrowphase.GJK;
import objects.Player;
import objects.RigidBody3;
import objects.ShapedObject3;
import physics.PhysicsDebug;
import physics.PhysicsShapeCreator;
import physics.PhysicsSpace;
import positionalcorrection.ProjectionCorrection;
import quaternion.Complexf;
import quaternion.Quaternionf;
import resolution.SimpleLinearImpulseResolution;
import shader.Shader;
import shape.Box;
import shape.MarchingSquaresGenerator;
import shape.Sphere;
import sound.NullSoundEnvironment;
import utils.Debugger;
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

	@Override
	public void init() {
		initDisplay(new GLDisplay(), new DisplayMode(800, 600, "Artful SHMUP", true), new PixelFormat(),
				new VideoSettings(), new NullSoundEnvironment());
		display.bindMouse();
		cam.setFlyCam(false);

		space = new PhysicsSpace(new VerletIntegration(), new SAP(), new GJK(new EPA()),
				new SimpleLinearImpulseResolution(), new ProjectionCorrection(0.01f),
				new SimpleManifoldManager<Vector3f>());
		space.setGlobalGravitation(new Vector3f(0, 0, 0));

		defaultshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
		addShader(defaultshader);
		Shader defaultshaderInterface = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
		addShaderInterface(defaultshaderInterface);

		Shader bluecolorshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert", "res/shaders/colorshader.frag"));
		bluecolorshader.addArgument("u_color", new Vector4f(0, 0, 1, 1));
		addShader(bluecolorshader);

		Font font = FontLoader.loadFont("res/fonts/DejaVuSans.ttf");
		debugger = new Debugger(inputs, defaultshader, defaultshaderInterface,
				font, cam);
		physicsdebug = new PhysicsDebug(inputs, font, space);

		generateLevel(100);

		player = new Player(0, 0, 0);
		space.addRigidBody(player, player.getBody());
		bluecolorshader.addObject(player);

		setupInputs();
		updatePlayerRotationVariables();
	}

	private void addStaticBox(float x, float y, float z, float width, float height, float depth) {
		Box box = new Box(x, y, z, width, height, depth);
		RigidBody3 rb = new RigidBody3(PhysicsShapeCreator.create(box));
		space.addRigidBody(box, rb);
		defaultshader.addObject(box);
	}

	public void generateLevel(int numboxes) {
		for (int i = 0; i < numboxes; i++) {
			addStaticBox((int) (Math.random() * 100), 0, (int) (Math.random() * 100), 1, 1, 1);
		}
		
		boolean[][] testarray = {
			{false, false, false, false, false},
			{false, true, true, true, false},
			{false, true, true, true, false},
			{false, true, true, true, false},
			{false, false, false, false, false},
		};
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
		Input inputMouseLeft = new Input(Input.MOUSE_EVENT, "Left", MouseInput.MOUSE_BUTTON_PRESSED);

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
		if (eventShoot.isActive()) {
			Vector3f spawnposition = new Vector3f(player.getTranslation());
			spawnposition.translate(-playerfront.x, 0, -playerfront.z);
			Sphere c = new Sphere(spawnposition, 0.2f, 36, 36);
			c.setColor(Color.RED);
			RigidBody3 rb = new RigidBody3(PhysicsShapeCreator.create(c));
			rb.setMass(1);
			rb.setInertia(new Quaternionf());
			rb.setLinearFactor(new Vector3f(1, 0, 1));
			rb.applyCentralImpulse(VecMath.scale(playerfront, -100));
			space.addRigidBody(c, rb);
			defaultshader.addObject(c);
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
		System.out.println(testrb.getTranslation() + "; " + testrb.getRotation());

		cam.translateTo(player.getTranslation());
		cam.translate(transformedCameraOffset);
		cam.rotateTo((float) Math.toDegrees(playerrotation.angle()), -45);
		cam.update(delta);
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
