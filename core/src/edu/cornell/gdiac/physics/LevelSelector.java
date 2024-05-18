package edu.cornell.gdiac.physics;

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
    private boolean[] buttonsUnlocked = new boolean[15];
    private int[][] allowedInputs;

    /* ====== Ursa Variables ====== */
    /** Film strip for ursa walk animation */
    private FilmStrip ursaWalkFilm;
    /** Film strip for ursa idling animation */
    private FilmStrip ursaIdleFilm;
    /** Texture for Ursa */
    private TextureRegion ursaTexture;
    /** Texture for Ursa's shadow */
    private TextureRegion ursaShadow;
    /** The current position of Ursa */
    private Vector2 ursaPos;
    /** The drawing scale of Ursa */
    private final float ursaScale = 0.5f;
    /** The direction Ursa is facing (1 for right, -1 for left) */
    private float direction = 1;
    /** How far Ursa can move in one update loop */
    private final float ursaMoveDist = 4.5f;
    /** The number of levels */
    private final int numButtons = 15;
    private ParticleEffect effect;
    private float levelsCompleted;
    private FilmStrip[] buttonsFilms = new FilmStrip[numButtons];
    private TextureRegion background;
    private ScreenListener listener;
    GameCanvas canvas;
    private boolean active;

    /** Represents the positions of the buttons in the level selection.
     * (buttonPositions[2i], buttonPositions[2i+1]) is the coordinate of the ith button */
    private final Vector2[] buttonPositions;
    /** Scales between original image size and what we draw. */
    private float scale;
    private Music levelSelectMusic;
    /** The frame we are on */
    private int time;

    /** The drawing scale of the buttons */
    private final float buttonScale = 0.6f;
    private float buttonRadius;
    private boolean enterPrevious = false;
    /** The index of the level Ursa is currently standing on.
     * If Ursa is between levels, currentLevel is the lower one */
    private int currentLevel;
    private boolean onCurrentLevel = true;

    /** The coordinates of where Ursa is trying to go */
    private Vector2 moveTarget;
    /** The index of the button target */
    private int moveTargetIndex;
    private boolean wasMoving;


    public LevelSelector(GameCanvas NewCanvas, float completion, int currLevel){
        currentLevel = currLevel;
        levelsCompleted = completion;
        active = false;
        canvas = NewCanvas;
        canvas.setCam(canvas.getWidth() / 2f, canvas.getHeight() / 2f);

        Gdx.input.setInputProcessor( this );
        Arrays.fill(buttonsUnlocked, false);
        buttonsUnlocked[0] = true;

        effect = new ParticleEffect();
        effect.load(Gdx.files.internal("particle.p"),Gdx.files.internal(""));
        effect.getEmitters().first().setPosition(canvas.getWidth()/2f,canvas.getHeight());
        effect.start();

        // I know this is bad but it works lol.
        buttonPositions = new Vector2[] {
                new Vector2(298.8f, 210.9f),
                new Vector2(755.9f, 210.9f),
                new Vector2(755.9f, 843.8f),
                new Vector2(1318.4f, 843.8f),
                new Vector2(1318.4f, 421.9f),
                new Vector2(1880.9f, 421.9f),
                new Vector2(1880.9f, 843.8f),
                new Vector2(1880.9f, 1265.6f),
                new Vector2(2443.4f, 1265.6f),
                new Vector2(2443.4f, 843.8f),
                new Vector2(2970.7f, 843.8f),
                new Vector2(3498, 843.8f),
                new Vector2(3498, 421.9f),
                new Vector2(4060.5f, 421.9f),
                new Vector2(4060.5f, 1125)
        };

        // Up Down Left Right
        // 0 = not allowed, 1 = allowed for forward, 2 = allowed for backward
        allowedInputs = new int[][]{
                {0, 0, 2, 1},
                {1, 2, 0, 0},
                {0, 0, 2, 1},
                {2, 1, 0, 0},
                {0, 0, 2, 1},
                {1, 2, 0, 0},
                {1, 2, 0, 0},
                {0, 0, 2, 1},
                {2, 1, 0, 0},
                {0, 0, 2, 1},
                {0, 0, 2, 1},
                {2, 1, 0, 0},
                {0, 0, 2, 1},
                {1, 2, 0, 0}
        };
    }
    public void setActive(boolean b){
        active = b;
    }

    public void gatherAssets(AssetDirectory directory) {
        TextureRegion[] buttons = new TextureRegion[numButtons];
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
        buttons[14] = new TextureRegion(directory.getEntry("levelSelect:Level15", Texture.class));


        TextureRegion ursaWalk = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        ursaWalkFilm = new FilmStrip(ursaWalk.getTexture(),2,16);
        ursaWalkFilm.setFrame(0);
        TextureRegion ursaIdle = new TextureRegion(directory.getEntry("player:ursaIdle", Texture.class));
        ursaIdleFilm = new FilmStrip(ursaIdle.getTexture(),4,16);
        ursaIdleFilm.setFrame(0);
        ursaShadow = new TextureRegion(directory.getEntry("player:ursaShadow", Texture.class));

        // Set the first level to unlocked and every other level to locked
        // TODO
        for(int i = 0; i < numButtons; i++) {
            buttonsFilms[i] = new FilmStrip(buttons[i].getTexture(),1,2);
            buttonsFilms[i].setFrame(0);
        }

        background = new TextureRegion(directory.getEntry("levelSelect:background", Texture.class));
        levelSelectMusic = directory.getEntry("soundtracks:level_select", Music.class);

        // Unlock levels
        for(int i = 1; i < numButtons; i++) {
            if (levelsCompleted >= i) {
                buttonsUnlocked[i] = true;
                if (levelsCompleted >= i + 1) {
                    buttonsFilms[i].setFrame(0);
                }
            }
        }

        // Scale all button positions down
        scale = (float) canvas.getHeight() / background.getRegionHeight();
        for (Vector2 buttonPosition : buttonPositions) {
            buttonPosition.x *= scale;
            buttonPosition.y *= scale;
        }

        // Set Ursa's position to be on top of the level we just finished
        ursaPos = new Vector2(buttonPositions[currentLevel].x, buttonPositions[currentLevel].y + ursaWalkFilm.getRegionHeight() / 2f * scale);
        buttonRadius = 180 * scale;
        moveTarget = ursaPos;
        moveTargetIndex = currentLevel;
        resetCam();
    }

    private void resetCam() {
        if(ursaPos.x > canvas.getWidth() / 2f && ursaPos.x < background.getRegionWidth() * scale - canvas.getWidth() / 2f) {
            canvas.setCam(ursaPos.x, canvas.getHeight() / 2f);
        } else if(ursaPos.x <= canvas.getWidth() / 2f) {
            canvas.setCam(canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        } else if(ursaPos.x >= background.getRegionWidth() * scale - canvas.getWidth() / 2f) {
            canvas.setCam(background.getRegionWidth() * scale - canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        }
    }

    private void update(float delta){
        time += 1;
        // Update the snow
        effect.update(delta);

        // Move the camera if we won't see outside the background
        if(ursaPos.x > canvas.getWidth() / 2f && ursaPos.x < background.getRegionWidth() * scale - canvas.getWidth() / 2f) {
            canvas.setCam(ursaPos.x, canvas.getHeight() / 2f);
        }

        // Check if we're standing still on the current level
        onCurrentLevel =
                buttonPositions[currentLevel].dst(ursaPos) < 2 * buttonRadius && !wasMoving;

        // Updates move target based on input
        if(onCurrentLevel) {
            wasMoving = checkForInput(currentLevel, true);
        } else {
            wasMoving = checkForInput(currentLevel, false);
        }

        Vector2 lastUrsaPos = new Vector2(ursaPos);
        // Stop moving if we're near the button
        if(ursaPos.dst(moveTarget) <= 2 * ursaMoveDist) {
            currentLevel = moveTargetIndex;
        } else {
            if (Math.abs(moveTarget.x - ursaPos.x) > ursaMoveDist) {
                ursaPos.x += Math.signum(moveTarget.x - ursaPos.x) * ursaMoveDist;
                wasMoving = true;
            }
            if (Math.abs(ursaPos.y - moveTarget.y) > ursaMoveDist) {
                ursaPos.y += Math.signum(moveTarget.y - ursaPos.y) * ursaMoveDist;
                wasMoving = true;
            }
        }

        // Animate Ursa to be walking or idling
        animateUrsa(wasMoving);

        if(lastUrsaPos.dst(ursaPos) < 2.5f) {
            wasMoving = false;
        }
    }

    /**
     * Checks for user input if applicable
     * Updates moveTarget accordingly
     * If we are on a button, then we should also check the controls for backwards movement
     * @return if Ursa is moving
     */
    private boolean checkForInput(int currLevel, boolean checkBack) {
        // If we are on a button, then we should look at the inputs for backwards movement
        if(checkBack && currLevel - 1 >= 0 && currLevel - 1 < allowedInputs.length) {
            for(int i = 0; i < 4; i++) {
                int effect = allowedInputs[currLevel - 1][i];
                if((effect == 2) && checkKeyPress(i)) {
                    // If it corresponds to moving backward
                    currentLevel--;
                    moveTarget = new Vector2(buttonPositions[currLevel - 1]);
                    direction = -1;
                    moveTarget.y += ursaWalkFilm.getRegionHeight() / 2f * scale;
                    moveTargetIndex = currLevel - 1;
                    return true;
                }
            }
        }

        if(currLevel < 0 || currLevel >= allowedInputs.length) {
            return false;
        }

        for(int i = 0; i < 4; i++) {
            int effect = allowedInputs[currLevel][i];
            if((effect == 1 || effect == 2) && checkKeyPress(i)) {
                // If it corresponds to moving backward
                if(effect == 2) {
                    moveTarget = new Vector2(buttonPositions[currLevel]);
                    direction = -1;
                    moveTarget.y += ursaWalkFilm.getRegionHeight() / 2f * scale;
                    moveTargetIndex = currLevel;
                } else if(currLevel < buttonPositions.length - 1) {
                    // Moving forward
                    moveTarget = new Vector2(buttonPositions[currLevel + 1]);
                    moveTarget.y += ursaWalkFilm.getRegionHeight() / 2f * scale;
                    moveTargetIndex = currLevel + 1;
                    direction = 1;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for a key press corresponding to given index
     * 0 -> Up, W
     * 1 -> Down, S
     * 2 -> Left, A
     * 3 -> Right, D
     * Invariant: 0 <= num <= 3
     * @param num index
     */
    private boolean checkKeyPress(int num) {
        if(num == 0) {
            return Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
        } else if(num == 1) {
            return Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
        } else if(num == 2) {
            return Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        } else if(num == 3) {
            return Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        }
        return false;
    }

    /**
     * Animates Ursa based on whether she is moving or not
     * @param isMoving whether Ursa is moving
     */
    private void animateUrsa(boolean isMoving) {
        if(isMoving){
            if(time % 2 == 0){
                if(ursaWalkFilm.getFrame() == 23){
                    ursaWalkFilm.setFrame(0);
                }
                ursaWalkFilm.setFrame(ursaWalkFilm.getFrame() + 1);
            }
            ursaTexture = ursaWalkFilm;
            ursaIdleFilm.setFrame(0);
        } else {
            if(time % 2 == 0){
                if(ursaIdleFilm.getFrame() == 45){
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


    private void draw(){
        canvas.clear();
        canvas.begin();

        // We only care about the full height of the image fitting in the image, the width can go off the screen
        canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * scale,background.getRegionHeight() * scale);
        float buttonOX = buttonsFilms[0].getRegionWidth() / 2f;
        float buttonOY = buttonsFilms[0].getRegionHeight() / 2f;
        for(int i = 0; i < numButtons; i++) {
            canvas.draw(buttonsFilms[i], Color.WHITE, buttonOX, buttonOY, buttonPositions[i].x, buttonPositions[i].y,0f,scale,scale);
        }
        canvas.draw(ursaShadow, Color.WHITE, ursaShadow.getRegionWidth() / 2f, 0, ursaPos.x, ursaPos.y - ursaScale * ursaWalkFilm.getRegionHeight() /2f, 0, ursaScale, ursaScale);
        canvas.draw(ursaTexture, Color.WHITE, ursaWalkFilm.getRegionWidth() / 2f, ursaWalkFilm.getRegionHeight() / 2f,ursaPos.x,ursaPos.y,0,direction * ursaScale,ursaScale);

        effect.update(1/60f);
        if(effect.isComplete()){
            effect.reset();
        }
        effect.draw(canvas.getSpriteBatch());

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
//        if(active){
//            OrthographicCamera cam = canvas.getCamera();
//            Vector3 touch = new Vector3();
//            cam.unproject(touch.set(screenX, screenY, 0));
//
//            for(int i = 0; i < buttonPositions.length; i++) {
//                // If the click was within the button radius, set to clicked frame
//                if(buttonPositions[i].dst(touch.x,touch.y) < buttonRadius) {
//                    if(i == 0){
//                        buttonsFilms[i].setFrame(1);
//                    }
//                    else {
//                        buttonsFilms[i].setFrame(4);
//                    }
//                    clickedLevel = i;
//                }
//            }
//        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
//        if(active){
//            OrthographicCamera cam = canvas.getCamera();
//            Vector3 touch = new Vector3();
//            cam.unproject(touch.set(screenX, screenY, 0));
//
//            if(clickedLevel >= 0 && clickedLevel < numButtons) {
//                Vector2 buttonPos = buttonPositions[clickedLevel];
//
//                // If the click is still in the same button we pressed
//                if (buttonPos.dst(touch.x, touch.y) < buttonRadius) {
//                    // Move Ursa to that button
//                    ultimateTargetIndex = clickedLevel;
//                    moveTargetIndex = currentLevel + (int) Math.signum(clickedLevel - currentLevel);
//                    moveTarget = new Vector2(buttonPositions[moveTargetIndex]);
//                    moveTarget.y += ursaWalkFilm.getRegionHeight() / 2f * scale;
//                    direction = Math.signum(moveTarget.x - ursaPos.x);
//                }
//            }
//        }
//        // Set all buttons to the unclicked frame
//        buttonsFilms[0].setFrame(0);
//        for(int i = 1; i < numButtons; i++) {
//            buttonsFilms[i].setFrame(3);
//        }

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


            boolean enterPressed = false;
            if(time >= 30){
                enterPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER) || Gdx.input.isKeyPressed(Input.Keys.SPACE);
            }

            if(enterPressed) {
                for(int i = 0; i < buttonPositions.length; i++) {
                    // If the click was within the button radius
                    if(buttonPositions[i].dst(ursaPos) < buttonRadius) {
                        buttonsFilms[i].setFrame(1);
                    }
                }
            }

            if(enterPrevious && !enterPressed){
                for (int i = 0; i < buttonPositions.length; i++) {
                    float interactDistance = buttonRadius;
                    if(buttonsUnlocked[i] && Math.abs(buttonPositions[i].x-ursaPos.x) < interactDistance
                            && Math.abs(buttonPositions[i].y-ursaPos.y) < interactDistance){
                        listener.exitScreen(this,i+1);
                    }
                }
                for(int i = 0; i < numButtons; i++) {
                    buttonsFilms[i].setFrame(0);
                }
            }
            enterPrevious = enterPressed;

            if(Gdx.input.isKeyPressed(Keys.ESCAPE) && time > 30) {
                listener.exitScreen(this,101);
            }
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

