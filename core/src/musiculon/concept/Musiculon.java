package musiculon.concept;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import musiculon.concept.data.CircleEntity;
import musiculon.concept.data.InputState;
import musiculon.concept.geometry.Direction;
import musiculon.concept.geometry.Line;

import java.util.*;

import static musiculon.concept.input.Keyboard.*;

public class Musiculon extends ApplicationAdapter {
	SpriteBatch batch;
	Texture playerTexture;
	Sprite playerSprite;
	Controller controller;
	TextureRegion inactivePolyTextureRegion;
	List<TextureRegion> activePolyTextureRegions = new ArrayList<>();
	private static final EarClippingTriangulator triangulator = new EarClippingTriangulator();
	List<PolygonSprite> inactiveTrapeziums = new ArrayList<>();
	List<PolygonSprite> activeTrapeziums = new ArrayList<>();
	PolygonSpriteBatch polyBatch;
	List<Sound> noteSounds = new ArrayList<>();
	Map<Long, SoundVolumeTuple> fadeOutSounds = new HashMap<>();

	public static class SoundVolumeTuple {
		public Sound sound;
		public float volume;

		public SoundVolumeTuple(Sound sound, float volume) {
			this.sound = sound;
			this.volume = volume;
		}
	}

	public static class GameState {
		public int screenMaxWidth;
		public int screenMaxHeight;
		public List<String> notes;

		public List<float[]> trapeziumVertices = new ArrayList<>();
		public List<Boolean> trapeziumActivity = new ArrayList<>();

		public List<Long> playingSounds = new ArrayList<>();

		public GameState(int screenMaxWidth, int screenMaxHeight) {
			this.screenMaxWidth = screenMaxWidth;
			this.screenMaxHeight = screenMaxHeight;

			notes = new ArrayList<>();

			String[] noteNames = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
			String first = "C";
			String last = "B";
			int startOctave = 2;
			int lastOctave = 6;
			boolean started = false;
			for (int octave = startOctave; octave <= lastOctave; octave++) {
				for (int note = 0; note < 12; note++) {
					String noteName = noteNames[note];
					if (started == false && noteName.equals(first)) {
						started = true;
					}
					else if (!started){
						continue;
					}

					notes.add(noteName + octave);

					if (started && octave == lastOctave && noteName.equals(last)) {
						break;
					}
				}
			}
		}

		public CircleEntity player = new CircleEntity(150, 150, 10, 0, 550);
	}

	GameState state;
	InputState inputState;

	public static void setupTrapeziums(GameState state,
									   List<PolygonSprite> activeTrapeziums, List<PolygonSprite> inactiveTrapeziums,
									   List<TextureRegion> activePolyTextureRegions, TextureRegion inactivePolyTextureRegion) {
		final float SCALE = 28;
		float SHORT_RADIUS = 17.5f*SCALE;
		float LONG_RADIUS = 30*SCALE;
		final float TRANSLATE_X = state.screenMaxWidth/2;
		final float TRANSLATE_Y = state.screenMaxHeight/2;
		final float START_ANGLE = 90;
		final float ANGLE_STEP = 25;
		final float ANGLE_GAP = 5;
		final int TRAPEZIUM_COUNT = state.notes.size();

		float twelfthRootOfTwo = 1/1.0594630943592952646f;

		activeTrapeziums.clear();
		inactiveTrapeziums.clear();
		state.trapeziumVertices.clear();

		for (int i = 0; i < TRAPEZIUM_COUNT; i++) {
			float leftAngle = START_ANGLE - (ANGLE_STEP + ANGLE_GAP) * i;
			float rightAngle = leftAngle - ANGLE_STEP;
			float modifier = (float) Math.pow(twelfthRootOfTwo, i);

			Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
			pix.setColor(1-1*modifier, 0.5f*modifier, 1*modifier, 0.75f);
			System.out.println(modifier);
			pix.fill();
			Texture solidColorTexture = new Texture(pix); //these textures NOT DISPOSED!!! Hack!
			TextureRegion coloredTextureRegion = new TextureRegion(solidColorTexture);
			activePolyTextureRegions.add(coloredTextureRegion);

			float[] vertices = getTrapeziumPolyVertices(leftAngle,rightAngle, SHORT_RADIUS*modifier, LONG_RADIUS*modifier, TRANSLATE_X, TRANSLATE_Y);
			PolygonSprite newInactiveSprite = getPolySprite(inactivePolyTextureRegion, vertices);
			PolygonSprite newActiveSprite = getPolySprite(coloredTextureRegion, vertices);

			inactiveTrapeziums.add(newInactiveSprite);
			activeTrapeziums.add(newActiveSprite);
			state.trapeziumVertices.add(vertices);
		}
	}

	@Override
	public void create () {
		//FULLSCREEN MODE!!!
//		Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

		state = new GameState(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		inputState = new InputState();

		batch = new SpriteBatch();
		playerTexture = new Texture("player.png");
		playerTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		playerSprite = new Sprite(playerTexture);
		//no hot plug/unplug logic
		if (Controllers.getControllers().size > 0) {
			controller = Controllers.getControllers().get(0);
		}

		polyBatch = new PolygonSpriteBatch();

		Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pix.setColor(.5f, 0.5f, .5f, 0.5f);
		pix.fill();
		Texture solidColorTexture = new Texture(pix);
		inactivePolyTextureRegion = new TextureRegion(solidColorTexture);


		setupTrapeziums(state,
						activeTrapeziums, inactiveTrapeziums,
						activePolyTextureRegions, inactivePolyTextureRegion);

		for (int i = 0; i < state.notes.size(); i++) {
			state.trapeziumActivity.add(false);
		}

		for (int i = 0; i < state.notes.size(); i++) {
			String noteName = state.notes.get(i);
			Sound newSound = Gdx.audio.newSound(Gdx.files.internal("notes/"+noteName+".ogg"));
			noteSounds.add(newSound);
			state.playingSounds.add(null);
		}

		Gdx.input.setInputProcessor(new InputAdapter() {

			@Override
			public boolean keyDown (int keyCode) {
				if (keyCode == Input.Keys.ESCAPE) {
					inputState.keyboardKeyState.put(ESCAPE, Boolean.TRUE);
				}
				else if (keyCode == Input.Keys.SPACE) {
					inputState.keyboardKeyState.put(SPACE, Boolean.TRUE);
				}
				return true;
			}

			@Override
			public boolean keyUp(int keyCode) {
				if (keyCode == Input.Keys.SPACE) {
					inputState.keyboardKeyState.put(SPACE, Boolean.FALSE);
				}
				return true;
			}
		});
	}

	public static float limit(float value) {
		return value < 0.2 && value > -0.2 ?
				0 :
				value;
	}

	//tested only on Dualshock 4!!!
	public static void handleControllerInput(Controller controller, InputState inputState) {
		if (controller == null) {
			//no controller attached
			return;
		}

		float leftJoypadX = limit(controller.getAxis(3));
		float leftJoypadY = -limit(controller.getAxis(2)); //flip Y-axis value
//		float rightJoypadX = limit(controller.getAxis(1));
//		float rightJoypadY = limit(controller.getAxis(0));
//		System.out.println("3: " + leftJoypadX + "\n2: " + leftJoypadY + "\n1: " + rightJoypadX + "\n0: " + rightJoypadY);


		if (leftJoypadX == 0 && leftJoypadY == 0) {
			//keep everything in the same place
			inputState.stickMoved = false;
		}
		else {
			inputState.directionAngle = Direction.getDirection(leftJoypadX, leftJoypadY).degreeAngle;
			inputState.stickMoved = true;
		}
	}

	public static void handleKeyboardInput(Input input, InputState inputState) {
		inputState.keyboardKeyState.put(W, Boolean.FALSE);
		inputState.keyboardKeyState.put(A, Boolean.FALSE);
		inputState.keyboardKeyState.put(S, Boolean.FALSE);
		inputState.keyboardKeyState.put(D, Boolean.FALSE);
		inputState.keyboardKeyState.put(UP, Boolean.FALSE);
		inputState.keyboardKeyState.put(DOWN, Boolean.FALSE);
		inputState.keyboardKeyState.put(LEFT, Boolean.FALSE);
		inputState.keyboardKeyState.put(RIGHT, Boolean.FALSE);

		if (input.isKeyPressed(Input.Keys.W)) {
			inputState.keyboardKeyState.put(W, Boolean.TRUE);
		}
		else if (input.isKeyPressed(Input.Keys.S)) {
			inputState.keyboardKeyState.put(S, Boolean.TRUE);
		}

		if (input.isKeyPressed(Input.Keys.A)) {
			inputState.keyboardKeyState.put(A, Boolean.TRUE);
		}
		else if (input.isKeyPressed(Input.Keys.D)) {
			inputState.keyboardKeyState.put(D, Boolean.TRUE);
		}

		if (input.isKeyPressed(Input.Keys.LEFT)) {
			inputState.keyboardKeyState.put(LEFT, Boolean.TRUE);
		}
		else if (input.isKeyPressed(Input.Keys.RIGHT)) {
			inputState.keyboardKeyState.put(RIGHT, Boolean.TRUE);
		}

		if (input.isKeyPressed(Input.Keys.UP)) {
			inputState.keyboardKeyState.put(UP, Boolean.TRUE);
		}
		else if (input.isKeyPressed(Input.Keys.DOWN)) {
			inputState.keyboardKeyState.put(DOWN, Boolean.TRUE);
		}
	}

	public static void checkExit(InputState inputState) {
		if (inputState.keyboardKeyState.get(ESCAPE) == Boolean.TRUE) {
			Gdx.app.exit();
			System.exit(0);
		}
	}

	public static void applyPlayerInput(InputState inputState, GameState state, float delta) {
		if (!inputState.stickMoved
				&& inputState.keyboardKeyState.get(LEFT) != Boolean.TRUE
				&& inputState.keyboardKeyState.get(RIGHT) != Boolean.TRUE
				&& inputState.keyboardKeyState.get(UP) != Boolean.TRUE
				&& inputState.keyboardKeyState.get(DOWN) != Boolean.TRUE) {
			//no input
			return;
		}

		float rotation = 0;
		if (inputState.stickMoved) {
			rotation = inputState.directionAngle;
		}
		else {
			float x = 0;
			if (inputState.keyboardKeyState.get(LEFT) == Boolean.TRUE) {
				x = -1;
			}
			else if (inputState.keyboardKeyState.get(RIGHT) == Boolean.TRUE) {
				x = 1;
			}

			float y = 0;
			if (inputState.keyboardKeyState.get(UP) == Boolean.TRUE) {
				y = 1;
			}
			else if (inputState.keyboardKeyState.get(DOWN) == Boolean.TRUE) {
				y = -1;
			}

			rotation = Direction.getDirection(x, y).degreeAngle;
		}

		float directionX = (float) Math.cos(Math.PI / 180 * rotation);
		float directionY = (float) Math.sin(Math.PI / 180 * rotation);

		float step = state.player.speed * delta;
		state.player.x += directionX * step;
		state.player.y += directionY * step;
		state.player.rotation = rotation;
	}

	public static void applySizeFunTransformation(GameState state) {
		CircleEntity player = state.player;

		float spiralCenterX = state.screenMaxWidth/2;
		float spiralCenterY = state.screenMaxHeight/2;

		float distance = Line.distance(player.x, player.y, spiralCenterX, spiralCenterY);

		if (distance > 300) {
			player.radius = 55;
			player.speed = 1000;
		}
		else if (distance > 25) {
			player.radius = 5 + (50*(distance-25)/300f);
			player.speed = 200 + (800*(distance-25)/275f);
		}
		else {
			player.radius = 5;
			player.speed = 200;
		}
	}

	public static void calculatePlayerCollisions(GameState state) {
		CircleEntity player = state.player;

		//against borders
		if (player.x > state.screenMaxWidth - state.player.radius) {
			player.x = state.screenMaxWidth - state.player.radius;
		} else if (player.x < player.radius) {
			player.x = player.radius;
		}

		if (player.y > state.screenMaxHeight - state.player.radius) {
			System.out.println(state.screenMaxHeight);
			player.y = state.screenMaxHeight - state.player.radius;
		} else if (player.y < player.radius) {
			player.y = player.radius;
		}
	}

	public static void calculateTrapeziumCollisions(GameState state) {
		CircleEntity player = state.player;
		float x = player.x;
		float y = player.y;
		float radius = player.radius;

		for (int i = 0; i < state.trapeziumVertices.size(); i++) {
			float[] vertices = state.trapeziumVertices.get(i);

			boolean playerCenterInPolygon = Intersector.isPointInPolygon(vertices, 0, vertices.length, x, y);

			boolean verticeInsidePlayerCircle = false;

			if (playerCenterInPolygon != true) {
				vertice_loop:
				for (int j = 0; j < vertices.length/2; j++) {
					float verticeX = vertices[j*2];
					float verticeY = vertices[j*2+1];

					float distance = Line.distance(x, y, verticeX, verticeY);

					if (distance < radius) {
						verticeInsidePlayerCircle = true;
						break vertice_loop;
					}
				}
			}

			boolean intersected = false;
			if (playerCenterInPolygon == false && verticeInsidePlayerCircle == false) {
				Vector2 center = new Vector2(x, y);
				float squaredRadius = radius*radius;

				intersection_loop:
				for (int j = 0; j < vertices.length/2; j++) {
					Vector2 start = new Vector2(vertices[j*2], vertices[j*2+1]);

					Vector2 end;

					//last vertex -> its edge end is very FIRST vertex;
					if (j == vertices.length/2-1) {
						end = new Vector2(vertices[0], vertices[1]);
					}
					else {
						end = new Vector2(vertices[j * 2 + 2], vertices[j * 2 + 3]);
					}

					boolean intersectionFound = Intersector.intersectSegmentCircle(start, end, center, squaredRadius);

					if (intersectionFound) {
						intersected = true;
						break intersection_loop;
					}
				}
			}

			if (playerCenterInPolygon || verticeInsidePlayerCircle || intersected) {
				state.trapeziumActivity.set(i, true);
			}
			else {
				state.trapeziumActivity.set(i, false);
			}
		}
	}

	public static float[] getTrapeziumPolyVertices(float leftAngle, float rightAngle,
												   float shortRadius, float longRadius,
												   float translateX, float translateY) {
		float leftKforX = (float) Math.cos(leftAngle*Math.PI/180);
		float leftKforY = (float) Math.sin(leftAngle*Math.PI/180);
		float rightKforX = (float) Math.cos(rightAngle*Math.PI/180);
		float rightKforY = (float) Math.sin(rightAngle*Math.PI/180);

		// In my terminology polygon is like
		//
		//    (x2,y2) ---- (x3,y3)
		//      |             |
		//      |             |
		//    (x1,y1) ---- (x4,y4)
		//

		float x1 = Line.getYfromXWithK(leftKforX, shortRadius)+translateX;
		float y1 = Line.getYfromXWithK(leftKforY, shortRadius)+translateY;
		float x2 = Line.getYfromXWithK(leftKforX, longRadius)+translateX;
		float y2 = Line.getYfromXWithK(leftKforY, longRadius)+translateY;
		float x3 = Line.getYfromXWithK(rightKforX, longRadius)+translateX;
		float y3 = Line.getYfromXWithK(rightKforY, longRadius)+translateY;
		float x4 = Line.getYfromXWithK(rightKforX, shortRadius)+translateX;
		float y4 = Line.getYfromXWithK(rightKforY, shortRadius)+translateY;

		return new float[] {x1,y1, x2,y2, x3,y3, x4,y4};
	}

	public static void handleSoundState(GameState state, List<Sound> sounds, Map<Long, SoundVolumeTuple> fadeOutSounds) {
		for (int i = 0; i < state.playingSounds.size(); i++) {
			boolean trapeziumActive = state.trapeziumActivity.get(i);

			Long playingSoundId = state.playingSounds.get(i);
			Sound sound = sounds.get(i);

			if (trapeziumActive && playingSoundId == null) {
				long soundId = sound.play();
				sound.setLooping(soundId, true);
				state.playingSounds.set(i, soundId);
			} else if (!trapeziumActive && playingSoundId != null) {
				Long soundId = state.playingSounds.get(i);
				fadeOutSounds.put(soundId, new SoundVolumeTuple(sound, 1f));
				state.playingSounds.set(i, null);
			}
		}

		Iterator<Long> fadeOutIterator= fadeOutSounds.keySet().iterator();
		while(fadeOutIterator.hasNext()) {
			Long soundId = fadeOutIterator.next();

			SoundVolumeTuple tuple = fadeOutSounds.get(soundId);
			tuple.volume -= 0.02;
			tuple.sound.setVolume(soundId, tuple.volume);

			if (tuple.volume < 0.1) {
				tuple.sound.stop(soundId);
				fadeOutIterator.remove();
			}
		}
	}

	public static PolygonSprite getPolySprite(TextureRegion polyTextureRegion, float[] vertices) {
		ShortArray triangleIndices = triangulator.computeTriangles(vertices);
		PolygonRegion polyReg = new PolygonRegion(polyTextureRegion, vertices, triangleIndices.toArray());

		return new PolygonSprite(polyReg);
	}

	public final int MAX_UPDATE_ITERATIONS = 3;
	public final float FIXED_TIMESTAMP = 1/60f;
	private float internalTimeTracker = 0;

	@Override
	public void render () {
		//input handling
		checkExit(inputState);
		handleControllerInput(controller, inputState);
		handleKeyboardInput(Gdx.input, inputState);

		//fixed-timestamp logic handling
		float delta = Gdx.graphics.getDeltaTime();
		internalTimeTracker += delta;
		int iterations = 0;

		while(internalTimeTracker > FIXED_TIMESTAMP && iterations < MAX_UPDATE_ITERATIONS) {
			//apply input
			applyPlayerInput(inputState, state, FIXED_TIMESTAMP);
			applySizeFunTransformation(state);

			//collision detection
			calculatePlayerCollisions(state);
			calculateTrapeziumCollisions(state);

			handleSoundState(state, noteSounds, fadeOutSounds);

			//time tracking logic
			internalTimeTracker -= FIXED_TIMESTAMP;
			iterations++;
		}

		//render
		Gdx.gl.glClearColor(0.25f, 1, 03.f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


		polyBatch.begin();

		for (int i = 0; i < state.trapeziumActivity.size(); i++) {
			boolean isActive = state.trapeziumActivity.get(i);
			if (isActive) {
				activeTrapeziums.get(i).draw(polyBatch);
			}
			else {
				inactiveTrapeziums.get(i).draw(polyBatch);
			}
		}

		polyBatch.end();


		batch.begin();

		CircleEntity player = state.player;
		playerSprite.setBounds(player.x-player.radius, player.y-player.radius, player.radius *2, player.radius *2);
		playerSprite.setOriginCenter();
		playerSprite.setRotation(player.rotation);
		playerSprite.draw(batch);

		batch.end();
	}

	@Override
	public void dispose () {
		batch.dispose();
		playerTexture.dispose();
		polyBatch.dispose();
		inactivePolyTextureRegion.getTexture().dispose();
		for (TextureRegion region: activePolyTextureRegions) {
			region.getTexture().dispose();
		}
		for (Sound sound: noteSounds) {
			sound.dispose();
		}
	}

}