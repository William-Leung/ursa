package edu.cornell.gdiac.physics.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;

public class Shark extends Enemy {

    private TextureRegion brownTextureRegion;
    private float[] vertices = new float[48];
    public Shark(float xStart,float yStart,float maxX, float minX, JsonValue data, float width, float height, float textureScale) {
        super(xStart, yStart, maxX, minX, data, width, height, textureScale);

        Pixmap brownPixmap = new Pixmap(1, 1, Format.RGBA8888);
        brownPixmap.setColor(new Color(0.89f, 0.73f, 0.56f, 0.3f));
        brownPixmap.fill();
        Texture brownTexture = new Texture(brownPixmap);
        brownTextureRegion = new TextureRegion(brownTexture);
        brownPixmap.dispose();
        super.setName("shark");
    }

    @Override public boolean isPlayerInLineOfSight(World world, SimpleObstacle player) {
        Vector2 sharkPos = new Vector2(getPosition());
        Vector2 playerPos = new Vector2(player.getPosition());
        double dst = playerPos.dst(sharkPos);

        if(dst < 10f) {
            super.playerCurrentInSight = true;
            return true;
        } else {
            super.playerCurrentInSight = false;
            return false;
        }
    }

    /* TODO: Make this much more idiomatic for drawing a 360 degree cone */
    @Override public void createSightCone(World world) {

        vertices[0] = 0f;
        vertices[1] = 0f;
        float starting_angle = 0f;
        float angle_decrement =  16.33f;

        for(int i = 2; i < vertices.length - 1; i += 2) {
            Vector2 sightConePoint = new Vector2();
            sightConePoint.x = 10f * (float) Math.cos(Math.toRadians(starting_angle));
            sightConePoint.y = 10f * (float) Math.sin(Math.toRadians(starting_angle));

            ObstacleCallback callback = new ObstacleCallback(getPosition());
            world.rayCast(callback, getPosition(), sightConePoint.cpy().add(getPosition()));
            coneVectors[i] = callback.rayTerm;
            if (coneVectors[i] != null) {
                coneVectors[i] = coneVectors[i].cpy();
            }

            if (callback.rayTerm != null) {
                sightConePoint = callback.rayTerm.cpy().sub(getPosition());
            }
            vertices[i] = sightConePoint.x * drawScale.x;
            vertices[i+1] = sightConePoint.y * drawScale.y;
            starting_angle -= angle_decrement;
        }

        short[] triangles = new short[66];
        short triangle_counter = 1;
        for(int i = 0; i < triangles.length - 2; i += 3) {
            triangles[i] = 0;
            triangles[i+1] = triangle_counter;
            triangle_counter++;
            triangles[i+2] = triangle_counter;
        }

        super.sightConeRegion = new PolygonRegion(brownTextureRegion, vertices, triangles);
    }


    }
