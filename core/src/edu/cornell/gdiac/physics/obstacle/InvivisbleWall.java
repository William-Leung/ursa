package edu.cornell.gdiac.physics.obstacle;

import com.badlogic.gdx.physics.box2d.BodyDef;

public class InvivisbleWall extends BoxObstacle{
    public InvivisbleWall(float x, float y, float w, float h){
        super(x,y,w,h);
        setBodyType(BodyDef.BodyType.StaticBody);

    }
}
