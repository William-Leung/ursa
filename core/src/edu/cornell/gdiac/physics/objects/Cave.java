package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

public class Cave extends BoxObstacle {

    public Cave(float x, float y, float width, float height) {
        super(x,y,width, height);
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),.05f,0.05f);
    }
}
