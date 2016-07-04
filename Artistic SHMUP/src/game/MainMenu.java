package game;

import gui.Font;
import gui.Text;
import input.Input;
import input.InputEvent;
import input.MouseInput;
import loader.FontLoader;
import loader.ShaderLoader;
import shader.Shader;

public class MainMenu implements WindowContent {
	MainWindow game;
	Text start, endless, quit;
	Shader defaultshader;
	boolean isActive;

	final int fontSize = 30;
	final int fontSizeMouseover = 40;

	InputEvent mouseclick;

	public MainMenu(MainWindow game) {
		this.game = game;
	}

	@Override
	public void init() {
		Font menufont = FontLoader.loadFont("res/fonts/DejaVuSans.ttf");

		defaultshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
		game.addShaderInterface(defaultshader);

		start = new Text("Start", 10, 10, menufont, fontSize);
		endless = new Text("Endless", 10, 200, menufont, fontSize);
		quit = new Text("Quit", 10, 400, menufont, fontSize);

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
		if (mouseX > 10 && mouseX < 100 && mouseY > 10 && mouseY > 100) {
			start.setFontsize(fontSizeMouseover);
			if (mouseclick.isActive()) {
				game.startGame();
			}
		} else {
			start.setFontsize(fontSize);
		}
	}

	@Override
	public void render() {
		// TODO Auto-generated method stub

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
		defaultshader.delete();
		game.inputs.removeEvent(mouseclick);
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

}
