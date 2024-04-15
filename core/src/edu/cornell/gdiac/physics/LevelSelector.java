package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;


public class LevelSelector implements Screen, InputProcessor, ControllerListener {

    private TextureRegion[] buttons = new TextureRegion[20];
    private TextureRegion background = new TextureRegion();
    private ScreenListener listener;
    private float buttonWidth;
    GameCanvas canvas;
    private boolean active;
    public LevelSelector(GameCanvas NewCanvas){
        canvas = NewCanvas;
        active = false;
        Gdx.input.setInputProcessor( this );


    }
    public void setActive(boolean b){
        active = b;
    }
    public void gatherAssets(AssetDirectory directory) {
        buttons[0] = new TextureRegion(directory.getEntry("levelSelect:Level1", Texture.class));
        buttons[1] = new TextureRegion(directory.getEntry("levelSelect:Level2", Texture.class));
        buttons[2] = new TextureRegion(directory.getEntry("levelSelect:Level3", Texture.class));
        background = new TextureRegion(directory.getEntry("levelSelect:background", Texture.class));

    }
    private void update(float delta){

    }

    private void draw(){
        canvas.begin();
        canvas.draw(background,0,0);
        canvas.draw(buttons[0],0,0);
        canvas.draw(buttons[1],canvas.getWidth()/2.5f,0);
        canvas.draw(buttons[2],canvas.getWidth()/1.2f,0);

        canvas.end();
    }




    @Override
    public boolean keyDown(int i) {
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        System.out.println("Screen x: " + screenX);
        System.out.println("Screen y: " + (canvas.getHeight()-screenY));
        float radius = buttons[0].getRegionWidth()/2.0f;
        System.out.println("RAdius is: " + radius);
        float centerX = 99f;
        float centerY = 92f;
        screenY = canvas.getHeight()-screenY;
        float formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
        formula = (float) Math.sqrt(formula);
        System.out.println("Formula: "+ formula);
        if(formula < radius){
            listener.exitScreen(this, 1);
        }
        centerX = 511;
        centerY = 92;
        formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
        formula = (float) Math.sqrt(formula);
        if(formula < radius){

            listener.exitScreen(this, 2);
        }
        centerX = 958;
        centerY = 92;
        formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
        formula = (float) Math.sqrt(formula);
        if(formula < radius){

            listener.exitScreen(this, 3);
        }
        return false;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        return false;
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        if(active){
            update(delta);
            draw();

        }


    }
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    @Override
    public void resize(int i, int i1) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void connected(Controller controller) {

    }

    @Override
    public void disconnected(Controller controller) {

    }

    @Override
    public boolean buttonDown(Controller controller, int i) {
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int i) {
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int i, float v) {
        return false;
    }
}

