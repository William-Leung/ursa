/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter.
 * There must be some undocumented OpenGL code in setScreen.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.physics;

import com.badlogic.gdx.*;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.assets.*;

/**
 * Root class for a LibGDX.  
 *
 * This class is technically not the ROOT CLASS. Each platform has another class above
 * this (e.g. PC games use DesktopLauncher) which serves as the true root.  However, 
 * those classes are unique to each platform, while this class is the same across all 
 * plaforms. In addition, this functions as the root class all intents and purposes, 
 * and you would draw it as a root class in an architecture specification.  
 */
public class GDXRoot extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	AssetDirectory directory;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas;
	private Preferences prefs;
	private float levelsCompleted;
	private float camX;
	private float camY;
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingMode loading;
	private LevelSelector levelSelector;
	private RetryMenu retryMenu;
	/** Player mode for the game proper (CONTROLLER CLASS) */
	private int current;
	/** List of all WorldControllers */
	private WorldController[] controllers;

	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public GDXRoot() { }
	public static void testing() {
		float[] arr = new float[]{19,236,77,39,147,31,234,140,132,249,195,267,187,301,227,332,244,396,232,455,123,505,40,471,33,366,53,297};



		float imageX = 256;
		float imageY = 512;
		float yOffset = imageY - 495;
		for(int i = 0; i < arr.length; i++) {
			float elem = arr[i];
			if(i % 2 == 1) {
				elem = (imageY-yOffset) - elem;
			} else {
				elem = elem - imageX / 2f;
			}
			elem *= 12/256f;
			System.out.print(Math.round(elem * 10) / 10.0 + ", ") ;
		}
		System.out.print("\n");
	}

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		//testing();
		prefs = Gdx.app.getPreferences("Completed Data");
		levelsCompleted = 10;
		System.out.println("levels completed " + levelsCompleted);
		canvas  = new GameCanvas();
		loading = new LoadingMode("assets.json",canvas,1);

		camX = canvas.getCameraX();
		camY = canvas.getCameraY();
		// Initialize the three game worlds

		controllers = new WorldController[25];
		controllers[0] = new SceneModel("rigel_level.json");
		controllers[1] = new SceneModel("levelD.json");
		controllers[2] = new SceneModel("level_athena.json");
		for(int i = 0; i < prefs.getFloat("completed");i++){
			controllers[i].setWasCompleted(true);
		}
		System.out.println("controller 0 " + controllers[0].isWasCompleted());

		current = 0;
		loading.setScreenListener(this);
		setScreen(loading);
	}

	/**
	 * Called when the Application is destroyed.
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		// Call dispose on our children
		setScreen(null);
		for(int ii = 0; ii < controllers.length; ii++) {
			if(controllers[ii] != null){
				controllers[ii].dispose();
			}

		}

		canvas.dispose();
		canvas = null;

		// Unload all of the resources
		// Unload all of the resources
		if (directory != null) {
			directory.unloadAssets();
			directory.dispose();
			directory = null;
		}
		super.dispose();
	}

	/**
	 * Called when the Application is resized.
	 *
	 * This can happen at any point during a non-paused state but will never happen
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize();
		super.resize(width,height);
	}

	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {

		if (screen == loading) {
			directory = loading.getAssets();
			levelSelector = new LevelSelector(canvas,levelsCompleted);
			levelSelector.gatherAssets(directory);
			levelSelector.setScreenListener(this);


			//controllers[current].reset();
			setScreen(levelSelector);
			levelSelector.setActive(true);

			loading.dispose();
			loading = null;
		} else if (screen == levelSelector && exitCode == 1) {
			current = 0;

			controllers[current].gatherAssets(directory);
			controllers[current].setScreenListener(this);
			controllers[current].setCanvas(canvas);
			controllers[current].reset();
			setScreen(controllers[current]);
			controllers[current].active = true;

			levelSelector.setActive(false);
			levelSelector.dispose();
			levelSelector = null;

		} else if (screen == levelSelector && exitCode == 2) {
			current = 1;

			controllers[current].gatherAssets(directory);
			controllers[current].setScreenListener(this);
			controllers[current].setCanvas(canvas);
			controllers[current].reset();
			setScreen(controllers[current]);
			controllers[current].active = true;

			levelSelector.setActive(false);
			levelSelector.dispose();
			levelSelector = null;

		}else if (screen == levelSelector && exitCode == 3) {
			current = 2;

			controllers[current].gatherAssets(directory);
			controllers[current].setScreenListener(this);
			controllers[current].setCanvas(canvas);
			controllers[current].reset();
			setScreen(controllers[current]);
			controllers[current].active = true;

			levelSelector.setActive(false);
			levelSelector.dispose();
			levelSelector = null;

		}
		else if (exitCode == 32 && screen != levelSelector && screen != loading) {
			canvas.setCam(camX,camY);
			setScreen(levelSelector);
			levelSelector.setActive(true);
			controllers[current].active = false;


		}
		else if (exitCode == 12 && screen != levelSelector && screen != loading) {

			if(!controllers[current].isWasCompleted()){
				levelsCompleted += 1;
				controllers[current].setWasCompleted(true);
				prefs.putFloat("completed", prefs.getFloat("completed")+1);
				System.out.println(prefs.getFloat("completed"));
				prefs.flush();
			}
			controllers[current].setWasCompleted(true);
			retryMenu = new RetryMenu(canvas,true);
			retryMenu.gatherAssets(directory);
			retryMenu.setScreenListener(this);
			canvas.setCam(camX,camY);
			setScreen(retryMenu);
			retryMenu.setActive(true);
			controllers[current].active = false;






		}else if (exitCode == 13 && screen != levelSelector && screen != loading) {

			retryMenu = new RetryMenu(canvas,false);
			retryMenu.gatherAssets(directory);
			retryMenu.setScreenListener(this);
			canvas.setCam(camX,camY);
			setScreen(retryMenu);
			retryMenu.setActive(true);
			controllers[current].active = false;






		}else if(screen == retryMenu && exitCode ==1){
			controllers[current].setScreenListener(this);
			controllers[current].setCanvas(canvas);
			controllers[current].reset();
			setScreen(controllers[current]);
			controllers[current].active = true;
			retryMenu.setActive(false);
			retryMenu.dispose();
			retryMenu = null;



		}else if(exitCode == 2 && screen == retryMenu){
			current = -1;
			levelSelector = new LevelSelector(canvas,levelsCompleted);
			levelSelector.gatherAssets(directory);
			levelSelector.setScreenListener(this);
			setScreen(levelSelector);
			levelSelector.setActive(true);
			retryMenu.setActive(false);
			retryMenu.dispose();
			retryMenu = null;

		}else if (exitCode == WorldController.EXIT_QUIT) {
			// We quit the main application
			Gdx.app.exit();
		}
	}

}