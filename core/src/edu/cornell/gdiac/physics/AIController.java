package edu.cornell.gdiac.physics;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.player.UrsaModel;
import java.util.ArrayDeque;
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
        /** Enemy is chasing the player */
        CHASE,
        /** Enemy is attacking the player */
        ATTACK,
        /** Enemy has caught the player and has won */
        WON,
        /** Enemy is currently stunned */
        STUNNED,
    }

    /**  */

    // Constants for chase algorithms
    /** Time when enemy spawns where they cannot do anything */
    private static final int SPAWN_TICKS = 30;
    /** Time in range before changing confused to attack */
    private static final int CONFUSE_TIME = 20;
    /** ticks enemy will stay engaged in if chasing but not in cone */
    private static final int CHASE_MEMORY = 40;
    /** Amount of uninterrupted time player can spend in cone before losing */
    private static final int LIFE_TIME = 120;


    /** Degrees enemy can rotate per tick */
    private static final int ROTATE_SPEED = 12;
    /** Distance from goal enemy needs to get to */
    private static final float GOAL_DIST = 30f;
    /** Distance from enemy that if player is within, they lose. */
    private static final float COLLISION_ERROR = 0f;
    /** Distance that enemy can detect a player regardless of where they are facing */
    private static final float ENEMY_RADIUS = 0f;

    // Instance Attributes
    /** The enemy being controlled by this AIController */
    private Enemy enemy;
    /** The player being targeted */
    private UrsaModel ursa;
    /** The enemy's current state in the FSM */
    private FSMState state;
    /** The number of ticks since we started this controller  */
    private long ticks;
    /** Determines whether this enemy has won and caught Ursa */
    private boolean hasWon = false;

    /** Ticks spent confused */
    private int ticks_confused = 0;
    /** Ticks ursa has spent in the cone */
    private int ticks_attacked = 0;
    /** Last location where this enemy detected the player */
    private Vector2 lastDetection = null;
    /** Last time ursa was detected */
    private long last_time_detected = 0;

    private int confused_anim_index = 0;

    /** The goal angle to face, used to prevent cone snapping */
    private float goalAngle = 0;
    /** The list of patrol tiles this enemy will visit if wandering */
    private ArrayDeque<Vector2> goalLocs;
    /** Current goal for the enemy */
    private Vector2 currGoal;

    /**
     * Creates an AIController for the ship with the given id.
     */
    public AIController(Enemy enemy,  UrsaModel ursa) {
        this.enemy = enemy;
        this.ursa = ursa;

        state = FSMState.SPAWN;
        ticks = 0;

        // add goal locs to player's deque
        goalLocs = new ArrayDeque<>();

        goalLocs.addLast(new Vector2(100f, 100f));
        currGoal = new Vector2(enemy.getX() - 40, enemy.getY() - 40);

    }

    public void reset() {
        hasWon = false;
        state = FSMState.SPAWN;
        ticks_confused = 0;
        ticks_attacked = 0;
    }


    private void changeStateIfApplicable() {
        switch(state) {
            case SPAWN:
                if (ticks < SPAWN_TICKS) {
                    state = FSMState.SPAWN;
                } else {
                    if (isDetected()) {
                        if (lastDetection == null) {
                            lastDetection = new Vector2(ursa.getX(), ursa.getY());
                        } else {
                            lastDetection.x = ursa.getX();
                            lastDetection.y = ursa.getY();
                        }
                        state = FSMState.CONFUSED;
                        last_time_detected = ticks;
                        ticks_confused = 1;
                    } else {
                        state = FSMState.WANDER;
                    }
                }

                break;

            case WANDER:
                if (isDetected()) {
                    if (lastDetection == null) {
                        lastDetection = new Vector2(ursa.getX(), ursa.getY());
                    } else {
                        lastDetection.x = ursa.getX();
                        lastDetection.y = ursa.getY();
                        last_time_detected = ticks;
                    }
                    state = FSMState.CONFUSED;
                    ticks_confused = 1;
                } else {
                    state = FSMState.WANDER;
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                }

                break;

            case CONFUSED:

                if (ticks_confused == 0) {
                    state = FSMState.WANDER;
                } else if (ticks_confused >= CONFUSE_TIME) {
                    state = FSMState.CHASE;
                } else if (isDetected()) {
                    ticks_confused++;
                    lastDetection.x = ursa.getX();
                    lastDetection.y = ursa.getY();
                    last_time_detected = ticks;
                    state = FSMState.CONFUSED;
                } else {
                    ticks_confused--;
                    state = FSMState.CONFUSED;
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                }

                break;

            case CHASE:

                if (isDetected()) {
                    last_time_detected = ticks;
                    lastDetection.x = ursa.getX();
                    lastDetection.y = ursa.getY();
                    if (enemy.isAlerted()) {
                        state = FSMState.ATTACK;
                    } else {
                        state = FSMState.CHASE;
                    }
                } else {
                    if (ticks - last_time_detected >= CHASE_MEMORY) {
                        state = FSMState.CONFUSED;
                    }
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                }

                break;

            case ATTACK:
                if (ticks_attacked >= LIFE_TIME /*|| checkRange(null)*/) {
                    state = FSMState.WON;
                } else if (ticks_attacked == 0) {
                    state = FSMState.CHASE;
                } else if (enemy.isAlerted()) {
                    last_time_detected = ticks;
                    lastDetection.x = ursa.getX();
                    lastDetection.y = ursa.getY();
                    ticks_attacked++;
                    state = FSMState.ATTACK;
                } else {
                    ticks_attacked--;
                    state = FSMState.ATTACK;
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                }

                break;

            case WON:
                state = FSMState.WON;
                break;

            case STUNNED:

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                } else {
                    state = FSMState.SPAWN;
                }

        }
    }

    public void update() {
        //enemy.applyForce();
    }

    public void getAction() {
        ticks++;

        Vector2 action = new Vector2();

        changeStateIfApplicable();
        System.out.println(state.toString());

        //if (checkRange()) return;

        switch (state) {
            case SPAWN:
                enemy.setVX(0);
                enemy.setVY(0);
                break;
            case WANDER:

                if (enemy.getX() >= currGoal.x - GOAL_DIST && enemy.getX() <= currGoal.x + GOAL_DIST &&
                        enemy.getY() >= currGoal.y - GOAL_DIST && enemy.getY() <= currGoal.y + GOAL_DIST) {
                    // they reached the goal, so add curr goal to end and make next goal the first in deque
                    goalLocs.addLast(currGoal);
                    currGoal = goalLocs.pop();
                    System.out.println("Goal reached");
                } else { // move enemy in direction of goal
                    action.x = currGoal.x - enemy.getX();
                    action.y = currGoal.y - enemy.getY();
                    action = action.nor();

                    enemy.setVX(action.x * 3);
                    enemy.setVY(action.y * 3);
                    enemy.setLookDirection(action.x, action.y);

                }


                break;

            case CONFUSED:
                System.out.println("Enemy is confused");
                enemy.setVX(0);
                enemy.setVY(0);

                break;
            case CHASE:
                System.out.println("CHASE");
                System.out.println("CHASE");
                System.out.println("CHASE");
                System.out.println("CHASE");
                System.out.println("CHASE");

                action.x = lastDetection.x - enemy.getX();
                action.y = lastDetection.y - enemy.getY();

                //int multiplier = curr_stamina != 0 ? 4 : 2;
                //curr_stamina = Math.max(curr_stamina - 1, 0);

                enemy.setVX(action.x * 2);
                enemy.setVY(action.y * 2);
                enemy.setLookDirection(action.x, action.y);

                break;

            case ATTACK:
                System.out.println("ATTACK");
                System.out.println("ATTACK");
                System.out.println("ATTACK");
                System.out.println("ATTACK");
                System.out.println("ATTACK");

                action.x = ursa.getX() - enemy.getX();
                action.y = ursa.getY() - enemy.getY();

                //int multiplier = curr_stamina != 0 ? 4 : 2;
                //curr_stamina = Math.max(curr_stamina - 1, 0);

                enemy.setVX(action.x * 2);
                enemy.setVY(action.y * 2);
                enemy.setLookDirection(action.x, action.y);

            case WON:
                System.out.println("WON");
                System.out.println("WON");
                System.out.println("WON");
                System.out.println("WON");
                System.out.println("WON");


                enemy.setVX(0);
                enemy.setVY(0);
                rotateEnemy(ROTATE_SPEED, enemy.getAngle() + ROTATE_SPEED);

                //enemy.setAngle(enemy.getAngle() + (360 / enemy.getMaxStun()));

                break;

            case STUNNED:

                rotateEnemy(360 / enemy.getMaxStun(), enemy.getAngle() + ROTATE_SPEED);
                if (ticks % 20 == 0) {
                    enemy.setVX(7 * (float) Math.random());
                    enemy.setVY(7 * (float) Math.random());
                }
        }
    }



    public Enemy getEnemy() { return enemy; }

    public boolean isWon() { return state == FSMState.WON; }

    //public void setWon(boolean value) { hasWon = value; }

    public void resetLifeTime() { ticks_attacked = 0; }

    public boolean checkRange(Float range) {

        if (range == null) {
            range = COLLISION_ERROR;
        }

        boolean checkX = Math.abs(ursa.getX() - enemy.getX()) <=
                (ursa.getWidth() + enemy.getWidth()) / 2 + range;
        boolean checkY = Math.abs(ursa.getY() - enemy.getY()) <=
                (ursa.getHeight() + enemy.getHeight()) / 2 + range;

        return checkX || checkY;

    }



    public void rotateEnemy(float rotSpeed, float goalAngle) {
        if (enemy.getAngle() < goalAngle) {
            enemy.rotateLookDirection(rotSpeed);
        } else if (enemy.getAngle() > goalAngle) {
            enemy.rotateLookDirection(-rotSpeed);
        }
    }

    private boolean isDetected() {
        return checkRange(ENEMY_RADIUS) || enemy.isAlerted();
    }

    public boolean isConfused() {
        return state == FSMState.CONFUSED;
    }

    public boolean isStunned() {
        return state == FSMState.STUNNED;
    }

    public int get_confused_anim_index() {
        return confused_anim_index;
    }

    public int inc_anim_index() { return confused_anim_index = (confused_anim_index + 1) % 30; }

    public void reset_anim_index() { confused_anim_index = 0; }

}
