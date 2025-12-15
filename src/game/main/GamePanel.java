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
import game.entity.monster.EnemyProjectile;
import game.save.SaveManager;
import game.save.SaveState;


public class GamePanel extends JPanel implements KeyListener {

    private final int SCREEN_WIDTH  = 800;
    private final int SCREEN_HEIGHT = 600;
    private final int FPS = 60;

    private double uiHpDisplay;
    private double uiExpDisplay;
    private int uiTick = 0;

    private int runKills = 0;
    private int runTimeSec = 0;
    private int runScore = 0;

    private int bestKills = 0;
    private int bestTimeSec = 0;
    private int bestScore = 0;

    private final java.util.prefs.Preferences prefs =
            java.util.prefs.Preferences.userNodeForPackage(GamePanel.class);

    private final MainScreen mainFrame;

    private final java.util.List<Monster> pendingMonstersToAdd = new ArrayList<>();

    public KeyHandler keyH = new KeyHandler(this);
    private java.util.List<DamageText> damageTexts = new ArrayList<>();

    public Player player;
    public java.util.List<Monster> monsters = new ArrayList<>();

    private javax.swing.Timer gameTimer;
    private GameOverPanel gameOverPanel;

    private java.util.List<ArrowProjectile> arrows       = new ArrayList<>();
    private java.util.List<FireballProjectile> fireballs = new ArrayList<>();
    private java.util.List<EnemyProjectile> enemyProjectiles = new ArrayList<>();

    private int eliteTimer = 0;
    private int nonBossKillCount = 0;
    private boolean bossAlive = false;
    private int bossKillThreshold = 50;

    private java.util.List<ExpOrb> expOrbs = new ArrayList<>();

    public GameState gameState = GameState.RUNNING;
    private boolean paused = false;

    private Random rand = new Random();
    private int spawnTimer = 0;
    private final int SPAWN_INTERVAL = 60;
    
    private int autosaveCounter = 0;
    private final int AUTOSAVE_INTERVAL_SEC = 10;

    private Image backgroundImage;
    private Image batImg, mummyImg, slimeImg, dogImg;
    private int bgWidth, bgHeight;

    private PausePanel pausePanel;
    private LevelUpPanel levelUpPanel;
    private WeaponSelectPanel weaponSelectPanel;

    private int killCount = 0;

    private boolean showStatusPanel = false;

    private boolean pendingLevelUpPanel = false;
    private int levelUpMessageTimer = 0;

    private enum ChoiceType { PASSIVE_ATK, PASSIVE_SPD, PASSIVE_HP, WEAPON }

    private static class LevelUpChoice {
        ChoiceType type;
        WeaponType weaponType;
        String title;
        String desc;
    }
    
    private void resetBestRecord() {
        bestScore = 0;
        bestKills = 0;
        bestTimeSec = 0;

        prefs.remove("bestScore");
        prefs.remove("bestKills");
        prefs.remove("bestTimeSec");

        System.out.println("[DEBUG] Best record reset");
    }

    private LevelUpChoice[] levelUpChoices = new LevelUpChoice[3];

    private boolean waitingWeaponSelect = true;

    private long playAccumNano = 0L;
    private long playResumeNano = 0L;

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

        player = new Player(this, keyH, null);

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

        loadBestRecord();

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
            dogImg = new ImageIcon(
                    getClass().getResource("/images/monsters/dog.png")
            ).getImage();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void spawnArrow(double startX, double startY,
                           double dirX, double dirY,
                           int damage, int hitsAllowed,
                           double speed) {
        arrows.add(new ArrowProjectile(this, startX, startY,
                dirX, dirY, damage, hitsAllowed, speed));
    }

    public void spawnFireball(double startX, double startY,
                              double dirX, double dirY,
                              int damage, int radius) {
        fireballs.add(new FireballProjectile(this, startX, startY,
                dirX, dirY, damage, radius));
    }

    public void startGameLoop() {
        gameTimer = new javax.swing.Timer(1000 / FPS, e -> {
            update();
            repaint();
        });
        gameTimer.start();
    }

    private boolean isTimeRunning() {
        if (paused) return false;
        if (waitingWeaponSelect) return false;
        return gameState == GameState.RUNNING;
    }

    private void onEnterRunning() {
        if (playResumeNano == 0L) {
            playResumeNano = System.nanoTime();
        }
    }

    private void onLeaveRunning() {
        if (playResumeNano != 0L) {
            playAccumNano += (System.nanoTime() - playResumeNano);
            playResumeNano = 0L;
        }
    }

    private int getElapsedPlaySec() {
        long total = playAccumNano;
        if (isTimeRunning() && playResumeNano != 0L) {
            total += (System.nanoTime() - playResumeNano);
        }
        return (int)(total / 1_000_000_000L);
    }

    private void resetPlayTime() {
        playAccumNano = 0L;
        playResumeNano = 0L;
    }

    private void update() {
        if (isTimeRunning()) {
            onEnterRunning();
        } else {
            onLeaveRunning();
        }

        if (paused) return;

        if (waitingWeaponSelect) {
            return;
        }

        if (gameState == GameState.RUNNING) {
            player.update();

            int elapsedSec = getElapsedPlaySec();
            int difficultyStage = elapsedSec / 30;

            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnMonster();
                spawnTimer = 0;
            }

            for (Iterator<Monster> it = monsters.iterator(); it.hasNext();) {
                Monster m = it.next();

                if (!m.isAlive()) {
                    it.remove();
                    killCount++;

                    if (!m.isBoss()) nonBossKillCount++;

                    if (m.getKind() == Monster.MonsterKind.SPLITTER) {
                        spawnSplitChildren(m, difficultyStage);
                    }

                    if (m.getKind() == Monster.MonsterKind.BOSS) {
                        bossAlive = false;
                        nonBossKillCount = 0;
                        bossKillThreshold += 20;
                    }

                    spawnExpOrb(m);
                    continue;
                }

                m.update(player.worldX, player.worldY, (sx, sy, dx, dy, dmg, spd) -> {
                    enemyProjectiles.add(new EnemyProjectile(sx, sy, dx, dy, spd, dmg));
                });

                if (player.getBounds().intersects(m.getBounds())) {
                    player.takeDamage(m.getDamage());
                }
            }

            if (!pendingMonstersToAdd.isEmpty()) {
                monsters.addAll(pendingMonstersToAdd);
                pendingMonstersToAdd.clear();
            }

            eliteTimer++;
            if (eliteTimer >= 30 * FPS) {
                spawnEliteMonster(difficultyStage);
                eliteTimer = 0;
            }

            if (!bossAlive && nonBossKillCount >= bossKillThreshold) {
                spawnBossMonster(difficultyStage);
                bossAlive = true;
            }

            for (Iterator<ExpOrb> it = expOrbs.iterator(); it.hasNext();) {
                ExpOrb orb = it.next();

                if (orb.update(player)) {
                    it.remove();
                    boolean leveledUp = player.gainExp(orb.getValue());
                    if (leveledUp) {
                        handleLevelUp();
                        break;
                    }
                }
            }

            for (Player.OwnedWeapon ow : player.getOwnedWeapons()) {
                if (ow.weapon == null) continue;

                ow.cooldownCounter++;
                int cd = ow.weapon.getCooldownFrames(player);
                if (ow.cooldownCounter >= cd) {
                    ow.cooldownCounter = 0;
                    ow.weapon.attack(this, player, monsters);
                }
            }

            for (Iterator<ArrowProjectile> it = arrows.iterator(); it.hasNext();) {
                ArrowProjectile arrow = it.next();
                arrow.update(monsters, player);
                if (!arrow.isAlive()) it.remove();
            }

            for (Iterator<FireballProjectile> it = fireballs.iterator(); it.hasNext();) {
                FireballProjectile fb = it.next();
                fb.update(monsters, player);
                if (!fb.isAlive()) it.remove();
            }

            for (Iterator<EnemyProjectile> itp = enemyProjectiles.iterator(); itp.hasNext();) {
                EnemyProjectile p = itp.next();
                p.update(player);
                if (!p.isAlive()) itp.remove();
            }

            damageTexts.removeIf(DamageText::update);

            if (player.getCurrentHp() <= 0 && gameState != GameState.GAMEOVER) {
                triggerGameOver();
            }

            double hpTarget = player.getCurrentHp();
            uiHpDisplay += (hpTarget - uiHpDisplay) * 0.15;

            double expTarget = 0.0;
            if (player.getExpToNextLevel() > 0) {
                expTarget = (double) player.getCurrentExp() / player.getExpToNextLevel();
            }
            uiExpDisplay += (expTarget - uiExpDisplay) * 0.25;

            // 자동 저장 (10초마다)
            autosaveCounter++;
            if (autosaveCounter >= AUTOSAVE_INTERVAL_SEC * FPS) {
                saveRun();
                autosaveCounter = 0;
            }

            uiTick++;
        }
    }

    private void spawnMonster() {
        int elapsedSec = getElapsedPlaySec();
        int difficultyStage = elapsedSec / 30;

        int spawnCount = 1 + Math.min(2, difficultyStage / 2);

        for (int i = 0; i < spawnCount; i++) {
            Monster.MonsterKind kind = Monster.MonsterKind.NORMAL;
            int roll = rand.nextInt(100);

            if (difficultyStage >= 1) {
                if (roll < 20) kind = Monster.MonsterKind.DASHER;
            }
            if (difficultyStage >= 2) {
                if (roll < 15) kind = Monster.MonsterKind.SHOOTER;
            }
            if (difficultyStage >= 3) {
                if (roll < 10) kind = Monster.MonsterKind.SPLITTER;
            }

            Image img = getImageForKind(kind);

            int spawnX = player.worldX + rand.nextInt(1600) - 800;
            int spawnY = player.worldY + rand.nextInt(1200) - 600;

            monsters.add(new Monster(spawnX, spawnY, img, kind, difficultyStage));
        }
    }

    private Image getImageForKind(Monster.MonsterKind kind) {
        switch (kind) {
            case SHOOTER:     return batImg;
            case DASHER:      return dogImg;
            case SPLITTER:    return slimeImg;
            case SPLIT_CHILD: return slimeImg;
            case NORMAL:      return mummyImg;
            case ELITE:       return mummyImg;
            case BOSS:        return mummyImg;
            default:          return mummyImg;
        }
    }

    private void spawnEliteMonster(int difficultyStage) {
        int spawnX = player.worldX + rand.nextInt(1600) - 800;
        int spawnY = player.worldY + rand.nextInt(1200) - 600;
        monsters.add(new Monster(spawnX, spawnY, getImageForKind(Monster.MonsterKind.ELITE),
                Monster.MonsterKind.ELITE, difficultyStage));
    }

    private void spawnBossMonster(int difficultyStage) {
        int spawnX = player.worldX + rand.nextInt(800) - 400;
        int spawnY = player.worldY + rand.nextInt(600) - 300;
        monsters.add(new Monster(spawnX, spawnY, getImageForKind(Monster.MonsterKind.BOSS),
                Monster.MonsterKind.BOSS, difficultyStage));
    }

    private void spawnSplitChildren(Monster parent, int difficultyStage) {
        int x = parent.worldX;
        int y = parent.worldY;

        Image img = getImageForKind(Monster.MonsterKind.SPLIT_CHILD);

        pendingMonstersToAdd.add(new Monster(x - 12, y - 12, img, Monster.MonsterKind.SPLIT_CHILD, difficultyStage));
        pendingMonstersToAdd.add(new Monster(x + 12, y + 12, img, Monster.MonsterKind.SPLIT_CHILD, difficultyStage));
    }

    private void spawnExpOrb(Monster m) {
        int x = m.worldX + m.width / 2;
        int y = m.worldY + m.height / 2;

        int value = 10;
        if (m.isElite()) value *= 2;
        if (m.isBoss())  value *= 10;

        expOrbs.add(new ExpOrb(x, y, value));
    }

    public void addDamageText(int screenX, int screenY, int damage) {
        damageTexts.add(new DamageText(screenX, screenY, damage));
    }

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

        player.healToFull();

        paused = true;
        gameState = GameState.LEVELUP;
        levelUpMessageTimer = 40;

        prepareLevelUpChoices();
        pendingLevelUpPanel = true;
        levelUpPanel.setVisible(false);
    }

    private void prepareLevelUpChoices() {
        java.util.List<LevelUpChoice> pool = new ArrayList<>();

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawBackground(g);

        for (Monster m : monsters) {
            m.draw(g, player);
        }

        for (ExpOrb orb : expOrbs) {
            orb.draw(g, player);
        }

        player.draw(g);

        Graphics2D g2 = (Graphics2D) g.create();
        for (ArrowProjectile arrow : arrows) arrow.draw(g2, player);
        for (FireballProjectile fb : fireballs) fb.draw(g2, player);
        for (EnemyProjectile p : enemyProjectiles) p.draw(g2, player);
        g2.dispose();

        for (Player.OwnedWeapon ow : player.getOwnedWeapons()) {
            if (ow.weapon != null) ow.weapon.draw(g, player);
        }

        Graphics2D g2d = (Graphics2D) g;
        for (DamageText dt : damageTexts) dt.draw(g2d);

        drawUI(g);

        if (gameState == GameState.LEVELUP && levelUpMessageTimer > 0) {
            Graphics2D gLv = (Graphics2D) g.create();
            gLv.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            String text = "LEVEL UP!";
            gLv.setFont(new Font("맑은 고딕", Font.BOLD, 48));
            FontMetrics fm = gLv.getFontMetrics();
            int tw = fm.stringWidth(text);

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
                pendingLevelUpPanel = false;
            }
        }
    }

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

    private void drawUI(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int hudX = 10;
        int hudY = 10;
        int hudW = 420;
        int hudH = 86;

        Shape hudRect = new RoundRectangle2D.Float(
                hudX, hudY, hudW, hudH, 18, 18);

        g2.setPaint(new GradientPaint(
                hudX, hudY,
                new Color(10, 10, 10, 200),
                hudX, hudY + hudH,
                new Color(20, 20, 20, 230)));
        g2.fill(hudRect);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(hudRect);

        int barAreaX = hudX + 16;
        int barAreaY = hudY + 10;
        int barW     = 260;
        int barH     = 16;

        double hpRatio = uiHpDisplay / player.getMaxHp();
        hpRatio = Math.max(0.0, Math.min(1.0, hpRatio));
        int hpCurW = (int) (barW * hpRatio);

        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(barAreaX, barAreaY, barW, barH, 12, 12);

        GradientPaint hpGp = new GradientPaint(
                barAreaX, barAreaY,
                new Color(220, 60, 60),
                barAreaX + barW, barAreaY + barH,
                new Color(140, 0, 0));
        g2.setPaint(hpGp);
        g2.fillRoundRect(barAreaX, barAreaY, hpCurW, barH, 12, 12);

        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawRoundRect(barAreaX, barAreaY, barW, barH, 12, 12);

        g2.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        String hpText = (int)Math.round(uiHpDisplay) + " / " + player.getMaxHp();
        FontMetrics fmHp = g2.getFontMetrics();
        int hpTw = fmHp.stringWidth(hpText);

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(hpText,
                barAreaX + (barW - hpTw) / 2 + 1,
                barAreaY + barH - 4 + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(hpText,
                barAreaX + (barW - hpTw) / 2,
                barAreaY + barH - 4);

        int shineWidth = 40;
        int sx = barAreaX + (uiTick % (barW + shineWidth)) - shineWidth;
        GradientPaint shineGp = new GradientPaint(
                sx, barAreaY,
                new Color(255, 255, 255, 80),
                sx + shineWidth, barAreaY + barH,
                new Color(255, 255, 255, 0));
        g2.setPaint(shineGp);
        g2.fillRoundRect(sx, barAreaY, shineWidth, barH, 12, 12);

        int expBarY = barAreaY + barH + 6;
        int curExp  = player.getCurrentExp();
        int nextExp = player.getExpToNextLevel();

        double expRatio = uiExpDisplay;
        expRatio = Math.max(0.0, Math.min(1.0, expRatio));
        int expCurW = (int) (barW * expRatio);

        g2.setColor(new Color(35, 35, 35));
        g2.fillRoundRect(barAreaX, expBarY, barW, 10, 10, 10);

        GradientPaint expGp = new GradientPaint(
                barAreaX, expBarY,
                new Color(160, 120, 255),
                barAreaX + barW, expBarY + 10,
                new Color(90, 40, 200));
        g2.setPaint(expGp);
        g2.fillRoundRect(barAreaX, expBarY, expCurW, 10, 10, 10);

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawRoundRect(barAreaX, expBarY, barW, 10, 10, 10);

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

        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        String lvText = "Lv " + player.getLevel();
        g2.setColor(new Color(255, 230, 180));
        g2.drawString(lvText, hudX + 20, hudY + hudH - 12);

        int elapsedSec = getElapsedPlaySec();
        String timeText = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);

        g2.setColor(new Color(220, 220, 220));
        int rightX = hudX + hudW - 130;
        g2.drawString("Kill: " + killCount, rightX, hudY + 28);
        g2.drawString("Time: " + timeText, rightX, hudY + 50);

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

            g2.setColor(Color.WHITE);
            g2.drawString(
                    "Lv " + player.getLevel() +
                            "   EXP " + player.getCurrentExp() + "/" + player.getExpToNextLevel(),
                    panelX + 18, y);

            y += 18;

            g2.setColor(new Color(220, 220, 255));
            g2.drawString("ATK +" + (player.getAttackLevel() * 20) + "%", panelX + 22, y);

            g2.drawString("SPD +" + player.getSpeedLevel(), panelX + 150, y);

            g2.drawString("HP +" + (player.getMaxHpLevel() * 20), panelX + 260, y);

            y += 18;
            g2.setColor(new Color(255, 230, 180));
            g2.drawString(
                    "Weapons  " + player.getWeaponStatusString(),
                    panelX + 18, y);

            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawString("[TAB] : 상태 패널 토글",
                    panelX + 18, panelY + panelH - 10);
        } else {
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.drawString("[TAB] : 상태 보기",
                    hudX + 20, hudY + hudH + 14);
        }
        
        // ===== 오른쪽 위 : 최고 기록 HUD =====
        int bestBoxW = 220;
        int bestBoxH = 72; // Best + Score 2줄
        int bestBoxX = getWidth() - bestBoxW - 14;
        int bestBoxY = 14;

        Shape bestRect = new RoundRectangle2D.Float(
                bestBoxX, bestBoxY, bestBoxW, bestBoxH, 18, 18);

        g2.setPaint(new GradientPaint(
                bestBoxX, bestBoxY,
                new Color(10, 10, 10, 200),
                bestBoxX, bestBoxY + bestBoxH,
                new Color(20, 20, 20, 230)
        ));
        g2.fill(bestRect);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(bestRect);

        // 폰트
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        // ===== Best =====
        g2.setColor(new Color(255, 210, 120));
        String bestText = "Best: " + bestScore;
        FontMetrics fmBest = g2.getFontMetrics();

        int twBest = fmBest.stringWidth(bestText);
        int txBest = bestBoxX + (bestBoxW - twBest) / 2;
        int tyBest = bestBoxY + 28;

        g2.drawString(bestText, txBest, tyBest);

        // ===== 현재 점수 (실시간) =====
        int currentScore = calcScore(killCount, getElapsedPlaySec());

        g2.setColor(new Color(220, 220, 220));
        String scoreText = "Score: " + currentScore;

        FontMetrics fmScore = g2.getFontMetrics();
        int twScore = fmScore.stringWidth(scoreText);
        int txScore = bestBoxX + (bestBoxW - twScore) / 2;
        int tyScore = bestBoxY + 54;

        g2.drawString(scoreText, txScore, tyScore);

        g2.dispose();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (gameState == GameState.GAMEOVER) return;

        if (code == KeyEvent.VK_TAB) {
            if (waitingWeaponSelect) return;
            if (gameState == GameState.LEVELUP || gameState == GameState.PAUSED) return;

            showStatusPanel = !showStatusPanel;
            repaint();
            return;
        }

        if (code == KeyEvent.VK_ESCAPE) {
            if (gameState == GameState.RUNNING) {
                showPauseMenu();
            } else if (gameState == GameState.PAUSED) {
                resumeGame();
            }
        }
        
        // 디버그용 최고기록 초기화
        if (code == KeyEvent.VK_F9) {
            resetBestRecord();
            repaint();
            return;
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

            GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

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
            menuBtn.addActionListener(e -> {
                saveRun();
                returnToMainMenu();
            });
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

            SwingUtilities.invokeLater(this::centerInner);
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

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));

            for (int i = 0; i < 3; i++) {
                optionButtons[i] = new ChoiceButton("옵션 " + (i + 1));
                optionButtons[i].setPreferredSize(new Dimension(220, 50));
                final int idx = i;
                optionButtons[i].addActionListener(e -> applyLevelUpChoice(idx));
                buttonsPanel.add(optionButtons[i]);
            }

            JLabel hint = new JLabel("각 옵션 위에 마우스를 올리면 상세 설명이 표시됩니다");
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            hint.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            hint.setForeground(new Color(90, 80, 70));

            JPanel southBox = new JPanel();
            southBox.setOpaque(false);
            southBox.setLayout(new BoxLayout(southBox, BoxLayout.Y_AXIS));
            southBox.add(buttonsPanel);
            southBox.add(Box.createVerticalStrut(6));
            southBox.add(hint);

            inner.add(southBox, BorderLayout.SOUTH);

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

            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, w, h);

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

    private class WeaponSelectPanel extends JPanel {

        private JPanel inner;

        public WeaponSelectPanel() {
            setOpaque(false);
            setLayout(null);

            inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BorderLayout());
            inner.setBorder(BorderFactory.createEmptyBorder(20, 30, 25, 30));

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

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 20));

            ChoiceButton swordBtn = new ChoiceButton("Sword");
            ChoiceButton bowBtn   = new ChoiceButton("Bow");
            ChoiceButton staffBtn = new ChoiceButton("Staff");

            swordBtn.setPreferredSize(new Dimension(160, 50));
            bowBtn.setPreferredSize(new Dimension(160, 50));
            staffBtn.setPreferredSize(new Dimension(160, 50));

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

            SwingUtilities.invokeLater(this::centerInner);
        }

        private void chooseWeapon(WeaponType type) {
            player.addOrUpgradeWeapon(type);
            waitingWeaponSelect = false;
            setVisible(false);

            resetPlayTime();
            paused = false;
            gameState = GameState.RUNNING;
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

            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, w, h);

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

    private class GameOverPanel extends JPanel {

        private JPanel inner;

        private JLabel runScoreLabel, runKillLabel, runTimeLabel;
        private JLabel bestScoreLabel, bestKillLabel, bestTimeLabel;

        private JLabel bestTitleLabel;
        private boolean showNew = false;

        private javax.swing.Timer blinkTimer;
        private boolean blinkOn = true;

        public GameOverPanel() {
            setOpaque(false);
            setLayout(null);

            inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setBorder(BorderFactory.createEmptyBorder(28, 44, 36, 44));

            inner.setPreferredSize(new Dimension(700, 390));

            JPanel titleRow = new JPanel();
            titleRow.setOpaque(false);
            titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));

            JLabel title = new JLabel("GAME OVER");
            title.setFont(new Font("Serif", Font.BOLD, 54));
            title.setForeground(new Color(220, 70, 40));

            titleRow.add(Box.createHorizontalGlue());
            titleRow.add(title);
            titleRow.add(Box.createHorizontalGlue());

            inner.add(titleRow);
            inner.add(Box.createVerticalStrut(18));

            JPanel table = new JPanel(new GridBagLayout());
            table.setOpaque(false);

            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(8, 14, 8, 14);
            gc.fill = GridBagConstraints.HORIZONTAL;

            double[] wx = {1.35, 1.0, 1.0, 1.0};

            JLabel runTitle = makeRowTitle("이번 판", Color.WHITE);

            bestTitleLabel = makeRowTitle("최고 기록", new Color(255, 210, 120));
            setBestTitleNew(false);

            runScoreLabel = makeCellLabel(Color.WHITE);
            runKillLabel  = makeCellLabel(Color.WHITE);
            runTimeLabel  = makeCellLabel(Color.WHITE);

            bestScoreLabel = makeCellLabel(new Color(255, 210, 120));
            bestKillLabel  = makeCellLabel(new Color(255, 210, 120));
            bestTimeLabel  = makeCellLabel(new Color(255, 210, 120));

            gc.gridy = 0;
            addCell(table, gc, 0, wx[0], runTitle, SwingConstants.LEFT);
            addCell(table, gc, 1, wx[1], runScoreLabel, SwingConstants.RIGHT);
            addCell(table, gc, 2, wx[2], runKillLabel,  SwingConstants.RIGHT);
            addCell(table, gc, 3, wx[3], runTimeLabel,  SwingConstants.RIGHT);

            gc.gridy = 1;
            addCell(table, gc, 0, wx[0], bestTitleLabel, SwingConstants.LEFT);
            addCell(table, gc, 1, wx[1], bestScoreLabel, SwingConstants.RIGHT);
            addCell(table, gc, 2, wx[2], bestKillLabel,  SwingConstants.RIGHT);
            addCell(table, gc, 3, wx[3], bestTimeLabel,  SwingConstants.RIGHT);

            table.setMaximumSize(new Dimension(900, 120));
            table.setAlignmentX(Component.CENTER_ALIGNMENT);

            inner.add(table);
            inner.add(Box.createVerticalStrut(24));

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
            buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            ChoiceButton contBtn = new ChoiceButton("Continue");
            ChoiceButton quitBtn = new ChoiceButton("Quit");

            Dimension btnSize = new Dimension(420, 60);
            contBtn.setPreferredSize(btnSize);
            quitBtn.setPreferredSize(btnSize);
            contBtn.setMaximumSize(btnSize);
            quitBtn.setMaximumSize(btnSize);
            contBtn.setMinimumSize(btnSize);
            quitBtn.setMinimumSize(btnSize);

            contBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            quitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

            contBtn.addActionListener(e -> restartRun());
            quitBtn.addActionListener(e -> returnToMainMenu());

            buttonsPanel.add(contBtn);
            buttonsPanel.add(Box.createVerticalStrut(16));
            buttonsPanel.add(quitBtn);

            inner.add(buttonsPanel);
            add(inner);

            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { centerInner(); }
            });

            blinkTimer = new javax.swing.Timer(600, e -> {
                blinkOn = !blinkOn;
                setBestTitleNew(showNew && blinkOn);
                repaint();
            });

            SwingUtilities.invokeLater(this::centerInner);
        }

        private void setBestTitleNew(boolean on) {
            if (!showNew) {
                bestTitleLabel.setText("최고 기록");
                bestTitleLabel.setForeground(new Color(255, 210, 120));
                return;
            }
            if (!on) {
                bestTitleLabel.setText("최고 기록");
                bestTitleLabel.setForeground(new Color(255, 210, 120));
                return;
            }
            bestTitleLabel.setText("<html>최고 기록&nbsp;<span style='color:#FFE678;font-size:16px;vertical-align:super;'>NEW!</span></html>");
            bestTitleLabel.setForeground(new Color(255, 210, 120));
        }

        public void updateRecords(int rk, int rt, int rs, int bk, int bt, int bs, boolean isNewBest) {
            String runTime  = String.format("%02d:%02d", rt / 60, rt % 60);
            String bestTime = String.format("%02d:%02d", bt / 60, bt % 60);

            runScoreLabel.setText("Score: " + rs);
            runKillLabel.setText("Kill: " + rk);
            runTimeLabel.setText("Time: " + runTime);

            bestScoreLabel.setText("Score: " + bs);
            bestKillLabel.setText("Kill: " + bk);
            bestTimeLabel.setText("Time: " + bestTime);

            showNew = isNewBest;

            if (isNewBest) {
                blinkOn = true;
                setBestTitleNew(true);
                if (!blinkTimer.isRunning()) blinkTimer.start();
            } else {
                if (blinkTimer.isRunning()) blinkTimer.stop();
                setBestTitleNew(false);
            }

            revalidate();
            repaint();
            centerInner();
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            g2.setColor(new Color(0, 0, 0, 230));
            g2.fillRect(0, 0, w, h);

            Rectangle r = inner.getBounds();
            int padding = 26;
            int dialogX = r.x - padding;
            int dialogY = r.y - padding;
            int dialogW = r.width  + padding * 2;
            int dialogH = r.height + padding * 2;

            int arc = 34;
            GradientPaint gp = new GradientPaint(
                    dialogX, dialogY,
                    new Color(25, 25, 25, 240),
                    dialogX, dialogY + dialogH,
                    new Color(5, 5, 5, 240)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.setColor(new Color(200, 80, 40, 220));
            g2.setStroke(new BasicStroke(2.8f));
            g2.drawRoundRect(dialogX, dialogY, dialogW, dialogH, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }

        private JLabel makeRowTitle(String text, Color c) {
            JLabel lb = new JLabel(text);
            lb.setFont(new Font("맑은 고딕", Font.BOLD, 20));
            lb.setForeground(c);
            return lb;
        }

        private JLabel makeCellLabel(Color c) {
            JLabel lb = new JLabel("");
            lb.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            lb.setForeground(c);
            return lb;
        }

        private void addCell(JPanel table, GridBagConstraints gc, int x, double wx,
                             JComponent comp, int align) {
            gc.gridx = x;
            gc.weightx = wx;

            if (comp instanceof JLabel) {
                ((JLabel) comp).setHorizontalAlignment(align);
            }

            table.add(comp, gc);
        }
    }

    private int calcScore(int kills, int timeSec) {
        return kills * 100 + timeSec * 5;
    }

    private void loadBestRecord() {
        bestScore = prefs.getInt("bestScore", 0);
        bestKills = prefs.getInt("bestKills", 0);
        bestTimeSec = prefs.getInt("bestTimeSec", 0);
    }
    
    public void saveRun() {
        // 무기 선택 중이거나 게임오버면 저장하지 않음
        if (waitingWeaponSelect) return;
        if (gameState == GameState.GAMEOVER) return;

        SaveState st = new SaveState();
        st.valid = true;

        st.killCount = this.killCount;
        st.elapsedPlaySec = getElapsedPlaySec();

        st.eliteTimer = this.eliteTimer;
        st.nonBossKillCount = this.nonBossKillCount;
        st.bossAlive = this.bossAlive;
        st.bossKillThreshold = this.bossKillThreshold;
        st.spawnTimer = this.spawnTimer;

        st.player = player.exportState();

        SaveManager.save(st);
    }

    public boolean loadFromSave(SaveState st) {
        if (st == null || !st.valid) return false;

        // 일단 새 플레이어 만들어둔 상태(GamePanel 생성자에서 이미 생성됨)에서 값만 덮어쓰기
        this.player.importState(st.player);
        this.player.updateScreenCenter();

        this.killCount = st.killCount;

        // 시간 복원
        this.playAccumNano = (long) st.elapsedPlaySec * 1_000_000_000L;
        this.playResumeNano = 0L;

        this.eliteTimer = st.eliteTimer;
        this.nonBossKillCount = st.nonBossKillCount;
        this.bossAlive = st.bossAlive;
        this.bossKillThreshold = st.bossKillThreshold;
        this.spawnTimer = st.spawnTimer;

        // 이어하기는 이미 진행 중인 판이므로 무기 선택 스킵
        this.waitingWeaponSelect = false;
        this.weaponSelectPanel.setVisible(false);

        // 투사체/이펙트는 로드시 초기화(안전)
        this.arrows.clear();
        this.fireballs.clear();
        this.enemyProjectiles.clear();
        this.damageTexts.clear();

        // 몬스터/구슬은 “간단 이어하기 버전”으로 초기화(원하면 다음 단계에서 몬스터까지 저장 가능)
        this.monsters.clear();
        this.expOrbs.clear();

        this.paused = false;
        this.gameState = GameState.RUNNING;

        // UI 표시값도 맞춰주기
        this.uiHpDisplay = player.getCurrentHp();
        double expTarget = 0.0;
        if (player.getExpToNextLevel() > 0) {
            expTarget = (double) player.getCurrentExp() / player.getExpToNextLevel();
        }
        this.uiExpDisplay = expTarget;

        return true;
    }

    private void saveBestRecord() {
        prefs.putInt("bestScore", bestScore);
        prefs.putInt("bestKills", bestKills);
        prefs.putInt("bestTimeSec", bestTimeSec);
    }

    private void triggerGameOver() {
    	SaveManager.clearSave();
        paused = true;
        gameState = GameState.GAMEOVER;
        showStatusPanel = false;

        onLeaveRunning();

        runKills = killCount;
        runTimeSec = getElapsedPlaySec();
        runScore = calcScore(runKills, runTimeSec);

        boolean isNewBest = false;

        if (runScore > bestScore) {
            bestScore = runScore;
            bestKills = runKills;
            bestTimeSec = runTimeSec;
            saveBestRecord();
            isNewBest = true;
        }

        if (gameOverPanel != null) {
            gameOverPanel.updateRecords(runKills, runTimeSec, runScore,
                    bestKills, bestTimeSec, bestScore, isNewBest);

            gameOverPanel.setBounds(0, 0, getWidth(), getHeight());
            gameOverPanel.setVisible(true);

            gameOverPanel.revalidate();
            gameOverPanel.repaint();
        }
    }

    private void restartRun() {
        monsters.clear();
        expOrbs.clear();
        arrows.clear();
        fireballs.clear();
        enemyProjectiles.clear();
        damageTexts.clear();

        eliteTimer = 0;
        nonBossKillCount = 0;
        bossAlive = false;
        bossKillThreshold = 50;
        spawnTimer = 0;

        player = new Player(this, keyH, null);
        player.updateScreenCenter();

        killCount = 0;
        waitingWeaponSelect = true;
        showStatusPanel = false;

        resetPlayTime();

        uiHpDisplay = player.getCurrentHp();
        uiExpDisplay = 0.0;

        if (gameOverPanel != null) gameOverPanel.setVisible(false);

        weaponSelectPanel.setVisible(true);
        weaponSelectPanel.setBounds(0, 0, getWidth(), getHeight());

        paused = false;
        gameState = GameState.RUNNING;

        requestFocusInWindow();
    }

    public int getScreenWidth()  { return SCREEN_WIDTH; }
    public int getScreenHeight() { return SCREEN_HEIGHT; }
}


