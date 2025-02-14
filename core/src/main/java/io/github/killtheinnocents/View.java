package io.github.killtheinnocents;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import entities.Entity;
import entities.Hitbox;
import helper.GameScreen;
import jdk.internal.org.jline.terminal.TerminalBuilder;

import java.util.*;

import static helper.Constants.*;

public class View extends ScreenAdapter {

    private static final int FRAME_COLS = 2, FRAME_ROWS = 2;

    public OrthographicCamera camera;
    public SpriteBatch batch;
    public World world;
    public Box2DDebugRenderer box2DDebugRenderer;

    //Font
    BitmapFont font;
    Texture font_texture;

    // Assets
    private Animation<TextureRegion> idleAnimation;
    private Texture idleAnimationSheet;
    private Sprite idleAnimationSprite;
    private Animation<TextureRegion> wizardAnimation;
    private Texture wizardSheet;
    private Sprite wizardSprite;
    float stateTime; // time for animation
    private Entity player;
    Body body;
    private float deltaTime;
    private float time;

    //Health Bar
    private Animation<TextureRegion> healthBarAnimation;
    private Texture healthSheet;
    private Sprite healthSprite;
    private int healthIndex;

    // Screens
    enum Screen {
        MENU, INTRO, MAIN_GAME, GAME_OVER, SETTINGS;
    }
    Screen currentScreen = Screen.MENU;
    private GameScreen overlay;
    private GameScreen homeScreen;
    private GameScreen dungeonScreen;

    // enemy
    public Array<Entity> enemies1;
    public Array<Sprite> enemies;
    int eVelocity = 5;
    double EPLD;
    double eStartD;
    //double EPLS;
    //double EPLA;
    double xMod;
    double yMod;
    float yMin;
    float yMax;
    float xMin;
    float xMax;
    public View(OrthographicCamera camera)
    {
        this.camera = camera;
        this.world = new World(new Vector2(0,0),false);
        this.box2DDebugRenderer = new Box2DDebugRenderer();
        this.batch = new SpriteBatch();

        // Player + Health
        player = new Entity( new Vector2(0, 0), this.world, 100, 100);
        this.body = player.getBody();
        healthIndex = 0;

        //font
        Texture font_texture = new Texture("game_font.png");
        font_texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font = new BitmapFont(Gdx.files.internal("game_font.fnt"), new TextureRegion(font_texture));
        font.getData().setScale(3f);

        stateTime = 0f;
        deltaTime = Gdx.graphics.getDeltaTime();
        time = 0;

        // Screens
        overlay = new GameScreen("mapOverlay.png");
        homeScreen = new GameScreen("home_screen.png");
        dungeonScreen = new GameScreen("dungeon_background.png");

        create();
    }

    public void create() {
        // Assets

        // sfx Gdx.audio.newSound(Gdx.files.internal(name));
        // music Gdx.audio.newMusic(Gdx.files.internal(name));
        //mapOverlay = new Texture("mapOverlay.png");
        //mapOverlaySprite = new Sprite(mapOverlay);

        idleAnimationSheet = new Texture(Gdx.files.internal("idlePlayer_sheet.png"));
        idleAnimationSprite = new Sprite(idleAnimationSheet);
        idleAnimation = new Animation<TextureRegion>(.25f, player.animationSplicer(idleAnimationSheet,2, 2));

        wizardSheet = new Texture(Gdx.files.internal("wizardSheet.png"));
        wizardSprite = new Sprite(wizardSheet);
        wizardAnimation = new Animation<TextureRegion>(.25f, player.animationSplicer(wizardSheet,2, 2));

        healthSheet = new Texture(Gdx.files.internal("HealthBar.png"));
        healthSprite = new Sprite(wizardSheet);
        healthBarAnimation = new Animation<TextureRegion>(.25f, player.animationSplicer(healthSheet,3, 6));

        enemies = new Array<>();
        enemies1 = new Array<>();
        createEnemies();
    }

    private void createEnemies() {

        Texture enemyTexture = new Texture("wizardSheet.png");
        Sprite enemy = new Sprite(enemyTexture);
        Entity enemy1 = new Entity(world,15,2,600,20);
        enemies.add(enemy);
        enemies1.add(enemy1);

    }

    @Override
    public void render(float delta)
    {
        checkDistance();
        this.update();
        super.render(delta);
        clamp(player,enemies1.get(0));

        logic();
        draw();

        if(currentScreen == Screen.INTRO && time > 150)
        {
            currentScreen = Screen.MAIN_GAME;
        } else if (currentScreen == Screen.INTRO)
        {
            time++;
        }
    }

    @Override
    public void show()
    {

    }

    public void draw() {

        stateTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

        // render objects here

        if(currentScreen == Screen.MENU)
        {
            Gdx.gl.glClearColor(0,0,0,1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();

            homeScreen.drawbg(this.batch);

            font.draw(batch, "[%$*!@#?]", -200, 100);
            font.draw(batch, "home Screen", -150, 0);
            font.draw(batch, "Press Space to play", -525, -100);

            batch.end();
        }
        else if(currentScreen == Screen.INTRO) {

            Gdx.gl.glClearColor(0,0,0,1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();
            font.draw(batch, "wait! they don't love you like I love you... WAIT!", -200, 100);

            TextureRegion wizardFrame = wizardAnimation.getKeyFrame(stateTime, true);
            batch.draw(wizardFrame, 0, 0, 500, 500);

            batch.end();
        }
        else if(currentScreen == Screen.MAIN_GAME) {

            Gdx.gl.glClearColor(0,0,0,1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();

            dungeonScreen.drawbg(this.batch);

            overlay.drawOverlay(this.batch);
            font.draw(batch, "ESC to settings", -200, -200);

            // Get current frame of animation for the current stateTime
            TextureRegion currentFrame = idleAnimation.getKeyFrame(stateTime, true);
            batch.draw(currentFrame, PlayerX, PlayerY, 170, 170);

            TextureRegion[] healthFrame = healthBarAnimation.getKeyFrames();
            batch.draw(healthFrame[healthIndex], -1000, -850);

            for(Sprite enemy: enemies)
            {
                batch.draw(currentFrame,(int)(enemies1.get(0).geteX()),(int)(enemies1.get(0).geteY()),170,170);
            }

            batch.end();

        } else if (currentScreen == Screen.GAME_OVER) {
            Gdx.gl.glClearColor(0,0,0,1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();

            font.draw(batch, "Game Over", 0, 0);

            batch.end();
        } else if (currentScreen == Screen.SETTINGS) {
            Gdx.gl.glClearColor(0,0,0,1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();

            font.draw(batch, "ESC to pause", -200, 0);
            font.draw(batch, "SPACE to unpause", -200, -100);
            font.draw(batch, "Q to exit", -200, -200);
            font.draw(batch, "WASD/Arrows to move", -200, -300);

            font.draw(batch, "Settings", 0, 100);

            batch.end();
        }

        world.step( 1/60, 6, 2);
        box2DDebugRenderer.render(world, camera.combined.scl(PPM));
    }

    // updates screen, handles key movements
    private void update()
    {
        world.step(1 / 60f, 6, 2);
        cameraUpdate();

        batch.setProjectionMatrix(camera.combined);

//        if (player.state == entityState.WALKING_N) {
//            player.position.add(player.velocity.x * deltaTime, player.velocity.y * deltaTime);
//      }

        input();

    }

    // updates pos. of camera
    private void cameraUpdate()
    {
        camera.position.set(new Vector3(0,0,0));
        camera.update();
    }

    // key word
    private void input() {
        float speed = 300f;
        float time = Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.Q))
        {
            Gdx.app.exit();
        } else if (currentScreen == Screen.MENU && Gdx.input.isKeyPressed(Input.Keys.SPACE))
        {
            currentScreen = Screen.INTRO;
        } else if (currentScreen == Screen.GAME_OVER && Gdx.input.isKeyPressed(Input.Keys.ENTER))
        {
            currentScreen = Screen.MENU;
        } else if (currentScreen == Screen.MAIN_GAME && Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
        {
            currentScreen = Screen.SETTINGS;
        } else if (currentScreen == Screen.SETTINGS && Gdx.input.isKeyPressed(Input.Keys.SPACE))
        {
            currentScreen = Screen.MAIN_GAME;
        } else if (Gdx.input.isKeyPressed(Input.Keys.R))
        {
            player.ouchies(1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.P)) {
            currentScreen = Screen.MAIN_GAME;
        } else{
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W))
            {
                PlayerY +=  time * speed;
                body.applyForceToCenter(new Vector2(10,0), true);
            }  if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
                PlayerY -= time * speed;
            }  if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
                PlayerX += time * speed;
            } if (Gdx.input.isKeyPressed(Input.Keys.LEFT)||Gdx.input.isKeyPressed(Input.Keys.A)) {
                PlayerX -= time * speed;
            }
            player.forceHUpdate(PlayerX,PlayerY);
        }
    }

    private void logic() {
        clamp(player,enemies1.get(0));


        for(Entity e : enemies1){
            e.setX((float)MathUtils.clamp(e.geteX(), (double) -Gdx.graphics.getWidth() /2, (double) Gdx.graphics.getWidth() /2));
            e.setY((float)MathUtils.clamp(e.geteY(), (double) -Gdx.graphics.getHeight() /2, (double) Gdx.graphics.getHeight() /2));
        }

        // Health Bar
        // Calculation: 17 - Math.floor(health lost / approximation)
        double approximation = 1.0 / 17;
        int percentHealthLost = (int) Math.floor((player.getCHealth() * 1.0 / player.getMaxHealth()) / approximation);
        healthIndex = 17 - percentHealthLost;
        if (!player.isLiving())
        {
            currentScreen = Screen.GAME_OVER;
        }
    }

    @Override
    public void dispose() {
        font.dispose();
    }


    public void checkDistance(){
        eVelocity = 2;
        for (Entity e: enemies1){
            EPLD = Math.sqrt(((PlayerX - e.geteX())*(PlayerX - e.geteX())) + ((PlayerY - e.geteY())*(PlayerY - e.geteY())));
            eStartD = Math.sqrt(((e.getStartX()-e.geteX())*(e.getStartX()-e.geteX())) + ((e.getStartY()-e.geteY())*(e.getStartY()-e.geteY())));

            if(EPLD<=800){
                xMod = PlayerX-e.geteX();
                yMod = PlayerY-e.geteY();
                xMod = xMod/EPLD;
                yMod = yMod/EPLD;
                //enemy1.modPos(eVelocity*Math.cos(EPLA)*Math.signum(PlayerX-enemy1.getX()),eVelocity*Math.sin(EPLA)*Math.signum(PlayerY-enemy1.getY()));
                e.modPos(xMod*eVelocity,yMod*eVelocity);
            }
            else {
                if((!(e.geteX() >= e.getStartX()-10) || !(e.geteX() <= e.getStartX()+10)) || (!(e.geteY() >= e.getStartY()-10) || !(e.geteY() <= e.getStartY()+10))){
                xMod = e.getStartX() - e.geteX();
                yMod = e.getStartY() - e.geteY();
                xMod = xMod/eStartD;
                yMod = yMod/eStartD;
                e.modPos(xMod*eVelocity,yMod*eVelocity);
                }
            }

        }
    }
    public boolean checkOverlap(Hitbox h1, Hitbox h2){
        System.out.println(h1.max >= h2.min && h2.max >= h1.min);
        System.out.println(": ("+h1.min+", "+h1.max+")");
        System.out.println(": ("+h2.min+", "+h2.max+")");
        return h1.max >= h2.min && h2.max >= h1.min;
    }
    public void clamp(Entity player, Entity enemy){
        float bufferDistance = 50;
        xMin = (float) -(Gdx.graphics.getWidth()) /2;
        xMax = (float) (Gdx.graphics.getWidth()) /2;
        yMin = (float) -Gdx.graphics.getHeight() /2;
        yMax = (float) Gdx.graphics.getHeight()/2;
        if (checkOverlap(player.getxHit(),enemy.getxHit())){
            if (PlayerY>enemy.geteY()){
                yMin= (float) (enemy.geteY()+bufferDistance);
                PlayerY += bufferDistance;
            }
            else if (PlayerY<enemy.geteY()){
                yMax= (float) (enemy.geteY()-bufferDistance);
                PlayerY += bufferDistance;
            }
        }
        else {
            yMin = (float) -Gdx.graphics.getHeight() /2;
            yMax = (float) Gdx.graphics.getHeight()/2;
        }


        if (checkOverlap(player.getyHit(),enemy.getyHit())){
            if (PlayerX>enemy.geteX()){
                xMin= (float) (enemy.geteX() +bufferDistance);
                PlayerX += bufferDistance;
            }
            else if (PlayerX<enemy.geteX()){
                xMax= (float) (enemy.geteX() -bufferDistance);
                PlayerX +=bufferDistance;


            }
        }
        else {
            xMin = (float) -(Gdx.graphics.getWidth()) /2;
            xMax = (float) (Gdx.graphics.getWidth()) /2;
        }

        System.out.println("X: ("+xMin+", "+xMax+")");
        System.out.println("Y: ("+yMin+", "+yMax+")");
        System.out.println("e: ("+enemy.geteX()+", "+enemy.geteY()+")");
        System.out.println("p: ("+PlayerX+", "+PlayerY+")");
        System.out.println();
    }

}
