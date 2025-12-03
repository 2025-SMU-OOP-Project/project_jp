package game.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import game.combat.Weapon;
import game.combat.ArrowProjectile;
import game.combat.WeaponType;
import game.entity.player.Player;
import game.entity.monster.Monster;
import game.state.GameState;
import game.effects.DamageText;

public class GamePanel extends JPanel implements KeyListener {

    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;
    private final int FPS = 60;

    private final MainScreen mainFrame;

    public KeyHandler keyH = new KeyHandler(this);
    private List<DamageText> damageTexts = new ArrayList<>();

    public Player player;
    public List<Monster> monsters = new ArrayList<>();
    
    private java.util.List<ArrowProjectile> arrows = new ArrayList<>();

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

    // 무기 자동 공격용 타이머
    private int weaponTimer = 0;

    // ================================
    //   ★ 추가: 킬 카운트 & 경과 시간
    // ================================
    private int killCount = 0;       // 처치한 몬스터 수
    private int elapsedFrames = 0;   // 진행된 프레임 수 (RUNNING 일 때만 증가)
    // ================================

    // ----------------------------------------------------
    // 생성자
    // ----------------------------------------------------
    public GamePanel(MainScreen mainFrame, WeaponType initialWeaponType) {
        this.mainFrame = mainFrame;

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        setLayout(null);

        addKeyListener(keyH);
        addKeyListener(this);

        loadImages();

        player = new Player(this, keyH, initialWeaponType);

        pausePanel = new PausePanel();
        pausePanel.setVisible(false);
        // 처음에는 크기를 0,0 에 놔두고 showPauseMenu 에서 맞춰줌
        add(pausePanel);

        // 창 크기 바뀔 때마다 PausePanel 을 화면 전체로 맞춰주기
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (pausePanel != null) {
                    pausePanel.setBounds(0, 0, getWidth(), getHeight());
                    pausePanel.revalidate();
                    pausePanel.repaint();
                }
            }
        });
    }

    // ----------------------------------------------------
    // 이미지 로드
    // ----------------------------------------------------
    private void loadImages() {
        try {
            backgroundImage = new ImageIcon(
                    getClass().getResource("/images/grass_2.png")
            ).getImage();
            bgWidth = backgroundImage.getWidth(this);
            bgHeight = backgroundImage.getHeight(this);

            batImg = new ImageIcon(
                    getClass().getResource("/images/monsters/bat.png")
            ).getImage();
            mummyImg = new ImageIcon(
                    getClass().getResource("/images/monsters/mummy.png")
            ).getImage();
            slimeImg = new ImageIcon(
                    getClass().getResource("/images/monsters/slime.png")
            ).getImage();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void spawnArrow(double startX, double startY, double dirX, double dirY, int damage, int hitsAllowed) {
    	arrows.add(new ArrowProjectile(this, startX, startY, dirX, dirY, damage, hitsAllowed));
    }

    // ----------------------------------------------------
    // 게임 루프 시작
    // ----------------------------------------------------
    public void startGameLoop() {
        gameTimer = new Timer(1000 / FPS, e -> {
            update();
            repaint();
        });
        gameTimer.start();
    }

    // ----------------------------------------------------
    // 업데이트
    // ----------------------------------------------------
    private void update() {
        if (paused) return;

        if (gameState == GameState.RUNNING) {

            //  경과 시간 프레임 증가
            elapsedFrames++;

            player.update();

            // 무기 자동 공격
            Weapon w = player.getWeapon();
            if (w != null) {
                weaponTimer++;
                if (weaponTimer >= w.getCooldownFrames()) {
                    weaponTimer = 0;
                    w.attack(this, player, monsters);
                }
            }

            // 몬스터 스폰
            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnMonster();
                spawnTimer = 0;
            }

            // 몬스터 이동 + 충돌 데미지
            for (Monster m : new ArrayList<>(monsters)) {
                // 죽은 몬스터 제거 + 킬 카운트 증가
                if (!m.isAlive()) {
                    monsters.remove(m);
                    killCount++;      // 한 마리 처치
                    continue;
                }

                m.update(player.worldX, player.worldY);

                if (player.getBounds().intersects(m.getBounds())) {
                    player.takeDamage(m.getDamage());
                }
            }
            
            // 화살 투사체 업데이트
            for (ArrowProjectile arrow : new ArrayList<>(arrows)) {
                if (!arrow.isAlive()) {
                    arrows.remove(arrow);
                    continue;
                }
                arrow.update(monsters, player);
            }

            // 데미지 텍스트 업데이트 및 제거
            damageTexts.removeIf(DamageText::update);
        }
    }

    // ----------------------------------------------------
    // 몬스터 스폰
    // ----------------------------------------------------
    private void spawnMonster() {
        int type = rand.nextInt(3);
        Image img = (type == 0 ? batImg : type == 1 ? mummyImg : slimeImg);

        int spawnX = player.worldX + rand.nextInt(1600) - 800;
        int spawnY = player.worldY + rand.nextInt(1200) - 600;

        monsters.add(new Monster(spawnX, spawnY, img));
    }

    // 데미지 텍스트 추가 (무기가 호출)
    public void addDamageText(int screenX, int screenY, int damage) {
        damageTexts.add(new DamageText(screenX, screenY, damage));
    }

    // ----------------------------------------------------
    // 그리기
    // ----------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 배경
        drawBackground(g);

        // 몬스터 + 플레이어
        for (Monster m : monsters) {
            m.draw(g, player);
        }
        player.draw(g);

        // 무기 이펙트
        Weapon w = player.getWeapon();
        if (w != null) {
            w.draw(g, player);
        }

        Graphics2D g2 = (Graphics2D) g;
        
        // 화살 투사체
        for (ArrowProjectile arrow : arrows) {
            arrow.draw(g2, player);
        }
        
        // 데미지 텍스트
        for (DamageText dt : damageTexts) {
            dt.draw(g2);
        }

        // UI (HP바 + 킬/시간)
        drawUI(g);
        // 어둡게 처리 + 일시정지 박스는 PausePanel 이 그려줌
    }

    // ----------------------------------------------------
    // 배경 타일링
    // ----------------------------------------------------
    private void drawBackground(Graphics g) {
        int offsetX = -(player.worldX % bgWidth);
        int offsetY = -(player.worldY % bgHeight);

        if (offsetX > 0) offsetX -= bgWidth;
        if (offsetY > 0) offsetY -= bgHeight;

        for (int x = offsetX - bgWidth; x < SCREEN_WIDTH + bgWidth; x += bgWidth) {
            for (int y = offsetY - bgHeight; y < SCREEN_HEIGHT + bgHeight; y += bgHeight) {
                g.drawImage(backgroundImage, x, y, this);
            }
        }
    }

    // ----------------------------------------------------
    // UI (HP 바 + 킬/시간)
    // ----------------------------------------------------
    private void drawUI(Graphics g) {
        // HP 바 배경
        g.setColor(Color.GRAY);
        g.fillRect(20, 20, 200, 20);

        // HP 바 채움
        g.setColor(Color.RED);
        int hpWidth = (int) (200 * ((double) player.getCurrentHp() / player.getMaxHp()));
        g.fillRect(20, 20, hpWidth, 20);

        // HP 바 테두리
        g.setColor(Color.WHITE);
        g.drawRect(20, 20, 200, 20);

        // ===== 텍스트(킬 카운트 & 시간) =====
        g.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        g.setColor(Color.WHITE);

        // 킬 카운트
        g.drawString("Kill: " + killCount, 240, 35);

        // 경과 시간 계산 (mm:ss)
        int totalSeconds = elapsedFrames / FPS;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);

        g.drawString("Time: " + timeText, 340, 35);
    }

    // ----------------------------------------------------
    // KeyListener : ESC 토글
    // ----------------------------------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_ESCAPE) {
            if (!paused) {
                showPauseMenu();
            } else {
                resumeGame();  // ESC 다시 누르면 뒤로가기(재개)
            }
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    // ----------------------------------------------------
    // 일시정지 메뉴 ON
    // ----------------------------------------------------
    private void showPauseMenu() {
        paused = true;
        gameState = GameState.PAUSED;

        // 화면 전체를 덮는 오버레이
        pausePanel.setBounds(0, 0, getWidth(), getHeight());
        pausePanel.setVisible(true);
        pausePanel.revalidate();
        pausePanel.repaint();
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

    // ----------------------------------------------------
    // PausePanel : 전체 화면 오버레이 + 가운데 박스
    // ----------------------------------------------------
    private class PausePanel extends JPanel {

        private JPanel inner;   // 제목 + 버튼

        public PausePanel() {
            setOpaque(false);
            setLayout(null);   // 직접 위치 지정

            inner = new JPanel();
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

            // PausePanel 크기 변경 시 inner 를 항상 가운데로
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    centerInner();
                }
            });
        }

        /** inner 패널을 화면 중앙에 배치 */
        private void centerInner() {
            Dimension pref = inner.getPreferredSize();
            int iw = pref.width;
            int ih = pref.height;

            int x = (getWidth()  - iw) / 2;
            int y = (getHeight() - ih) / 2;

            inner.setBounds(x, y, iw, ih);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1) 화면 전체 어둡게
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, w, h);

            // 2) inner 주변에 유리창 박스
            Rectangle r = inner.getBounds();
            int padding = 20;
            int dialogX = r.x - padding;
            int dialogY = r.y - padding;
            int dialogW = r.width  + padding * 2;
            int dialogH = r.height + padding * 2;
            int arc = 35;

            GradientPaint gp = new GradientPaint(
                    dialogX, dialogY,
                    new Color(255, 255, 255, 190),
                    dialogX, dialogY + dialogH,
                    new Color(235, 235, 235, 175)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.setColor(new Color(200, 200, 200, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(dialogX + 1, dialogY + 1,
                    dialogW - 2, dialogH - 2, arc, arc);

            g2.dispose();

            // 버튼/텍스트는 그 위에
            super.paintComponent(g);
        }
    }

    // ----------------------------------------------------
    // 화면 크기 getter
    // ----------------------------------------------------
    public int getScreenWidth()  { return SCREEN_WIDTH; }
    public int getScreenHeight() { return SCREEN_HEIGHT; }
}
