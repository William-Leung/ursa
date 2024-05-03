package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
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
    private static final TextureRegion redTextureregion;

    static {
        Pixmap redPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        redPixmap.setColor(new Color(1, 0, 0, 1));
        redPixmap.fill();
        Texture redTexture = new Texture(redPixmap);
        redTextureregion = new TextureRegion(redTexture);
    }
    private TextureRegion[] buttons = new TextureRegion[20];
    private boolean button1Pressed;
    private TextureRegion ursa;
    private JsonReader json;
    private float ursaStartX;
    private float ursaStartY;
    private float ursaNewX;
    private float ursaNewY;
    private float time;
    private JsonValue jsonData;
    private boolean button2Pressed;
    private boolean button3Pressed;
    private float[] completedLevels;
    private boolean button1Locked;
    private boolean button2Locked;
    private FilmStrip ursaFilm;

    private boolean button3Locked;
    private boolean button4Locked;
    private float levelsCompleted;
    private FilmStrip button1;
    private FilmStrip[] buttonsFilms = new FilmStrip[20];
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

    private final float[] buttonPositions = new float[]{153, 337, 388, 337, 388, 760,};
    float backgroundScaleFactor;

    private Music levelSelectMusic;
    public LevelSelector(GameCanvas NewCanvas,float completion){
        ursaStartX = 78;
        ursaStartY = 196f;
        ursaNewX = 78;
        ursaNewY = 196f;
        levelsCompleted = completion;
        canvas = NewCanvas;
        active = false;
        Gdx.input.setInputProcessor( this );
        button1Pressed = false;






    }
    public void setActive(boolean b){
        active = b;
    }
    public void gatherAssets(AssetDirectory directory) {
        buttons[0] = new TextureRegion(directory.getEntry("levelSelect:Level1", Texture.class));
        buttons[1] = new TextureRegion(directory.getEntry("levelSelect:Level2", Texture.class));
        buttons[2] = new TextureRegion(directory.getEntry("levelSelect:Level3", Texture.class));
        buttons[3] = new TextureRegion(directory.getEntry("levelSelect:Level4", Texture.class));

        ursa = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        ursaFilm = new FilmStrip(ursa.getTexture(),2,8);
        ursaFilm.setFrame(0);

        buttonsFilms[0] = new FilmStrip(buttons[0].getTexture(),1,2);
        buttonsFilms[0].setFrame(0);
        buttonsFilms[1] = new FilmStrip(buttons[1].getTexture(),1,5);
        buttonsFilms[1].setFrame(0);
        buttonsFilms[2] = new FilmStrip(buttons[2].getTexture(),1,5);
        buttonsFilms[2].setFrame(0);
        buttonsFilms[3] = new FilmStrip(buttons[3].getTexture(),1,5);
        buttonsFilms[3].setFrame(0);
        background = new TextureRegion(directory.getEntry("levelSelect:background", Texture.class));
        levelSelectMusic = directory.getEntry("soundtracks:level_select", Music.class);

        button2Locked = true;
        button3Locked = true;
        button4Locked = true;
        if(levelsCompleted >= 1){
            button2Locked = false;
            if(levelsCompleted >= 2){
                buttonsFilms[1].setFrame(3);
            }
        }

        if(levelsCompleted >= 2){
            button3Locked = false;
            if(levelsCompleted >= 3){
                buttonsFilms[2].setFrame(3);
            }

        }
        if(levelsCompleted >= 3){
            button4Locked = false;
        }

    }
    private void update(float delta){

        if(levelsCompleted == 1){
            button2Locked = false;
        }
        if(ursaFilm.getFrame() == 11){
            ursaFilm.setFrame(0);
        }

        time += 1;

        if(button1Pressed){
            buttonsFilms[0].setFrame(1);
        }else {
            buttonsFilms[0].setFrame(0);
        }
        if(button2Pressed){

            buttonsFilms[1].setFrame(4);
        }
        else if(button2Locked ){
            buttonsFilms[1].setFrame(0);
        }
        else {

            if(buttonsFilms[1].getFrame() == 0 ){
                if(time%30 == 0){
                    buttonsFilms[1].setFrame(1);
                }

            }
            else if(buttonsFilms[1].getFrame() ==1 ){
                if(time%30 == 0){
                    buttonsFilms[1].setFrame(2);
                }
            }
            else if(buttonsFilms[1].getFrame() ==2 ){
                if(time%30 == 0){
                    buttonsFilms[1].setFrame(3);
                }
            }
            else{
                buttonsFilms[1].setFrame(3);
            }

        }

        if(button3Pressed){
            buttonsFilms[2].setFrame(4);
        }
        else if(button3Locked){
            buttonsFilms[2].setFrame(0);
        }
        else {

            if(buttonsFilms[2].getFrame() == 0 ){
                if(time%30 == 0){
                    buttonsFilms[2].setFrame(1);
                }

            }
            else if(buttonsFilms[2].getFrame() ==1 ){
                if(time%30 == 0){
                    buttonsFilms[2].setFrame(2);
                }
            }
            else if(buttonsFilms[2].getFrame() ==2 ){
                if(time%30 == 0){
                    buttonsFilms[2].setFrame(3);
                }
            }
            else buttonsFilms[2].setFrame(3);
        }
        if(Math.abs(ursaStartX-ursaNewX) > 5 && ursaStartX < ursaNewX){

            if(time% 2 == 0){
                ursaFilm.setFrame(ursaFilm.getFrame() +1);
            }

            ursaStartX += 2.0f;
        }
        if(Math.abs(ursaStartX-ursaNewX) > 5 && ursaStartX > ursaNewX){
            if(time% 2 == 0){
                ursaFilm.setFrame(ursaFilm.getFrame() +1);
            }

            ursaStartX -= 2.0f;
        }
        if(Math.abs(ursaStartY-ursaNewY) > 5 && ursaStartY < ursaNewY){
            ursaStartY += 3.0f;


        }
        if(Math.abs(ursaStartY-ursaNewY) > 5 && ursaStartY > ursaNewY){

            ursaStartY -= 3.0f;
        }
        if(Math.abs(ursaStartY-ursaNewY) > 5 && Math.abs(ursaStartX - ursaNewX) <5 && ursaFilm.getFrame() < 20){
            ursaFilm.setFrame(ursaFilm.getFrame() + 1);
        }

        if(Math.abs(ursaStartX - ursaNewX) < 10 && Math.abs(ursaStartY - ursaNewY) < 10){
            ursaFilm.setFrame(3);
        }

    }

    private void draw(){

        canvas.clear();

        canvas.begin();



        // We only care about the full height of the image fitting in the image, the width can go off the screen
        backgroundScaleFactor = (float) canvas.getHeight() / background.getRegionHeight();
        float buttonWidth = 128 * backgroundScaleFactor;
        float buttonHeight = 128 * backgroundScaleFactor;

        //System.out.println("Button " + buttonPositions[0] * backgroundScaleFactor + " " + buttonPositions[1] * backgroundScaleFactor);
        //System.out.println("Ursa " + ursaStartX + " " + ursaStartY);
        //System.out.println(buttonsFilms[0].getRegionWidth() + " " + buttonsFilms[0].getRegionHeight());

        /**canvas.draw(buttonsFilms[0],Color.WHITE,buttonsFilms[0].getRegionWidth() / 4f,buttonsFilms[0].getRegionHeight() / 4f,buttonPositions[0] * backgroundScaleFactor,buttonPositions[1] * backgroundScaleFactor, buttonWidth, buttonHeight);
        //canvas.draw(buttonsFilms[0],Color.WHITE, buttonCenter.x, buttonCenter.y, buttonPositions[0] * backgroundScaleFactor,buttonPositions[1] * backgroundScaleFactor,256 * backgroundScaleFactor,256 * backgroundScaleFactor);
        canvas.draw(buttonsFilms[1],Color.WHITE,buttonsFilms[1].getRegionWidth() / 4f,buttonsFilms[1].getRegionHeight() / 4f,buttonPositions[2] * backgroundScaleFactor,buttonPositions[3] * backgroundScaleFactor, buttonWidth, buttonHeight);
        canvas.draw(buttonsFilms[2],Color.WHITE,buttonsFilms[2].getRegionWidth() / 4f,buttonsFilms[2].getRegionHeight() / 4f,buttonPositions[4] * backgroundScaleFactor,buttonPositions[5] * backgroundScaleFactor, buttonWidth, buttonHeight);*/
         canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * backgroundScaleFactor,background.getRegionHeight() * backgroundScaleFactor);
        //canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * .25f,background.getRegionHeight() * .285f);
        canvas.draw(buttonsFilms[0],Color.WHITE,40,155,buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[1],Color.WHITE,152.5f,155,buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[2],Color.WHITE,152.5f,395,buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[3],Color.WHITE,285f,395,buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(ursaFilm,Color.WHITE,ursaStartX,ursaStartY,ursaFilm.getRegionWidth() * .3f,ursaFilm.getRegionHeight() * .3f);
        //canvas.draw(redTextureregion,Color.WHITE, 0,0,86,189,10,10);

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
            float radius = (buttonsFilms[0].getRegionWidth()/2.0f) -25;
            //System.out.println("RAdius is: " + radius);
            float centerX = 78f;
            float centerY = 196f;
            screenY = canvas.getHeight()-screenY;
            float formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            //System.out.println("Formula: "+ formula);
            if(formula < radius  ){
                System.out.println("running this");
                if(Math.abs(ursaStartX - centerX) < 10 && Math.abs(ursaStartY - centerY) < 10){
                    button1Pressed = true;
                }
               ursaNewX = centerX;
               ursaNewY = centerY;
            }
            centerX = 189f;
            centerY = 196f;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            System.out.println("levels complete: "+ levelsCompleted);
            if(formula < radius && button2Locked == false ){
                if(Math.abs(ursaStartX - centerX) < 7 && Math.abs(ursaStartY - centerY) < 7){
                    button2Pressed = true;
                }

                ursaNewX = centerX;
                ursaNewY = centerY;
            }


            centerX = 189f;
            centerY = 434;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);

            if(formula < radius && button3Locked == false ){
                System.out.println(button3Locked + "button 3 locked");
                if(Math.abs(ursaStartX - centerX) < 7 && Math.abs(ursaStartY - centerY) < 7){
                    button3Pressed = true;
                }

                ursaNewX = centerX;
                ursaNewY = centerY;
                System.out.println(ursaNewY + " ursa new y is ");
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
            float radius = (buttonsFilms[0].getRegionWidth()/2.0f) -25;
            System.out.println("RAdius is: " + radius);
            float centerX = 78f;
            float centerY = 196f;
            screenY = canvas.getHeight()-screenY;
            float formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            System.out.println("Formula: "+ formula);
            if(formula < radius && Math.abs(ursaStartX - centerX) < 7 && Math.abs(ursaStartX - centerX) < 7){

               listener.exitScreen(this,1);
            }
            centerX = 189f;
            centerY = 196f;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);

            if(formula < radius && button2Locked == false && Math.abs(ursaStartX - centerX) <7 && Math.abs(ursaStartY - centerY) < 7){
               listener.exitScreen(this,2);
            }
            if(formula < radius && levelsCompleted == 1 ){
                button2Locked = false;
            }
            centerX = 189f;
            centerY = 434;
            formula = (screenX-centerX)*(screenX-centerX)+(screenY-centerY)*(screenY-centerY);
            formula = (float) Math.sqrt(formula);
            if(formula < radius && button3Locked == false && Math.abs(ursaStartX - centerX) <7 && Math.abs(ursaStartY - centerY) < 7){

                listener.exitScreen(this,3);
            }
            if(formula < radius && levelsCompleted == 2 ){
                button3Locked = false;
            }



        }
        button3Pressed = false;
        button2Pressed = false;
        button1Pressed = false;

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

