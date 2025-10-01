package game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.helper.opencv_core.*;
import org.bytedeco.opencv.opencv_java;
import org.bytedeco.javacpp.Loader;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.system.MemoryUtil;

public class TankMazeGame {

    static {
        Loader.load(opencv_java.class);
    }

    private Mat currentFrame = null;
    private OpenCVFrameGrabber grabber;
    private CascadeClassifier leftGesture;
    private CascadeClassifier rightGesture;
    private CascadeClassifier upGesture;
    private CascadeClassifier downGesture;
    private CanvasFrame debugCanvas;

    // Window properties
    private long window;
    private final int width = 800;
    private final int height = 800;
    private final int rows = 21; // must be odd

    // Game components
    private MazeGenerator generator;
    private int[][] maze;
    private Player player;
    private GameRenderer renderer;
    private FontRenderer fontRenderer;
    private MenuRenderer menuRenderer;
    private WinRenderer winRenderer;
    private LoseRenderer loseRenderer;
    private LevelRenderer levelRenderer;
    private PauseRenderer pauseRenderer;
    private GameState state;

    // Gameplay elements
    private final List<Bullet> bullets = new ArrayList<>();
    private List<Enemy> enemies;
    private List<Bullet> enemyBullets;
    private final Random rand = new Random();

    // Shooting
    private final double shootCooldown = 0.5;
    private double lastShootTime = 0.0;

    // Level progression
    private int currentLevel = 1;
    private final int maxLevel = 5;
    private final int enemyCount = 2; // base enemies

    // Background music
    private AudioPlayer backgroundMusic;

    // Background colors (darker tones matching each level theme)
    private final float[][] backgroundColors = {
        {0.1f, 0.1f, 0.1f},
        {0.05f, 0.1f, 0.2f},
        {0.1f, 0.05f, 0.15f},
        {0.1f, 0.05f, 0.02f},
        {0.02f, 0.1f, 0.05f}
    };

    // Key state tracking to prevent repeated triggering
    private boolean eKeyPressed = false;
    private boolean plusKeyPressed = false;
    private boolean minusKeyPressed = false;
    private boolean upKeyPressed = false;
    private boolean downKeyPressed = false;
    
    // Volume and brightness control
    private float volume = 0.7f; // default volume 70%
    private float brightness = 0.8f; // default brightness 80%

    private void initGestures() {
        grabber = new OpenCVFrameGrabber(0);
        try {
            grabber.start();
            debugCanvas = new CanvasFrame("Webcam Preview");
            debugCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            debugCanvas.setCanvasSize(grabber.getImageWidth(), grabber.getImageHeight());
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }

        leftGesture = new CascadeClassifier("src/main/resources/cascade/left.xml");
        rightGesture = new CascadeClassifier("src/main/resources/cascade/right.xml");
        upGesture = new CascadeClassifier("src/main/resources/cascade/up.xml");
        downGesture = new CascadeClassifier("src/main/resources/cascade/down.xml");
    }

    // ──────────────────────────────────────────────
    // Main game loop
    // ──────────────────────────────────────────────
    public void run() throws IOException {
        initWindow();

        fontRenderer = new FontRenderer();
        menuRenderer = new MenuRenderer(fontRenderer);
        winRenderer = new WinRenderer(fontRenderer);
        loseRenderer = new LoseRenderer(fontRenderer);
        levelRenderer = new LevelRenderer(fontRenderer);
        pauseRenderer = new PauseRenderer(fontRenderer);

        // Enter the game directly when starting
        currentLevel = 1;
        startNewGame(currentLevel);
        state = GameState.PLAYING;

        double lastTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            // Set background color dynamically by current level
            float[] bg = backgroundColors[Math.min(currentLevel - 1, backgroundColors.length - 1)];
            glClearColor(bg[0], bg[1], bg[2], 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            if (state == GameState.PLAYING) {
                glMatrixMode(GL_PROJECTION);
                glLoadIdentity();
                glOrtho(-1, 1, -1, 1, -1, 1);
                glMatrixMode(GL_MODELVIEW);

                handleGame();

                // Update enemies
                List<Bullet> newEnemyShots = new ArrayList<>();
                for (Enemy e : enemies) {
                    e.update(dt, player, newEnemyShots);
                }
                enemyBullets.addAll(newEnemyShots);

                // Enemy bullets
                for (int i = 0; i < enemyBullets.size(); i++) {
                    Bullet b = enemyBullets.get(i);
                    b.update();

                    if (Math.abs(b.x - player.getX()) < player.getSize() &&
                        Math.abs(b.y - player.getY()) < player.getSize()) {
                        state = GameState.LOSE;
                        break;
                    }

                    if (!b.isAlive()) {
                        enemyBullets.remove(i--);
                    } else {
                        glColor3f(1f, 0f, 0f);
                        b.render();
                    }
                }

                // Player bullets → hit enemies
                for (int i = 0; i < bullets.size(); i++) {
                    Bullet pb = bullets.get(i);
                    for (int j = 0; j < enemies.size(); j++) {
                        Enemy en = enemies.get(j);
                        if (Math.abs(pb.x - en.getX()) < en.getSize() &&
                            Math.abs(pb.y - en.getY()) < en.getSize()) {
                            bullets.remove(i--);
                            if (en.hit()) enemies.remove(j--);
                            break;
                        }
                    }
                }

                // Render everything
                renderer.render(state, enemies.isEmpty(), enemies, currentLevel);

                // Player bullets rendering
                for (int i = 0; i < bullets.size(); i++) {
                    Bullet b = bullets.get(i);
                    b.update();
                    if (!b.isAlive()) {
                        bullets.remove(i--);
                    } else {
                        glColor3f(1f, 1f, 1f);
                        b.render();
                    }
                }

                // Apply brightness effect - fixed screen overlay
                if (brightness < 1.0f) {
                    // Save current projection and modelview matrices
                    glMatrixMode(GL_PROJECTION);
                    glPushMatrix();
                    glLoadIdentity();
                    glOrtho(0, width, height, 0, -1, 1); // Switch to screen coordinates
                    
                    glMatrixMode(GL_MODELVIEW);
                    glPushMatrix();
                    glLoadIdentity();
                    
                    // Apply brightness overlay in screen space
                    glEnable(GL_BLEND);
                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    glColor4f(0.0f, 0.0f, 0.0f, 1.0f - brightness);
                    glBegin(GL_QUADS);
                    glVertex2f(0, 0);
                    glVertex2f(width, 0);
                    glVertex2f(width, height);
                    glVertex2f(0, height);
                    glEnd();
                    glDisable(GL_BLEND);
                    
                    // Restore previous matrix state
                    glPopMatrix();
                    glMatrixMode(GL_PROJECTION);
                    glPopMatrix();
                    glMatrixMode(GL_MODELVIEW);
                }

            } else {
                // UI states
                glMatrixMode(GL_PROJECTION);
                glLoadIdentity();
                glOrtho(0, width, height, 0, -1, 1);
                glMatrixMode(GL_MODELVIEW);

                switch (state) {
                    case MENU:
                        menuRenderer.render();
                        handleMenu();
                        break;

                    case PAUSED:
                        pauseRenderer.render(volume, brightness);
                        handlePause();
                        break;

                    case WIN:
                        winRenderer.render();
                        handleWin();
                        break;

                    case LOSE:
                        loseRenderer.render();
                        handleLose();
                        break;

                    case LEVEL_COMPLETE:
                        levelRenderer.render(currentLevel);
                        handleLevelComplete();
                        break;

                    default:
                        break;
                }
            }
            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        // Stop music before exiting
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
        
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    // ──────────────────────────────────────────────
    // Initialization
    // ──────────────────────────────────────────────
    private void initWindow() {
        initGestures();

        if (!glfwInit()) throw new IllegalStateException("Failed to init GLFW");

        window = glfwCreateWindow(width, height, "Tank Maze Game", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) throw new RuntimeException("Window creation failed");

        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null) {
            glfwSetWindowPos(window, (vid.width() - width) / 2, (vid.height() - height) / 2);
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        backgroundMusic = new AudioPlayer("audio/music.wav");
        backgroundMusic.setVolume(volume);
        backgroundMusic.play();

        glfwSwapInterval(1);
        glfwShowWindow(window);
        glClearColor(0.12f, 0.12f, 0.15f, 1.0f);
    }

    // ──────────────────────────────────────────────
    // Game start logic
    // ──────────────────────────────────────────────
    private void startNewGame(int level) {
        generator = new MazeGenerator(rows, rows);
        maze = generator.getMaze();
        player = new Player(maze, rows);
        renderer = new GameRenderer(maze, player, fontRenderer);

        bullets.clear();
        enemyBullets = new ArrayList<>();
        lastShootTime = 0;
        enemies = new ArrayList<>();

        float cellSize = 2f / rows;
        float margin = cellSize * 6;

        int levelEnemyCount = (level == 5) ? 12 : enemyCount + level;

        int normalCount = 0, miniCount = 0, sniperCount = 0, tankCount = 0;

        for (int i = 0; i < levelEnemyCount; i++) {
            int er, ec;
            float ex, ey;
            do {
                er = rand.nextInt(rows - 2) + 1;
                ec = rand.nextInt(rows - 2) + 1;
                ex = -1 + ec * cellSize + cellSize / 2f;
                ey = 1 - er * cellSize - cellSize / 2f;
            } while (Math.hypot(ex - player.getX(), ey - player.getY()) < margin || maze[er][ec] == 1);

            Enemy enemy;

            switch (level) {
                case 1:
                    enemy = new Enemy(ex, ey, maze);
                    break;
                case 2:
                    enemy = new TankEnemy(ex, ey, maze);
                    break;
                case 3:
                    enemy = new MiniEnemy(ex, ey, maze);
                    break;
                case 4:
                    enemy = new SniperEnemy(ex, ey, maze);
                    break;
                default:
                    while (true) {
                        int type = rand.nextInt(4);
                        if (type == 0 && normalCount < 3) { enemy = new Enemy(ex, ey, maze); normalCount++; break; }
                        else if (type == 1 && miniCount < 3) { enemy = new MiniEnemy(ex, ey, maze); miniCount++; break; }
                        else if (type == 2 && sniperCount < 3) { enemy = new SniperEnemy(ex, ey, maze); sniperCount++; break; }
                        else if (type == 3 && tankCount < 3) { enemy = new TankEnemy(ex, ey, maze); tankCount++; break; }
                    }
                    break;
            }

            enemies.add(enemy);
        }

        state = GameState.PLAYING;
    }

    // ────────────────────────────────────────────────
    // Input Handlers
    // ────────────────────────────────────────────────
    private void handleMenu() {
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            currentLevel = 1;
            startNewGame(currentLevel);
            state = GameState.PLAYING;
        }
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
            glfwSetWindowShouldClose(window, true);
    }

    private void handlePause() {
        // Check E key to resume game
        boolean currentEKeyState = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        if (currentEKeyState && !eKeyPressed) {
            state = GameState.PLAYING;
        }
        eKeyPressed = currentEKeyState;
        
        // Check + key to increase volume
        boolean currentPlusKeyState = glfwGetKey(window, GLFW_KEY_EQUAL) == GLFW_PRESS || 
                                     glfwGetKey(window, GLFW_KEY_KP_ADD) == GLFW_PRESS;
        if (currentPlusKeyState && !plusKeyPressed) {
            volume = Math.min(1.0f, volume + 0.1f);
            backgroundMusic.setVolume(volume);
        }
        plusKeyPressed = currentPlusKeyState;
        
        // Check - key to decrease volume
        boolean currentMinusKeyState = glfwGetKey(window, GLFW_KEY_MINUS) == GLFW_PRESS || 
                                      glfwGetKey(window, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS;
        if (currentMinusKeyState && !minusKeyPressed) {
            volume = Math.max(0.0f, volume - 0.1f);
            backgroundMusic.setVolume(volume);
        }
        minusKeyPressed = currentMinusKeyState;
        
        // Check up arrow key to increase brightness
        boolean currentUpKeyState = glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS;
        if (currentUpKeyState && !upKeyPressed) {
            brightness = Math.min(1.0f, brightness + 0.1f);
        }
        upKeyPressed = currentUpKeyState;
        
        // Check down arrow key to decrease brightness
        boolean currentDownKeyState = glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS;
        if (currentDownKeyState && !downKeyPressed) {
            brightness = Math.max(0.0f, brightness - 0.1f);
        }
        downKeyPressed = currentDownKeyState;
        
        // ESC key to return to GameMenu
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            // Stop background music
            if (backgroundMusic != null) {
                backgroundMusic.stop();
            }
            
            // Close current window
            glfwSetWindowShouldClose(window, true);
            
            // Launch GameMenu
            javax.swing.SwingUtilities.invokeLater(() -> {
                GameMenu.main(new String[0]);
            });
        }
    }

    private void handleGame() {
        // Check E key to pause game
        boolean currentEKeyState = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        
        if (currentEKeyState && !eKeyPressed) {
            state = GameState.PAUSED;
        }
        eKeyPressed = currentEKeyState;
        
        // If game is paused, don't process other inputs
        if (state == GameState.PAUSED) {
            return;
        }

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        try {
            org.bytedeco.javacv.Frame frame = grabber.grab();
            currentFrame = converter.convert(frame);
        } catch (Exception e) { e.printStackTrace(); }

        boolean moveLeft  = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS || detectGesture(currentFrame, leftGesture, "Left");
        boolean moveRight = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS || detectGesture(currentFrame, rightGesture, "Right");
        boolean moveUp    = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS || detectGesture(currentFrame, upGesture, "Up");
        boolean moveDown  = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS || detectGesture(currentFrame, downGesture, "Down");

        if (moveLeft)  player.move(-1, 0);
        if (moveRight) player.move(1, 0);
        if (moveUp)    player.move(0, 1);
        if (moveDown)  player.move(0, -1);

        if (enemies.isEmpty() && player.reachedGoal()) {
            state = (currentLevel < maxLevel) ? GameState.LEVEL_COMPLETE : GameState.WIN;
        }

        if (glfwGetKey(window, GLFW_KEY_N) == GLFW_PRESS) {
            state = (currentLevel < maxLevel) ? GameState.LEVEL_COMPLETE : GameState.WIN;
        }

        double now = glfwGetTime();
        if (glfwGetKey(window, GLFW_KEY_F) == GLFW_PRESS && now - lastShootTime >= shootCooldown) {
            bullets.add(player.shoot());
            lastShootTime = now;
        }
    }

    private void handleWin() {
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            if (currentLevel < maxLevel) {
                currentLevel++;
                startNewGame(currentLevel);
            } else {
                state = GameState.MENU;
            }
        }
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
            glfwSetWindowShouldClose(window, true);
    }

    private void handleLose() {
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            currentLevel = 1;
            startNewGame(currentLevel);
        }
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            // ESC → Back to GameMenu
            glfwSetWindowShouldClose(window, true);
            javax.swing.SwingUtilities.invokeLater(() -> {
                GameMenu.main(new String[0]);
            });
        }
    }

    private void handleLevelComplete() {
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            currentLevel++;
            startNewGame(currentLevel);
        }
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
            glfwSetWindowShouldClose(window, true);
    }

    private boolean detectGesture(Mat mat, CascadeClassifier classifier, String label) {
        try {
            org.bytedeco.javacv.Frame frame = grabber.grab();
            if (frame == null) return false;

            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            Mat frameMat = converter.convert(frame);
            if (frameMat == null || frameMat.empty()) return false;

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(frameMat, gray, opencv_imgproc.COLOR_BGR2GRAY);
            RectVector detections = new RectVector();
            classifier.detectMultiScale(gray, detections);

            for (int i = 0; i < detections.size(); i++) {
                Rect rect = detections.get(i);
                opencv_imgproc.rectangle(frameMat, rect, Scalar.RED);
                opencv_imgproc.putText(frameMat, label, new Point(rect.x(), rect.y() - 5), opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.7, Scalar.YELLOW);
            }

            if (debugCanvas != null && debugCanvas.isVisible()) {
                debugCanvas.showImage(converter.convert(frameMat));
            }

            return detections.size() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        new TankMazeGame().run();
    }
}