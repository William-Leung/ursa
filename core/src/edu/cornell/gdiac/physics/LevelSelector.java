package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.ScreenListener;
import java.util.Arrays;


public class LevelSelector implements Screen, InputProcessor, ControllerListener {
    private TextureRegion[] buttons = new TextureRegion[20];
    private final float ursaMoveDist = 2.5f;
    private boolean[] buttonsUnlocked = new boolean[14];
    private boolean exiting;
    private FilmStrip ursaWalkFilm;
    private FilmStrip ursaIdleFilm;
    private TextureRegion ursaTexture;
    private TextureRegion ursaShadow;
    private ParticleEffect effect;
    private float levelsCompleted;
    private FilmStrip[] buttonsFilms = new FilmStrip[20];
    private TextureRegion background = new TextureRegion();
    private ScreenListener listener;
    GameCanvas canvas;
    private boolean active;

    /** Represents the positions of the buttons in the level selection.
     * (buttonPositions[2i], buttonPositions[2i+1]) is the coordinate of the ith button */
    private final float[] buttonPositions;
    private final float interactDistance = 45f;
    /** Scales between original image size and what we draw. */
    private float scale;
    /** The number of currently active levels */
    private final int numButtons = 8;
    /** The index of the level we last played. */
    private final int startingLevel;
    private Music levelSelectMusic;
    /** The frame we are on */
    private int time;
    /** The current position of Ursa */
    private Vector2 ursaPos;
    /** The drawing scale of the buttons */
    private final float buttonScale = 0.6f;
    /** The drawing scale of Ursa */
    private final float ursaScale = 0.4f;
    /** The direction Ursa is facing (1 for right, -1 for left) */
    private float direction = 1;
    private boolean isMovingToButton = false;
    private int clickedLevel = -1;
    private boolean isSecondPress = false;
    private float moveX;
    private float moveY;
    private final int moveDuration = 90;
    private Vector2 buttonTarget;
    private float buttonRadius;



    public LevelSelector(GameCanvas NewCanvas, float completion, int currLevel){
        startingLevel = currLevel;
        levelsCompleted = completion;
        canvas = NewCanvas;
        active = false;

        Gdx.input.setInputProcessor( this );
        Arrays.fill(buttonsUnlocked, false);
        buttonsUnlocked[0] = true;

       exiting = false;
        effect = new ParticleEffect();
        effect.load(Gdx.files.internal("particle.p"),Gdx.files.internal(""));
        effect.getEmitters().first().setPosition(canvas.getWidth()/2f,canvas.getHeight());
        effect.start();

        // I know this is bad but it works lol.
        buttonPositions = new float[]{304,674,770,674,769.5f,1520,1286,1520,1285,955,1859,955,1860,387,2442,387,2442,1115,2442,1841,3040,1841,3040,1237,3626.5f,1237,3626.5f,676.6f};
    }
    public void setActive(boolean b){
        active = b;

    }

    public void gatherAssets(AssetDirectory directory) {
        buttons[0] = new TextureRegion(directory.getEntry("levelSelect:Level1", Texture.class));
        buttons[1] = new TextureRegion(directory.getEntry("levelSelect:Level2", Texture.class));
        buttons[2] = new TextureRegion(directory.getEntry("levelSelect:Level3", Texture.class));
        buttons[3] = new TextureRegion(directory.getEntry("levelSelect:Level4", Texture.class));
        buttons[4] = new TextureRegion(directory.getEntry("levelSelect:Level5", Texture.class));
        buttons[5] = new TextureRegion(directory.getEntry("levelSelect:Level6", Texture.class));
        buttons[6] = new TextureRegion(directory.getEntry("levelSelect:Level7", Texture.class));
        buttons[7] = new TextureRegion(directory.getEntry("levelSelect:Level8", Texture.class));
        buttons[8] = new TextureRegion(directory.getEntry("levelSelect:Level9", Texture.class));
        buttons[9] = new TextureRegion(directory.getEntry("levelSelect:Level10", Texture.class));
        buttons[10] = new TextureRegion(directory.getEntry("levelSelect:Level11", Texture.class));
        buttons[11] = new TextureRegion(directory.getEntry("levelSelect:Level12", Texture.class));
        buttons[12] = new TextureRegion(directory.getEntry("levelSelect:Level13", Texture.class));
        buttons[13] = new TextureRegion(directory.getEntry("levelSelect:Level14", Texture.class));

        TextureRegion ursaWalk = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        ursaWalkFilm = new FilmStrip(ursaWalk.getTexture(),2,16);
        ursaWalkFilm.setFrame(0);
        TextureRegion ursaIdle = new TextureRegion(directory.getEntry("player:ursaIdle", Texture.class));
        ursaIdleFilm = new FilmStrip(ursaIdle.getTexture(),2,16);
        ursaIdleFilm.setFrame(0);
        ursaShadow = new TextureRegion(directory.getEntry("player:ursaShadow", Texture.class));

        // Set the first level to unlocked and every other level to locked
        buttonsFilms[0] = new FilmStrip(buttons[0].getTexture(),1,2);
        buttonsFilms[0].setFrame(0);
        for(int i = 1; i < numButtons; i++) {
            buttonsFilms[i] = new FilmStrip(buttons[i].getTexture(),1,5);
            buttonsFilms[i].setFrame(0);
        }

        background = new TextureRegion(directory.getEntry("levelSelect:background", Texture.class));
        levelSelectMusic = directory.getEntry("soundtracks:level_select", Music.class);

        // Unlock levels
        for(int i = 1; i < numButtons; i++) {
            if (levelsCompleted >= i) {
                buttonsUnlocked[i] = true;
                if (levelsCompleted >= i + 1) {
                    buttonsFilms[i].setFrame(3);
                }
            }
        }

        // Scale all button positions down
        scale = (float) canvas.getHeight() / background.getRegionHeight();
        for(int i = 0; i < buttonPositions.length; i++) {
            buttonPositions[i] *= scale;
        }

        // Set Ursa's position to be on top of the level we just finished
        ursaPos = new Vector2(buttonPositions[startingLevel * 2], buttonPositions[startingLevel * 2 + 1] + ursaWalkFilm.getRegionHeight() / 2f * scale);
        buttonRadius = 64 * buttonScale;
    }


    private void update(float delta){
        time += 1;

        // Update the snow
        effect.update(delta);

        // Handle movement for Ursa
        boolean isMoving = false;
        if((Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))){
            ursaPos.x -= ursaMoveDist;
            direction = -1;
            isMoving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            ursaPos.x += ursaMoveDist;
            direction = 1;
            isMoving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            ursaPos.y += ursaMoveDist;
            isMoving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            ursaPos.y -= ursaMoveDist;
            isMoving = true;
        }

        if(isMoving) {
            isMovingToButton = false;
        }

        if(isMovingToButton) {
            ursaPos.x += moveX;
            ursaPos.y += moveY;
            isMoving = true;
        }

        // Animate Ursa to be walking or idling
        animateUrsa(isMoving);
    }

    private void animateUrsa(boolean isMoving) {
        if(isMoving){
            if(time % 2 == 0){
                if(ursaWalkFilm.getFrame() == 19){
                    ursaWalkFilm.setFrame(0);
                }
                ursaWalkFilm.setFrame(ursaWalkFilm.getFrame() + 1);
            }
            ursaTexture = ursaWalkFilm;
            ursaIdleFilm.setFrame(0);
        } else {
            if(time % 2 == 0){
                if(ursaIdleFilm.getFrame() == 29){
                    ursaIdleFilm.setFrame(0);
                }
                ursaIdleFilm.setFrame(ursaIdleFilm.getFrame() + 1);
            }
            ursaTexture = ursaIdleFilm;
            ursaWalkFilm.setFrame(0);
        }
    }

    private void updateButtons(){
        for(int i = 1; i < numButtons; i++){
            if(buttonsFilms[i].getFrame() == 0 ){
                if(time%30 == 0){
                    buttonsFilms[i].setFrame(1);
                }

            }
            else if(buttonsFilms[i].getFrame() ==1 ){
                if(time%30 == 0){
                    buttonsFilms[i].setFrame(2);
                }
            }
            else if(buttonsFilms[i].getFrame() ==2 ){
                if(time%30 == 0){
                    buttonsFilms[i].setFrame(3);
                }
            }
            else{
                buttonsFilms[i].setFrame(3);
            }
        }
    }

    private void enterPressed() {
        System.out.println("Enter Pressed");
        for (int j = 0; j < buttonPositions.length / 2; j++) {
            float posX = buttonPositions[j * 2];
            float posY = buttonPositions[j * 2 + 1];

            if(buttonsUnlocked[j] && Math.abs(posX-ursaPos.x) < interactDistance && Math.abs(posY-ursaPos.y) < interactDistance){
                listener.exitScreen(this,j+1);
            }
        }
    }

    private void draw(){
        canvas.clear();
        canvas.begin();


        // We only care about the full height of the image fitting in the image, the width can go off the screen
        canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * scale,background.getRegionHeight() * scale);
        float buttonOX = buttonsFilms[0].getRegionWidth() / 2f;
        float buttonOY = buttonsFilms[0].getRegionHeight() / 2f;
        for(int i = 0; i < numButtons; i++) {
            canvas.draw(buttonsFilms[i], Color.WHITE, buttonOX, buttonOY, buttonPositions[2*i], buttonPositions[2*i+1],0f,buttonScale,buttonScale);
        }
        canvas.draw(ursaShadow, Color.WHITE, ursaShadow.getRegionWidth() / 2f, 0, ursaPos.x, ursaPos.y - ursaScale * ursaWalkFilm.getRegionHeight() / 2f, 0, ursaScale, ursaScale);
        canvas.draw(ursaTexture, Color.WHITE, ursaWalkFilm.getRegionWidth() / 2f, ursaWalkFilm.getRegionHeight() / 2f,ursaPos.x,ursaPos.y,0,direction * ursaScale,ursaScale);


        effect.update(1/60f);
        if(effect.isComplete()){
            effect.reset();
        }

        effect.draw(canvas.getSpriteBatch());
        canvas.end();


        if((Gdx.input.isKeyPressed(Input.Keys.ENTER) || Gdx.input.isKeyPressed(Keys.SPACE)) && !exiting){
            enterPressed();
        }

        // If we've fully moved to the button
        if(isMovingToButton && ursaPos.dst(buttonTarget) < buttonRadius) {
            listener.exitScreen(this,clickedLevel+1);
        }
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
            OrthographicCamera cam = canvas.getCamera();
            Vector3 touch = new Vector3();
            cam.unproject(touch.set(screenX, screenY, 0));

            for(int i = 0; i < buttonPositions.length / 2; i++) {
                Vector2 buttonPos = new Vector2(buttonPositions[i * 2], buttonPositions[i * 2 + 1]);

                // If the click was within the button radius
                if(buttonPos.dst(touch.x,touch.y) < buttonRadius) {
                    if(i == 0){
                        buttonsFilms[i].setFrame(1);
                    }
                    else {
                        buttonsFilms[i].setFrame(4);
                    }
                    clickedLevel = i;
                }
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if(active){
            OrthographicCamera cam = canvas.getCamera();
            Vector3 touch = new Vector3();
            cam.unproject(touch.set(screenX, screenY, 0));

            if(clickedLevel >= 0 && clickedLevel < numButtons) {
                Vector2 buttonPos = new Vector2(buttonPositions[clickedLevel * 2], buttonPositions[clickedLevel * 2 + 1]);

                // If the click is still in the same button we pressed
                if (buttonPos.dst(touch.x, touch.y) < buttonRadius) {
                    // Move Ursa to that button
                    isMovingToButton = true;
                    moveX = (buttonPos.x - ursaPos.x) / moveDuration;
                    direction = Math.signum(buttonPos.x - ursaPos.x);
                    moveY = (buttonPos.y - ursaPos.y + buttonRadius) / moveDuration;

                    buttonTarget = buttonPos;
                }
            }
        }
        buttonsFilms[0].setFrame(0);
        for(int i = 1; i < numButtons; i++) {
            buttonsFilms[i].setFrame(3);
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

