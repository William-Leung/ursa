package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.ScreenListener;


public class RetryMenu implements Screen, InputProcessor, ControllerListener {
    private TextureRegion backgroundWin = new TextureRegion();
    private  TextureRegion backgroundLose = new TextureRegion();
    private TextureRegion retryButton;
    private TextureRegion selectButton;
    private FilmStrip retryFilm;
    private FilmStrip levelSelectFilm;
    private ScreenListener listener;
    private float buttonWidth;
    private boolean win;
    GameCanvas canvas;
    private TextureRegion[] retryTextures = new TextureRegion[4];
    private TextureRegion blackTexture;
    private boolean active;
    private Music levelRetryMusic;
    private float textureScale = .4f;
    private float changeOffset;
    public RetryMenu(GameCanvas NewCanvas,boolean win){
        canvas = NewCanvas;
        this.win = win;
        active = false;
        Gdx.input.setInputProcessor( this );
        changeOffset = 0;

    }
    public void setActive(boolean b){
        active = b;
    }
    public void gatherAssets(AssetDirectory directory) {
       retryButton = new TextureRegion(directory.getEntry("levelSelect:retryUI", Texture.class));
        selectButton = new TextureRegion(directory.getEntry("levelSelect:levelSelectUI", Texture.class));
        retryFilm = new FilmStrip(retryButton.getTexture(),1,2);
        retryFilm.setFrame(0);
        levelSelectFilm =  new FilmStrip(selectButton.getTexture(),1,2);
        levelSelectFilm.setFrame(0);
        backgroundLose = new TextureRegion(directory.getEntry("levelSelect:lose", Texture.class));
        backgroundWin = new TextureRegion(directory.getEntry("levelSelect:win", Texture.class));
        levelRetryMusic = directory.getEntry("soundtracks:level_retry", Music.class);
        retryTextures[0] =  new TextureRegion(directory.getEntry( "retry:lose1",Texture.class));
        retryTextures[1] =  new TextureRegion(directory.getEntry( "retry:lose2",Texture.class));
        retryTextures[2] =  new TextureRegion(directory.getEntry( "retry:victory1",Texture.class));
        retryTextures[3] =  new TextureRegion(directory.getEntry( "retry:victory2",Texture.class));
        blackTexture = new TextureRegion(directory.getEntry("polar:black", Texture.class));
    }

    private void update(float delta){
        changeOffset  = Math.min(changeOffset + 50, 500);
        if(!win){
            if(Gdx.input.isKeyPressed(Input.Keys.SPACE)){
                listener.exitScreen(this,1);
            }
            if(Gdx.input.isKeyPressed(Input.Keys.ESCAPE)){
                listener.exitScreen(this,2);
            }
            changeOffset = Math.min(changeOffset +50, 500);

        }
        else{
            if(Gdx.input.isKeyPressed(Input.Keys.ESCAPE)){
                listener.exitScreen(this,2);
            }
            if(Gdx.input.isKeyPressed(Input.Keys.SPACE)){
                listener.exitScreen(this,3);
            }

        }

    }

    private void draw(){
        if(this.canvas != null){
            canvas.clear();
        }


        canvas.begin();
        Color color = new Color(1,1,1,1);
        if(win){
            canvas.draw(blackTexture,color, canvas.getCameraX() - canvas.getWidth() /2f, canvas.getCameraY() - canvas.getHeight() /2f, canvas.getWidth(), canvas.getHeight());
            canvas.draw(retryTextures[2],Color.WHITE, 0,retryTextures[0].getRegionHeight() / 2f,canvas.getCameraX() - canvas.getWidth()/2  + changeOffset * 1.4f - 700, canvas.getCameraY(),0,.65f,.65f );
            canvas.draw(retryTextures[3],Color.WHITE,retryTextures[1].getRegionWidth(),retryTextures[1].getRegionHeight() /2f,canvas.getCameraX()  + canvas.getWidth() - changeOffset * 1.5f + 250  , canvas.getCameraY(),0,.65f,.65f);
        }
        else {

            canvas.draw(blackTexture,color, canvas.getCameraX() - canvas.getWidth() /2f, canvas.getCameraY() - canvas.getHeight() /2f, canvas.getWidth(), canvas.getHeight());
            canvas.draw(retryTextures[0],Color.WHITE, 0,retryTextures[0].getRegionHeight() / 2f,canvas.getCameraX() - canvas.getWidth()/2 - 500 + changeOffset, canvas.getCameraY(),0,.65f,.65f );
            canvas.draw(retryTextures[1],Color.WHITE,retryTextures[1].getRegionWidth(),retryTextures[1].getRegionHeight() /2f,canvas.getCameraX() + 250 + canvas.getWidth() - changeOffset * 1.5f   , canvas.getCameraY(),0,.65f,.65f);
        }





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
        if(active) {
            screenY = canvas.getHeight() - screenY;
            if(screenX >= 133 && screenX <=274 && screenY >= 330 && screenY <= 385){
                retryFilm.setFrame(1);
            }
            if(screenX >= 130 && screenX <= 272 && screenY >= 250 && screenY <= 305){
                levelSelectFilm.setFrame(1);
            }
        }


        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (active) {
            screenY = canvas.getHeight() - screenY;
            if(screenX >= 133 && screenX <=274 && screenY >= 330 && screenY <= 385){
                listener.exitScreen(this,1);
            }
            if(screenX >= 130 && screenX <= 272 && screenY >= 250 && screenY <= 305){
                listener.exitScreen(this,2);
            }
            retryFilm.setFrame(0);
            levelSelectFilm.setFrame(0);
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
        active = true;
        levelRetryMusic.setLooping(true);
        levelRetryMusic.play();
    }

    @Override
    public void render(float delta) {
        if(active){

            draw();
            update(delta);

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
        levelRetryMusic.stop();
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

