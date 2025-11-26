package game.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import game.combat.Weapon;
import game.entity.player.Player;
import game.entity.monster.Monster;
import game.state.GameState;

public class GamePanel extends JPanel implements KeyListener {

    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;
    private final int FPS = 60;

    private final MainScreen mainFrame;

    public KeyHandler keyH = new KeyHandler(this);

    public Player player;
    public List<Monster> monsters = new ArrayList<>();

    public GameState gameState = GameState.RUNNING;
    private boolean paused = false;

    private Random rand = new Random();
    private int spawnTimer = 0;
    private final int SPAWN_INTERVAL = 60;

    private Image backgroundImage;
    private Image batImg, mummyImg, slimeImg;
    private int bgWidth, bgHeight;

    private Timer gameTimer;

    private PausePanel pausePanel;

    // === 무기 자동 공격용 타이머 ===
    private int weaponTimer = 0;

    public GamePanel(MainScreen mainFrame) {
        this.mainFrame = mainFrame;

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        setLayout(null);

        addKeyListener(keyH);
        addKeyListener(this);

        loadImages();

        player = new Player(this, keyH);

        pausePanel = new PausePanel();
        pausePanel.setVisible(false);
        add(pausePanel);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (paused && pausePanel != null && pausePanel.isVisible()) {
                    centerPausePanel();
                    repaint();
                }
            }
        });
    }
    private void centerPausePanel() {
        if (pausePanel == null) return;

        int pw = 380;
        int ph = 230;

        int w = getWidth();
        int h = getHeight();

        pausePanel.setBounds(
                (w - pw) / 2,
                (h - ph) / 2,
                pw, ph
        );
    }

    private void loadImages() {
        try {
            backgroundImage = new ImageIcon(getClass().getResource("/images/grass_2.png")).getImage();
            bgWidth = backgroundImage.getWidth(this);
            bgHeight = backgroundImage.getHeight(this);

            batImg = new ImageIcon(getClass().getResource("/images/monsters/bat.png")).getImage();
            mummyImg = new ImageIcon(getClass().getResource("/images/monsters/mummy.png")).getImage();
            slimeImg = new ImageIcon(getClass().getResource("/images/monsters/slime.png")).getImage();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGameLoop() {
        gameTimer = new Timer(1000 / FPS, e -> {
            update();
            repaint();
        });
        gameTimer.start();
    }

    private void update() {
        if (paused) return;

        if (gameState == GameState.RUNNING) {
            player.update();

            // === 무기 자동 공격 ===
            Weapon w = player.getWeapon();
            if (w != null) {
                weaponTimer++;
                if (weaponTimer >= w.getCooldownFrames()) {
                    weaponTimer = 0;
                    w.attack(this, player, monsters);
                }
            }

            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnMonster();
                spawnTimer = 0;
            }

            // 몬스터 이동 + 충돌 데미지
            for (Monster m : new ArrayList<>(monsters)) {
                if (!m.isAlive()) {
                    monsters.remove(m);
                    continue;
                }
                m.update(player.worldX, player.worldY);

                if (player.getBounds().intersects(m.getBounds())) {
                    player.takeDamage(m.getDamage());
                }
            }
        }
    }

    private void spawnMonster() {
        int type = rand.nextInt(3);
        Image img = (type == 0 ? batImg : type == 1 ? mummyImg : slimeImg);

        int spawnX = player.worldX + rand.nextInt(1600) - 800;
        int spawnY = player.worldY + rand.nextInt(1200) - 600;

        monsters.add(new Monster(spawnX, spawnY, img));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawBackground(g);

        for (Monster m : monsters) m.draw(g, player);
        player.draw(g);

        // 무기 이펙트
        Weapon w = player.getWeapon();
        if (w != null) {
            w.draw(g, player);
        }

        drawUI(g);

        if (paused && pausePanel.isVisible()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private void drawBackground(Graphics g) {
        int offsetX = -(player.worldX % bgWidth);
        int offsetY = -(player.worldY % bgHeight);

        if (offsetX > 0) offsetX -= bgWidth;
        if (offsetY > 0) offsetY -= bgHeight;

        for (int x = offsetX - bgWidth; x < SCREEN_WIDTH + bgWidth; x += bgWidth)
            for (int y = offsetY - bgHeight; y < SCREEN_HEIGHT + bgHeight; y += bgHeight)
                g.drawImage(backgroundImage, x, y, this);
    }

    private void drawUI(Graphics g) {
        g.setColor(Color.GRAY);
        g.fillRect(20, 20, 200, 20);

        g.setColor(Color.RED);
        int hpWidth = (int) (200 * ((double) player.getCurrentHp() / player.getMaxHp()));
        g.fillRect(20, 20, hpWidth, 20);

        g.setColor(Color.WHITE);
        g.drawRect(20, 20, 200, 20);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_ESCAPE) {
            if (!paused) {
                showPauseMenu();
            } else {
                resumeGame();
            }
            return;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    private void showPauseMenu() {
        paused = true;
        gameState = GameState.PAUSED;

        centerPausePanel(); 

        pausePanel.setVisible(true);
        repaint();
    }

    private void resumeGame() {
        paused = false;
        gameState = GameState.RUNNING;
        pausePanel.setVisible(false);
        requestFocusInWindow();
    }

    private void returnToMainMenu() {
        if (gameTimer != null) gameTimer.stop();
        mainFrame.returnToMainMenu();
    }

    private class PausePanel extends JPanel {
        public PausePanel() {
            setOpaque(false);

            setLayout(new GridBagLayout());
            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

            JLabel title = new JLabel("일시정지");
            title.setFont(new Font("맑은 고딕", Font.BOLD, 26));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton resumeBtn = new JButton("뒤로가기");
            JButton menuBtn   = new JButton("메인화면");
            JButton exitBtn   = new JButton("종료");

            resumeBtn.addActionListener(e -> resumeGame());
            menuBtn.addActionListener(e -> returnToMainMenu());
            exitBtn.addActionListener(e -> System.exit(0));

            for (JButton b : new JButton[]{resumeBtn, menuBtn, exitBtn}) {
                b.setFont(new Font("맑은 고딕", Font.BOLD, 18));
                b.setMaximumSize(new Dimension(220, 45));
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            inner.add(title);
            inner.add(Box.createVerticalStrut(18));
            inner.add(resumeBtn);
            inner.add(Box.createVerticalStrut(10));
            inner.add(menuBtn);
            inner.add(Box.createVerticalStrut(10));
            inner.add(exitBtn);

            add(inner);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 35;

            g2.setPaint(new GradientPaint(
                    0, 0, new Color(255, 255, 255, 190),
                    0, getHeight(), new Color(235, 235, 235, 175)
            ));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            g2.setColor(new Color(200, 200, 200, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public int getScreenWidth() { return SCREEN_WIDTH; }
    public int getScreenHeight() { return SCREEN_HEIGHT; }
}
