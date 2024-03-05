package edu.cornell.gdiac.physics;

import edu.cornell.gdiac.physics.gameobjects.entities.Enemy;
import edu.cornell.gdiac.physics.gameobjects.entities.Player;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.physics.shadows.ShadowController;

/**
 * The class collection of all existing GameObjects, used for various controllers and to be rendered to the scene
 */
public class GameScene {

	/**
	 * The single player object for the GameScene.
	 */
	private Player player = null;

	/**
	 * The collection of existing enemies currently in the GameScene
	 */
	private PooledList<Enemy> enemies = new PooledList<>();

	/**
	 * The collection of existing GameObjects currently in the GameScene
	 */
	private PooledList<Obstacle> objects = new PooledList<>();

	/**
	 * The collection of shadows in this GameScene
	 */
	private ShadowController shadows = null;

	/**
	 * Draws all of the existing scene objects to the provided GameCanvas
	 * @param canvas The GameCanvas to draw to
	 */
	public void draw(GameCanvas canvas) {
		if (player != null) {
			player.draw(canvas);
		}

		for (Enemy enemy : enemies) {
			enemy.draw(canvas);
		}

		for (Obstacle object : objects) {
			object.draw(canvas);
		}

		if (shadows != null) shadows.drawAllShadows(canvas);
	}

//	/**
//	 * The single instance of the GameScene. There can never (nor should ever be) another
//	 * instance of this object created.
//	 */
//	private static GameScene gameScene = null;
//
//	private GameScene() {
//
//	}
//
//	/**
//	 * Gets the singleton instance of GameScene
//	 * @return the singleton instance of GameScene
//	 */
//	public static GameScene getInstance() {
//		if (gameScene == null) {
//			gameScene = new GameScene();
//		}
//		return gameScene;
//	}
}
