package edu.cornell.gdiac.physics.cave;

import com.badlogic.gdx.graphics.Color;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

public class Cave extends BoxObstacle {

    public Cave(float width, float height) {
        super(5,25,width, height);
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),.05f,0.05f);
    }
}
