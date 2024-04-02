package edu.cornell.gdiac.physics;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.player.UrsaModel;
import java.util.HashMap;
import java.util.List;

public class AIController {

    /**
     * Enumeration to encode the FSM
     */
    private static enum FSMState {
        /** The enemy just spawned in */
        SPAWN,
        /** The enemy is patrolling around without a target */
        WANDER,
        /** Enemy has seen/heard something but hasn't been triggered yet. */
        CONFUSED,
        /** Enemy knows you're there, preparing to chase/attack */
        ALERT,
        /** Enemy is chasing the player */
        CHASE,
        /** Enemy is attacking the player */
        ATTACK,
        /** Enemy has been stunned and can not move */
        STUN
    }

    /**  */

    // Constants for chase algorithms
    /** Time in range before changing confused to attack */
    private static final int CONFUSE_TIME = 20;
    /** Time enemy will be stunned for */
    private static final int STUN_TIME = 30;
    /** Range enemy will stay engaged in if chasing but not in cone */
    private static final int MEMORY_RANGE = 4;
    /** Amount of uninterrupted time player can spend in cone before losing */
    private static final int LIFE_TIME = 120;
    /** Amount of time between wandering direction change */
    private static final int DIR_TIME = 70;
    /** Amount of time enemy can sprint */
    private static final int MAX_STAMINA = 70;
    /** Degrees enemy can rotate per tick */
    private static final int ROTATE_SPEED = 12;

    // Instance Attributes
    /** The enemy being controlled by this AIController */
    private Enemy enemy;
    /** The player being targeted */
    private UrsaModel ursa;
    /** The enemy's current state in the FSM */
    private FSMState state;
    /** world */
    private WorldController world;
    /** The ship's next action (may include firing). */
    private int move; // A ControlCode
    /** Ticks spent confused */
    private int ticks_confused = 0;
    /** Ticks spent stunned */
    private int ticks_stunned = 0;
    /** Ticks spent in range of attack */
    private int ticks_attacked = 0;
    /** Amount of "sprint" enemy has */
    private int curr_stamina = MAX_STAMINA;
    /** Ticks since last wander dir change */
    private int ticks_since_change = 0;
    /** The number of ticks since we started this controller  */
    private long ticks;
    /** Determines whether this enemy has won and caught Ursa */
    private boolean hasWon = false;
    /** The goal angle to face, used to prevent cone snapping */
    private float goalAngle = 0;
    /** The list of patrol tiles this enemy will visit if wandering */
    private List<Float> goalLocs;

    /**
     * Creates an AIController for the ship with the given id.
     */
    public AIController(Enemy enemy,  UrsaModel ursa) {
        this.enemy = enemy;
        //this.board = board;
        this.ursa = ursa;
        //this.world = world;

        state = FSMState.SPAWN;
        //move  = CONTROL_NO_ACTION;
        ticks = 0;

    }

    public void reset() {
        hasWon = false;
        state = FSMState.SPAWN;
        ticks_attacked = 0;
        ticks_confused = 0;
        ticks_stunned = 0;
        ticks_since_change = 0;
        curr_stamina = MAX_STAMINA;
    }


    private void changeStateIfApplicable() {
        switch(state) {
            case SPAWN:
                if (ticks > CONFUSE_TIME) state = FSMState.WANDER;

                break;

            case WANDER:
                if (enemy.isAlerted()) {
                    state = FSMState.CONFUSED;
                }

                break;

            case CONFUSED:
                if (enemy.isAlerted() && ticks_confused < CONFUSE_TIME) {
                    state = FSMState.CONFUSED;
                    ticks_confused++;
                } else if (enemy.isAlerted()) {
                    state = FSMState.CHASE;
                    ticks_confused = 0;
                } else {
                    state = FSMState.WANDER;
                    ticks_confused = 0;
                }

                break;
            case ALERT:

                break;
            case CHASE:
                if (ticks_attacked >= LIFE_TIME ) {
                    state = FSMState.CHASE;
                    hasWon = true;
                }
                if (enemy.isAlerted()) {
                    state = FSMState.CHASE;
                    ticks_attacked++;
                } else {
                    if (ticks_confused < CONFUSE_TIME) {
                        ticks_confused++;
                        ticks_attacked = 0;
                        state = FSMState.CHASE;
                    } else {
                        ticks_confused = 0;
                        state = FSMState.WANDER;
                    }
                }

                break;

            case ATTACK:

                break;

            case STUN:

                break;

        }
    }

    public void update() {
        //enemy.applyForce();
    }

    public void getAction() {
        ticks++;

        changeStateIfApplicable();
        checkRange();
        //System.out.println(state.toString());

        switch (state) {
            case SPAWN:
                enemy.setVX(0);
                enemy.setVY(0);
                break;
            case WANDER:
                curr_stamina = Math.min(MAX_STAMINA, curr_stamina + 1);

                break;

            case CONFUSED:
                //System.out.println("Enemy is confused");
                enemy.setVX(0);
                enemy.setVY(0);
                curr_stamina = Math.min(MAX_STAMINA, curr_stamina + 1);
                break;
            case CHASE:
                Vector2 action =  new Vector2(ursa.getX() - enemy.getX(), ursa.getY() - enemy.getY()).nor();

                //int multiplier = curr_stamina != 0 ? 4 : 2;
                //curr_stamina = Math.max(curr_stamina - 1, 0);

                enemy.setVX(action.x * 8);
                enemy.setVY(action.y * 8);
                enemy.setLookDirection(action.x, action.y);

                break;
        }
    }



    public Enemy getEnemy() { return enemy; }

    public boolean isWon() { return hasWon; }

    public void setWon(boolean value) { hasWon = value; }

    public void resetLifeTime() { ticks_attacked = 0; }

    public void checkRange() {
        if (Math.pow(enemy.getX() - ursa.getX(), 2) + Math.pow(enemy.getY() - ursa.getY(), 2)
                <= enemy.getHeight() + ursa.getHeight()) {

            //enemy.setLookDirection(ursa.getY() - enemy.getY(), ursa.getY() - enemy.getY());

            float goalAngle = (float) Math.atan(ursa.getY() - enemy.getY() / ursa.getY() - enemy.getY());
            if (enemy.isAlerted()) {
                setWon(true);
            } else {
                rotateEnemy(ROTATE_SPEED, goalAngle);
            }

        }
    }


    public void rotateEnemy(float rotSpeed, float goalAngle) {
        if (enemy.getAngle() < goalAngle) {
            enemy.rotateLookDirection(rotSpeed);
        } else if (enemy.getAngle() > goalAngle) {
            enemy.rotateLookDirection(-rotSpeed);
        }
    }

    private class Board {
        /** grid to store whether an object exists in any location */
        private boolean[][] grid;
        /** The height of each grid box */
        float tileHeight;
        /** The width of each grid box */
        float tileWidth;
        /** the height of the canvas */
        float canvasHeight;
        /** The width of the canvas */
        float canvasWidth;

        public Board(WorldController world, float enemyHeight, float enemyWidth) {
            this.canvasHeight = world.canvas.height;
            this.canvasWidth = world.canvas.width;

            tileHeight = enemyHeight;
            tileWidth = enemyWidth;

            grid = new boolean[(int) (canvasHeight / tileHeight) + 1][(int) (canvasWidth / tileWidth) + 1];

            // fill in every grid slot with false
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    grid[i][j] = false;
                }
            }
        }

        public void initBoard() {
            for (Obstacle o : world.objects) {
                grid[getXCell(o.getX())][getYCell(o.getY())] = true;
            }
        }

        public int getXCell(float x_loc) {
            if (x_loc < 0) return 0;
            return (int) Math.min((int) x_loc / tileWidth, grid[0].length);
        }

        public int getYCell(float y_loc) {
            if (y_loc < 0) return 0;
            return (int) Math.min((int) y_loc / tileHeight, grid.length);
        }
    }
}
