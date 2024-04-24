package edu.cornell.gdiac.physics.pathing;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.player.UrsaModel;
import edu.cornell.gdiac.physics.objects.Tree;
import edu.cornell.gdiac.util.PooledList;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

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
    private static final int CONFUSE_TIME = 30;
    /** ticks enemy will stay engaged in if chasing but not in cone */
    private static final int CHASE_MEMORY = 80;
    /** Amount of uninterrupted time player can spend in cone before losing */
    private static final int LIFE_TIME = 500;
    /** Maximum amount of time enemy can spend looking around in one time */
    private static final float MAX_LOOKING = 700;
    /** Ticks Ursa can be within range before being detected */
    private static final int DETECTION_DELAY = 20;


    /** Degrees enemy can rotate per tick */
    private static final int ROTATE_SPEED = 12;
    /** Distance from goal enemy needs to get to */
    private static final float GOAL_DIST = 2f;
    /** Distance from enemy that if player is within, they lose. */
    private static final float COLLISION_ERROR = 1.5f;
    /** Distance that enemy can detect a player regardless of where they are facing */
    private static final float ENEMY_RADIUS = 3f;
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

    /** Last locations where Ursa was spotted */
    private ArrayDeque<Vector2> locs_spotted;
    /** Maximum number of locations where ursa was spotted an enemy can detect */
    private static final int MAX_SPOTTED = 5;
    /** Amount of times Ursa must be spotted before enemies start patrolling her locations */
    private static final int MIN_PATROL_CHANGE = 3;
    /** Amount of time since the last spotting that enemies will continue patrolling old locations */
    private static final int ADAPTIVE_AI_MEM = 500;

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
    /** Ticks spent spotted (consecutive) */
    private int ticks_spotted = 0;
    /** Ticks spent colliding (consecutive) */
    private int ticks_collided = 0;

    private int confused_anim_index = 0;

    /* WANDER STATE DATA STRUCTURES */
    LinkedList<Vector2> queue;
    HashMap<Vector2, Vector2> backpack;
    LinkedList<Vector2> adjacents;
    boolean[][] isAdded;


    /** The goal angle to face, used to prevent cone snapping */
    private float goalAngle = 0;
    /** Whether the enemy is currently looking around randomly */
    private boolean isLooking = false;
    /** The list of patrol tiles this enemy will visit if wandering */
    private ArrayDeque<Vector2> goalLocs;
    /** Current goal for the enemy */
    private Vector2 currGoal;

    private int times_detected;

    private Vector2 firstGoal;

    private boolean isStupid = false;

    private Board board;

    private Vector2 startLoc;

    /**
     * Creates an AIController for the ship with the given id.
     */
    public AIController(Enemy enemy,  UrsaModel ursa, Board board, Vector2[] patrolLocs) {
        this.enemy = enemy;
        this.ursa = ursa;
        //this.prevLoc = new Vector2(enemy.getX(), enemy.getY());
        this.board = board;

        state = FSMState.SPAWN;
        ticks = 0;
        this.times_detected = 0;
        locs_spotted  = new ArrayDeque<>();

        // add goal locs to player's deque
        goalLocs = new ArrayDeque<>();
        for(Vector2 v: patrolLocs) {
            if(v != null) {
                goalLocs.addLast(v);
            }
        }
        currGoal = goalLocs.peek();
        firstGoal = currGoal;

        queue = new LinkedList<>();
        backpack = new HashMap<>();

        adjacents = new LinkedList<>();

        startLoc = new Vector2(enemy.getX(), enemy.getY());
        //isAdded = new boolean[board.width()][board.height()];
    }

    public AIController(Enemy enemy,  UrsaModel ursa, Board board, Vector2[] patrolLocs, boolean stupid) {
        this(enemy, ursa, board, patrolLocs);
        isStupid = false;
    }


    public void setGameBoard(Board b) { this.board = b; }


    public void reset() {
        hasWon = false;
        state = FSMState.SPAWN;
        ticks_confused = 0;
        ticks_attacked = 0;
        currGoal = firstGoal;
        times_detected = 0;
        enemy.setX(startLoc.x);
        enemy.setY(startLoc.y);
    }

    public ArrayDeque<Vector2> getPatrol() {
        return goalLocs;
    }

    private void changeStateIfApplicable() {

        if (isDetected()) {
            ticks_detected++;
        } else {
            ticks_detected = 0;
        }

        if (ticks_detected == DETECTION_DELAY) times_detected++;

        if (enemy.isAlerted()) {
            ticks_spotted++;
        } else {
            ticks_spotted = 0;
        }

        if (checkRange(null)) state = FSMState.ATTACK;

        switch(state) {
            case SPAWN:
                if (ticks < SPAWN_TICKS) {
                    state = FSMState.SPAWN;
                } else {
                    if (ticks_detected >= DETECTION_DELAY) {
                        state = FSMState.CONFUSED;
                        last_time_detected = ticks;
                        ticks_confused = 1;
                    } else {
                        state = FSMState.LOOKING;
                        ticks_looking = 26;
                    }
                }

                break;

            case WANDER:

                if (ticks % 50 == 0 && Math.random() > 0.95) {
                    ticks_looking = 26;
                    state = FSMState.LOOKING;
                } else if (times_detected >= MIN_PATROL_CHANGE &&
                        ticks % 30 == 0 && Math.random() > 0.85) {
                    ticks_looking = 0;
                    state = FSMState.LOOKING;
                } else if (ticks_detected >= DETECTION_DELAY) {
                    state = FSMState.CONFUSED;
                    ticks_confused = 1;
                } else {
                    state = FSMState.WANDER;
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                    times_detected--;
                }

                break;

            case LOOKING:

                ticks_looking++;

                if (checkSpotted()) {
                        state = FSMState.CHASE;
                        ticks_looking = 0;
                } else if (enemy.isAlerted() && ticks_spotted >= DETECTION_DELAY) {
                    state = FSMState.CONFUSED;
                    ticks_confused = 1;
                } else if (isDetected() || ticks - last_time_detected <= CONFUSE_TIME) {
                    state = FSMState.LOOKING;
                } else if (ticks_looking >= MAX_LOOKING || Math.random() * ticks_looking > MAX_LOOKING / 3) {
                    state = FSMState.WANDER;
                }

                break;

            case CONFUSED:

                if (ticks_confused == 0) {
                    state = FSMState.LOOKING;
                } else if (ticks_confused >= CONFUSE_TIME) {
                    if (checkSpotted()) {
                        state = FSMState.CHASE;
                    } else state = FSMState.LOOKING;

                } else if (isDetected()) {
                    ticks_confused++;
                    last_time_detected = ticks;
                    state = FSMState.CONFUSED;
                } else {
                    ticks_confused--;
                    state = FSMState.CONFUSED;
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                    times_detected--;
                }

                break;

            case CHASE:

                if (checkSpotted()) {
                    state = FSMState.ATTACK;
                    ticks_attacked = 1;
                } else if (checkRange(CHASE_RADIUS)){
                    state = FSMState.CHASE;
                } else {
                    if (ticks - last_time_detected >= CHASE_MEMORY) {
                        state = FSMState.CONFUSED;
                    } else state = FSMState.LOOKING;
                }

                if (enemy.isStunned()) {
                    state = FSMState.STUNNED;
                    times_detected--;
                }

                break;

            case ATTACK:
                if (checkRange(null)) {
                    if (ticks_collided >= DETECTION_DELAY * 0.75) {
                        state = FSMState.WON;
                    } else {
                        ticks_collided++;
                    }
                } else if (checkSpotted()) {
                    ticks_attacked++;
                    state = FSMState.ATTACK;
                } else {
                    ticks_attacked = 0;
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

        if(!isStupid) {
            changeStateIfApplicable();
        } else {
            this.state = FSMState.WANDER;
        }

        isAdaptive();

        //prevLoc.x = enemy.getX();
        //prevLoc.y = enemy.getY();

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

//               if (enemy.getX() >= currGoal.x - GOAL_DIST && enemy.getX() <= currGoal.x + GOAL_DIST /*||
//                       Math.abs(prevLoc.x - enemy.getX()) <= 0.5 && Math.abs(prevLoc.x - enemy.getY()) <= 0.5*/) {
//                   // they reached the goal, so add curr goal to end and make next goal the first in deque
//                   goalLocs.addLast(currGoal);
//                   currGoal = goalLocs.pop();
//               } else { // move enemy in direction of goal
//                   action.x = currGoal.x - enemy.getX();
//                   action.y = currGoal.y - enemy.getY();
//                   action = action.nor();
//
//                   enemy.setVX(action.x * 3);
//                   enemy.setVY(action.y * 3);
//                   enemy.setLookDirection(action.x, action.y);
//
//               }

                wanderMove();


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
                action.x = locs_spotted.getLast().x - enemy.getX();
                action.y = locs_spotted.getLast().y - enemy.getY();
                action = action.nor();

                enemy.setVX(action.x * 4);
                enemy.setVY(action.y * 4);
                enemy.setLookDirection(action.x, action.y);

                break;

            case ATTACK:
                //System.out.println("ATTACK");
                action.x = ursa.getX() - enemy.getX();
                action.y = ursa.getY() - enemy.getY();
                action = action.nor();

                enemy.setVX(action.x * 4);
                enemy.setVY(action.y * 4);
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

    public void wanderMove() {
        //System.out.println("begin wander move");
        if (ticks % 40 != 0) return;


        board.clearMarks();
        Vector2 nxtGoal = goalLocs.peek();
        Vector2 nextGoal = setNextGoal();

//        System.out.println("   nextGoal: " + nextGoal.x + ", " + nextGoal.y);
//        System.out.println("   currTile: " + board.getXTile(enemy.getX()) + ", " + board.getYTile(enemy.getY()));
//        System.out.println("   playerLoc: " + ursa.getX() + ", " + ursa.getY());

        if (board.getTile(board.getXTile(enemy.getX()), board.getYTile(enemy.getY())) ==
                board.getTile(nextGoal.x, nextGoal.y)) {

            if (times_detected >= MIN_PATROL_CHANGE && ticks - last_time_detected <= ADAPTIVE_AI_MEM
                    && !locs_spotted.isEmpty()) {
                locs_spotted.pop();
            } else {
                //System.out.println("REACHED GOAL");
                Vector2 temp = goalLocs.pop();
                goalLocs.addLast(temp);
                nxtGoal = goalLocs.peek();
                //System.out.println("nxtGoal: " + nxtGoal.x + ", " + nxtGoal.y);
                nextGoal = new Vector2(board.getXTile(nxtGoal.x), board.getYTile(nxtGoal.y));
                //System.out.println("nextGoal: " + nextGoal.x + ", " + nextGoal.y);
            }
        }

        if (times_detected >= MIN_PATROL_CHANGE && ticks - last_time_detected <= ADAPTIVE_AI_MEM
                && !locs_spotted.isEmpty()) {
            nxtGoal = locs_spotted.getLast();
            nextGoal.x = board.getXTile(nxtGoal.x);
            nextGoal.x = board.getXTile(nxtGoal.x);
        }

        board.setGoal((int) nextGoal.x, (int) nextGoal.y, true);

        // clear data structures
        queue.clear();
        backpack.clear();

        Vector2 currTile = new Vector2(board.getXTile(enemy.getX()), board.getYTile(enemy.getY()));

        if (!board.getBlocked((int) currTile.x, (int) currTile.y)) {
            queue.add(currTile);
            backpack.put(currTile, null);
        } else {
            //System.out.println("Not on safe tile");
              moveToSafeTile(currTile);
        }

        boolean foundGoal = false; // determines whether we have found a goal tile yet
        Vector2 closest_goal = null; // coordinate of first found goal tile


        //System.out.println("about to start bfs while loop");
        // loop for BFS
        while (!foundGoal && !queue.isEmpty()) {
            //System.out.println("bfs while loop iteration");
            Vector2 nextTile = queue.poll(); // get first item in queue
            // System.out.println(ship.getId() + " current tile is " + nextTile.x + " " + nextTile.y);
            adjacents.clear();

            if (!board.getVisited((int) nextTile.x, (int) nextTile.y)) {

                board.setVisited((int) nextTile.x, (int) nextTile.y, true);

                if (!board.getVisited((int) nextTile.x + 1, (int) nextTile.y + 1)) {
                    adjacents.add(new Vector2((int) nextTile.x + 1, (int) nextTile.y + 1));
                }
                if (!board.getVisited((int) nextTile.x + 1, (int) nextTile.y - 1)) {
                    adjacents.add(new Vector2((int) nextTile.x + 1, (int) nextTile.y - 1));
                }
                if (!board.getVisited((int) nextTile.x - 1, (int) nextTile.y + 1)) {
                    adjacents.add(new Vector2((int) nextTile.x - 1, (int) nextTile.y + 1));
                }
                if (!board.getVisited((int) nextTile.x - 1, (int) nextTile.y - 1)) {
                    adjacents.add(new Vector2((int) nextTile.x - 1, (int) nextTile.y - 1));
                }
                if (!board.getVisited((int) nextTile.x + 1, (int) nextTile.y)) {
                    adjacents.add(new Vector2((int) nextTile.x + 1, (int) nextTile.y));
                }
                if (!board.getVisited((int) nextTile.x, (int) nextTile.y)) {
                    adjacents.add(new Vector2((int) nextTile.x, (int) nextTile.y));
                }
                if (!board.getVisited((int) nextTile.x - 1, (int) nextTile.y + 1)) {
                    adjacents.add(new Vector2((int) nextTile.x - 1, (int) nextTile.y + 1));
                }
                if (!board.getVisited((int) nextTile.x, (int) nextTile.y - 1)) {
                    adjacents.add(new Vector2((int) nextTile.x, (int) nextTile.y - 1));
                }

                for (Vector2 c : adjacents) {
                    // break out of loop if we already found a goal tile
                    if (foundGoal) break;

                    if ((c.x == nextGoal.x && c.y == nextGoal.y) ||
                            !board.getBlocked((int) c.x, (int) c.y) &&
                            !board.getVisited((int) c.x, (int) c.y)) {
                        queue.add(c);
                        backpack.put(c, nextTile);

                        // check if this tile is a goal tile
                        if (board.getGoal((int) c.x, (int) c.y)) {
                            closest_goal = c;
                            foundGoal = true;
                        }
                    }
                }
            }
        }

        if (!foundGoal) {
            //System.out.println("CANNOT FIND GOAL");
            return; // pick random direction
        } else { // if we found a goal, backtrace to find the first step to get there
            Vector2 this_tile = closest_goal;
            Vector2 prev_tile = backpack.get(this_tile);

            while (prev_tile != null && (prev_tile.x != currTile.x || prev_tile.y != currTile.y)) {
                //System.out.println(prev_tile.x + ", " + prev_tile.y);
                this_tile = prev_tile;
                prev_tile = backpack.get(this_tile);
            }

            action.x = (this_tile.x * board.tileWidth()) + (board.tileWidth() / 2) - enemy.getX();
            action.y = (this_tile.y * board.tileHeight()) + (board.tileHeight() / 2) - enemy.getY();
            action.nor();
            enemy.setVX(action.x * 2);
            enemy.setVY(action.y * 2);
            enemy.setLookDirection(action.x, action.y);
        }

//        if (board.getBlocked((int) currTile.x, (int) currTile.y)) {
//            if (!board.getBlocked((int) currTile.x + 1, (int) currTile.y)) {
//                board.setGoal(board.getXTile((int) currTile.x + 1),
//                        board.getYTile((int) currTile.y), true);
//            }
//            if (!board.getBlocked((int) currTile.x, (int) currTile.y + 1)) {
//                board.setGoal(board.getXTile((int) currTile.x),
//                        board.getYTile((int) currTile.y + 1), true);
//            }
//            if (!board.getBlocked((int) currTile.x + 1, (int) currTile.y + 1)) {
//                board.setGoal(board.getXTile((int) currTile.x + 1),
//                        board.getYTile((int) currTile.y + 1), true);
//            }
//            if (!board.getBlocked((int) currTile.x - 1, (int) currTile.y)) {
//                board.setGoal(board.getXTile((int) currTile.x - 1),
//                        board.getYTile((int) currTile.y), true);
//            }
//            if (!board.getBlocked((int) currTile.x, (int) currTile.y - 1)) {
//                board.setGoal(board.getXTile((int) currTile.x),
//                        board.getYTile((int) currTile.y - 1), true);
//            }
//            if (!board.getBlocked((int) currTile.x - 1, (int) currTile.y - 1)) {
//                board.setGoal(board.getXTile((int) currTile.x - 1),
//                        board.getYTile((int) currTile.y - 1), true);
//            }
//            if (!board.getBlocked((int) currTile.x + 1, (int) currTile.y - 1)) {
//                board.setGoal(board.getXTile((int) currTile.x + 1),
//                        board.getYTile((int) currTile.y - 1), true);
//            }
//            if (!board.getBlocked((int) currTile.x - 1, (int) currTile.y + 1)) {
//                board.setGoal(board.getXTile((int) currTile.x - 1),
//                        board.getYTile((int) currTile.y + 1), true);
//            }
//        }


    }

    public void moveToSafeTile(Vector2 currTile) {
        ArrayList<Vector2> safeTiles = new ArrayList<>();

        if (!board.getBlocked((int) currTile.x + 1, (int) currTile.y)) {
            safeTiles.add(new Vector2((int) currTile.x + 1, (int) currTile.y));
        }
        if (!board.getBlocked((int) currTile.x, (int) currTile.y + 1)) {
            safeTiles.add(new Vector2((int) currTile.x, (int) currTile.y + 1));
        }
        if (!board.getBlocked((int) currTile.x + 1, (int) currTile.y + 1)) {
            safeTiles.add(new Vector2((int) currTile.x + 1, (int) currTile.y + 1));
        }
        if (!board.getBlocked((int) currTile.x - 1, (int) currTile.y)) {
            safeTiles.add(new Vector2((int) currTile.x - 1, (int) currTile.y));
        }
        if (!board.getBlocked((int) currTile.x, (int) currTile.y - 1)) {
            safeTiles.add(new Vector2((int) currTile.x, (int) currTile.y - 1));
        }
        if (!board.getBlocked((int) currTile.x - 1, (int) currTile.y - 1)) {
            safeTiles.add(new Vector2((int) currTile.x - 1, (int) currTile.y - 1));
        }
        if (!board.getBlocked((int) currTile.x + 1, (int) currTile.y - 1)) {
            safeTiles.add(new Vector2((int) currTile.x + 1, (int) currTile.y - 1));
        }
        if (!board.getBlocked((int) currTile.x - 1, (int) currTile.y + 1)) {
            safeTiles.add(new Vector2((int) currTile.x - 1, (int) currTile.y + 1));
        }

        Vector2 nextTile = safeTiles.get((int) Math.random() * safeTiles.size());

        action.x = nextTile.x - enemy.getX();
        action.y = nextTile.y - enemy.getY();
        enemy.setVX(action.x);
        enemy.setVY(action.y);
        enemy.setLookDirection(action.x, action.y);
    }

    public Enemy getEnemy() { return enemy; }

    public boolean isWon() { return state == FSMState.WON; }

    //public void setWon(boolean value) { hasWon = value; }

    public void resetLifeTime() { ticks_attacked = 0; }

    public boolean checkRange(Float range) {

        if (range == null) {
            range = COLLISION_ERROR;
        } else range += ENEMY_RADIUS;

//        boolean checkX = Math.abs(ursa.getX() - enemy.getX()) <=
//                ENEMY_RADIUS + range;
//        boolean checkY = Math.abs(ursa.getY() - enemy.getY()) <=
//                ENEMY_RADIUS + range;
//
//        return checkX || checkY;

        return Math.sqrt(Math.pow(ursa.getX() - enemy.getX(), 2) +
                Math.pow(ursa.getY() - enemy.getY(), 2)) <= range;

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

    public boolean earlyLooking() {
        return (state == FSMState.LOOKING && ticks_looking <= 25);
    }

    public boolean checkSpotted() {
        if (enemy.isAlerted() && ticks_spotted >= DETECTION_DELAY) {
            if (locs_spotted.size() >= MAX_SPOTTED) {
                Vector2 temp = locs_spotted.pollFirst();
                temp.x = ursa.getX();
                temp.y = ursa.getY();
                locs_spotted.add(temp);
            } else {
                locs_spotted.add(new Vector2(ursa.getX(), ursa.getY()));
            }
            return true;
        } else if (enemy.isAlerted()) { ticks_spotted++; }

        if (!enemy.isAlerted()) ticks_spotted = 0;
        return false;
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

    public Vector2 setNextGoal() {
        Vector2 nxtGoal = goalLocs.peek();
        Vector2 nextGoal = new Vector2(board.getXTile(nxtGoal.x), board.getYTile(nxtGoal.y));

        while (board.getBlocked((int) nextGoal.x, (int) nextGoal.y)) {
            if (!board.getBlocked((int) nextGoal.x - 1, (int) nextGoal.y)) {
                nextGoal.x = nextGoal.x - 1;
            } else if (!board.getBlocked((int) nextGoal.x + 1, (int) nextGoal.y)) {
                nextGoal.x = nextGoal.x + 1;
            } else if (!board.getBlocked((int) nextGoal.x, (int) nextGoal.y - 1)) {
                nextGoal.y = nextGoal.y - 1;
            } else {
                nextGoal.y = nextGoal.y + 1;
            }
        }

        return nextGoal;
    }

    public boolean isSurprised() {
        return isDetected() && state == FSMState.ATTACK && ticks_attacked <= 20;
    }

    public void isAdaptive() {
        if (times_detected >= MIN_PATROL_CHANGE &&
                ticks - last_time_detected <= ADAPTIVE_AI_MEM && !locs_spotted.isEmpty()) {
            enemy.setAdaptive(true);
        } else {
            enemy.setAdaptive(false);
        }
    }

    private class Coordinate {
        public int x;
        public int y;

        Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Coordinate() {
            this.x = 0;
            this.y = 0;
        }

    }

}
