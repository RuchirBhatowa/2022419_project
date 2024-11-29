package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainGame extends Game {
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        this.setScreen(new StartMenuScreen(this));
    }

    @Override
    public void render() {
        super.render(); // Important!
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    public void startGame() {
        this.setScreen(new GameScreen(this));
    }
}
