package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.ScreenListener;


public class LevelSelector implements Screen, InputProcessor, ControllerListener {

    private TextureRegion[] buttons = new TextureRegion[20];
    private boolean button1Pressed;
    private JsonReader json;
    private JsonValue jsonData;
    private boolean button2Pressed;
    private boolean button3Pressed;
    private float[] completedLevels;
    private float[] unlockedLevels;
    private boolean button1Locked;
    private boolean button2Locked;
    private boolean button3Locked;
    private FilmStrip button1;
    private FilmStrip button2;
    private FilmStrip button3;
    private FilmStrip button4;
    private TextureRegion background = new TextureRegion();
    private ScreenListener listener;
    private final float ScreenWidthStart = 1024;
    private final float ScreenHeightStart = 576;
    private float buttonWidth;
    GameCanvas canvas;
    private boolean active;

    private Music levelSelectMusic;
    public LevelSelector(GameCanvas NewCanvas){
        canvas = NewCanvas;
        active = false;
        Gdx.input.setInputProcessor( this );
        button1Pressed = false;
        json = new JsonReader();
        jsonData = json.parse(Gdx.files.internal("saveData.json"));
        completedLevels = new float[20];
        unlockedLevels = new float[20];


        for (int i = 0;i<jsonData.get("completed").size;i++){
            completedLevels[i] = jsonData.get("completed").get(i).asFloat();
            unlockedLevels[i] = jsonData.get("unlocked").get(i).asFloat();

            }





    }
    public void setActive(boolean b){
        active = b;
    }
    public void gatherAssets(AssetDirectory directory) {
        buttons[0] = new TextureRegion(directory.getEntry("levelSelect:Level1", Texture.class));
        buttons[1] = new TextureRegion(directory.getEntry("levelSelect:Level2", Texture.class));
        buttons[2] = new TextureRegion(directory.getEntry("levelSelect:Level3", Texture.class));
        buttons[3] = new TextureRegion(directory.getEntry("levelSelect:Level3", Texture.class));
        button2Locked = true;
        button3Locked = true;
        button1 = new FilmStrip(buttons[0].getTexture(),1,2);

        button2 = new FilmStrip(buttons[1].getTexture(),1,5);

        button3 = new FilmStrip(buttons[2].getTexture(),1,5);

        button4 = new FilmStrip(buttons[3].getTexture(),1,5);
        button4.setFrame(0);
        background = new TextureRegion(directory.getEntry("levelSelect:background", Texture.class));
        levelSelectMusic = directory.getEntry("soundtracks:level_select", Music.class);
        button1.setFrame(0);

        if(unlockedLevels[1] ==1){
            button2.setFrame(4);
        }
        else {
            button2.setFrame(0);
        }
        if(unlockedLevels[2] == 1){
            button3.setFrame(4);
        }
        else {
            button3.setFrame(0);
        }


    }
    private void update(float delta){


    }

    private void draw(){

        canvas.clear();

        canvas.begin();

        if(button1Pressed){
            button1.setFrame(1);
        }else {
            button1.setFrame(0);
        }
        if(button2Pressed){
            button2.setFrame(4);
        }
        else if(unlockedLevels[1] == 1){
            button2.setFrame(3);
        }
        else {
            button2.setFrame(0);
        }
        if(button3Pressed){
            button3.setFrame(4);
        }
        else if(unlockedLevels[2] == 1){
            button3.setFrame(3);
        }
        else {
            button3.setFrame(0);
        }

        canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * .25f,background.getRegionHeight() * .285f);
        canvas.draw(button1,Color.WHITE,40,155,button1.getRegionWidth() * .6f,button1.getRegionHeight() * .6f);
        canvas.draw(button2,Color.WHITE,152.5f,155,button1.getRegionWidth() * .6f,button1.getRegionHeight() * .6f);
        canvas.draw(button3,Color.WHITE,152.5f,395,button1.getRegionWidth() * .6f,button1.getRegionHeight() * .6f);
        canvas.draw(button4,Color.WHITE,285f,395,button1.getRegionWidth() * .6f,button1.getRegionHeight() * .6f);


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
        if(active){
            //System.out.println("Screen x + L: " + screenX);
            //System.out.println("Screen y: " + (canvas.getHeight()-screenY));
            float radius = (button1.getRegionWidth()/2.0f) -25;
            //System.out.println("RAdius is: " + radius);
            float centerX = 78f;
            float centerY = 196f;
            screenY = canvas.getHeight()-screenY;
            float formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            //System.out.println("Formula: "+ formula);
            if(formula < radius){
                System.out.println("hit");
                button1Pressed = true;
            }
            centerX = 189f;
            centerY = 196f;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            if(formula < radius){
                button2Pressed = true;
            }
            centerX = 189f;
            centerY = 434;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            if(formula < radius){
                System.out.println("Button 3");
                button3Pressed = true;
            }


        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if(active){
            button3Pressed = false;
            button2Pressed = false;
            button1Pressed = false;
            System.out.println("Screen x + L: " + screenX);
            System.out.println("Screen y: " + (canvas.getHeight()-screenY));
            float radius = (button1.getRegionWidth()/2.0f) -25;
            System.out.println("RAdius is: " + radius);
            float centerX = 78f;
            float centerY = 196f;
            screenY = canvas.getHeight()-screenY;
            float formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            System.out.println("Formula: "+ formula);
            if(formula < radius){

               listener.exitScreen(this,1);
            }
            centerX = 189f;
            centerY = 196f;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            if(formula < radius){
               listener.exitScreen(this,2);
            }
            centerX = 189f;
            centerY = 434;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            if(formula < radius){
                listener.exitScreen(this,3);
            }


        }

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
        levelSelectMusic.setLooping(true);
        levelSelectMusic.play();
        active = true;
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
        levelSelectMusic.stop();
    }

    @Override
    public void dispose() {
        active = false;
        canvas = null;
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

