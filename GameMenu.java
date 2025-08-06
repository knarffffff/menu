package gamemanu; 

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GameMenu extends JPanel
        implements ActionListener, SettingsChangeListener {

    private Timer timer;
    private JButton playButton, settingsButton, rulesButton, exitButton;

    
    private Random rand = new Random();

    // Tank position
    private int tankX = -100, tankY = 450;

    // Effects
    private java.util.List<Explosion> explosions   = new ArrayList<>();
    private java.util.List<SmokeTrail> smokeTrails = new ArrayList<>();
    private java.util.List<Gunfire> gunfires       = new ArrayList<>();
    private java.util.List<Cloud> clouds           = new ArrayList<>();

    /** Brightness level 1–10 */
    private int brightnessLevel = 10;

    public GameMenu() {
        setLayout(null);

        // Initialize music
        // music.init("res/game_music.wav");

        // Create buttons
        playButton     = createButton("Play",     300, 180);
        settingsButton = createButton("Settings", 300, 250);
        rulesButton    = createButton("Rules",    300, 320);
        exitButton     = createButton("Exit",     300, 390);
        add(playButton);
        add(settingsButton);
        add(rulesButton);
        add(exitButton);

        // Generate clouds
        for (int i = 0; i < 5; i++) {
            clouds.add(new Cloud(
                rand.nextInt(800),
                rand.nextInt(150)
            ));
        }

        // Start animation timer
        timer = new Timer(30, this);
        timer.start();
    }

    private JButton createButton(String text, int x, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(x, y, 200, 50);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Monospaced", Font.BOLD, 20));
        btn.addActionListener(e -> handleButtonClick(text));
        return btn;
    }

    private void handleButtonClick(String buttonText) {
       

        switch (buttonText) {
            case "Play":
                System.out.println("Play the game!");
                break;

            case "Settings":
                JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                SettingsDialog dlg = new SettingsDialog(parent, this);
                dlg.pack();
                dlg.setLocationRelativeTo(parent);
                dlg.setVisible(true);
                break;

            case "Rules":
                System.out.println("Show how to play.");
                break;

            case "Exit":
                System.exit(0);
                break;
        }
    }

    // === SettingsChangeListener  ===
    @Override
    public void onVolumeChanged(int level) {
        float vol = Math.max(0f, Math.min(1f, level / 10f));
        // If you had a music player, you would set its volume here
        System.out.println("Volume set to: " + level);
    }

    @Override
    public void onBrightnessChanged(int level) {
        brightnessLevel = Math.max(1, Math.min(10, level));
        System.out.println("Brightness set to: " + brightnessLevel);
    }
    // === SettingsChangeListener Over ===

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Adjust sky color based on brightness level
        int baseR = 135, baseG = 206, baseB = 235;
        float f = brightnessLevel / 10f;
        int r = (int)(baseR * f), gg = (int)(baseG * f), b = (int)(baseB * f);
        g2.setColor(new Color(r, gg, b));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Floor
        g2.setColor(new Color(34, 139, 34));
        g2.fillRect(0, getHeight() - 150, getWidth(), 150);

        // Tank and title
        drawTank(g2);
        drawTitle(g2);

        // Effects
        updateAndDraw(explosions,   g2);
        updateAndDraw(smokeTrails,  g2);
        updateAndDraw(gunfires,     g2);

        // Randomly generate new effects
        if (rand.nextInt(20) == 0) explosions.add(new Explosion());
        if (rand.nextInt(10) == 0) smokeTrails.add(new SmokeTrail());
        if (rand.nextInt( 6) == 0) gunfires.add(new Gunfire());
    }

    private <T extends Effect> void updateAndDraw(java.util.List<T> list, Graphics2D g) {
        for (Iterator<T> it = list.iterator(); it.hasNext(); ) {
            T e = it.next();
            e.update();
            e.draw(g);
            if (e.isDone()) it.remove();
        }
    }

    private void drawTitle(Graphics2D g2) {
        String title = "IRON VANGUARD";
        Font font = new Font("Monospaced", Font.BOLD, 48);
        g2.setFont(font);
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        FontMetrics fm = g2.getFontMetrics(font);
        int w = fm.stringWidth(title), x = (getWidth() - w) / 2, y = 100;
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(title, x + 3, y + 3);
        g2.setColor(Color.GREEN.darker());
        g2.drawString(title, x, y);
    }

    private void drawTank(Graphics2D g) {
        g.setColor(new Color(60, 80, 60));
        g.fillRect(tankX, tankY, 100, 30);
        g.fillRect(tankX + 20, tankY - 20, 60, 20);
        g.fillRect(tankX + 75, tankY - 15, 30, 5);
        g.setColor(Color.BLACK);
        for (int i = 0; i < 5; i++) {
            g.fillOval(tankX + i * 20, tankY + 25, 15, 15);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankX += 2;
        if (tankX > getWidth()) tankX = -120;
        for (Cloud c : clouds) {
            c.x -= 1;
            if (c.x < -100) c.x = getWidth();
        }
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Iron Vanguard");
        GameMenu menu = new GameMenu();
        frame.add(menu);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }


    
    private abstract class Effect {
        abstract void update();
        abstract void draw(Graphics2D g);
        abstract boolean isDone();
    }

    public class Explosion extends Effect {
        int x = rand.nextInt(800), y = rand.nextInt(300) + 100;
        int radius = 10, max = 30;
        boolean done = false;
        void update() { radius += 2; if (radius > max) done = true; }
        void draw(Graphics2D g) {
            g.setColor(new Color(255, rand.nextInt(100), 0, 180));
            g.fillOval(x - radius/2, y - radius/2, radius, radius);
        }
        boolean isDone() { return done; }
    }

    public class SmokeTrail extends Effect {
        int x = rand.nextInt(800), y = rand.nextInt(200) + 250;
        int size = 20, alpha = 200;
        void update() { y -= 1; alpha -= 4; }
        void draw(Graphics2D g) {
            g.setColor(new Color(120, 120, 120, Math.max(0, alpha)));
            g.fillOval(x, y, size, size);
        }
        boolean isDone() { return alpha <= 0; }
    }

    public class Gunfire extends Effect {
        int x = rand.nextInt(800), y = rand.nextInt(200) + 150;
        int length = 40;
        void update() { length -= 5; }
        void draw(Graphics2D g) {
            g.setColor(Color.YELLOW);
            g.drawLine(x, y, x, y - length);
        }
        boolean isDone() { return length <= 0; }
    }

    static class Cloud {
        int x, y;
        Cloud(int x, int y) { this.x = x; this.y = y; }
    }
}
