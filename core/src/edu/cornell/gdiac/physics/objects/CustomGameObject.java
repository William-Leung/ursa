package edu.cornell.gdiac.physics.objects;

/**
 * This class represents a house object.
 * The only reason for its existence is because Ursa draws on top of the sides of the house
 * with the current Pokemon 3/4 implementation (sort by top of hitbox) and I think that looks bad.
 */
public class CustomGameObject extends GameObject{
    float playerHeight;
    public CustomGameObject(float[] points, float x, float y, float yOffset, float textureScale, float playerHeight) {
        super(points, x, y, yOffset, textureScale);
        this.playerHeight = playerHeight;
    }

    /**
     * @return custom sortingY for determining drawing order of objects
     */
    @Override
    public float getSortingY() {
        return getY() + playerHeight;
    }
}
