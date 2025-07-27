package gamemanu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Enhanced game menu with improved structure, performance and visual effects
 * @author rocka
 */
public class GameMenu extends JPanel implements ActionListener {
    // Configuration constants
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 50;
    private static final int STAR_COUNT = 100;
    private static final int STAR_SPEED = 2;
    private static final Color BG_COLOR = new Color(10, 10, 40);
    private static final Color STAR_COLOR = new Color(200, 220, 255);
    private static final Color TITLE_COLOR = new Color(255, 215, 0);
    private static final Color TITLE_SHADOW = new Color(150, 120, 0);
    
    private final Timer timer;
    private int backgroundY = 0;
    private final Point[] stars = new Point[STAR_COUNT]; // Pre-generated stars
    private final JButton playButton, settingsButton, rulesButton, exitButton;
    private final Random random = new Random();

    public GameMenu() {
        setLayout(null);  // Manual layout for precise positioning
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        initializeStars();  // Pre-generate star positions
        
        // Create buttons with consistent styling
        int centerX = (FRAME_WIDTH - BUTTON_WIDTH) / 2;
        playButton = createButton("Play", centerX, 180);
        settingsButton = createButton("Settings", centerX, 250);
        rulesButton = createButton("Rules", centerX, 320);
        exitButton = createButton("Exit", centerX, 390);
        
        // Add buttons to panel
        add(playButton);
        add(settingsButton);
        add(rulesButton);
        add(exitButton);

        // Animation timer (60 FPS)
        timer = new Timer(16, this);
        timer.start();
    }

    /**
     * Pre-generates star positions for optimized rendering
     */
    private void initializeStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i] = new Point(
                random.nextInt(FRAME_WIDTH),
                random.nextInt(FRAME_HEIGHT)
            );
        }
    }


    private JButton createButton(String text, int x, int y) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                // Custom button rendering for smooth edges
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Hover effect
                if (getModel().isRollover()) {
                    g2d.setColor(new Color(60, 60, 90));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2d.setColor(Color.CYAN);
                } else {
                    g2d.setColor(new Color(40, 40, 70, 200));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2d.setColor(Color.WHITE);
                }
                
                // Text rendering
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                Rectangle r = new Rectangle(getWidth(), getHeight());
                int textY = (r.height - fm.getHeight()) / 2 + fm.getAscent();
                int textX = (r.width - fm.stringWidth(getText())) / 2;
                g2d.drawString(getText(), textX, textY);
                g2d.dispose();
            }
        };
        
        button.setBounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        
        // Add action listener
        button.addActionListener(e -> handleButtonClick(text));
        
        return button;
    }


    private void handleButtonClick(String buttonText) {
        switch (buttonText) {
            case "Play" -> System.out.println("Starting game...");
            case "Settings" -> System.out.println("Opening settings panel...");
            case "Rules" -> System.out.println("Displaying game rules...");
            case "Exit" -> System.exit(0);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawBackground(g2d);
        drawTitle(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        // Draw space background
        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw animated stars
        g2d.setColor(STAR_COLOR);
        for (Point star : stars) {
            int drawY = (star.y + backgroundY) % getHeight();
            int size = 1 + (star.x % 3); // Vary star size
            g2d.fillOval(star.x, drawY, size, size);
        }
    }

    private void drawTitle(Graphics2D g2d) {
        String title = "ARMS OF TANKS";
        Font titleFont = new Font("Arial", Font.BOLD, 56);
        g2d.setFont(titleFont);
        
        // Calculate centered position
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int x = (getWidth() - titleWidth) / 2;
        int y = 100;
        
        // Draw text shadow
        g2d.setColor(TITLE_SHADOW);
        g2d.drawString(title, x + 3, y + 3);
        
        // Draw main title with gradient
        GradientPaint gradient = new GradientPaint(
            x, y, TITLE_COLOR, 
            x, y + fm.getHeight(), TITLE_COLOR.darker()
        );
        g2d.setPaint(gradient);
        g2d.drawString(title, x, y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update animation state
        backgroundY = (backgroundY + STAR_SPEED) % getHeight();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Arms of Tanks");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            
            GameMenu menu = new GameMenu();
            frame.add(menu);
            frame.pack(); // Use preferred size from panel
            frame.setLocationRelativeTo(null); // Center window
            
            frame.setVisible(true);
        });
    }
    
    /**
     * Helper class for star coordinates
     */
    private static class Point {
        final int x;
        final int y;
        
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}