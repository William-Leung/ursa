package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
    private JsonReader json;
    private float ursaStartX;
    private float ursaStartY;
    private float ursaNewX;
    private float ursaNewY;
    private float direction;
    private float[] positions;
    private float time;
    private JsonValue jsonData;
    private float ursaMoveDist;
    private boolean[] buttonsPressed = new boolean[14];
    private boolean[] buttonsUnlocked = new boolean[14];
    private boolean button2Pressed;
    private boolean button3Pressed;
    private boolean exiting;
    private float[] completedLevels;
    private boolean button1Locked;
    private boolean button2Locked;
    private FilmStrip ursaWalkFilm;
    private FilmStrip ursaIdleFilm;
    private TextureRegion ursaTexture;
    private ParticleEffect effect;


    private boolean button3Locked;
    private boolean button4Locked;
    private float levelsCompleted;
    private FilmStrip button1;
    private FilmStrip[] buttonsFilms = new FilmStrip[20];

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
        direction = 1;
        ursaMoveDist = 2.5f;
        ursaStartX = 78;
        ursaStartY = 196f;
        ursaNewX = 78;
        ursaNewY = 196f;
        levelsCompleted = completion;
        canvas = NewCanvas;
        active = false;
        Gdx.input.setInputProcessor( this );
        button1Pressed = false;
        for(int i = 0; i < buttonsPressed.length;i++){
            buttonsPressed[i] = false;
        }
        buttonsUnlocked[0] = true;
        for(int i = 1; i < buttonsUnlocked.length;i++){
            buttonsUnlocked[i] = false;
        }
        positions = new float[]{.0391f * canvas.getWidth(),.2691f * canvas.getHeight(),.1523f* canvas.getWidth(),.2691f * canvas.getHeight(),
                .1523f * canvas.getWidth(),.684f * canvas.getHeight(),.2773f * canvas.getWidth(),.684f * canvas.getHeight(),
                .2773f * canvas.getWidth(),.41f * canvas.getHeight(),0.416015625f * canvas.getWidth(),.41f * canvas.getHeight(),
                0.416015625f * canvas.getWidth(),.1181f * canvas.getHeight(),.5586f * canvas.getWidth(),.1181f * canvas.getHeight(),
                .5586f * canvas.getWidth(),.4931f * canvas.getHeight(),.5586f * canvas.getWidth(),.8507f * canvas.getHeight(),
                .706f * canvas.getWidth(),.8507f * canvas.getHeight(),.706f * canvas.getWidth(),.5417f * canvas.getHeight(),
                .8496f * canvas.getWidth(),.5417f * canvas.getHeight(),.8496f * canvas.getWidth(),.2656f * canvas.getHeight()};
        exiting = false;
        effect = new ParticleEffect();
        effect.load(Gdx.files.internal("particle.p"),Gdx.files.internal(""));
        effect.getEmitters().first().setPosition(canvas.getWidth()/2,canvas.getHeight());
        effect.start();




        System.out.println(effect.isComplete() + " is comple");


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

        buttonsFilms[0] = new FilmStrip(buttons[0].getTexture(),1,2);
        buttonsFilms[0].setFrame(0);
        buttonsFilms[1] = new FilmStrip(buttons[1].getTexture(),1,5);
        buttonsFilms[1].setFrame(0);
        buttonsFilms[2] = new FilmStrip(buttons[2].getTexture(),1,5);
        buttonsFilms[2].setFrame(0);
        buttonsFilms[3] = new FilmStrip(buttons[3].getTexture(),1,5);
        buttonsFilms[3].setFrame(0);
        buttonsFilms[4] = new FilmStrip(buttons[4].getTexture(),1,5);
        buttonsFilms[4].setFrame(0);
        buttonsFilms[5] = new FilmStrip(buttons[5].getTexture(),1,5);
        buttonsFilms[5].setFrame(0);
        buttonsFilms[6] = new FilmStrip(buttons[6].getTexture(),1,5);
        buttonsFilms[6].setFrame(0);
        buttonsFilms[7] = new FilmStrip(buttons[7].getTexture(),1,5);
        buttonsFilms[7].setFrame(0);
        buttonsFilms[8] = new FilmStrip(buttons[8].getTexture(),1,5);
        buttonsFilms[8].setFrame(0);
        buttonsFilms[9] = new FilmStrip(buttons[9].getTexture(),1,5);
        buttonsFilms[9].setFrame(0);
        buttonsFilms[10] = new FilmStrip(buttons[10].getTexture(),1,5);
        buttonsFilms[10].setFrame(0);
        buttonsFilms[11] = new FilmStrip(buttons[11].getTexture(),1,5);
        buttonsFilms[11].setFrame(0);
        buttonsFilms[12] = new FilmStrip(buttons[12].getTexture(),1,5);
        buttonsFilms[12].setFrame(0);
        buttonsFilms[13] = new FilmStrip(buttons[13].getTexture(),1,5);
        buttonsFilms[13].setFrame(0);
        background = new TextureRegion(directory.getEntry("levelSelect:background", Texture.class));
        levelSelectMusic = directory.getEntry("soundtracks:level_select", Music.class);


        if(levelsCompleted >= 1){
            buttonsUnlocked[1] = true;
            if(levelsCompleted >= 2){
                buttonsFilms[1].setFrame(3);
            }
        }

        if(levelsCompleted >= 2){
            buttonsUnlocked[2] = true;
            if(levelsCompleted >= 3){
                buttonsFilms[2].setFrame(3);
            }

        }
        if(levelsCompleted >= 3){
            buttonsUnlocked[3] = true;
            if(levelsCompleted >= 4){
                buttonsFilms[3].setFrame(3);
            }
        }
        if(levelsCompleted >= 4){
            buttonsUnlocked[4] = true;
            if(levelsCompleted >= 5){
                buttonsFilms[4].setFrame(3);
            }
        }
        if(levelsCompleted >= 5){
            buttonsUnlocked[5] = true;
            if(levelsCompleted >= 6){
                buttonsFilms[5].setFrame(3);
            }
        }
        if(levelsCompleted >= 6){
            buttonsUnlocked[6] = true;
            if(levelsCompleted >= 7){
                buttonsFilms[6].setFrame(3);
            }
        }
        if(levelsCompleted >= 7){
            buttonsUnlocked[7] = true;
            if(levelsCompleted >= 8){
                buttonsFilms[7].setFrame(3);
            }
        }
        if(levelsCompleted >= 8){
            buttonsUnlocked[8] = true;
            if(levelsCompleted >= 9){
                buttonsFilms[8].setFrame(3);
            }
        }
        if(levelsCompleted >= 9){
            buttonsUnlocked[9] = true;
            if(levelsCompleted >= 10){
                buttonsFilms[9].setFrame(3);
            }
        }
        if(levelsCompleted >= 10){
            buttonsUnlocked[10] = true;
            if(levelsCompleted >= 11){
                buttonsFilms[10].setFrame(3);
            }
        }
        if(levelsCompleted >= 11){
            buttonsUnlocked[11] = true;
            if(levelsCompleted >= 12){
                buttonsFilms[11].setFrame(3);
            }
        }
        if(levelsCompleted >= 12){
            buttonsUnlocked[12] = true;
            if(levelsCompleted >= 13){
                buttonsFilms[12].setFrame(3);
            }
        }
        if(levelsCompleted >= 13){
            buttonsUnlocked[13] = true;
            if(levelsCompleted >= 14){
                buttonsFilms[13].setFrame(3);
            }
        }

    }
    private void update(float delta){
        effect.update(delta);

        time += 1;
        boolean isMoving = false;
        if((Gdx.input.isKeyPressed(Input.Keys.LEFT))){
            ursaStartX -= ursaMoveDist;
            ursaNewX = ursaStartX;
            direction = -1;
            isMoving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
            ursaStartX += ursaMoveDist;
            ursaNewX = ursaStartX;
            direction = 1;
            isMoving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP)){
            ursaStartY += ursaMoveDist;
            ursaNewY = ursaStartY;
            isMoving = true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN)){
            ursaStartY -= ursaMoveDist;
            ursaNewY = ursaStartY;
            isMoving = true;
        }

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


        updateButtons();






        if(Math.abs(ursaStartX-ursaNewX) > 5 && ursaStartX < ursaNewX){

            if(time% 2 == 0){
                ursaWalkFilm.setFrame(ursaWalkFilm.getFrame() +1);
            }

            ursaStartX += 2.0f;
        }
        if(Math.abs(ursaStartX-ursaNewX) > 5 && ursaStartX > ursaNewX){
            if(time% 2 == 0){
                ursaWalkFilm.setFrame(ursaWalkFilm.getFrame() +1);
            }

            ursaStartX -= 2.0f;
        }
        if(Math.abs(ursaStartY-ursaNewY) > 5 && ursaStartY < ursaNewY){
            ursaStartY += 3.0f;


        }
        if(Math.abs(ursaStartY-ursaNewY) > 5 && ursaStartY > ursaNewY){

            ursaStartY -= 3.0f;
        }
        if(Math.abs(ursaStartY-ursaNewY) > 5 && Math.abs(ursaStartX - ursaNewX) <5 && ursaWalkFilm.getFrame() < 20){
            ursaWalkFilm.setFrame(ursaWalkFilm.getFrame() + 1);
        }

        if(Math.abs(ursaStartX - ursaNewX) < 10 && Math.abs(ursaStartY - ursaNewY) < 10 && !Gdx.input.isKeyPressed(Input.Keys.LEFT) && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)&& !Gdx.input.isKeyPressed(Input.Keys.UP) && !Gdx.input.isKeyPressed(Input.Keys.DOWN)){
            ursaWalkFilm.setFrame(3);
        }


    }
    private void updateButtons(){

        if(buttonsPressed[0]){
            buttonsFilms[0].setFrame(1);
        }else {
            buttonsFilms[0].setFrame(0);
        }


        for(int i =1; i< buttonsPressed.length; i++){
            if(buttonsPressed[i]){

                buttonsFilms[i].setFrame(4);
            }
            else if(!buttonsUnlocked[i] ){
                buttonsFilms[i].setFrame(0);
            }
            else {

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






    }

    private void enterPressed() {
        System.out.println("enter Pressed");
        for (int j = 0; j < positions.length / 2; j++) {
            float posX = positions[j * 2];
            float posY = positions[j * 2 + 1];

            if(buttonsUnlocked[j] && Math.abs(posX-ursaStartX) < 30 && Math.abs(posY-ursaStartY)<30){
                listener.exitScreen(this,j+1);
            }


        }
    }

    private void draw(){

        canvas.clear();

        canvas.begin();


        float defaultHeight = 576;
        float defaultWidth = 1024;

        // We only care about the full height of the image fitting in the image, the width can go off the screen
        backgroundScaleFactor = (float) canvas.getHeight() / background.getRegionHeight();
        float buttonWidth = 128 * backgroundScaleFactor;
        float buttonHeight = 128 * backgroundScaleFactor;
        canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * backgroundScaleFactor,background.getRegionHeight() * backgroundScaleFactor);
        //System.out.println(background.getRegionWidth() * backgroundScaleFactor + " " + background.getRegionHeight() * backgroundScaleFactor);
        //canvas.draw(buttonsFilms[0],Color.WHITE, buttonCenter.x, buttonCenter.y, buttonPositions[0] * backgroundScaleFactor,buttonPositions[1] * backgroundScaleFactor,256 * backgroundScaleFactor,256 * backgroundScaleFactor

        //System.out.println("Button " + buttonPositions[0] * backgroundScaleFactor + " " + buttonPositions[1] * backgroundScaleFactor);
        //System.out.println("Ursa " + ursaStartX + " " + ursaStartY);
        //System.out.println(buttonsFilms[0].getRegionWidth() + " " + buttonsFilms[0].getRegionHeight());

        /**canvas.draw(buttonsFilms[0],Color.WHITE,buttonsFilms[0].getRegionWidth() / 4f,buttonsFilms[0].getRegionHeight() / 4f,buttonPositions[0] * backgroundScaleFactor,buttonPositions[1] * backgroundScaleFactor, buttonWidth, buttonHeight);
        //canvas.draw(buttonsFilms[0],Color.WHITE, buttonCenter.x, buttonCenter.y, buttonPositions[0] * backgroundScaleFactor,buttonPositions[1] * backgroundScaleFactor,256 * backgroundScaleFactor,256 * backgroundScaleFactor);
        canvas.draw(buttonsFilms[1],Color.WHITE,buttonsFilms[1].getRegionWidth() / 4f,buttonsFilms[1].getRegionHeight() / 4f,buttonPositions[2] * backgroundScaleFactor,buttonPositions[3] * backgroundScaleFactor, buttonWidth, buttonHeight);
        canvas.draw(buttonsFilms[2],Color.WHITE,buttonsFilms[2].getRegionWidth() / 4f,buttonsFilms[2].getRegionHeight() / 4f,buttonPositions[4] * backgroundScaleFactor,buttonPositions[5] * backgroundScaleFactor, buttonWidth, buttonHeight);*/
        canvas.draw(background,Color.WHITE,0,0,background.getRegionWidth() * .25f,background.getRegionHeight() * .285f);
       canvas.draw(buttonsFilms[0],Color.WHITE,.0391f * canvas.getWidth(),.2691f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[1],Color.WHITE,.1523f* canvas.getWidth(),.2691f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[2],Color.WHITE,.1523f * canvas.getWidth(),.684f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[3],Color.WHITE,.2773f * canvas.getWidth(),.684f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[4],Color.WHITE,.2773f * canvas.getWidth(),.41f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[5],Color.WHITE,0.416015625f * canvas.getWidth(),.41f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[6],Color.WHITE,0.416015625f * canvas.getWidth(),.1181f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[7],Color.WHITE,.5586f * canvas.getWidth(),.1181f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[8],Color.WHITE,.5586f * canvas.getWidth(),.4931f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[9],Color.WHITE,.5586f * canvas.getWidth(),.8507f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[10],Color.WHITE,.706f * canvas.getWidth(),.8507f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[11],Color.WHITE,.706f * canvas.getWidth(),.5417f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[12],Color.WHITE,.8496f * canvas.getWidth(),.5417f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(buttonsFilms[13],Color.WHITE,.8496f * canvas.getWidth(),.2656f * canvas.getHeight(),buttonsFilms[0].getRegionWidth() * .6f,buttonsFilms[0].getRegionHeight() * .6f);
        canvas.draw(ursaTexture, Color.WHITE, ursaWalkFilm.getRegionWidth() / 2f, ursaWalkFilm.getRegionHeight() / 2f,ursaStartX + ursaWalkFilm.getRegionWidth() /2f * 0.4f,ursaStartY+ursaWalkFilm.getRegionHeight() /2f * 0.4f,0,direction * 0.4f,0.4f);
        effect.update(1/60f);
        if(effect.isComplete()){
            effect.reset();
        }

        effect.draw(canvas.getSpriteBatch());

        //canvas.draw(redTextureregion,Color.WHITE, 0,0,86,189,10,10);

        canvas.end();
        if(Gdx.input.isKeyPressed(Input.Keys.ENTER) && !exiting){

            enterPressed();
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
            ursaNewX = touch.x;
            ursaNewY = touch.y;


            for (int j = 0; j < positions.length / 2; j++) {
                float posX = positions[j * 2];
                float posY = positions[j * 2 + 1];

                if(touch.x - posX < 80 && touch.y - posY < 80 && touch.x - posX > 0 && touch.y - posY > 0)
                {
                   if(Math.abs(ursaStartX - touch.x) < 8){
                       if(j == 0){
                           buttonsFilms[j].setFrame(1);
                       }
                       else {
                           buttonsFilms[j].setFrame(4);
                       }

                       updateButtons();
                   }
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
            ursaNewX = touch.x;
            ursaNewY = touch.y;


            for (int j = 0; j < positions.length / 2; j++) {
                float posX = positions[j * 2];
                float posY = positions[j * 2 + 1];

                if(touch.x - posX < 80 && touch.y - posY < 80 && touch.x - posX > 0 && touch.y - posY > 0)
                {
                    if(Math.abs(ursaStartX - touch.x) < 8){
                        listener.exitScreen(this,j+1);
                    }
                }
            }



        }
        for(int i = 0; i < buttonsPressed.length; i++ ){
            buttonsPressed[i] = false;
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

