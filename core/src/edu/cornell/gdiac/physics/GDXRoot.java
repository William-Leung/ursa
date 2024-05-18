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
import java.awt.EventQueue;
import java.awt.Frame;

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

	private HomeScreen homeScreen;
	/** Player mode for the game proper (CONTROLLER CLASS) */
	private int current;
	/** List of all WorldControllers */
	private WorldController[] controllers;
	private int threadNum = 0;


	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public GDXRoot() { }

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		prefs = Gdx.app.getPreferences("Completed Data");
		levelsCompleted = 20;
		canvas  = new GameCanvas();
		loading = new LoadingMode("assets.json",canvas,1);

		camX = canvas.getCameraX();
		camY = canvas.getCameraY();

		controllers = new WorldController[25];

		controllers[0] = new SceneModel("rigel_tutorial_1_Ver_W.json");
		controllers[1] = new SceneModel("rigel_tutorial_2_Ver_Z.json");
		controllers[2] = new SceneModel("rigel_tutorial_2.5_Ver_Z.json");
		controllers[3] = new SceneModel("rigel_tutorial_3_Ver_Z.json");
		controllers[4] = new SceneModel("rigel_tutorial_5_Ver_Z.json");
		controllers[5] = new SceneModel("rigel_tutorial_6_Ver_W.json");
		controllers[6] = new SceneModel("rigel_tutorial_7_Ver_Z.json");
		controllers[7] = new SceneModel("dylan_level_1_Ver_A.json");
		controllers[8] = new SceneModel("athena_level_Ver_A.json");
		controllers[9] = new SceneModel("rigel_tutorial_4_Ver_Z.json");
		controllers[10] = new SceneModel("danny_level_Ver_Z.json");
		controllers[11] = new SceneModel("dylan_level_2_Ver_Z.json");
		controllers[12] = new SceneModel("zekai_level_complete.json");
		controllers[13] = new SceneModel("rigel_level_hard_1_Ver_Z.json");
		controllers[14] = new SceneModel("yusuf_level_Ver_Z.json");


		for(int i = 0; i < prefs.getFloat("completed");i++){
			controllers[i].setWasCompleted(true);
		}

		current = 0;
		loading.setScreenListener(this);
		setScreen(loading);
	}

	private void shutdownApplication() {
		// Ensure all AWT components are disposed of
		EventQueue.invokeLater(() -> {
			// Close all frames
			Frame[] frames = Frame.getFrames();
			for (Frame frame : frames) {
				frame.dispose();
			}

			// Force terminate non-daemon threads if still running
			terminateNonDaemonThreads();

			// Attempt to explicitly exit
			System.exit(0);
		});
	}

	private void terminateNonDaemonThreads() {
		Thread.getAllStackTraces().keySet().forEach(thread -> {
			if (!thread.isDaemon() && thread != Thread.currentThread()) {
				threadNum++;
				try {
					thread.interrupt(); // Interrupt the thread
					thread.join(1000); // Wait for the thread to die
					if (thread.isAlive()) {
						System.err.println(
								"Thread " + thread.getName()
										+ " did not terminate, forcing exit");
					}
				} catch (InterruptedException e) {
					// Handle exception
				}
			}
		});
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

		if(canvas != null) {
			canvas.dispose();
		}
		canvas = null;

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
		boolean debug = false;
		if (screen == loading) {
			if(exitCode == 11) {
				dispose();
				shutdownApplication();
				Gdx.app.exit();
				return;
			}
			directory = loading.getAssets();
			if(debug) {
				levelSelector = new LevelSelector(canvas,levelsCompleted, 0);
				levelSelector.gatherAssets(directory);
				levelSelector.setScreenListener(this);
				setScreen(levelSelector);
				levelSelector.setActive(true);

				loading.dispose();
				loading = null;
			} else {
				// Go into the level select
				homeScreen = new HomeScreen(canvas, true);
				homeScreen.gatherAssets(directory);
				homeScreen.setScreenListener(this);
				setScreen(homeScreen);
				homeScreen.setActive(true);

				loading.dispose();
				loading = null;
			}
		} else if(screen == homeScreen) {
			if(exitCode == 11) {
				dispose();
				shutdownApplication();
				Gdx.app.exit();
			} else if(exitCode == 30) {
				levelSelector = new LevelSelector(canvas,levelsCompleted, 0);
				levelSelector.gatherAssets(directory);
				levelSelector.setScreenListener(this);
				setScreen(levelSelector);
				levelSelector.setActive(true);

				homeScreen.dispose();
				homeScreen = null;
			}
		} else if (screen == levelSelector) {
			if(exitCode == 111) {
				homeScreen = new HomeScreen(canvas, false);
				homeScreen.gatherAssets(directory);
				homeScreen.setScreenListener(this);
				setScreen(homeScreen);
				homeScreen.setActive(true);

				levelSelector.dispose();
				levelSelector = null;
				return;
			}
			// Enter the corresponding level from the level select
			for(int i = 1; i < 16; i++) {
				if(exitCode != i) {
					continue;
				}
				current = i - 1;

				controllers[current].gatherAssets(directory);
				controllers[current].setScreenListener(this);
				controllers[current].setCanvas(canvas);
				controllers[current].reset();
				setScreen(controllers[current]);
				controllers[current].active = true;

				levelSelector.setActive(false);
				levelSelector.dispose();
				levelSelector = null;
				break;
			}
		} else if(screen == retryMenu) {
			if (exitCode == 1) {
				// Go back into the level
				controllers[current].setScreenListener(this);
				controllers[current].setCanvas(canvas);
				controllers[current].reset();
				setScreen(controllers[current]);
				controllers[current].active = true;
			} else if (exitCode == 2) {
				// Go to the level selector
				levelSelector = new LevelSelector(canvas, levelsCompleted, current);
				current = -1;
				levelSelector.gatherAssets(directory);
				levelSelector.setScreenListener(this);
				setScreen(levelSelector);
				levelSelector.setActive(true);
			}
			else if(exitCode == 3){
				current = Math.min(current + 1, 14);

				controllers[current].gatherAssets(directory);
				controllers[current].setScreenListener(this);
				controllers[current].setCanvas(canvas);
				controllers[current].reset();
				setScreen(controllers[current]);
				controllers[current].active = true;



			}
			retryMenu.dispose();
			retryMenu.setActive(false);

			retryMenu = null;
		} else if (exitCode == WorldController.LEVEL_COMPLETE) {
			if(!controllers[current].wasCompleted()){
				levelsCompleted += 1;
				controllers[current].setWasCompleted(true);
				prefs.putFloat("completed", prefs.getFloat("completed")+1);
				prefs.flush();
			}
			// Create the retry menu where we've won
			controllers[current].setWasCompleted(true);
			retryMenu = new RetryMenu(canvas,true);
			retryMenu.gatherAssets(directory);
			retryMenu.setScreenListener(this);
			setScreen(retryMenu);
			retryMenu.setActive(true);
			controllers[0].setScreenListener(null);
			canvas.setCam(camX,camY);
			controllers[current].active = false;
		} else if (exitCode == WorldController.LEVEL_FAILED) {
			// Create the retry menu where we've lost
			retryMenu = new RetryMenu(canvas,false);
			retryMenu.gatherAssets(directory);
			retryMenu.setScreenListener(this);
			retryMenu.setActive(true);
			setScreen(retryMenu);
			controllers[0].setScreenListener(null);
			canvas.setCam(camX,camY);
			controllers[current].active = false;
		} else if (exitCode == WorldController.EXIT_QUIT) {
			levelSelector = new LevelSelector(canvas,levelsCompleted, current);
			canvas.setCam(camX,camY);
			levelSelector.gatherAssets(directory);
			levelSelector.setScreenListener(this);
			setScreen(levelSelector);
			levelSelector.setActive(true);
			controllers[current].active = false;
		}
	}

}