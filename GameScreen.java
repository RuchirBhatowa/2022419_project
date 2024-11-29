package io.github.some_example_name;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.*;

;
public class GameScreen implements Screen {

    // Physics World and Renderer
    public World world;
    private Box2DDebugRenderer debugRenderer;

    // Camera
    private OrthographicCamera camera;

    // Pixels per meter
    static final float PPM = 100;

    // Textures
    private Texture birdTexture;
    private Texture pillarTexture;
    private Texture pigTexture;
    private Texture platformTexture;
    private Texture backgroundTexture;

    // Batch for rendering
    private Batch batch;

    // Image scaling factor
    private static final float SCALE = 0.1f;   // Scale down to 10% of the original size

    // Slingshot and bird-related variables
    private Body birdBody;
    private Vector2 slingshotBase;
    private Body groundBody; // Will be set after platform creation
    private boolean isDragging = false;
    private Vector2 dragPosition = new Vector2();
    private boolean isBirdLanded = false;
    private static final int COLLISION_CHECK_THRESHOLD = 150;

    // List of bodies
    public List<Body> pigBodies = new ArrayList<>();
    public List<Body> pillarBodies = new ArrayList<>();

    private CollisionListener collisionListener;

    // Platform variables
    private Body platformBody;
    private float platformWidth = 0.1f;
    private float platformHeight = 2.0f;
    private boolean isPlatformVisible = true;

    private final MainGame game;
    private boolean shouldCheckGameOver = false;
    private boolean collisionOccurred = false;
    private boolean birdHitGround = false;
    private int noCollisionFrames = 0;

    public GameScreen(MainGame game) {
        this.game = game;
        create();
    }

    @Override
    public void show() {
        // Initialization if needed
    }

    public void create() {
        try {
            System.out.println("Creating game assets...");

            // Initialize physics and camera
            Box2D.init();
            world = new World(new Vector2(0, -7f), true);
            collisionListener = new CollisionListener();
            world.setContactListener(collisionListener);
            debugRenderer = new Box2DDebugRenderer();
            camera = new OrthographicCamera();
            camera.setToOrtho(false, Gdx.graphics.getWidth() / PPM, Gdx.graphics.getHeight() / PPM);

            // Initialize batch
            batch = new SpriteBatch();

            // Initialize textures
            birdTexture = new Texture(Gdx.files.internal("Red_png.png"));
            pillarTexture = new Texture(Gdx.files.internal("Toons_Wood_Block.png"));
            pigTexture = new Texture(Gdx.files.internal("Pig_29 (1).png"));
            platformTexture = new Texture(Gdx.files.internal("Slingshot_Classic.png"));
            backgroundTexture = new Texture(Gdx.files.internal("seamless-game-nature-landscape-parallax-background-2d-game-outdoor-mountains-trees-clouds-illustrations_80590-7362.jpg"));

            createPlatform();
            createGround();
            initializeBird();
            createPillars();
            createPigs();

            System.out.println("Assets created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            Gdx.app.exit(); // Fail gracefully by exiting the app
        }
    }

    @Override
    public void render(float delta) {
        // Clear screen
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        // Update camera and physics world
        camera.update();
        world.step(1 / 60f, 6, 2);

        // Render debug shapes
        debugRenderer.render(world, camera.combined);

        // Handle input
        handleInput();

        // Toggle platform visibility
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            togglePlatformVisibility();
        }

        // Check for game over or level completion
        if (birdHitGround) {
            noCollisionFrames++;

            if (noCollisionFrames > COLLISION_CHECK_THRESHOLD) {
                if (areAllPillarsDestroyed() || areAllPigsOnGround()) {
                    System.out.println("Transitioning to LevelCompleteScreen");
                    game.setScreen(new LevelCompleteScreen(game));
                    dispose();
                    return;
                }

                if (!areAllPillarsDestroyed() || !areAllPigsOnGround()) {
                    game.setScreen(new GameOverScreen(game));
                    dispose();
                    return;
                }

                birdHitGround = false;
            }
        }

        // Begin batch rendering
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, camera.viewportWidth * PPM, camera.viewportHeight * PPM);

        // Render game elements
        renderGameElements();

        // End batch
        batch.end();
    }

    private void renderGameElements() {
        // Render bird
        Vector2 birdPosition = birdBody.getPosition();
        batch.draw(birdTexture, birdPosition.x * PPM - (birdTexture.getWidth() * SCALE / 2),
            birdPosition.y * PPM - (birdTexture.getHeight() * SCALE / 2),
            birdTexture.getWidth() * SCALE, birdTexture.getHeight() * SCALE);

        // Render pillars
        for (Body pillarBody : pillarBodies) {
            Vector2 pillarPosition = pillarBody.getPosition();
            float pillarAngle = pillarBody.getAngle() * 180.0f / (float) Math.PI;
            float physicalWidth = 20;
            float physicalHeight = 178;
            batch.draw(pillarTexture,
                pillarPosition.x * PPM - physicalWidth / 2,
                pillarPosition.y * PPM - physicalHeight / 2,
                physicalWidth / 2,
                physicalHeight / 2,
                physicalWidth,
                physicalHeight,
                1, 1,
                pillarAngle,
                0, 0,
                pillarTexture.getWidth(), pillarTexture.getHeight(),
                false, false
            );
        }

        // Render pigs
        for (Body pigBody : pigBodies) {
            Vector2 pigPosition = pigBody.getPosition();
            batch.draw(pigTexture, pigPosition.x * PPM - (pigTexture.getWidth() * SCALE / 2),
                pigPosition.y * PPM - (pigTexture.getHeight() * SCALE / 2),
                pigTexture.getWidth() * SCALE, pigTexture.getHeight() * SCALE);
        }

        // Render platform if visible
        if (isPlatformVisible) {
            Vector2 platformPosition = platformBody.getPosition();
            batch.draw(platformTexture,
                platformPosition.x * PPM - (platformWidth * PPM / 2),
                platformPosition.y * PPM - (platformHeight * PPM / 2),
                platformWidth * PPM,
                platformHeight * PPM);
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
        debugRenderer.dispose();
        world.dispose();
        batch.dispose();
        birdTexture.dispose();
        pillarTexture.dispose();
        pigTexture.dispose();
        platformTexture.dispose();
        backgroundTexture.dispose();
    }

    private void togglePlatformVisibility() {
        isPlatformVisible = !isPlatformVisible;
        platformBody.setActive(isPlatformVisible);
    }

    private void initializeBird() {
        slingshotBase = new Vector2(
            platformBody.getPosition().x,
            platformBody.getPosition().y + (platformHeight / 2) + (10 / PPM)
        );

        BodyDef birdDef = new BodyDef();
        birdDef.type = BodyDef.BodyType.DynamicBody;
        birdDef.position.set(slingshotBase);

        birdBody = world.createBody(birdDef);

        CircleShape birdShape = new CircleShape();
        birdShape.setRadius(10 / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = birdShape;
        fixtureDef.density = 1.0f;
        fixtureDef.restitution = 0.5f;
        birdBody.createFixture(fixtureDef);
        birdShape.dispose();
    }

    private void createPlatform() {
        BodyDef postDef = new BodyDef();
        postDef.type = BodyDef.BodyType.StaticBody;
        postDef.position.set(1, 0);
        platformBody = world.createBody(postDef);

        PolygonShape platformShape = new PolygonShape();
        platformShape.setAsBox(platformWidth / 2, platformHeight / 2);
        FixtureDef platformFixture = new FixtureDef();
        platformFixture.shape = platformShape;
        platformFixture.friction = 0.5f;
        platformFixture.restitution = 0.1f;
        platformFixture.filter.categoryBits = 0x0002;
        platformBody.createFixture(platformFixture);
        platformShape.dispose();
    }

    private void createGround() {
        BodyDef groundBodyDef = new BodyDef();
        groundBodyDef.type = BodyDef.BodyType.StaticBody;
        groundBodyDef.position.set(0, -10 / PPM);
        groundBody = world.createBody(groundBodyDef);

        PolygonShape groundShape = new PolygonShape();
        groundShape.setAsBox(1000 / PPM, 10 / PPM);

        groundBody.createFixture(groundShape, 0);
        groundShape.dispose();
    }

    private void createPillars() {
        for (int i = 0; i < 4; i++) {
            BodyDef pillarDef = new BodyDef();
            pillarDef.type = BodyDef.BodyType.DynamicBody;
            pillarDef.position.set((200 + 100 * (i + 1)) / PPM, 100 / PPM);

            Body pillarBody = world.createBody(pillarDef);

            PolygonShape pillarShape = new PolygonShape();
            pillarShape.setAsBox(5 / PPM, 75 / PPM);

            FixtureDef pillarFixture = new FixtureDef();
            pillarFixture.shape = pillarShape;
            pillarFixture.density = 1f;
            pillarFixture.restitution = 0.3f;

            pillarBody.createFixture(pillarFixture);
            pillarShape.dispose();

            pillarBodies.add(pillarBody);
        }
    }

    private void createPigs() {
        for (Body pillarBody : pillarBodies) {
            BodyDef pigDef = new BodyDef();
            pigDef.type = BodyDef.BodyType.DynamicBody;
            Vector2 pillarPosition = pillarBody.getPosition();
            pigDef.position.set(pillarPosition.x, pillarPosition.y + (75 + 10) / PPM);

            Body pigBody = world.createBody(pigDef);
            pigBody.setUserData(new Pig());
            CircleShape pigShape = new CircleShape();
            pigShape.setRadius(10 / PPM);

            FixtureDef pigFixture = new FixtureDef();
            pigFixture.shape = pigShape;
            pigFixture.density = 0.5f;
            pigFixture.restitution = 0.5f;

            pigBody.createFixture(pigFixture);
            pigShape.dispose();

            pigBodies.add(pigBody);
        }
    }

    private void handleInput() {
        if (!isBirdLanded && birdBody.getType() != BodyDef.BodyType.StaticBody) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                handleDragInput();
            } else if (isDragging) {
                launchBird();
            }
        }
    }

    private void handleDragInput() {
        Vector3 unprojected = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 mousePosition = new Vector2(unprojected.x, unprojected.y);

        if (!isDragging && mousePosition.dst(birdBody.getPosition()) < 20 / PPM) {
            isDragging = true;
        }

        if (isDragging) {
            dragPosition.set(mousePosition);

            if (dragPosition.dst(slingshotBase) > 50 / PPM) {
                dragPosition = slingshotBase.cpy().add(dragPosition.sub(slingshotBase).nor().scl(50 / PPM));
            }

            confineDragPositionToViewport();
            birdBody.setTransform(dragPosition, birdBody.getAngle());
        }
    }

    private void confineDragPositionToViewport() {
        dragPosition.x = Math.max(0 / PPM, Math.min(camera.viewportWidth, dragPosition.x));
        dragPosition.y = Math.max(0 / PPM, Math.min(camera.viewportHeight, dragPosition.y));
    }

    private void launchBird() {
        Vector2 launchForce = slingshotBase.cpy().sub(dragPosition).scl(0.45F);
        birdBody.applyLinearImpulse(launchForce, birdBody.getWorldCenter(), true);

        Filter filter = new Filter();
        filter.categoryBits = 0x0001;
        filter.maskBits = ~(0x0002);

        for (Fixture fixture : birdBody.getFixtureList()) {
            fixture.setFilterData(filter);
        }

        isDragging = false;
    }



    class CollisionListener implements ContactListener {
        @Override
        public void beginContact(Contact contact) {
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            Body bodyA = fixtureA.getBody();
            Body bodyB = fixtureB.getBody();

            if (bodyA == birdBody || bodyB == birdBody) {
                Body otherBody = (bodyA == birdBody) ? bodyB : bodyA;

                if (otherBody == groundBody) {
                    birdHitGround = true;
                    noCollisionFrames = 0;
                    System.out.println("Bird has hit the ground.");
                }
            }
        }

        @Override
        public void endContact(Contact contact) {}

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {}

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {}
    }

    private boolean areAllPillarsDestroyed() {
        float pillarAngleThreshold = 45.0f;
        float pillarHeightThreshold = 50.0f / PPM;
        for (Body pillarBody : pillarBodies) {
            float angle = Math.abs(pillarBody.getAngle() * 180.0f / (float) Math.PI);
            float height = pillarBody.getPosition().y;
            if (angle < pillarAngleThreshold && height > pillarHeightThreshold) {
                System.out.println("Pillar not yet destroyed: angle = " + angle + ", height = " + height);
                return false;
            }
        }
        System.out.println("All pillars destroyed!");
        return true;
    }

    private boolean areAllPigsOnGround() {
        float groundLevelThreshold = 10.0f / PPM;
        for (Body pigBody : pigBodies) {
            if (pigBody.getPosition().y > groundLevelThreshold) {
                System.out.println("Pig not on ground: y = " + pigBody.getPosition().y);
                return false;
            }
        }
        System.out.println("All pigs are on the ground!");
        return true;
    }
}
