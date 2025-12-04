package game.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import game.combat.ArrowProjectile;
import game.combat.FireballProjectile;
import game.combat.WeaponType;
import game.entity.ExpOrb;
import game.entity.monster.Monster;
import game.entity.player.Player;
import game.effects.DamageText;
import game.state.GameState;

public class GamePanel extends JPanel implements KeyListener {

    private final int SCREEN_WIDTH  = 800;
    private final int SCREEN_HEIGHT = 600;
    private final int FPS = 60;
    
    // UI 애니메이션용
    private double uiHpDisplay;
    private double uiExpDisplay;
    private int uiTick = 0;


    private final MainScreen mainFrame;

    public KeyHandler keyH = new KeyHandler(this);
    private java.util.List<DamageText> damageTexts = new ArrayList<>();

    public Player player;
    public java.util.List<Monster> monsters = new ArrayList<>();

    private javax.swing.Timer gameTimer;
    private GameOverPanel gameOverPanel;   // 게임오버 화면

    // 투사체들
    private java.util.List<ArrowProjectile> arrows       = new ArrayList<>();
    private java.util.List<FireballProjectile> fireballs = new ArrayList<>();

    // 경험치 구슬
    private java.util.List<ExpOrb> expOrbs = new ArrayList<>();

    public GameState gameState = GameState.RUNNING;
    private boolean paused = false;

    private Random rand = new Random();
    private int spawnTimer = 0;
    private final int SPAWN_INTERVAL = 60;

    private Image backgroundImage;
    private Image batImg, mummyImg, slimeImg;
    private int bgWidth, bgHeight;

    private PausePanel pausePanel;
    private LevelUpPanel levelUpPanel;
    private WeaponSelectPanel weaponSelectPanel;

    // 통계
    private int killCount = 0;
    private long startNanoTime = 0L;
    
    // TAB으로 여닫는 상태 패널
    private boolean showStatusPanel = false;
    
    // LEVEL UP 패널을 나중에 띄우기 위한 플래그
    private boolean pendingLevelUpPanel = false;


    // LEVEL UP! 텍스트 표시용
    private int levelUpMessageTimer = 0;

    // 레벨업 선택지
    private enum ChoiceType { PASSIVE_ATK, PASSIVE_SPD, PASSIVE_HP, WEAPON }

    private static class LevelUpChoice {
        ChoiceType type;
        WeaponType weaponType; // WEAPON일 때만 사용
        String title;
        String desc;           // 툴팁용 설명
    }

    private LevelUpChoice[] levelUpChoices = new LevelUpChoice[3];

    // 시작 무기 선택 중인지 여부
    private boolean waitingWeaponSelect = true;

    // ----------------------------------------------------
    // 생성자
    // ----------------------------------------------------
    public GamePanel(MainScreen mainFrame) {
        this.mainFrame = mainFrame;

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        setLayout(null);
        setFocusTraversalKeysEnabled(false); 

        addKeyListener(keyH);
        addKeyListener(this);

        loadImages();

        // 처음에는 무기 없이 생성 (시작 시 선택)
        player = new Player(this, keyH, null);
        
        // UI 표시용 초기값
        uiHpDisplay = player.getMaxHp();
        uiExpDisplay = 0.0;

        pausePanel = new PausePanel();
        pausePanel.setVisible(false);
        add(pausePanel);

        levelUpPanel = new LevelUpPanel();
        levelUpPanel.setVisible(false);
        add(levelUpPanel);

        weaponSelectPanel = new WeaponSelectPanel();
        weaponSelectPanel.setVisible(true);
        add(weaponSelectPanel);
        
        gameOverPanel = new GameOverPanel();
        gameOverPanel.setVisible(false);
        add(gameOverPanel);

        // 창 크기 바뀔 때 오버레이 패널과 플레이어 화면 위치 맞추기
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (pausePanel != null) {
                    pausePanel.setBounds(0, 0, getWidth(), getHeight());
                    pausePanel.revalidate();
                    pausePanel.repaint();
                }
                if (levelUpPanel != null) {
                    levelUpPanel.setBounds(0, 0, getWidth(), getHeight());
                    levelUpPanel.revalidate();
                    levelUpPanel.repaint();
                }
                if (weaponSelectPanel != null) {
                    weaponSelectPanel.setBounds(0, 0, getWidth(), getHeight());
                    weaponSelectPanel.revalidate();
                    weaponSelectPanel.repaint();
                }
                if (player != null) {
                    player.updateScreenCenter();
                }
                if (gameOverPanel != null) {
                    gameOverPanel.setBounds(0, 0, getWidth(), getHeight());
                    gameOverPanel.revalidate();
                    gameOverPanel.repaint();
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
            bgWidth  = backgroundImage.getWidth(this);
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

    // 활 발사체 생성
    public void spawnArrow(double startX, double startY,
                           double dirX, double dirY,
                           int damage, int hitsAllowed,
                           double speed) {
        arrows.add(new ArrowProjectile(this, startX, startY,
                dirX, dirY, damage, hitsAllowed, speed));
    }

    // 파이어볼 발사체 생성
    public void spawnFireball(double startX, double startY,
                              double dirX, double dirY,
                              int damage, int radius) {
        fireballs.add(new FireballProjectile(this, startX, startY,
                dirX, dirY, damage, radius));
    }

    // ----------------------------------------------------
    // 게임 루프 시작
    // ----------------------------------------------------
    public void startGameLoop() {
        // JOptionPane 으로 무기 선택하던 부분 제거
        // 무기 선택은 WeaponSelectPanel에서 처리
        gameTimer = new javax.swing.Timer(1000 / FPS, e -> {
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

        // 아직 시작 무기를 선택하지 않았다면 게임 로직 진행 X
        if (waitingWeaponSelect) {
            return;
        }

        if (gameState == GameState.RUNNING) {
            player.update();

            // 몬스터 스폰
            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnMonster();
                spawnTimer = 0;
            }

            // 몬스터 이동 + 충돌/사망 처리
            for (Iterator<Monster> it = monsters.iterator(); it.hasNext();) {
                Monster m = it.next();

                if (!m.isAlive()) {
                    // 죽은 몬스터 → 제거 + 킬 카운트 증가 + 경험치 구슬 드랍
                    it.remove();
                    killCount++;
                    spawnExpOrb(m);
                    continue;
                }

                m.update(player.worldX, player.worldY);

                if (player.getBounds().intersects(m.getBounds())) {
                    player.takeDamage(m.getDamage());
                }
            }

            // 경험치 구슬 먹기
            for (Iterator<ExpOrb> it = expOrbs.iterator(); it.hasNext();) {
                ExpOrb orb = it.next();

                // orb.update(player)가 true면 플레이어에게 흡수된 것
                if (orb.update(player)) {
                    it.remove();
                    boolean leveledUp = player.gainExp(orb.getValue());
                    if (leveledUp) {
                        handleLevelUp();
                        break; // 이번 프레임은 여기까지
                    }
                }
            }

            // 무기 자동 공격 (보유한 모든 무기)
            for (Player.OwnedWeapon ow : player.getOwnedWeapons()) {
                if (ow.weapon == null) continue;

                ow.cooldownCounter++;
                int cd = ow.weapon.getCooldownFrames(player);
                if (ow.cooldownCounter >= cd) {
                    ow.cooldownCounter = 0;
                    ow.weapon.attack(this, player, monsters);
                }
            }

            // 화살 업데이트
            for (Iterator<ArrowProjectile> it = arrows.iterator(); it.hasNext();) {
                ArrowProjectile arrow = it.next();
                arrow.update(monsters, player);
                if (!arrow.isAlive()) {
                    it.remove();
                }
            }

            // 파이어볼 업데이트
            for (Iterator<FireballProjectile> it = fireballs.iterator(); it.hasNext();) {
                FireballProjectile fb = it.next();
                fb.update(monsters, player);
                if (!fb.isAlive()) {
                    it.remove();
                }
            }

            // 데미지 텍스트 업데이트 및 제거
            damageTexts.removeIf(DamageText::update);
            if (player.getCurrentHp() <= 0 && gameState != GameState.GAMEOVER) {
                triggerGameOver();
            }
            
            // ───── UI 애니메이션 (HP/EXP 보간, 틱 증가) ─────
            double hpTarget = player.getCurrentHp();
            uiHpDisplay += (hpTarget - uiHpDisplay) * 0.15;   // 부드럽게 따라가기

            double expTarget = 0.0;
            if (player.getExpToNextLevel() > 0) {
                expTarget = (double) player.getCurrentExp() / player.getExpToNextLevel();
            }
            uiExpDisplay += (expTarget - uiExpDisplay) * 0.25;

            uiTick++;
        }
    }

    // 몬스터 스폰
    private void spawnMonster() {
        int type = rand.nextInt(3);
        Image img = (type == 0 ? batImg : type == 1 ? mummyImg : slimeImg);

        int spawnX = player.worldX + rand.nextInt(1600) - 800;
        int spawnY = player.worldY + rand.nextInt(1200) - 600;

        monsters.add(new Monster(spawnX, spawnY, img));
    }

    // 경험치 구슬 드랍
    private void spawnExpOrb(Monster m) {
        int x = m.worldX + m.width / 2;
        int y = m.worldY + m.height / 2;
        int value = 10; // 몬스터당 경험치 양 (필요하면 조정)
        expOrbs.add(new ExpOrb(x, y, value));
    }

    // 데미지 텍스트 추가
    public void addDamageText(int screenX, int screenY, int damage) {
        damageTexts.add(new DamageText(screenX, screenY, damage));
    }

    // ----------------------------------------------------
    // 레벨업 처리
    // ----------------------------------------------------
    private LevelUpChoice makePassiveChoice(ChoiceType type, String title, String desc) {
        LevelUpChoice c = new LevelUpChoice();
        c.type = type;
        c.title = title;
        c.desc = desc;
        return c;
    }

    private LevelUpChoice makeWeaponChoice(WeaponType wt, String title, String desc) {
        LevelUpChoice c = new LevelUpChoice();
        c.type = ChoiceType.WEAPON;
        c.weaponType = wt;
        c.title = title;
        c.desc = desc;
        return c;
    }

    private void handleLevelUp() {
        if (player.getLevel() >= player.getMaxLevel()) return;

        paused = true;
        gameState = GameState.LEVELUP;
        levelUpMessageTimer = 40; // 60이 약 1초 정도 LEVEL UP! 띄우기

        prepareLevelUpChoices();        // 미리 선택지 생성
        pendingLevelUpPanel = true;     // 나중에 패널 띄우겠다는 표시
        levelUpPanel.setVisible(false); // 일단 숨겨둠
    }

    private void prepareLevelUpChoices() {
        java.util.List<LevelUpChoice> pool = new ArrayList<>();

        // 패시브 3개
        pool.add(makePassiveChoice(
                ChoiceType.PASSIVE_ATK,
                "공격력 증가 (+20%)",
                "모든 무기 데미지 +20% 증가"));
        pool.add(makePassiveChoice(
                ChoiceType.PASSIVE_SPD,
                "이동 속도 증가",
                "플레이어 이동 속도가 1만큼 증가"));
        pool.add(makePassiveChoice(
                ChoiceType.PASSIVE_HP,
                "최대 체력 증가 (+20)",
                "최대 체력이 +20 증가하며, 그만큼 체력 즉시 회복"));

        // 무기 3개 (이미 3단계면 후보에서 제외)
        if (player.canUpgradeWeapon(WeaponType.SWORD)) {
            pool.add(makeWeaponChoice(
                    WeaponType.SWORD,
                    "Sword 강화/획득",
                    "근거리 360도 공격\n레벨마다 데미지↑, 범위↑, 쿨타임↓"));
        }
        if (player.canUpgradeWeapon(WeaponType.BOW)) {
            pool.add(makeWeaponChoice(
                    WeaponType.BOW,
                    "Bow 강화/획득",
                    "중거리 투사체\n레벨마다 데미지↑, 화살 개수↑, 관통 수↑, 속도↑"));
        }
        if (player.canUpgradeWeapon(WeaponType.STAFF)) {
            pool.add(makeWeaponChoice(
                    WeaponType.STAFF,
                    "Staff 강화/획득",
                    "먼 거리 폭발 마법\n레벨마다 데미지↑, 폭발 반경↑, 쿨타임↓"));
        }

        Collections.shuffle(pool, rand);

        for (int i = 0; i < 3; i++) {
            levelUpChoices[i] = (i < pool.size()) ? pool.get(i) : null;
        }
    }

    private void applyLevelUpChoice(int idx) {
        LevelUpChoice choice = levelUpChoices[idx];
        if (choice == null) return;

        switch (choice.type) {
            case PASSIVE_ATK:
                player.upgradeAttack();
                break;
            case PASSIVE_SPD:
                player.upgradeSpeed();
                break;
            case PASSIVE_HP:
                player.upgradeMaxHp();
                break;
            case WEAPON:
                player.addOrUpgradeWeapon(choice.weaponType);
                break;
        }

        levelUpPanel.setVisible(false);
        paused = false;
        gameState = GameState.RUNNING;
        requestFocusInWindow();
    }

    // ----------------------------------------------------
    // 그리기
    // ----------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 배경
        drawBackground(g);

        // 몬스터
        for (Monster m : monsters) {
            m.draw(g, player);
        }

        // 경험치 구슬
        for (ExpOrb orb : expOrbs) {
            orb.draw(g, player);
        }

        // 플레이어
        player.draw(g);

        // 투사체들
        Graphics2D g2 = (Graphics2D) g.create();
        for (ArrowProjectile arrow : arrows) {
            arrow.draw(g2, player);
        }
        for (FireballProjectile fb : fireballs) {
            fb.draw(g2, player);
        }
        g2.dispose();

        // 무기 이펙트 (보유한 모든 무기)
        for (Player.OwnedWeapon ow : player.getOwnedWeapons()) {
            if (ow.weapon != null) {
                ow.weapon.draw(g, player);
            }
        }

        // 데미지 텍스트
        Graphics2D g2d = (Graphics2D) g;
        for (DamageText dt : damageTexts) {
            dt.draw(g2d);
        }

        // UI (HP + Kill + Time + 레벨/패시브/무기현황)
        drawUI(g);

        // LEVEL UP! 텍스트 (잠깐 크게)
        if (gameState == GameState.LEVELUP && levelUpMessageTimer > 0) {
            Graphics2D gLv = (Graphics2D) g.create();
            gLv.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            String text = "LEVEL UP!";
            gLv.setFont(new Font("맑은 고딕", Font.BOLD, 48));
            FontMetrics fm = gLv.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();

            int x = (getWidth() - tw) / 2;
            int y = (getHeight() / 2) - 120;

            gLv.setColor(new Color(0, 0, 0, 160));
            gLv.drawString(text, x + 4, y + 4);

            gLv.setColor(new Color(255, 240, 100));
            gLv.drawString(text, x, y);

            gLv.dispose();
            levelUpMessageTimer--;
            
            if (pendingLevelUpPanel && levelUpMessageTimer <= 0) {
                levelUpPanel.refreshButtons();
                levelUpPanel.setVisible(true);
                levelUpPanel.repaint();
                pendingLevelUpPanel = false;   // 다시 안 뜨게
            }
            
            double lowHpRatio = (double) player.getCurrentHp() / player.getMaxHp();
            if (lowHpRatio < 0.25) {
                Graphics2D gLow = (Graphics2D) g.create();
                int alpha = (int)(160 * (0.25 - lowHpRatio) / 0.25); // 0~160
                alpha = Math.max(40, Math.min(160, alpha));          // 최소는 조금 보이게
                gLow.setColor(new Color(180, 0, 0, alpha));
                gLow.fillRect(0, 0, getWidth(), getHeight());
                gLow.dispose();
            }
        }
    }

    // 배경 타일링
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

    // UI (HP 바 + 킬/시간 + 레벨 + 패시브 + 무기현황)
    // UI (HP/EXP 바 + 킬/시간 + (TAB) 레벨/패시브/무기현황)
    private void drawUI(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // ───────── 상단 HUD 박스 ─────────
        int hudX = 10;
        int hudY = 10;
        int hudW = 420;
        int hudH = 70;   // 높이 조금 키움 (EXP 바까지)

        Shape hudRect = new RoundRectangle2D.Float(
                hudX, hudY, hudW, hudH, 18, 18);

        // 어두운 반투명 박스 + 살짝 빛나는 테두리
        g2.setPaint(new GradientPaint(
                hudX, hudY,
                new Color(10, 10, 10, 200),
                hudX, hudY + hudH,
                new Color(20, 20, 20, 230)));
        g2.fill(hudRect);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(hudRect);

        // ───────── HP & EXP 영역 ─────────
        int barAreaX = hudX + 16;
        int barAreaY = hudY + 10;
        int barW     = 260;
        int barH     = 16;

        // ===== HP 바 =====
        // 현재 HP (애니메이션 값 사용)
        double hpRatio = uiHpDisplay / player.getMaxHp();
        hpRatio = Math.max(0.0, Math.min(1.0, hpRatio));
        int hpCurW = (int) (barW * hpRatio);

        // HP 바 배경
        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(barAreaX, barAreaY, barW, barH, 12, 12);

        // HP 바 (붉은 그라데이션)
        GradientPaint hpGp = new GradientPaint(
                barAreaX, barAreaY,
                new Color(220, 60, 60),
                barAreaX + barW, barAreaY + barH,
                new Color(140, 0, 0));
        g2.setPaint(hpGp);
        g2.fillRoundRect(barAreaX, barAreaY, hpCurW, barH, 12, 12);

        // 테두리
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawRoundRect(barAreaX, barAreaY, barW, barH, 12, 12);

        // HP 수치 텍스트 (예: 80 / 100)
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        String hpText = (int)Math.round(uiHpDisplay) + " / " + player.getMaxHp();
        FontMetrics fmHp = g2.getFontMetrics();
        int hpTw = fmHp.stringWidth(hpText);

        // 검은 그림자 + 흰 글씨
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(hpText,
                barAreaX + (barW - hpTw) / 2 + 1,
                barAreaY + barH - 4 + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(hpText,
                barAreaX + (barW - hpTw) / 2,
                barAreaY + barH - 4);

        // HP 바 위로 지나가는 반짝이는 라인
        int shineWidth = 40;
        int sx = barAreaX + (uiTick % (barW + shineWidth)) - shineWidth;
        GradientPaint shineGp = new GradientPaint(
                sx, barAreaY,
                new Color(255, 255, 255, 80),
                sx + shineWidth, barAreaY + barH,
                new Color(255, 255, 255, 0));
        g2.setPaint(shineGp);
        g2.fillRoundRect(sx, barAreaY, shineWidth, barH, 12, 12);

        // ===== EXP 바 =====
        int expBarY = barAreaY + barH + 6;
        int curExp  = player.getCurrentExp();
        int nextExp = player.getExpToNextLevel();

        // 애니메이션된 exp 비율 사용 (0.0 ~ 1.0)
        double expRatio = uiExpDisplay;
        expRatio = Math.max(0.0, Math.min(1.0, expRatio));
        int expCurW = (int) (barW * expRatio);

        // EXP 배경
        g2.setColor(new Color(35, 35, 35));
        g2.fillRoundRect(barAreaX, expBarY, barW, 10, 10, 10);

        // 보라색 EXP 바
        GradientPaint expGp = new GradientPaint(
                barAreaX, expBarY,
                new Color(160, 120, 255),
                barAreaX + barW, expBarY + 10,
                new Color(90, 40, 200));
        g2.setPaint(expGp);
        g2.fillRoundRect(barAreaX, expBarY, expCurW, 10, 10, 10);

        // 테두리
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawRoundRect(barAreaX, expBarY, barW, 10, 10, 10);

        // EXP 수치 텍스트 (예: 10 / 50)
        String expText = curExp + " / " + nextExp;
        FontMetrics fmExp = g2.getFontMetrics();
        int expTw = fmExp.stringWidth(expText);

        g2.setColor(new Color(0, 0, 0, 170));
        g2.drawString(expText,
                barAreaX + (barW - expTw) / 2 + 1,
                expBarY + 9);
        g2.setColor(new Color(230, 220, 255));
        g2.drawString(expText,
                barAreaX + (barW - expTw) / 2,
                expBarY + 8);

        // 왼쪽에 현재 레벨 표시
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        String lvText = "Lv " + player.getLevel();
        g2.setColor(new Color(255, 230, 180));
        g2.drawString(lvText, hudX + 20, hudY + hudH - 12);

        // ───────── Kill / Time (우측) ─────────
        int elapsedSec = (int) ((System.nanoTime() - startNanoTime) / 1_000_000_000L);
        String timeText = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("Kill: " + killCount, hudX + hudW - 130, hudY + 26);
        g2.drawString("Time: " + timeText, hudX + hudW - 130, hudY + 46);

        // ───────── TAB 상태 패널 ─────────
        if (showStatusPanel) {
            int panelX = hudX;
            int panelY = hudY + hudH + 8;
            int panelW = 460;
            int panelH = 80;

            Shape panelRect = new RoundRectangle2D.Float(
                    panelX, panelY, panelW, panelH, 18, 18);

            g2.setPaint(new GradientPaint(
                    panelX, panelY,
                    new Color(0, 0, 0, 210),
                    panelX, panelY + panelH,
                    new Color(0, 0, 0, 235)));
            g2.fill(panelRect);

            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(panelRect);

            int y = panelY + 24;
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

            // 레벨 / 경험치
            g2.setColor(Color.WHITE);
            g2.drawString(
                    "Lv " + player.getLevel() +
                            "   EXP " + player.getCurrentExp() + "/" + player.getExpToNextLevel(),
                    panelX + 18, y);

            // 패시브 (작은 아이콘 + 텍스트)
            y += 18;
            int iconY = y - 10;

            // ATK 아이콘 + 글자
            drawSwordIcon(g2, panelX + 22, iconY, 12);
            g2.setColor(new Color(220, 220, 255));
            g2.drawString("ATK +" + (player.getAttackLevel() * 20) + "%",
                          panelX + 34, y);

            // SPD 아이콘 + 글자
            drawBootIcon(g2, panelX + 150, iconY, 12);
            g2.drawString("SPD +" + player.getSpeedLevel(),
                          panelX + 162, y);

            // HP 아이콘 + 글자
            drawHeartIcon(g2, panelX + 260, iconY, 12);
            g2.drawString("HP +" + (player.getMaxHpLevel() * 20),
                          panelX + 272, y);

            // 무기 현황
            y += 18;
            g2.setColor(new Color(255, 230, 180));
            g2.drawString(
                    "Weapons  " + player.getWeaponStatusString(),
                    panelX + 18, y);

            // 안내 문구
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawString("[TAB] : 상태 패널 토글",
                    panelX + 18, panelY + panelH - 10);
        } else {
            // 닫혀 있을 때 작은 힌트
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.drawString("[TAB] : 상태 보기",
                          hudX + 20, hudY + hudH + 14);
        }

        g2.dispose();
    }

    // ----------------------------------------------------
    // KeyListener : ESC → 일시정지/재개
    // ----------------------------------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        
        // 게임오버 중에는 ESC / TAB 입력 다 무시
        if (gameState == GameState.GAMEOVER) return;

        // ───────── TAB : 상태 패널 on/off ─────────
        if (code == KeyEvent.VK_TAB) {

            // 아직 시작 무기 선택 중이면 무시
            if (waitingWeaponSelect) return;

            // 레벨업 창, 일시정지창 떠 있을 때는 TAB 무시
            if (gameState == GameState.LEVELUP || gameState == GameState.PAUSED) {
                return;
            }

            // 단순히 on/off 토글
            showStatusPanel = !showStatusPanel;
            repaint();
            return;   // 아래 ESC 처리와 겹치지 않게 바로 종료
        }

        // ───────── ESC : 일시정지 / 재개 ─────────
        if (code == KeyEvent.VK_ESCAPE) {
            if (gameState == GameState.RUNNING) {
                showPauseMenu();
            } else if (gameState == GameState.PAUSED) {
                resumeGame();
            }
            // LEVELUP 상태에서는 ESC 무시
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    private void showPauseMenu() {
        paused = true;
        gameState = GameState.PAUSED;

        if (player != null) {
            player.updateScreenCenter();
        }

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
    // 공통 커스텀 버튼 : ChoiceButton
    // ----------------------------------------------------
    private static class ChoiceButton extends JButton {

        public ChoiceButton(String text) {
            super(text);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(new Color(30, 30, 30));
            setFont(new Font("맑은 고딕", Font.BOLD, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 20;

            ButtonModel model = getModel();

            // 상태에 따른 색
            Color top, bottom, border;
            if (model.isPressed()) {
                top = new Color(250, 200, 120);
                bottom = new Color(230, 150, 80);
                border = new Color(180, 100, 40);
            } else if (model.isRollover()) {
                top = new Color(255, 230, 170);
                bottom = new Color(245, 190, 120);
                border = new Color(200, 130, 60);
            } else {
                top = new Color(240, 240, 240);
                bottom = new Color(220, 220, 220);
                border = new Color(170, 170, 170);
            }

            // 그라데이션 배경
            GradientPaint gp = new GradientPaint(
                    0, 0, top,
                    0, h, bottom
            );
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

            // 테두리
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            // 텍스트
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();
            int tx = (w - tw) / 2;
            int ty = (h + th) / 2 - 2;

            g2.setColor(getForeground());
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }

    // ----------------------------------------------------
    // PausePanel : 전체 화면 오버레이 + 가운데 박스
    // ----------------------------------------------------
    private class PausePanel extends JPanel {

        private JPanel inner;

        public PausePanel() {
            setOpaque(false);
            setLayout(null);

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

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    centerInner();
                }
            });
        }

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

            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, w, h);

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
            super.paintComponent(g);
        }
    }

    // ----------------------------------------------------
    // LevelUpPanel : 레벨업 선택창 (멋있게 + 툴팁)
    // ----------------------------------------------------
    private class LevelUpPanel extends JPanel {

        private JPanel inner;
        private ChoiceButton[] optionButtons = new ChoiceButton[3];

        public LevelUpPanel() {
            setOpaque(false);
            setLayout(null);

            inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BorderLayout());
            inner.setBorder(BorderFactory.createEmptyBorder(20, 30, 25, 30));

            // ─ 타이틀 ─
            JLabel title = new JLabel("LEVEL UP!");
            title.setFont(new Font("맑은 고딕", Font.BOLD, 30));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setForeground(new Color(40, 25, 10));

            JLabel subtitle = new JLabel("강화할 능력 또는 무기를 선택하세요");
            subtitle.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            subtitle.setHorizontalAlignment(SwingConstants.CENTER);
            subtitle.setForeground(new Color(80, 60, 40));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
            titlePanel.add(title);
            titlePanel.add(Box.createVerticalStrut(4));
            titlePanel.add(subtitle);

            inner.add(titlePanel, BorderLayout.NORTH);

            // ─ 버튼 3개 가로 배치 ─
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 20));

            for (int i = 0; i < 3; i++) {
                optionButtons[i] = new ChoiceButton("옵션 " + (i + 1));
                optionButtons[i].setPreferredSize(new Dimension(220, 50));
                final int idx = i;
                optionButtons[i].addActionListener(e -> applyLevelUpChoice(idx));
                buttonsPanel.add(optionButtons[i]);
            }

            inner.add(buttonsPanel, BorderLayout.CENTER);

            // 힌트 텍스트
            JLabel hint = new JLabel("각 옵션 위에 마우스를 올리면 상세 설명이 표시됩니다");
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            hint.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            hint.setForeground(new Color(90, 80, 70));
            inner.add(hint, BorderLayout.SOUTH);

            add(inner);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    centerInner();
                }
            });
        }

        public void refreshButtons() {
            for (int i = 0; i < 3; i++) {
                LevelUpChoice c = levelUpChoices[i];
                ChoiceButton btn = optionButtons[i];

                if (c != null) {
                    btn.setText(c.title);
                    if (c.desc != null && !c.desc.isEmpty()) {
                        btn.setToolTipText("<html>" + c.desc.replace("\n", "<br>") + "</html>");
                    } else {
                        btn.setToolTipText(null);
                    }
                    btn.setEnabled(true);
                } else {
                    btn.setText("선택 불가");
                    btn.setToolTipText(null);
                    btn.setEnabled(false);
                }
            }
        }

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

            // 화면 전체 어둡게
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, w, h);

            // 가운데 유리창 박스
            Rectangle r = inner.getBounds();
            int padding = 18;
            int dialogX = r.x - padding;
            int dialogY = r.y - padding;
            int dialogW = r.width  + padding * 2;
            int dialogH = r.height + padding * 2;
            int arc = 35;

            GradientPaint gp = new GradientPaint(
                    dialogX, dialogY,
                    new Color(255, 245, 230, 240),
                    dialogX, dialogY + dialogH,
                    new Color(235, 215, 190, 220)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            // 빛나는 테두리
            g2.setColor(new Color(255, 255, 255, 160));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.setColor(new Color(170, 120, 80, 190));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(dialogX + 2, dialogY + 2,
                    dialogW - 4, dialogH - 4, arc - 6, arc - 6);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ----------------------------------------------------
    // WeaponSelectPanel : 시작 무기 선택 창
    // ----------------------------------------------------
    private class WeaponSelectPanel extends JPanel {

        private JPanel inner;

        public WeaponSelectPanel() {
            setOpaque(false);
            setLayout(null);

            inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BorderLayout());
            inner.setBorder(BorderFactory.createEmptyBorder(20, 30, 25, 30));

            // 타이틀
            JLabel title = new JLabel("무기 선택");
            title.setFont(new Font("맑은 고딕", Font.BOLD, 30));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setForeground(new Color(40, 25, 10));

            JLabel subtitle = new JLabel("처음 사용할 무기를 골라주세요");
            subtitle.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            subtitle.setHorizontalAlignment(SwingConstants.CENTER);
            subtitle.setForeground(new Color(80, 60, 40));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
            titlePanel.add(title);
            titlePanel.add(Box.createVerticalStrut(5));
            titlePanel.add(subtitle);

            inner.add(titlePanel, BorderLayout.NORTH);

            // 버튼 3개 (가로)
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 20));

            ChoiceButton swordBtn = new ChoiceButton("Sword");
            ChoiceButton bowBtn   = new ChoiceButton("Bow");
            ChoiceButton staffBtn = new ChoiceButton("Staff");

            swordBtn.setPreferredSize(new Dimension(160, 50));
            bowBtn.setPreferredSize(new Dimension(160, 50));
            staffBtn.setPreferredSize(new Dimension(160, 50));

            // 툴팁
            swordBtn.setToolTipText("<html>근거리 360도 공격<br>레벨마다 데미지↑, 범위↑, 쿨타임↓</html>");
            bowBtn.setToolTipText("<html>중거리 투사체<br>다단 히트, 레벨마다 화살 수↑, 관통 수↑</html>");
            staffBtn.setToolTipText("<html>먼 거리 폭발 마법<br>큰 범위, 레벨마다 데미지↑, 폭발 반경↑</html>");

            swordBtn.addActionListener(e -> chooseWeapon(WeaponType.SWORD));
            bowBtn.addActionListener(e -> chooseWeapon(WeaponType.BOW));
            staffBtn.addActionListener(e -> chooseWeapon(WeaponType.STAFF));

            buttonsPanel.add(swordBtn);
            buttonsPanel.add(bowBtn);
            buttonsPanel.add(staffBtn);

            inner.add(buttonsPanel, BorderLayout.CENTER);

            add(inner);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    centerInner();
                }
            });
            
            SwingUtilities.invokeLater(() -> centerInner());
        }

        private void chooseWeapon(WeaponType type) {
            player.addOrUpgradeWeapon(type); // 기본 무기 등록 (레벨 1)
            waitingWeaponSelect = false;
            setVisible(false);

            // 시간 측정 시작 시점을 선택 이후로
            startNanoTime = System.nanoTime();

            requestFocusInWindow();
        }

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

            // 화면 전체 어둡게
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, w, h);

            // 유리창 박스
            Rectangle r = inner.getBounds();
            int padding = 18;
            int dialogX = r.x - padding;
            int dialogY = r.y - padding;
            int dialogW = r.width  + padding * 2;
            int dialogH = r.height + padding * 2;
            int arc = 35;

            GradientPaint gp = new GradientPaint(
                    dialogX, dialogY,
                    new Color(240, 245, 255, 240),
                    dialogX, dialogY + dialogH,
                    new Color(210, 220, 245, 220)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.setColor(new Color(255, 255, 255, 190));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.setColor(new Color(120, 140, 200, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(dialogX + 2, dialogY + 2,
                    dialogW - 4, dialogH - 4, arc - 6, arc - 6);

            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    // ----------------------------------------------------
    // GameOverPanel : HP 0일 때 뜨는 게임오버 화면
    // ----------------------------------------------------
    private class GameOverPanel extends JPanel {

        private JPanel inner;

        public GameOverPanel() {
            setOpaque(false);
            setLayout(null);

            inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BorderLayout());
            inner.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

            // ─ 타이틀 ─
            JLabel title = new JLabel("GAME OVER");
            title.setFont(new Font("Serif", Font.BOLD, 48));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setForeground(new Color(220, 70, 40));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
            titlePanel.add(title);

            inner.add(titlePanel, BorderLayout.NORTH);

            // ─ 버튼 2개 (세로) ─
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

            ChoiceButton contBtn = new ChoiceButton("Continue");
            ChoiceButton quitBtn = new ChoiceButton("Quit");

            contBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            quitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            contBtn.setPreferredSize(new Dimension(220, 45));
            quitBtn.setPreferredSize(new Dimension(220, 45));
            contBtn.setMaximumSize(new Dimension(220, 45));
            quitBtn.setMaximumSize(new Dimension(220, 45));

            contBtn.addActionListener(e -> restartRun());
            quitBtn.addActionListener(e -> returnToMainMenu());

            buttonsPanel.add(Box.createVerticalStrut(25));
            buttonsPanel.add(contBtn);
            buttonsPanel.add(Box.createVerticalStrut(12));
            buttonsPanel.add(quitBtn);
            buttonsPanel.add(Box.createVerticalStrut(10));

            inner.add(buttonsPanel, BorderLayout.CENTER);

            add(inner);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    centerInner();
                }
            });
        }

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

            // 전체를 어둡게
            g2.setColor(new Color(0, 0, 0, 230));
            g2.fillRect(0, 0, w, h);

            // 가운데 다크 박스
            Rectangle r = inner.getBounds();
            int padding = 24;
            int dialogX = r.x - padding;
            int dialogY = r.y - padding;
            int dialogW = r.width  + padding * 2;
            int dialogH = r.height + padding * 2;
            int arc = 30;

            GradientPaint gp = new GradientPaint(
                    dialogX, dialogY,
                    new Color(25, 25, 25, 240),
                    dialogX, dialogY + dialogH,
                    new Color(5, 5, 5, 240)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            // 붉은 테두리
            g2.setColor(new Color(200, 80, 40, 220));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    private void drawHeartIcon(Graphics2D g2, int cx, int cy, int size) {
        int w = size;
        int h = size;
        int x = cx - w / 2;
        int y = cy - h / 2;

        g2.setColor(new Color(220, 40, 70));
        g2.fillOval(x, y, w / 2, h / 2);
        g2.fillOval(x + w / 2, y, w / 2, h / 2);

        Polygon p = new Polygon();
        p.addPoint(x, y + h / 4);
        p.addPoint(x + w, y + h / 4);
        p.addPoint(x + w / 2, y + h);
        g2.fillPolygon(p);

        g2.setColor(new Color(120, 0, 30));
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(p);
    }

    private void drawSwordIcon(Graphics2D g2, int cx, int cy, int size) {
        int bladeLen = size;
        int bladeW   = size / 4;

        int x = cx - bladeW / 2;
        int y = cy - bladeLen / 2;

        // 칼날
        g2.setColor(new Color(200, 200, 220));
        g2.fillRoundRect(x, y, bladeW, bladeLen, 4, 4);

        // 손잡이
        g2.setColor(new Color(120, 80, 40));
        g2.fillRect(cx - size / 2, cy + bladeLen / 2 - 2, size, 4);
    }
    
    // ─────────────────────────────────────
    // 작은 UI 아이콘들 (하트 / 검 / 부츠)
    // ─────────────────────────────────────
    private void drawBootIcon(Graphics2D g2, int cx, int cy, int size) {
        int w = size;
        int h = size;
        int x = cx - w / 2;
        int y = cy - h / 2;

        g2.setColor(new Color(150, 110, 60));
        g2.fillRoundRect(x, y + h / 4, w, h / 2, 4, 4);

        // 앞코
        g2.fillRoundRect(x + w / 2, y + h / 2, w / 2, h / 3, 4, 4);

        g2.setColor(new Color(90, 60, 30));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y + h / 4, w, h / 2, 4, 4);
    }
    
 // 게임오버 진입
    private void triggerGameOver() {
        paused = true;
        gameState = GameState.GAMEOVER;
        showStatusPanel = false; // TAB 패널 끄기

        if (gameOverPanel != null) {
            gameOverPanel.setBounds(0, 0, getWidth(), getHeight());
            gameOverPanel.setVisible(true);
            gameOverPanel.revalidate();
            gameOverPanel.repaint();
        }
    }

    // Continue 버튼에서 호출: 한 판을 완전히 새로 시작
    private void restartRun() {

        // 기존 객체들 싹 정리
        monsters.clear();
        expOrbs.clear();
        arrows.clear();
        fireballs.clear();
        damageTexts.clear();

        // 플레이어 새로 만들기 (처음 상태)
        player = new Player(this, keyH, null);
        player.updateScreenCenter();

        // 통계/상태 리셋
        killCount = 0;
        startNanoTime = 0L;
        waitingWeaponSelect = true;  // 다시 무기 선택부터
        showStatusPanel = false;

        // HP/EXP UI 애니메이션 값도 초기화 (있다면)
        uiHpDisplay = player.getCurrentHp();
        uiExpDisplay = 0.0;

        // 패널 상태
        gameOverPanel.setVisible(false);
        weaponSelectPanel.setVisible(true);
        weaponSelectPanel.setBounds(0, 0, getWidth(), getHeight());

        paused = false;
        gameState = GameState.RUNNING;

        requestFocusInWindow();
    }

    // 화면 크기 getter
    public int getScreenWidth()  { return SCREEN_WIDTH; }
    public int getScreenHeight() { return SCREEN_HEIGHT; }
}
