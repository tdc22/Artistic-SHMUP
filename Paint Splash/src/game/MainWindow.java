package game;

import curves.BezierCurve3;
import curves.SquadCurve3;
import display.DisplayMode;
import display.GLDisplay;
import display.PixelFormat;
import display.VideoSettings;
import loader.FontLoader;
import loader.ShaderLoader;
import loader.TextureLoader;
import objects.Camera2;
import objects.RigidBody3;
import physics.PhysicsShapeCreator;
import physics.PhysicsSpace;
import quaternion.Quaternionf;
import shader.PostProcessingShader;
import shader.Shader;
import shape.Box;
import shape2d.Quad;
import sound.ALSoundEnvironment;
import texture.FramebufferObject;
import texture.Texture;
import utils.Debugger;
import utils.DefaultValues;
import utils.ProjectionHelper;
import vector.Vector2f;
import vector.Vector3f;
import vector.Vector4f;

public class MainWindow extends StandardGame {
	WindowContent content;
	Debugger debugger;
	float zoomtimer = 0;
	BezierCurve3 inCurve, outCurve;
	SquadCurve3 inRotCurve, outRotCurve;
	boolean zoomingIn = true;
	final int levelsizeX = 160;
	final int levelsizeZ = 160;
	final int halflevelsizeX = levelsizeX / 2;
	final int halflevelsizeZ = levelsizeZ / 2;
	final int splashResolution = 512; // power of 2 !!!
	final int splashSubdivision = 4;
	final int wallsizeX = 190;
	final int wallsizeZ = 190;

	Shader[][] splashGroundShaders, splashObjectShaders;
	FramebufferObject[][] splashGroundFramebuffers;
	FramebufferObject[][] splashGroundFramebufferHelpers;
	FramebufferObject[][] newSplashFramebuffer;
	PostProcessingShader[][] splashCombinationShaders;
	boolean[][] splashGroundFramebufferFirst;
	Texture[][] currentSplashTextures;
	Box[][] groundboxes;
	Texture whitePixelTexture;

	Shader newSplashShader;

	final Quaternionf lookDown = new Quaternionf(0.70710677, -0.70710677, 0.0, 0.0);

	public MainWindow() {
		this.content = new MainMenu(this, new Vector3f(1, 0, 0), true);
		this.content.setActive(true);
	}

	@Override
	public void update(int delta) {
		debugger.update(fps, 0, 0);

		if (content.isActive()) {
			content.update(delta);
		} else {
			if (zoomtimer < 1)
				zoomtimer += delta * 0.0002;
			else {
				zoomtimer = 1;
			}
			if (zoomingIn) {
				cam.translateTo(inCurve.getPoint(zoomtimer));
				cam.rotateTo(inRotCurve.getRotation(zoomtimer));
			} else {
				cam.translateTo(outCurve.getPoint(zoomtimer));
				cam.rotateTo(outRotCurve.getRotation(zoomtimer));
			}
			if (zoomtimer == 1) {
				if (zoomingIn) {
					content.setActive(true);
				} else {
					setContent(new MainMenu(this, content.getPlayerColor(), content.isBackgroundWhite()));
					content.setActive(true);
				}
				zoomtimer = 0;
				zoomingIn = !zoomingIn;
			}
		}
	}

	@Override
	public void render() {
		debugger.begin();
		content.render();
	}

	@Override
	public void render2d() {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderInterface() {
		content.renderInterface();
		debugger.end();
	}

	public void setContent(WindowContent windowcontent) {
		if (content != null) {
			content.delete();
		}
		content = windowcontent;
		content.init();
	}

	public void startGame(boolean endless) {
		setContent(
				new Game(this, levelsizeX, levelsizeZ, content.getPlayerColor(), content.isBackgroundWhite(), endless));
		zoomIn(halflevelsizeX, halflevelsizeZ);
		content.setActive(false);
	}

	@Override
	public void init() {
		initDisplay(new GLDisplay(),
				new DisplayMode(DefaultValues.DEFAULT_DISPLAY_POSITION_X, 20, 1280, 720, "Artful SHMUP", false,
						DefaultValues.DEFAULT_DISPLAY_RESIZEABLE, DefaultValues.DEFAULT_DISPLAY_FULLSCREEN),
				new PixelFormat(), new VideoSettings(1280, 720), new ALSoundEnvironment());
		layer2d.setProjectionMatrix(ProjectionHelper.ortho(0, levelsizeX / (float) splashSubdivision,
				levelsizeZ / (float) splashSubdivision, 0, -1, 1));
		cam.setFlyCam(false);
		cam.translateTo(halflevelsizeX, 100, halflevelsizeZ);
		cam.rotateTo(lookDown);
		setRendered(true, false, true);

		int defaultshaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		Shader defaultshader = new Shader(defaultshaderID);
		addShader(defaultshader);
		Shader defaultshaderInterface = new Shader(defaultshaderID);
		addShaderInterface(defaultshaderInterface);

		debugger = new Debugger(inputs, defaultshader, defaultshaderInterface,
				FontLoader.loadFont("res/fonts/DejaVuSans.ttf"), cam);

		int textureshaderID = ShaderLoader.loadShaderFromFile("res/shaders/textureshader.vert",
				"res/shaders/textureshader.frag");
		Shader frameShader = new Shader(textureshaderID);
		frameShader.addArgument("u_texture", new Texture(TextureLoader.loadTexture("res/textures/wood.png")));
		addShader(frameShader);
		Shader wallpaperShader = new Shader(textureshaderID);
		wallpaperShader.addArgument("u_texture", new Texture(TextureLoader.loadTexture("res/textures/wallpaper.png")));
		addShader(wallpaperShader);

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

		Box wall = new Box(halflevelsizeX, -2, halflevelsizeZ, wallsizeX, 0.1f, wallsizeZ);
		wall.setRenderHints(false, true, false);
		wallpaperShader.addObject(wall);

		whitePixelTexture = new Texture(TextureLoader.loadTexture("res/textures/whitePixel.png"));

		newSplashShader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/splashshader.vert", "res/shaders/splashshader.frag"));
		newSplashShader.addArgument("u_texture", new Texture());
		newSplashShader.addArgument("u_color", new Vector4f(0, 1, 0, 1));
		addShader2d(newSplashShader);

		initCanvas();
		resetCanvas(true);
		content.init();
	}

	public void zoomOut(int halflevelsizeX, int halflevelsizeZ) {
		display.unbindMouse();
		Vector3f b = new Vector3f(cam.getTranslation());
		b.scale(0.4f);
		Vector3f d = new Vector3f(halflevelsizeX, 100, halflevelsizeZ);
		Vector3f c = new Vector3f(halflevelsizeX, 40, halflevelsizeZ);
		outCurve = new BezierCurve3(cam.getTranslation(), cam.getTranslation(), c, d);
		outRotCurve = new SquadCurve3(cam.getRotation(), cam.getRotation(), lookDown, lookDown);
	}

	public void zoomIn(int halflevelsizeX, int halflevelsizeZ) {
		Vector3f target = new Vector3f(halflevelsizeX, 10, halflevelsizeZ + 10);
		Vector3f d = new Vector3f(halflevelsizeX, 100, halflevelsizeZ);
		Vector3f c = new Vector3f(halflevelsizeX, 40, halflevelsizeZ);
		inCurve = new BezierCurve3(d, c, target, target);
		Quaternionf targetRot = new Quaternionf(0.9238795, -0.38268346, 0.0, 0.0);
		inRotCurve = new SquadCurve3(lookDown, targetRot, lookDown, targetRot);
	}

	public void initCanvas() {
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

				Shader groundshader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/textureshader.vert",
						"res/shaders/textureshader.frag"));
				groundshader.addArgument("u_texture", currentSplashTextures[x][z]);
				addShader(groundshader);
				splashGroundShaders[x][z] = groundshader;

				Box groundbox = new Box(halfboxsizeX + (2 * halfboxsizeX * x), -2f,
						halfboxsizeZ + (2 * halfboxsizeZ * z), halfboxsizeX, 1, halfboxsizeZ);
				groundbox.setRenderHints(false, true, false);
				groundshader.addObject(groundbox);
			}
		}
	}

	public void resetCanvas(boolean white) {
		Quad a = new Quad(halflevelsizeX, halflevelsizeZ, halflevelsizeX, halflevelsizeZ);
		a.setRenderHints(false, true, false);
		newSplashShader.addObject(a);
		newSplashShader.setArgument("u_texture", whitePixelTexture);
		if (white) {
			newSplashShader.setArgument("u_color", new Vector4f(1f, 1f, 1f, 1f));
		} else {
			newSplashShader.setArgument("u_color", new Vector4f(0f, 0f, 0f, 1f));
		}
		for (FramebufferObject[] fbos : splashGroundFramebuffers) {
			for (FramebufferObject fbo : fbos) {
				fbo.updateTexture();
			}
		}
		for (FramebufferObject[] fbos : splashGroundFramebufferHelpers) {
			for (FramebufferObject fbo : fbos) {
				fbo.updateTexture();
			}
		}
		newSplashShader.removeObject(a);
		a.delete();
	}

	public Box addStaticBox(float x, float y, float z, float width, float height, float depth, PhysicsSpace space) {
		Box box = new Box(x, y, z, width, height, depth);
		box.setRenderHints(false, false, false);
		RigidBody3 rb = new RigidBody3(PhysicsShapeCreator.create(box));
		if (space != null)
			space.addRigidBody(box, rb);
		splashObjectShaders[calculateSplashGridX((int) x)][calculateSplashGridZ((int) z)].addObject(box);
		return box;
	}

	public int calculateSplashGridX(int x) {
		return x / (int) (levelsizeX / (float) splashSubdivision);
	}

	public int calculateSplashGridZ(int z) {
		return z / (int) (levelsizeZ / (float) splashSubdivision);
	}
}
