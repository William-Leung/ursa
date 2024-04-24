package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class GenericObstacle extends BoxObstacle {
    private float sx = 0.5f;
    private float sy = 0.5f;
    private float heightModifier = 8;

    public GenericObstacle(float x, float y, float width, float height) {
        super(x,y,width, height);
    }
    public GenericObstacle(float x, float y, float width, float height, float sx, float sy) {
        super(x,y,width, height);
        this.sx = sx;
        this.sy = sy;
    }

    public GenericObstacle(float x, float y, float width, float height, float sx, float sy, float heightModifier) {
        super(x,y,width, height);
        this.sx = sx;
        this.sy = sy;
        assert heightModifier != 0;
        this.heightModifier = heightModifier;
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y + (float) texture.getRegionHeight()
                /heightModifier,getAngle(),sx,sy);
    }
}
