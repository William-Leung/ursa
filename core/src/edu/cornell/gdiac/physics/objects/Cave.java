package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class Cave extends PolygonObstacle {
    private boolean canInteract;
    private float textureScale;
    private Vector2 ZZZposition;
    private TextureRegion ZZZTexture;
    private boolean isUrsaSleeping;

    public Cave(float[] points, float x, float y,float yOffset, float textureScale, Vector2 ZZZpos) {
        super(points, x, y, yOffset, textureScale);
        setBodyType(BodyDef.BodyType.StaticBody);
        canInteract = true;
        this.textureScale = textureScale;
        this.ZZZposition = ZZZpos;
    }

    public boolean canInteract() {
        return canInteract;
    }

    public void interact() {
        canInteract = false;
    }

    public void setIsUrsaSleeping(boolean b) {
        isUrsaSleeping = b;
    }

    public void setZZZTexture(TextureRegion texture) {
        ZZZTexture = texture;
    }

    public void postDraw(GameCanvas canvas) {
        if(!isUrsaSleeping) {
            return;
        }
        Affine2 affine = new Affine2()
                .translate((getX() + ZZZposition.x) * drawScale.x, (getY() + ZZZposition.y) * drawScale.y )
                .scale(textureScale, textureScale)
                ;
            canvas.draw(ZZZTexture, Color.WHITE, 0, 0, affine);
    }
}
