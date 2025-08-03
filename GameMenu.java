package gamemanu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GameMenu extends JPanel implements ActionListener, SettingsChangeListener {
    private static final int FRAME_WIDTH  = 800;
    private static final int FRAME_HEIGHT = 600;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT= 50;
    private static final int STAR_COUNT  = 100;
    private static final int STAR_SPEED  = 2;
    private static Color BG_COLOR        = new Color(10, 10, 40);
    private static final Color STAR_COLOR= new Color(200, 220, 255);

    private final Point[] stars = new Point[STAR_COUNT];
    private int backgroundY = 0;
    private final Random random = new Random();

    private final JButton playButton;
    private final JButton settingsButton;
    private final JButton rulesButton;
    private final JButton exitButton;

    private int fps = 0;
    private int framesCount = 0;
    private long lastFpsTime = System.currentTimeMillis();

    private final Timer timer;

    public GameMenu() {
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setLayout(null);
        initStars();

        int cx = (FRAME_WIDTH - BUTTON_WIDTH) / 2;
        playButton     = createButton("Play",     cx, 180);
        settingsButton = createButton("Settings", cx, 250);
        rulesButton    = createButton("Rules",    cx, 320);
        exitButton     = createButton("Exit",     cx, 390);

        add(playButton);
        add(settingsButton);
        add(rulesButton);
        add(exitButton);

        timer = new Timer(16, this);
        timer.start();
    }

    private void initStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i] = new Point(random.nextInt(FRAME_WIDTH), random.nextInt(FRAME_HEIGHT));
        }
    }

    private JButton createButton(String text, int x, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setForeground(Color.WHITE);
        btn.setActionCommand(text.toUpperCase());
        btn.addActionListener(this);
        return btn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd) {
            case "PLAY"     -> System.out.println("Starting game...");
            case "SETTINGS" -> openSettingsDialog();
            case "RULES"    -> System.out.println("Showing game rules...");
            case "EXIT"     -> System.exit(0);
        }
        backgroundY = (backgroundY + STAR_SPEED) % getHeight();
        repaint();
    }

    private void openSettingsDialog() {
        Window win = SwingUtilities.getWindowAncestor(this);
        new SettingsDialog((Frame) win, this).setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        long now = System.currentTimeMillis();
        framesCount++;
        if (now - lastFpsTime >= 1000) {
            fps = framesCount;
            framesCount = 0;
            lastFpsTime = now;
        }

        drawBackground(g2d);
        drawTitle(g2d);
        g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("FPS: " + fps, 10, 20);
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(STAR_COLOR);
        for (Point p : stars) {
            int y = (p.y + backgroundY) % getHeight();
            int size = 1 + (p.x % 3);
            g.fillOval(p.x, y, size, size);
        }
    }

    private void drawTitle(Graphics2D g2d) {
        String title = "ARMS OF TANKS";
        Font font = new Font("Arial", Font.BOLD, 56);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(title);
        int x = (getWidth() - w) / 2;
        int y = 100;
        g2d.setColor(new Color(150,120,0));
        g2d.drawString(title, x + 3, y + 3);
        GradientPaint gp = new GradientPaint(x, y, new Color(255,215,0),
                                             x, y + fm.getHeight(), new Color(200,170,0));
        g2d.setPaint(gp);
        g2d.drawString(title, x, y);
    }

    /**
     * SettingsChangeListener methods
     * These will update the game settings based on user input
     */
    @Override
    public void onVolumeChanged(int level) {
        float gain = (level - 1) / 9.0f;  // 0.0 - 1.0
        setGameMasterGain(gain);
    }

    @Override
    public void onBrightnessChanged(int level) {
        float factor = 0.2f + (level - 1) * 0.08f;
        int r = (int)(10 * factor);
        int g = (int)(10 * factor);
        int b = (int)(40 * factor);
        BG_COLOR = new Color(r, g, b);
        repaint();
    }


    // Sets the master gain for the audio system
    private void setGameMasterGain(float gain) {
        try {
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            Mixer mixer = AudioSystem.getMixer(infos[0]);
            mixer.open();
            Line.Info[] lines = mixer.getTargetLineInfo();
            for (Line.Info li : lines) {
                Line line = mixer.getLine(li);
                line.open();
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl ctrl =
                        (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float min = ctrl.getMinimum();
                    float max = ctrl.getMaximum();
                    float value = min + gain * (max - min);
                    ctrl.setValue(value);
                }
                line.close();
            }
            mixer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === Game menu ===
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Arms of Tanks");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new GameMenu());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}