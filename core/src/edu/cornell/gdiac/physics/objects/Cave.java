package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
    private TextureRegion portalTexture;
    private float yOffset;

    public Cave(float[] points, float x, float y, float yOffset, float textureScale, Vector2 ZZZpos) {
        super(points, x, y, yOffset, textureScale);
        setBodyType(BodyDef.BodyType.StaticBody);
        canInteract = true;
        this.textureScale = textureScale;
        this.ZZZposition = ZZZpos;
        this.yOffset = yOffset;
    }

    public boolean canInteract() {
        return canInteract;
    }

    public void interact() {
        canInteract = false;
    }

    public void setZZZTexture(TextureRegion texture) {
        ZZZTexture = texture;
    }

    public void setPortalTexture(TextureRegion texture) {
        portalTexture = texture;
    }
    public void draw(GameCanvas canvas) {
        if (region != null) {
            Affine2 affine = new Affine2()
                    .translate(getX() * drawScale.x, getY()* drawScale.y)
                    .scale(textureScale, textureScale)
                    ;
            canvas.draw(portalTexture, Color.WHITE, texture.getRegionWidth() / 2f, yOffset, affine);
            canvas.draw(texture, tint, texture.getRegionWidth() / 2f, yOffset, affine);
        }
    }
    public void postDraw(GameCanvas canvas) {
        Affine2 affine = new Affine2()
                .translate((getX() + ZZZposition.x) * drawScale.x, (getY() + ZZZposition.y) * drawScale.y )
                .scale(textureScale, textureScale)
                ;
            canvas.draw(ZZZTexture, Color.WHITE, 0, 0, affine);
    }
}
