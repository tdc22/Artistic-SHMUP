package game;

import curves.BezierCurve3;
import curves.SquadCurve3;
import display.DisplayMode;
import display.GLDisplay;
import display.PixelFormat;
import display.VideoSettings;
import loader.FontLoader;
import loader.ShaderLoader;
import quaternion.Quaternionf;
import shader.Shader;
import sound.NullSoundEnvironment;
import utils.Debugger;
import utils.DefaultValues;
import vector.Vector3f;

public class MainWindow extends StandardGame {
	WindowContent content;
	Debugger debugger;
	float zoomtimer = 0;
	BezierCurve3 inCurve, outCurve;
	SquadCurve3 inRotCurve, outRotCurve;
	boolean zoomingIn = true;
	final int levelsizeX = 160;
	final int levelsizeZ = 160;

	public MainWindow() {
		this.content = new MainMenu(this);
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
			if (zoomtimer == 1 && zoomingIn) {
				zoomtimer = 0;
				zoomingIn = false;
				content.setActive(true);
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

	public void startGame() {
		setContent(new Game(this, levelsizeX, levelsizeZ));
		zoomIn(levelsizeX / 2, levelsizeZ / 2);
		content.setActive(false);
	}

	@Override
	public void init() {
		initDisplay(new GLDisplay(),
				new DisplayMode(DefaultValues.DEFAULT_DISPLAY_POSITION_X, 20, 1280, 720, "Artful SHMUP", false,
						DefaultValues.DEFAULT_DISPLAY_RESIZEABLE, DefaultValues.DEFAULT_DISPLAY_FULLSCREEN),
				new PixelFormat(), new VideoSettings(1280, 720), new NullSoundEnvironment());
		cam.setFlyCam(false);
		setRendered(true, false, true);

		int defaultshaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		Shader defaultshader = new Shader(defaultshaderID);
		addShader(defaultshader);
		Shader defaultshaderInterface = new Shader(defaultshaderID);
		addShaderInterface(defaultshaderInterface);

		debugger = new Debugger(inputs, defaultshader, defaultshaderInterface,
				FontLoader.loadFont("res/fonts/DejaVuSans.ttf"), cam);

		content.init();
	}

	public void zoomOut(int halflevelsizeX, int halflevelsizeZ) {
		display.unbindMouse();
		Vector3f b = new Vector3f(cam.getTranslation());
		b.scale(0.4f);
		Vector3f d = new Vector3f(halflevelsizeX, 100, halflevelsizeZ);
		Vector3f c = new Vector3f(halflevelsizeX, 40, halflevelsizeZ);
		outCurve = new BezierCurve3(cam.getTranslation(), cam.getTranslation(), c, d);
		Quaternionf lookdown = new Quaternionf();
		lookdown.rotate(-90, new Vector3f(1, 0, 0));
		outRotCurve = new SquadCurve3(cam.getRotation(), cam.getRotation(), lookdown, lookdown);
	}

	public void zoomIn(int halflevelsizeX, int halflevelsizeZ) {
		Vector3f target = new Vector3f(halflevelsizeX, 10, halflevelsizeZ + 10);
		Vector3f d = new Vector3f(halflevelsizeX, 100, halflevelsizeZ);
		Vector3f c = new Vector3f(halflevelsizeX, 40, halflevelsizeZ);
		inCurve = new BezierCurve3(d, c, target, target);
		Quaternionf lookdown = new Quaternionf();
		lookdown.rotate(-90, new Vector3f(1, 0, 0));
		Quaternionf targetRot = new Quaternionf(0.9238795, -0.38268346, 0.0, 0.0);
		inRotCurve = new SquadCurve3(lookdown, targetRot, lookdown, targetRot);
	}
}
