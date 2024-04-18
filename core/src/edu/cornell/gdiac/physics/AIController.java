package edu.cornell.gdiac.physics;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.player.UrsaModel;
import edu.cornell.gdiac.physics.objects.Tree;
import edu.cornell.gdiac.util.PooledList;
import java.util.ArrayDeque;

public class AIController {

    /**
     * Enumeration to encode the FSM
     */
    private static enum FSMState {
        /** The enemy just spawned in */
        SPAWN,
        /** The enemy is patrolling around without a target */
        WANDER,
        /** The enemy is looking around */
        LOOKING,
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
    private static final int CONFUSE_TIME = 50;
    /** ticks enemy will stay engaged in if chasing but not in cone */
    private static final int CHASE_MEMORY = 40;
    /** Amount of uninterrupted time player can spend in cone before losing */
    private static final int LIFE_TIME = 500;
    /** Maximum amount of time enemy can spend looking around in one time */
    private static final float MAX_LOOKING = 500;
    /** Ticks Ursa can be within range before being detected */
    private static final int DETECTION_DELAY = 20;


    /** Degrees enemy can rotate per tick */
    private static final int ROTATE_SPEED = 12;
    /** Distance from goal enemy needs to get to */
    private static final float GOAL_DIST = 2f;
    /** Distance from enemy that if player is within, they lose. */
    private static final float COLLISION_ERROR = 0f;
    /** Distance that enemy can detect a player regardless of where they are facing */
    private static final float ENEMY_RADIUS = 2f;
    /** Distance that if chasing the player, the enemy will still attack them */
    private static final float CHASE_RADIUS = 2f;

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
    /** Vector representing this enemy's next move */
    private Vector2 action = new Vector2();
    /** Vector of previous location of this enemy */
    private Vector2 prevLoc;
    /** Trees on the map */
    private PooledList<Tree> trees;

    /** Ticks spent confused */
    private int ticks_confused = 0;
    /** Ticks ursa has spent in the cone */
    private int ticks_attacked = 0;
    /** Ticks this enemy has spent looking around */
    private int ticks_looking = 0;
    /** Last location where this enemy detected the player */
    private Vector2 lastDetection = null;
    /** Last time ursa was detected */
    private long last_time_detected = 0;
    /** Ticks spent detected (consecutive) */
    private int ticks_detected = 0;

    private int confused_anim_index = 0;

    /** The goal angle to face, used to prevent cone snapping */
    private float goalAngle = 0;
    /** Whether the enemy is currently looking around randomly */
    private boolean isLooking = false;
    /** The list of patrol tiles this enemy will visit if wandering */
    private ArrayDeque<Vector2> goalLocs;
    /** Current goal for the enemy */
    private Vector2 currGoal;

    private boolean turnAround = false;
    private Vector2 otherDir = new Vector2();

    /**
     * Creates an AIController for the ship with the given id.
     */
    public AIController(Enemy enemy,  UrsaModel ursa, PooledList<Tree> trees, Vector2[] patrolLocs) {
        this.enemy = enemy;
        this.ursa = ursa;
        this.trees = trees;
        this.prevLoc = new Vector2(enemy.getX(), enemy.getY());

        state = FSMState.SPAWN;
        ticks = 0;

        // add goal locs to player's deque
        goalLocs = new ArrayDeque<>();
        for(Vector2 v: patrolLocs) {
            if(v != null) {
                goalLocs.addLast(v);
            }
        }
        currGoal = goalLocs.peek();

    }

    public void reset() {
        hasWon = false;
        state = FSMState.SPAWN;
        ticks_confused = 0;
        ticks_attacked = 0;
    }


    private void changeStateIfApplicable() {

        if (isDetected()) {
            ticks_detected++;
        } else {
            ticks_detected = 0;
        }

        System.out.println(state);
        switch(state) {
            case SPAWN:
                if (ticks < SPAWN_TICKS) {
                    state = FSMState.SPAWN;
                } else {
                    if (ticks_detected >= DETECTION_DELAY) {
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
                        state = FSMState.LOOKING;
                    }
                }

                break;

            case WANDER:

                if (ticks % 50 == 0 && Math.random() > 0.95) {
                    ticks_looking = 0;
                    state = FSMState.LOOKING;
                } else if (ticks_detected >= DETECTION_DELAY) {
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

            case LOOKING:

                ticks_looking++;

                if (ticks_detected >= DETECTION_DELAY) {

                    if (lastDetection == null) {
                        lastDetection = new Vector2(ursa.getX(), ursa.getY());
                    } else {
                        lastDetection.x = ursa.getX();
                        lastDetection.y = ursa.getY();
                        last_time_detected = ticks;
                    }

                    state = FSMState.CHASE;
                    ticks_looking = 0;
                } else if (ticks_looking >= MAX_LOOKING || Math.random() * ticks_looking > MAX_LOOKING / 3) {
                    state = FSMState.WANDER;
                }

                break;

            case CONFUSED:

                if (ticks_confused == 0) {
                    state = FSMState.LOOKING;
                } else if (ticks_confused >= CONFUSE_TIME && isDetected()) {
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
                        ticks_attacked = 1;
                    } else {
                        state = FSMState.CHASE;
                    }
                } else if (checkRange(CHASE_RADIUS)){
                    state = FSMState.CHASE;
                } else {
                    if (ticks - last_time_detected >= CHASE_MEMORY) {
                        state = FSMState.LOOKING;
                    }
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                }

                break;

            case ATTACK:
                if (ticks_attacked >= LIFE_TIME || checkRange(null)) {
                    state = FSMState.WON;
                } else if (enemy.isAlerted() || checkRange(ENEMY_RADIUS)) {
                    last_time_detected = ticks;
                    lastDetection.x = ursa.getX();
                    lastDetection.y = ursa.getY();
                    ticks_attacked++;
                    state = FSMState.ATTACK;
                } else {
                    ticks_attacked--;
                    state = FSMState.CHASE;
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

        changeStateIfApplicable();
        //System.out.println(state.toString());

        prevLoc.x = enemy.getX();
        prevLoc.y = enemy.getY();

        // update stunned
        if (state == FSMState.STUNNED) {
            enemy.setStunned(true);
        } else enemy.setStunned(false);

        switch (state) {
            case SPAWN:
                enemy.setVX(0);
                enemy.setVY(0);
                break;
            case WANDER:

               /**for (Tree t : trees) {
                   if (Math.abs(t.getY() - enemy.getY()) <= t.getHeight() / 2 + enemy.getHeight() / 2 ) {
                       if (t.getY() > enemy.getY()) {
                           enemy.setVY(-2);
                       } else {
                           enemy.setVY(2);
                       }
                       return;
                   }

//                   if (Math.abs(t.getX() - enemy.getX()) <= t.getWidth() / 2 + enemy.getWidth() / 2 ) {
//                       if (Math.random() < 0.5) {
//                           enemy.setVY(1);
//                       } else {
//                           enemy.setVY(-1);
//                       }
//                       return;
//                   }
               }*/

//                if (Math.abs(prevLoc.x - enemy.getX()) <= 0.5) {
//                    enemy.setY(enemy.getY() - 100);
//                    System.out.println("teleporting down");
//                }
//
//                if (Math.abs(prevLoc.x - enemy.getX()) <= 0.5) {
//                    enemy.setY(enemy.getY() + 100);
//                    System.out.println("teleporting up");
//                }

//               if (turnAround) {
//                   System.out.println("turning around");
//                   if (Math.abs(enemy.getVY()) == 0) {
//                       if (Math.abs(prevLoc.x - enemy.getX()) <= 0.5 && Math.abs(prevLoc.x - enemy.getY()) <= 0.5) {
//                           enemy.setVY(1000);
//                       } else {
//                           enemy.setVY(-10000);
//                       }
//                   } //else enemy.setVY(-enemy.getVY());
//
//                   if (Math.random() > 0.9) turnAround = false;
//
//               }

               if (enemy.getX() >= currGoal.x - GOAL_DIST && enemy.getX() <= currGoal.x + GOAL_DIST /*||
                       Math.abs(prevLoc.x - enemy.getX()) <= 0.5 && Math.abs(prevLoc.x - enemy.getY()) <= 0.5*/) {
                   // they reached the goal, so add curr goal to end and make next goal the first in deque
                   goalLocs.addLast(currGoal);
                   currGoal = goalLocs.pop();
               } else { // move enemy in direction of goal
                   action.x = currGoal.x - enemy.getX();
                   action.y = currGoal.y - enemy.getY();
                   action = action.nor();

                   enemy.setVX(action.x * 3);
                   enemy.setVY(action.y * 3);
                   enemy.setLookDirection(action.x, action.y);

               }



                break;

            case LOOKING:

                enemy.setVX(0);
                enemy.setVY(0);

                if (ticks_looking > CONFUSE_TIME) {
                    if (enemy.getAngle() >= goalAngle - ROTATE_SPEED && enemy.getAngle() <= goalAngle + ROTATE_SPEED) {
                        goalAngle = enemy.getAngle() - 30 + (float) (Math.random() * 60);
                    }

                    rotateEnemy((float) Math.random() * 4, goalAngle);
                }


                break;

            case CONFUSED:
                //System.out.println("Enemy is confused");
                enemy.setVX(0);
                enemy.setVY(0);

                break;
            case CHASE:

                //System.out.println("CHASE");
                action.x = lastDetection.x - enemy.getX();
                action.y = lastDetection.y - enemy.getY();
                action = action.nor();

                enemy.setVX(action.x * 5);
                enemy.setVY(action.y * 5);
                enemy.setLookDirection(action.x, action.y);

                break;

            case ATTACK:
                //System.out.println("ATTACK");
                action.x = ursa.getX() - enemy.getX();
                action.y = ursa.getY() - enemy.getY();
                action = action.nor();

                enemy.setVX(action.x * 7);
                enemy.setVY(action.y * 7);
                enemy.setLookDirection(action.x, action.y);

                break;

            case WON:

                enemy.setVX(0);
                enemy.setVY(0);
                enemy.setLookDirection(ursa.getX() - enemy.getX(), ursa.getY() - enemy.getY());
                //rotateEnemy(ROTATE_SPEED, enemy.getAngle() + ROTATE_SPEED);

                //enemy.setAngle(enemy.getAngle() + (360 / enemy.getMaxStun()));

                break;

            case STUNNED:

                rotateEnemy(360 / enemy.getMaxStun(), enemy.getAngle() + ROTATE_SPEED);
                //enemy.setVX(0);
                //enemy.setVY(0);
                if (ticks % 20 == 0) {
                    enemy.setVX(4 * (float) Math.random());
                    enemy.setVY(4 * (float) Math.random());
                }

                break;
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

//        boolean checkX = Math.abs(ursa.getX() - enemy.getX()) <=
//                ENEMY_RADIUS + range;
//        boolean checkY = Math.abs(ursa.getY() - enemy.getY()) <=
//                ENEMY_RADIUS + range;
//
//        return checkX || checkY;

        return Math.sqrt(Math.pow(ursa.getX() - enemy.getX(), 2) +
                Math.pow(ursa.getY() - enemy.getY(), 2)) <= ENEMY_RADIUS + range;

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
        return (state == FSMState.CONFUSED);
    }

    public boolean isStunned() {
        return (state == FSMState.STUNNED);
    }

    public boolean isChase() { return state == FSMState.CHASE; }

    public int get_confused_anim_index() {
        return confused_anim_index;
    }

    public int inc_anim_index() {
        if (isStunned()) {
            return confused_anim_index = (confused_anim_index + 1) % 30;
        }
        return confused_anim_index = Math.min(confused_anim_index + 1, 24);
    }

    public void reset_anim_index() { confused_anim_index = 0; }

//    public void lookAround() {
//        if (goalAngle == 0 || enemy.getAngle() == goalAngle) {
//            goalAngle = enemy.getAngle() - (ticks_wander / 2) + (float) (Math.random() * ticks_wander);
//        }
//
//        rotateEnemy(Math.abs(goalAngle - enemy.getAngle()) / 20, goalAngle);
//    }

    public boolean isSurprised() {
        return isDetected() && state == FSMState.ATTACK && ticks_attacked <= 20;
    }

}
