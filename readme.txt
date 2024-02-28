Basic Physics Questions
    1) Newton's Second Law
        Pushing a ball causes it to roll in that direction and pushing it harder makes it go faster.
    2) Momentum & Impulse
        Momentum encapsulates what keeps objects in motion while impulse disrupts that motion.
    3) Elastic Collision
        Elastic collisions conserve all energy prior to the collision such as breaking in 8ball.
    4) Inelastic Collision
        Perfectly inelastic collisions result in objects sticking together such as throwing wet paper
        at a wall.
    5) Coefficient of Restitution
        This measures the extent to which velocities changed after a collision and can be
        anywhere from 0 to 1.
    6) Angular Velocity + Acceleration
        Angular velocity and acceleration are analogous to their linear counterparts except exist
        in the context of angular dynamics.
    7) Inertia + Angular Acceleration
        Moment of inertia is what helps objects resist the forces of angular acceleration.
    8) Torque
        Torque is force except in a rotational dimension and affects angular momentum.
    9) Torque + Inertia + Angular Acceleration
        Torque induces angular acceleration which is subsequently resisted by the moment of inertia.

LibGDX Questions
    1) Shape vs Body
        Bodies represent the physical objects that the physics engines interact with while shapes
        determine collisions for those objects (think BoundingBox).
    2) Physical Force vs Property
        Physical properties should be dealt with in shapes since an object's appearance, for example,
        would influence their collision boundaries. On the other hand, physical forces are the domain
        of bodies since bodies work closely with physics simulation.
    3) Multiple Shapes per Body
        If you are working with a very complex geometric structure, breaking it into many smaller
        shapes could make it easier to work with or more efficient. In addition, objects made up
        of other objects such as a vacuum cleaner could require multiple shapes.
    4) World
        The world contains all the bodies and thus is the center for all physics calculations and
        interactions.
    5) Sleeping Objects
        Sleeping objects allows you to save computational space since objects that are off screen
        or not involved in any collisions do not need to be involved with the physics simulation.
    6) Static vs Dynamic Bodies
        Static bodies do not move upon collisions unlike dynamic bodies which are susceptible to
        collision mechanics. You can specify the type with
            bodyDef.type = BodyDef.BodyType.StaticBody;
            bodyDef.type = BodyDef.BodyType.DynamicBody;
    7) Bullets
        Bullets are bodies that are supposed to be moving very quickly, thus requiring more collision
        checks than other objects. Setting bodies as bullets prevents them from phasing through
        other bodies.
    8) ContactListener
        ContactListeners help to keep track of collisions within a World and can be used to implement
        sensors to detect when objects enter certain areas. For example, a sensor could detect when
        the player enters the end zone and start the ending animation.
    9) Shape Implementation
        You must create a body and a shape that you want to attach to it. Then, using the shape,
        construct a fixture and attach that to the body.

Spinner StaticBody
    We cannot set the Spinner to StaticBody because we want the spinner to be able to spin while
    fixed in space. The pin can be set to static since that keeps the body fixed in space, however,
    we need the rotation to be able to happen so the spinner must be dynamic. If it was static,
    then the spinner would just remain fixed in space and unmoving.
