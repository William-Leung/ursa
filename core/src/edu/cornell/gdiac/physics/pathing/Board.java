package edu.cornell.gdiac.physics.pathing;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.units.Enemy;
import edu.cornell.gdiac.physics.objects.GenericObstacle;
import edu.cornell.gdiac.util.PooledList;

public class Board {

    private static class TileState {
        public boolean goal = false;
        public boolean visited = false;
        public boolean blocked = false;
    }

    /** The board width (in number of tiles) */
    private int width;
    /** The board height (in number of tiles) */
    private int height;
    /** The tile grid (with above dimensions) */
    private TileState[][] tiles;
    /** The width of a tile */
    private float tile_width;
    /** The height of a tile */
    private float tile_height;
    /** The dimensions of the board */
    private Vector2 dims;

    public Board(PooledList<GenericObstacle> obstacles, Enemy[] enemies) {
        getDims(obstacles);

        tile_width = 1.5f * enemies[0].getWidth();
        tile_height = 1.5f * enemies[0].getHeight();

        width = (int) (dims.x / tile_width) + 1;
        height = (int) (dims.y / tile_height) + 1;

        tiles = new TileState[width][height];
        populateTiles(obstacles);

        //printTiles();
    }

    public TileState[][] getBoard() { return tiles; }

    public int width() { return this.width; }
    public int height() { return this.height; }
    public float maxX() { return dims.x; }
    public float maxY() { return dims.y; }
    public float tileWidth() { return this.tile_width; }
    public float tileHeight() { return this.tile_height; }

    public void getDims(PooledList<GenericObstacle> obstacles) {
        float min_x = Float.MAX_VALUE;
        float max_x = Float.MIN_VALUE;
        float min_y = Float.MAX_VALUE;
        float max_y = Float.MIN_VALUE;

        //if (obstacles.isEmpty()) System.out.println("empty");

        for (GenericObstacle o : obstacles) {

//            System.out.println("o.getX(): " + o.getX());
//            System.out.println("o.getWidth(): " + o.getWidth());
//            System.out.println("o.getHeight(): " + o.getHeight());
//
//            System.out.println("max x of this is: " + o.getX() + (o.getWidth() / 2));
//            System.out.println("max y of this is: " + o.getY() + (o.getHeight() / 2));


            if (o.getX() - (o.getWidth() / 2) < min_x) min_x = o.getX() - (o.getWidth() / 2);
            if (o.getX() + (o.getWidth() / 2) > max_x) max_x = o.getX() + (o.getWidth() / 2);
            if (o.getY() - (o.getHeight() / 2) < min_y) min_y = o.getY() - (o.getHeight() / 2);
            if (o.getY() + (o.getHeight() / 2) > max_y) max_y = o.getY() + (o.getHeight() / 2);
        }

//        System.out.println("min_x is: " + min_x);
//        System.out.println("max_x is: " + max_x);
//        System.out.println("min_y is: " + min_y);
//        System.out.println("max_y is: " + max_y);

        dims = new Vector2(max_x, max_y);
    }

    public void populateTiles(PooledList<GenericObstacle> obstacles) {

//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//                // check if there is an obstacle in this tile
//                boolean inTile = false;
//                for (GenericObstacle o : obstacles) {
//                    if (checkInTile(i, j, o)) inTile = true;
//                }
//                // set to true if there is no obstacle in the tile
//                tiles[i][j] = !inTile;
//            }
//        }

        // fill in all with true
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                tiles[i][j] = new TileState();
            }
        }


        // make tiles with obstacle in them false
        for (GenericObstacle o : obstacles) {

            //System.out.println("This object's tile is: " + getXTile(o.getX()) + ", " + getYTile(o.getY()));

            int min_x_tile = getXTile(o.getX() - (o.getWidth() / 2));
            int max_x_tile = getXTile(o.getX() + (o.getWidth() / 2));
            int min_y_tile = getYTile(o.getY() - (o.getHeight() / 2));
            int max_y_tile = getYTile(o.getY() + (o.getHeight() / 2));

            for (int i = min_x_tile; i <= max_x_tile; i++) {
                for (int j = min_y_tile; j <= max_y_tile; j++) {
                    setBlocked(i, j, true);
                }
            }
        }

    }

    public boolean checkInTile(int x_tile, int y_tile, GenericObstacle o) {
        float x_min = (x_tile * tile_width) - (tile_width / 2);
        float x_max = (x_tile * tile_width) + (tile_width / 2);
        float y_min = (y_tile * tile_height) - (tile_height / 2);
        float y_max = (y_tile * tile_height) + (tile_height / 2);

        return checkXIn(o, x_min, x_max) && checkYIn(o, y_min, y_max);
    }

    public boolean checkXIn(GenericObstacle o, float min_x, float max_x) {
        float left_x = o.getX() - (o.getWidth() / 2);
        float right_x = o.getX() + (o.getWidth() / 2);

        // left side greater than min_x and less than max_x
        if (left_x >= min_x && right_x <= max_x) return true;

        // right side greater than min_x and less than max_x
        if (right_x >= min_x && right_x <= max_x) return true;

        // left less than min_x and right greater than max_x
        if (left_x <= min_x && right_x >= max_x) return true;

        return false;
    }

    public boolean checkYIn(GenericObstacle o, float min_y, float max_y) {
        float top_y = o.getY() + (o.getWidth() / 2);
        float bot_y = o.getY() - (o.getWidth() / 2);

        // top greater than min_y and less than max_y
        if (top_y >= min_y && top_y <= max_y) return true;

        // bottom greater than min_y and less than max_y
        if (bot_y >= min_y && bot_y <= max_y) return true;

        // bottom less than min_y and top greater than max_y
        if (bot_y <= min_y && top_y >= max_y) return true;

        return false;

    }

    public int getXTile(float x) {
        return Math.min((int) (x / tile_width), width - 1);
    }

    public int getYTile(float y) {
        return Math.min((int) (y / tile_height), height - 1);
    }

    public void printTiles() {
        System.out.println("[");
        for (int i = 0; i < width; i++) {
            System.out.print("    [");
            for (int j = 0; j < height; j++) {
                if (!getBlocked(i, j)) System.out.print("O");
                else System.out.print("X");
            }
            System.out.println("]");
        }
        System.out.println("]");
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public TileState getTile(int x, int y) { return tiles[x][y]; }

    public TileState getTile(Vector2 loc) {
        return tiles[getXTile(loc.x)][getYTile(loc.y)];
    }

    public TileState getTile(float x, float y) {
        return tiles[getXTile(x)][getYTile(y)];
    }

    public boolean getBlocked(TileState t) { return t.blocked; }

    public boolean getGoal(TileState t) { return t.goal; }

    public boolean getVisited(TileState t) { return t.visited; }

    public boolean getBlocked(int x, int y) { return inBounds(x, y) && getTile(x, y).blocked; }

    public boolean getGoal(int x, int y) { return inBounds(x, y) && getTile(x, y).goal; }

    public boolean getVisited(int x, int y) { return inBounds(x, y) && getTile(x, y).visited; }

    public void setBlocked(int x, int y, boolean v) {
        if (!inBounds(x, y)) return;
        getTile(x, y).blocked = v;
    }

    public void setGoal(int x, int y, boolean v) {
        if (!inBounds(x, y)) return;
        getTile(x, y).goal = v;
    }

    public void setVisited(int x, int y, boolean v) {
        if (!inBounds(x, y)) return;
        getTile(x, y).visited = v;
    }

    public boolean isSafe(int x, int y) {
        return inBounds(x, y) && !getBlocked(x, y);
    }

    public void setVisited(TileState t) { t.visited = true; }

    public void reset() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                setVisited(i, j, false);
                setBlocked(i, j, false);
                setGoal(i, j, false);
            }
        }
    }

    public void clearMarks() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                setVisited(i, j, false);
                setGoal(i, j, false);
            }
        }
    }

}
