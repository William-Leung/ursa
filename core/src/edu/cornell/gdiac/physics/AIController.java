package edu.cornell.gdiac.physics;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.player.UrsaModel;

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

    // Instance Attributes
    /** The enemy being controlled by this AIController */
    private Enemy enemy;
    /** The player being targeted */
    private UrsaModel ursa;
    /** The enemy's current state in the FSM */
    private FSMState state;
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

    /**
     * Creates an AIController for the ship with the given id.
     */
    public AIController(Enemy enemy,  UrsaModel ursa) {
        this.enemy = enemy;
        //this.board = board;
        this.ursa = ursa;

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
        System.out.println(state.toString());

        switch (state) {
            case SPAWN:
                enemy.setVX(0);
                enemy.setVY(0);
                break;
            case WANDER:

                Vector2 action = new Vector2(0,0);

                if (enemy.getX() < 5) {

                } else if (enemy.getX() > 15) {

                }

                if (enemy.getVX() < 0) {
                    enemy.setLookDirection(-1,0);
                } else {
                    enemy.setLookDirection(1,0);
                }

//                enemy.setLookDirection(action.x, 0);
//                enemy.setVX(action.x * 10);

//                if ((enemy.getVX() != 0 || enemy.getVY() != 0) && ticks_since_change < DIR_TIME) {
//                    ticks_since_change++;
//                    return;
//                }
//                Vector2 action = new Vector2((float) (2 * Math.random() - 1),
//                        (float) (2 * Math.random() - 1)).nor();
//                enemy.setLookDirection(action.x, action.y);
//                enemy.setVX(action.x);
//                enemy.setVY(action.y);
//                ticks_since_change = 0;
                curr_stamina = Math.min(MAX_STAMINA, curr_stamina + 1);
                return;

            case CONFUSED:
                //System.out.println("Enemy is confused");
                enemy.setVX(0);
                enemy.setVY(0);
                curr_stamina = Math.min(MAX_STAMINA, curr_stamina + 1);
                break;
            case CHASE:
                action =  new Vector2(ursa.getX() - enemy.getX(), ursa.getY() - enemy.getY()).nor();

                int multiplier = curr_stamina != 0 ? 4 : 2;
                curr_stamina = Math.max(curr_stamina - 1, 0);

                action.x *= multiplier;
                action.y *= multiplier;
                enemy.setVX(action.x);
                enemy.setVY(action.y);
                enemy.setLookDirection(action.x, action.y);

                break;
        }

    }



    public Enemy getEnemy() { return enemy; }

    public boolean isWon() { return hasWon; }

    public void setWon(boolean value) { hasWon = value; }

    public void resetLifeTime() { ticks_attacked = 0; }

}
