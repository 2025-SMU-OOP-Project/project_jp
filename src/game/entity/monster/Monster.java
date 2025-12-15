package game.entity.monster;

import java.awt.*;
import game.entity.player.Player;
import game.save.SaveState;

public class Monster {

    // ✅ 이제 kind는 “행동 타입”만
    public enum MonsterKind {
        NORMAL,
        DASHER,
        SHOOTER,
        SPLITTER,
        SPLIT_CHILD
    }

    private MonsterKind kind;
    private int difficultyStage;

    // ✅ 등급 플래그
    private boolean elite;
    private boolean boss;

    // ===== 위치 / 이동 =====
    public int worldX, worldY;
    public int speed = 1;
    public int width = 30, height = 30;

    // ===== 그래픽 =====
    public Image image;

    // ===== 체력 =====
    private int maxHp = 50;
    private int currentHp = maxHp;

    // ===== 데미지 =====
    private int damage = 10;

    private Rectangle hitbox = new Rectangle();
    private int hitboxOffsetX, hitboxOffsetY, hitboxW, hitboxH;

    // ===== DASHER =====
    private int dashCooldown = 0;
    private int dashTimer = 0;
    private int dashDirX = 0, dashDirY = 0;

    // ===== SHOOTER =====
    private int shootCooldown = 0;

    public Monster(int x, int y, Image image, MonsterKind kind, int difficultyStage,
                   boolean elite, boolean boss) {
        this.worldX = x;
        this.worldY = y;
        this.image  = image;
        this.kind   = kind;
        this.difficultyStage = difficultyStage;

        this.elite = elite;
        this.boss  = boss;

        int hpBase = 50;
        int spdBase = 1;
        int dmgBase = 10;

        double hpMul  = 1.0 + difficultyStage * 0.25;
        int spdBonus  = difficultyStage / 2;
        int dmgBonus  = difficultyStage * 1;

        // ===== 행동 타입(kind)별 기본 스탯 =====
        switch (kind) {
            case NORMAL:
                this.width = 45; this.height = 45;
                this.maxHp = (int)(hpBase * hpMul);
                this.speed = spdBase + spdBonus;
                this.damage = dmgBase + dmgBonus;
                break;

            case DASHER:
                this.width = 45; this.height = 45;
                this.maxHp = (int)(hpBase * 0.9 * hpMul);
                this.speed = (spdBase + spdBonus);
                this.damage = dmgBase + dmgBonus + 2;
                this.dashCooldown = 60;
                break;

            case SHOOTER:
                this.width = 45; this.height = 45;
                this.maxHp = (int)(hpBase * 0.8 * hpMul);
                this.speed = Math.max(1, spdBase + spdBonus - 1);
                this.damage = dmgBase + dmgBonus;
                this.shootCooldown = 90;
                break;

            case SPLITTER:
                this.width = 45; this.height = 45;
                this.maxHp = (int)(hpBase * 0.7 * hpMul);
                this.speed = spdBase + spdBonus;
                this.damage = dmgBase + dmgBonus;
                break;

            case SPLIT_CHILD:
                this.width = 28; this.height = 28;
                this.maxHp = (int)(hpBase * 0.35 * hpMul);
                this.speed = (spdBase + spdBonus) + 2;
                this.damage = Math.max(6, dmgBase + dmgBonus - 2);
                break;
        }

        // ===== 등급 보정 =====
        if (elite) {
            this.width  = (int)(this.width  * 1.5);
            this.height = (int)(this.height * 1.5);
            this.maxHp  = (int)(this.maxHp  * 3.0);
            this.damage += 5;
        }

        if (boss) {
            this.width  = (int)(this.width  * 2.0);
            this.height = (int)(this.height * 2.0);
            this.maxHp  = (int)(this.maxHp  * 15.0);
            this.damage += 10;

            // 보스가 슈터면 더 자주 발사
            if (kind == MonsterKind.SHOOTER) {
                this.shootCooldown = 45;
            }
        }

        this.currentHp = this.maxHp;
        setupHitboxByKind();
    }

    private void setupHitboxByKind() {
        hitboxOffsetX = (int)(width * 0.25);
        hitboxOffsetY = (int)(height * 0.20);
        hitboxW = (int)(width * 0.50);
        hitboxH = (int)(height * 0.65);

        switch (kind) {
            case SPLITTER:
            case SPLIT_CHILD:
                hitboxOffsetX = (int)(width * 0.18);
                hitboxOffsetY = (int)(height * 0.28);
                hitboxW = (int)(width * 0.64);
                hitboxH = (int)(height * 0.55);
                break;

            case DASHER:
                hitboxOffsetX = (int)(width * 0.22);
                hitboxOffsetY = (int)(height * 0.30);
                hitboxW = (int)(width * 0.56);
                hitboxH = (int)(height * 0.55);
                break;

            case SHOOTER:
                hitboxOffsetX = (int)(width * 0.24);
                hitboxOffsetY = (int)(height * 0.22);
                hitboxW = (int)(width * 0.52);
                hitboxH = (int)(height * 0.60);
                break;

            default:
                break;
        }

        if (hitboxW < 8) hitboxW = 8;
        if (hitboxH < 8) hitboxH = 8;
    }

    public MonsterKind getKind() { return kind; }
    public boolean isBoss() { return boss; }
    public boolean isElite() { return elite; }

    public interface ShooterCallback {
        void fireEnemyProjectile(double startX, double startY, double dirX, double dirY, int damage, double speed);
    }

    public void update(int playerWorldX, int playerWorldY, ShooterCallback shooterCb) {
        if (!isAlive()) return;

        int dx = Integer.compare(playerWorldX, worldX);
        int dy = Integer.compare(playerWorldY, worldY);

        switch (kind) {
            case NORMAL:
            case SPLITTER:
            case SPLIT_CHILD:
                worldX += dx * speed;
                worldY += dy * speed;
                break;

            case DASHER:
                if (dashTimer > 0) {
                    worldX += dashDirX * (speed + 6);
                    worldY += dashDirY * (speed + 6);
                    dashTimer--;
                    return;
                }

                worldX += dx * speed;
                worldY += dy * speed;

                if (dashCooldown > 0) dashCooldown--;
                int manhattan = Math.abs(playerWorldX - worldX) + Math.abs(playerWorldY - worldY);
                if (dashCooldown <= 0 && manhattan < 260) {
                    dashDirX = dx;
                    dashDirY = dy;
                    if (dashDirX == 0 && dashDirY == 0) {
                        dashDirX = (Math.random() < 0.5) ? -1 : 1;
                    }
                    dashTimer = 12;
                    dashCooldown = 90;
                }
                break;

            case SHOOTER:
                worldX += dx * speed;
                worldY += dy * speed;

                if (shootCooldown > 0) shootCooldown--;
                if (shootCooldown <= 0 && shooterCb != null) {
                    double sx = worldX + width / 2.0;
                    double sy = worldY + height / 2.0;

                    double vx = (playerWorldX + 15) - sx;
                    double vy = (playerWorldY + 15) - sy;
                    double len = Math.sqrt(vx * vx + vy * vy);
                    if (len < 0.0001) len = 1;
                    vx /= len;
                    vy /= len;

                    double projSpeed = boss ? 6.0 : 5.0;
                    shooterCb.fireEnemyProjectile(sx, sy, vx, vy, damage, projSpeed);

                    int baseCooldown = boss ? 100 : 300;
                    shootCooldown = Math.max(20, baseCooldown - difficultyStage * 5);
                }
                break;
        }
    }

    public void draw(Graphics g, Player player) {
        if (!isAlive()) return;

        int screenX = worldX - player.worldX + player.screenX;
        int screenY = worldY - player.worldY + player.screenY;

        g.drawImage(image, screenX, screenY, width, height, null);

        int barWidth  = width;
        int barHeight = 4;
        int barX = screenX;
        int barY = screenY - 6;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);

        double ratio = (double) currentHp / maxHp;
        int hpFill = (int) (barWidth * ratio);

        if (boss) g.setColor(new Color(255, 80, 80));
        else if (elite) g.setColor(new Color(255, 200, 80));
        else g.setColor(new Color(0, 220, 0));

        g.fillRect(barX, barY, hpFill, barHeight);

        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }

    public Rectangle getBounds() {
        hitbox.setBounds(worldX + hitboxOffsetX, worldY + hitboxOffsetY, hitboxW, hitboxH);
        return hitbox;
    }

    public int getDamage() { return damage; }

    public void takeDamage(int damage) {
        if (!isAlive()) return;
        currentHp -= damage;
        if (currentHp < 0) currentHp = 0;
    }

    public int getHitCenterX() {
        return worldX + hitboxOffsetX + hitboxW / 2;
    }

    public int getHitCenterY() {
        return worldY + hitboxOffsetY + hitboxH / 2;
    }

    public boolean isDead()  { return currentHp <= 0; }
    public boolean isAlive() { return currentHp > 0; }

    public int getCurrentHp() { return currentHp; }
    public int getMaxHp()     { return maxHp; }

    // ===== 저장 =====
    public SaveState.MonsterState exportState() {
        SaveState.MonsterState ms = new SaveState.MonsterState();

        ms.worldX = this.worldX;
        ms.worldY = this.worldY;
        ms.width = this.width;
        ms.height = this.height;

        ms.kind = this.kind.name();
        ms.difficultyStage = this.difficultyStage;

        ms.elite = this.elite;
        ms.boss  = this.boss;

        ms.currentHp = this.currentHp;
        ms.maxHp = this.maxHp;

        ms.damage = this.damage;
        ms.speed = this.speed;

        ms.dashCooldown = this.dashCooldown;
        ms.dashTimer = this.dashTimer;
        ms.dashDirX = this.dashDirX;
        ms.dashDirY = this.dashDirY;

        ms.shootCooldown = this.shootCooldown;

        return ms;
    }

    // ===== 로드 =====
    public static Monster fromState(SaveState.MonsterState ms, Image img) {
        if (ms == null) return null;

        // 구버전 호환: 예전 세이브에 ELITE/BOSS가 들어있던 경우를 방어
        boolean elite = ms.elite;
        boolean boss  = ms.boss;

        MonsterKind k;
        if ("ELITE".equals(ms.kind)) {
            k = MonsterKind.NORMAL;
            elite = true;
        } else if ("BOSS".equals(ms.kind)) {
            k = MonsterKind.SHOOTER; // 예전 보스는 슈터 취급(원하면 NORMAL로)
            boss = true;
        } else {
            k = MonsterKind.valueOf(ms.kind);
        }

        Monster m = new Monster(ms.worldX, ms.worldY, img, k, ms.difficultyStage, elite, boss);

        m.width = ms.width;
        m.height = ms.height;

        m.maxHp = ms.maxHp;
        m.currentHp = Math.min(ms.currentHp, m.maxHp);

        m.damage = ms.damage;
        m.speed = ms.speed;

        m.dashCooldown = ms.dashCooldown;
        m.dashTimer = ms.dashTimer;
        m.dashDirX = ms.dashDirX;
        m.dashDirY = ms.dashDirY;

        m.shootCooldown = ms.shootCooldown;

        m.setupHitboxByKind();
        return m;
    }
}
