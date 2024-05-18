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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.ScreenListener;

public class HomeScreen implements Screen, InputProcessor, ControllerListener {
    private FilmStrip homeScreenFilm;
    private FilmStrip logoFilm;
    private TextureRegion homeScreen;
    private TextureRegion startButton;
    private TextureRegion startButtonClicked;
    private TextureRegion startButtonTexture;
    private TextureRegion aboutButton;
    private TextureRegion aboutButtonClicked;
    private TextureRegion aboutButtonTexture;
    private TextureRegion aboutScreen;
    private Music homeMusic;


    GameCanvas canvas;
    private boolean active;
    private TextureRegion blackTexture;
    private TextureRegion logo;
    private int time;
    private ScreenListener listener;
    private Color tint;
    private final int logoDuration = 240;
    private boolean isAnimatingHomeScreen;
    private final float logoScale = 0.04f;
    /** Current state of the button (0 for never interacted, 1 for pressed, 2 for unpressed) */
    private int startPressState = 0;
    private int aboutPressState = 0;
    private boolean inAboutScreen = false;

    public HomeScreen(GameCanvas NewCanvas, boolean playAnimation) {
        time = 0;
        canvas = NewCanvas;
        canvas.setCam(canvas.getWidth() / 2f, canvas.getHeight() /2f);
        active = false;
        Gdx.input.setInputProcessor( this );
        tint = new Color(1,1,1,0);
        isAnimatingHomeScreen = false;

        if(!playAnimation) {
            time = logoDuration;
        }
    }

    public void setActive(boolean b){
        active = b;
    }

    public void gatherAssets(AssetDirectory directory) {
        homeMusic =directory.getEntry("soundtracks:home_track", Music.class);

        TextureRegion homeScreenAnimation = new TextureRegion(directory.getEntry("homeScreen:homeScreenAnimation", Texture.class));
        homeScreenFilm = new FilmStrip(homeScreenAnimation.getTexture(), 3, 16);
        homeScreenFilm.setFrame(0);
        TextureRegion logoAnimation = new TextureRegion(directory.getEntry("homeScreen:logoAnimation", Texture.class));
        logoFilm = new FilmStrip(logoAnimation.getTexture(), 2, 16);
        logoFilm.setFrame(0);
        blackTexture = new TextureRegion(directory.getEntry("polar:black", Texture.class));
        logo = new TextureRegion(directory.getEntry("homeScreen:logo", Texture.class));
        homeScreen = new TextureRegion(directory.getEntry("homeScreen:homeScreen", Texture.class));
        startButton = new TextureRegion(directory.getEntry("homeScreen:startButton", Texture.class));
        startButtonClicked = new TextureRegion(directory.getEntry("homeScreen:startButtonClicked", Texture.class));
        aboutButton = new TextureRegion(directory.getEntry("homeScreen:aboutButton", Texture.class));
        aboutButtonClicked = new TextureRegion(directory.getEntry("homeScreen:aboutButtonClicked", Texture.class));
        aboutScreen = new TextureRegion(directory.getEntry("homeScreen:aboutScreen", Texture.class));
    }

    private void update(float delta){

        time++;


        if(inAboutScreen) {
            if(Gdx.input.isKeyPressed(Keys.ESCAPE) || Gdx.input.isKeyPressed(Keys.Q)) {
                homeScreenFilm.setFrame(0);
                time = logoDuration;
                isAnimatingHomeScreen = true;
                inAboutScreen = false;
            } else {
                return;
            }
        }

        if(startPressState == 1) {
            startButtonTexture = startButtonClicked;
        } else {
            startButtonTexture = startButton;
        }

        if(aboutPressState == 1) {
            aboutButtonTexture = aboutButtonClicked;
        } else {
            aboutButtonTexture = aboutButton;
        }

        if(isAnimatingHomeScreen && time % 2 == 0) {
            if(homeScreenFilm.getFrame() == 35) {
                homeScreenFilm.setFrame(0);
                isAnimatingHomeScreen = false;
                return;
            }
            homeScreenFilm.setFrame(homeScreenFilm.getFrame() + 1);
        }
    }

    private void draw(){
        canvas.clear();
        canvas.begin();

        canvas.draw(blackTexture, Color.WHITE, canvas.getCameraX() - canvas.getWidth() /2f, canvas.getCameraY() - canvas.getHeight() / 2f, canvas.getWidth(), canvas.getHeight());
        if(time < logoDuration / 2) {
            // Fade the logo in
            tint.a = (logoDuration / 2f - time) / (logoDuration / 2f);
            canvas.draw(logo, Color.WHITE, logo.getRegionWidth() / 2f, logo.getRegionHeight() /2f, canvas.getCameraX(), canvas.getCameraY(), 0, logoScale,logoScale);
            canvas.draw(blackTexture, tint, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else if(time < logoDuration) {
            // Fade the logo out
            tint.a = 1 - (logoDuration - time) / (logoDuration / 2f);
            canvas.draw(logo, Color.WHITE, logo.getRegionWidth() / 2f, logo.getRegionHeight() /2f, canvas.getCameraX(), canvas.getCameraY(), 0, logoScale,logoScale);
            canvas.draw(blackTexture, tint, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            // If we're here for the first time, start rolling the home screen film strip
            if(time == logoDuration) {
                isAnimatingHomeScreen = true;
            }
            if(isAnimatingHomeScreen) {
                // Draw the animated home screen
                float scaleFactor = canvas.getHeight() / (float) homeScreenFilm.getRegionHeight();

               // canvas.draw(homeScreenFilm, Color.WHITE, homeScreenFilm.getRegionWidth() / 2f, homeScreenFilm.getRegionHeight() / 2f, canvas.getCameraX(), canvas.getCameraY(), 0,scaleFactor, scaleFactor);
            } else if(inAboutScreen) {
                canvas.draw(aboutScreen, Color.WHITE, 0, 0, canvas.getWidth(), canvas.getHeight());
            } else {
                // Switch to the static home screen
                float scaleFactor = canvas.getHeight() / (float) homeScreen.getRegionHeight();
                canvas.draw(homeScreen, Color.WHITE, homeScreen.getRegionWidth() /2f, homeScreen.getRegionHeight() / 2f, canvas.getCameraX(), canvas.getCameraY(), 0, scaleFactor, scaleFactor);
                canvas.draw(logoFilm, Color.WHITE, logoFilm.getRegionWidth() / 2f, logoFilm.getRegionHeight() / 2f, 1281.5f * scaleFactor, 600f * scaleFactor, 0, scaleFactor, scaleFactor);
                canvas.draw(startButtonTexture, Color.WHITE, startButtonTexture.getRegionWidth() / 2f, startButtonTexture.getRegionHeight() / 2f, 1281.5f * scaleFactor, 489.5f * scaleFactor, 0,scaleFactor,scaleFactor);
                canvas.draw(aboutButtonTexture, Color.WHITE, aboutButtonTexture.getRegionWidth() / 2f, aboutButtonTexture.getRegionHeight() / 2f, 1281.5f * scaleFactor, 350.5f * scaleFactor, 0,scaleFactor,scaleFactor);
            }
        }


        canvas.end();
    }


    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
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
        float touchY = canvas.getHeight() - screenY;

        if(screenX > 410.2 && screenX < 615 && touchY > 170.2 && touchY < 221.4) {
            startPressState = 1;
        } else if(screenX > 410.2 && screenX < 615 && touchY > 114.6 && touchY < 165.8) {
            aboutPressState = 1;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (startPressState == 1) {
            float touchY = canvas.getHeight() - screenY;

            if(screenX > 410.2 && screenX < 615 && touchY > 170.2 && touchY < 221.4) {
                startPressState = 2;
            } else {
                startPressState = 0;
            }
            return false;
        } else if(aboutPressState == 1) {
            float touchY = canvas.getHeight() - screenY;

            if(screenX > 410.2 && screenX < 615 && touchY > 114.6 && touchY < 165.8) {
                aboutPressState = 2;
            } else {
                aboutPressState = 0;
            }
        }
        return true;
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
        homeMusic.setLooping(true);
        homeMusic.play();

    }

    @Override
    public void render(float delta) {
        if(active) {
            update(delta);
            draw();

            if(time > logoDuration && !isAnimatingHomeScreen) {
                if(startPressState == 2) {
                    listener.exitScreen(this, 30);
                } else if(aboutPressState == 2) {
                    aboutPressState = 0;
                    inAboutScreen = true;
                }
            }
        }
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
        homeMusic.stop();
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
