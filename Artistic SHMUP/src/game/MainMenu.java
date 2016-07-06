package game;

import java.util.ArrayList;
import java.util.List;

import gui.Font;
import gui.Text;
import input.Input;
import input.InputEvent;
import input.MouseInput;
import loader.FontLoader;
import loader.ShaderLoader;
import shader.Shader;
import shape2d.Quad;
import vector.Vector3f;
import vector.Vector4f;

public class MainMenu implements WindowContent {
	MainWindow game;
	Text start, endless, quit;
	Shader defaultshader, colorshader, backgroundcolorshader;
	Quad colorpickerbackground, backgroundpickerbackground;
	boolean isActive;

	final int fontSize = 30;
	final int fontSizeMouseover = 40;
	final int translationMouseoverOffsetY = 3;

	final int minMouseoverX = 120;
	final int maxMouseoverX = 350;

	boolean shift1 = false;
	boolean shift2 = false;
	boolean shift3 = false;
	boolean scalecolor = false;
	boolean scalebackground = false;

	List<Vector3f> colors;
	int colorID;
	boolean whitebackground;

	InputEvent mouseclick;
	boolean clickLock = false;

	public MainMenu(MainWindow game, Vector3f playercolor, boolean whiteBackground) {
		this.game = game;

		colors = new ArrayList<Vector3f>();
		colors.add(new Vector3f(1, 0, 0));
		colors.add(new Vector3f(0, 1, 0));
		colors.add(new Vector3f(0, 0, 1));
		colors.add(new Vector3f(1, 1, 0));
		colors.add(new Vector3f(1, 0, 1));
		colors.add(new Vector3f(0, 1, 1));
		colorID = colors.indexOf(playercolor);
		this.whitebackground = whiteBackground;
	}

	@Override
	public void init() {
		Font menufont = FontLoader.loadFont("res/fonts/DejaVuSans.ttf");

		defaultshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
		game.addShaderInterface(defaultshader);

		Vector3f currentcolor = colors.get(colorID);
		colorshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert", "res/shaders/colorshader.frag"));
		colorshader.addArgumentName("u_color");
		colorshader.addArgument(new Vector4f(currentcolor.x, currentcolor.y, currentcolor.z, 1f));
		game.addShaderInterface(colorshader);

		backgroundcolorshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert", "res/shaders/colorshader.frag"));
		backgroundcolorshader.addArgumentName("u_color");
		if (whitebackground) {
			backgroundcolorshader.addArgument(new Vector4f(1f, 1f, 1f, 1f));
		} else {
			backgroundcolorshader.addArgument(new Vector4f(0f, 0f, 0f, 1f));
		}
		game.addShaderInterface(backgroundcolorshader);

		start = new Text("Start", 240, 280, menufont, fontSize);
		endless = new Text("Endless", 205, 350, menufont, fontSize);
		quit = new Text("Quit", 255, 420, menufont, fontSize);
		defaultshader.addObject(new Text("Pick a color:", 1020, 160, menufont, fontSize));
		colorpickerbackground = new Quad(1110, 250, 52, 52);
		defaultshader.addObject(colorpickerbackground);
		colorshader.addObject(new Quad(1110, 250, 50, 50));
		defaultshader.addObject(new Text("Pick a background:", 980, 400, menufont, fontSize));
		backgroundpickerbackground = new Quad(1110, 490, 52, 52);
		defaultshader.addObject(backgroundpickerbackground);
		backgroundcolorshader.addObject(new Quad(1110, 490, 50, 50));

		defaultshader.addObject(start);
		defaultshader.addObject(endless);
		defaultshader.addObject(quit);

		Input inputMouseClick = new Input(Input.MOUSE_EVENT, "Left", MouseInput.MOUSE_BUTTON_DOWN);
		mouseclick = new InputEvent("Mouseclick", inputMouseClick);
		game.inputs.addEvent(mouseclick);
	}

	@Override
	public void update(int delta) {
		float mouseX = game.inputs.getMouseX();
		float mouseY = game.inputs.getMouseY();

		if (mouseX > minMouseoverX && mouseX < maxMouseoverX && mouseY > 240 && mouseY < 300) {
			start.setFontsize(fontSizeMouseover);
			if (mouseclick.isActive()) {
				game.startGame();
			}
			if (!shift1) {
				start.translate(-20, translationMouseoverOffsetY);
			}
			shift1 = true;
		} else {
			start.setFontsize(fontSize);
			if (shift1) {
				start.translate(20, -translationMouseoverOffsetY);
			}
			shift1 = false;
		}

		if (mouseX > minMouseoverX && mouseX < maxMouseoverX && mouseY > 310 && mouseY < 370) {
			endless.setFontsize(fontSizeMouseover);
			if (mouseclick.isActive()) {
				game.startGame();
			}
			if (!shift2) {
				endless.translate(-31, translationMouseoverOffsetY);
			}
			shift2 = true;
		} else {
			endless.setFontsize(fontSize);
			if (shift2) {
				endless.translate(31, -translationMouseoverOffsetY);
			}
			shift2 = false;
		}

		if (mouseX > minMouseoverX && mouseX < maxMouseoverX && mouseY > 380 && mouseY < 440) {
			quit.setFontsize(fontSizeMouseover);
			if (mouseclick.isActive()) {
				game.setRunning(false);
			}
			if (!shift3) {
				quit.translate(-14, translationMouseoverOffsetY);
			}
			shift3 = true;
		} else {
			quit.setFontsize(fontSize);
			if (shift3) {
				quit.translate(14, -translationMouseoverOffsetY);
			}
			shift3 = false;
		}

		if (mouseX > 1058 && mouseX < 1162 && mouseY > 200 && mouseY < 300) {
			if (mouseclick.isActive() && !clickLock) {
				colorID++;
				colorID %= colors.size();
				Vector3f color = colors.get(colorID);
				colorshader.setArgument("u_color", new Vector4f(color.x, color.y, color.z, 1f));
				clickLock = true;
			}
			if (!scalecolor) {
				colorpickerbackground.scaleTo(1.05f);
			}
			scalecolor = true;
		} else {
			if (scalecolor) {
				colorpickerbackground.scaleTo(1f);
			}
			scalecolor = false;
		}

		if (mouseX > 1058 && mouseX < 1162 && mouseY > 440 && mouseY < 540) {
			if (mouseclick.isActive() && !clickLock) {
				whitebackground = !whitebackground;
				if (whitebackground) {
					backgroundcolorshader.setArgument("u_color", new Vector4f(1f, 1f, 1f, 1f));
				} else {
					backgroundcolorshader.setArgument("u_color", new Vector4f(0f, 0f, 0f, 1f));
				}
				game.resetCanvas(whitebackground);
				clickLock = true;
			}
			if (!scalebackground) {
				backgroundpickerbackground.scaleTo(1.05f);
			}
			scalebackground = true;
		} else {
			if (scalebackground) {
				backgroundpickerbackground.scaleTo(1f);
			}
			scalebackground = false;
		}

		if (clickLock && !mouseclick.isActive()) {
			clickLock = false;
		}
	}

	@Override
	public void render() {
		game.render3dLayer();
	}

	@Override
	public void renderInterface() {
		game.renderInterfaceLayer();
	}

	@Override
	public void setActive(boolean active) {
		this.isActive = active;
	}

	@Override
	public void delete() {
		game.getShaderInterface().remove(defaultshader);
		game.getShaderInterface().remove(colorshader);
		game.getShaderInterface().remove(backgroundcolorshader);
		defaultshader.delete();
		colorshader.delete();
		backgroundcolorshader.delete();
		game.inputs.removeEvent(mouseclick);
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public Vector3f getPlayerColor() {
		return colors.get(colorID);
	}

	@Override
	public boolean isBackgroundWhite() {
		return whitebackground;
	}

}
