package musiculon.concept.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import musiculon.concept.Musiculon;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.fullscreen = true;
		config.samples = 4; //antialiasing
		config.title = "Musiculon The Noisy Concept";
		config.width = 1920;
		config.height = 768;
		new LwjglApplication(new Musiculon(), config);
	}
}
