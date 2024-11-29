package io.github.some_example_name;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.ScreenUtils;

public class LevelCompleteScreen implements Screen {

    private final MainGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private Texture backgroundTexture;

    public LevelCompleteScreen(MainGame game) {
        this.game = game;
        this.batch = game.batch;
        this.font = new BitmapFont(); // Use a default font or load a custom one
        this.backgroundTexture = new Texture(Gdx.files.internal("levelcomplete.png"));
    }

    @Override
    public void show() {
        // Anything to initialize when the screen is shown
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1); // Clear with black

        batch.begin();
        // Draw the background to cover the whole screen
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Draw the text on top of the background
        batch.end();

        handleInput();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // Switch to Level Two once space is pressed
            game.setScreen(new LevelTwoScreen(game));
        }
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.startGame(); // Start a new game or level
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new StartMenuScreen(game)); // Go back to the start menu
        }
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        font.dispose();
        backgroundTexture.dispose();
    }
}
