package game.entity.monster;

import java.awt.*;
import game.entity.player.Player;

public class Monster {

    public enum MonsterKind {
        NORMAL,
        DASHER,
        SHOOTER,
        SPLITTER,
        SPLIT_CHILD,
        ELITE,
        BOSS
    }

    private MonsterKind kind;
    private int difficultyStage;

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

    // ----------------------------------------------------
    // 생성자
    // difficultyStage: 0(0~29초), 1(30~59초) ...
    // ----------------------------------------------------
    public Monster(int x, int y, Image image, MonsterKind kind, int difficultyStage) {
        this.worldX = x;
        this.worldY = y;
        this.image  = image;
        this.kind   = kind;
        this.difficultyStage = difficultyStage;

        // 기본 스케일 (시간에 따라 증가)
        int hpBase = 50;
        int spdBase = 1;
        int dmgBase = 10;

        // 스테이지가 올라갈수록 조금씩 강해지게
        double hpMul  = 1.0 + difficultyStage * 0.25;  // 30초마다 25% 증가
        int spdBonus  = difficultyStage / 2;           // 60초마다 +1
        int dmgBonus  = difficultyStage * 1;           // 30초마다 +1

        // 타입별 보정
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
                this.speed = (spdBase + spdBonus); // 평소는 비슷, 돌진 때만 확 빨라짐
                this.damage = dmgBase + dmgBonus + 2;
                this.dashCooldown = 60; // 시작 쿨
                break;

            case SHOOTER:
                this.width = 45; this.height = 45;
                this.maxHp = (int)(hpBase * 0.8 * hpMul);
                this.speed = Math.max(1, spdBase + spdBonus - 1); // 조금 느리게
                this.damage = dmgBase + dmgBonus; // 투사체 데미지
                this.shootCooldown = 90; // 시작 쿨
                break;

            case SPLITTER:
                this.width = 45; this.height = 45;
                this.maxHp = (int)(hpBase * 0.7 * hpMul);
                this.speed = spdBase + spdBonus;
                this.damage = dmgBase + dmgBonus;
                break;
                
            case SPLIT_CHILD:
                this.width = 28; this.height = 28;                 // 더 작게
                this.maxHp = (int)(hpBase * 0.35 * hpMul);         // 낮은 HP
                this.speed = (spdBase + spdBonus) + 2;             // 더 빠르게
                this.damage = Math.max(6, dmgBase + dmgBonus - 2); // 원하면 조금 약하게
                break;

            case ELITE:
                this.width = 65; this.height = 65;
                this.maxHp = (int)(hpBase * 2.2 * hpMul);
                this.speed = spdBase + spdBonus;
                this.damage = dmgBase + dmgBonus + 5;
                break;

            case BOSS:
                this.width = 85; this.height = 90;
                this.maxHp = (int)(hpBase * 12.0 * hpMul);
                this.speed = Math.max(1, spdBase + spdBonus - 1);
                this.damage = dmgBase + dmgBonus + 10;
                this.shootCooldown = 45; // 보스는 더 자주 쏨(탄막 느낌)
                break;
        }

        this.currentHp = this.maxHp;
        setupHitboxByKind();
    }
    
    private void setupHitboxByKind() {
        // 기본값: 이미지 대비 살짝 줄인 사각형
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

            case ELITE:
                hitboxOffsetX = (int)(width * 0.20);
                hitboxOffsetY = (int)(height * 0.18);
                hitboxW = (int)(width * 0.60);
                hitboxH = (int)(height * 0.70);
                break;

            case BOSS:
                hitboxOffsetX = (int)(width * 0.18);
                hitboxOffsetY = (int)(height * 0.20);
                hitboxW = (int)(width * 0.64);
                hitboxH = (int)(height * 0.70);
                break;

            case NORMAL:
            default:
                break;
        }

        // 최소 크기 보정(너무 작아지는 것 방지)
        if (hitboxW < 8) hitboxW = 8;
        if (hitboxH < 8) hitboxH = 8;
    }

    public MonsterKind getKind() { return kind; }
    public boolean isBoss() { return kind == MonsterKind.BOSS; }
    public boolean isElite() { return kind == MonsterKind.ELITE; }

    // ----------------------------------------------------
    // AI 업데이트
    //  - GamePanel에서 shooterFire(...)를 호출할 수 있게 콜백 전달
    // ----------------------------------------------------
    public interface ShooterCallback {
        void fireEnemyProjectile(double startX, double startY, double dirX, double dirY, int damage, double speed);
    }

    public void update(int playerWorldX, int playerWorldY, ShooterCallback shooterCb) {
        if (!isAlive()) return;

        int dx = Integer.compare(playerWorldX, worldX);
        int dy = Integer.compare(playerWorldY, worldY);

        switch (kind) {
            case NORMAL:
            case ELITE:
            case SPLITTER:
                worldX += dx * speed;
                worldY += dy * speed;
                break;
            case SPLIT_CHILD:  
                worldX += dx * speed;
                worldY += dy * speed;
                break;

            case DASHER:
                // 돌진 중이면 그 방향으로 빠르게
                if (dashTimer > 0) {
                    worldX += dashDirX * (speed + 6);
                    worldY += dashDirY * (speed + 6);
                    dashTimer--;
                    return;
                }

                // 일반 추적
                worldX += dx * speed;
                worldY += dy * speed;

                // 쿨 감소 -> 가까우면 돌진 시작
                if (dashCooldown > 0) dashCooldown--;
                int manhattan = Math.abs(playerWorldX - worldX) + Math.abs(playerWorldY - worldY);
                if (dashCooldown <= 0 && manhattan < 260) {
                    dashDirX = dx;
                    dashDirY = dy;
                    if (dashDirX == 0 && dashDirY == 0) {
                        dashDirX = (Math.random() < 0.5) ? -1 : 1;
                    }
                    dashTimer = 12;      // 12프레임 돌진
                    dashCooldown = 90;   // 다음 돌진까지
                }
                break;

            case SHOOTER:
            case BOSS:
                // 약간 접근
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

                    double projSpeed = (kind == MonsterKind.BOSS) ? 6.0 : 5.0;
                    shooterCb.fireEnemyProjectile(sx, sy, vx, vy, damage, projSpeed);

                    int baseCooldown = (kind == MonsterKind.BOSS) ? 100 : 300;
                    shootCooldown = Math.max(20, baseCooldown - difficultyStage * 5);
                }
                break;
        }
    }

    // ----------------------------------------------------
    // 그리기 (플레이어 기준 카메라 변환 + HP 바)
    // ----------------------------------------------------
    public void draw(Graphics g, Player player) {
        if (!isAlive()) return;

        int screenX = worldX - player.worldX + player.screenX;
        int screenY = worldY - player.worldY + player.screenY;

        g.drawImage(image, screenX, screenY, width, height, null);

        // HP 바
        int barWidth  = width;
        int barHeight = 4;
        int barX = screenX;
        int barY = screenY - 6;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);

        double ratio = (double) currentHp / maxHp;
        int hpFill = (int) (barWidth * ratio);

        // 보스/엘리트는 색을 다르게
        if (kind == MonsterKind.BOSS) g.setColor(new Color(255, 80, 80));
        else if (kind == MonsterKind.ELITE) g.setColor(new Color(255, 200, 80));
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
    
 // ====== 저장용 ======
    public game.save.SaveState.MonsterState exportState() {
        game.save.SaveState.MonsterState ms = new game.save.SaveState.MonsterState();

        ms.worldX = this.worldX;
        ms.worldY = this.worldY;
        ms.width = this.width;
        ms.height = this.height;

        ms.kind = this.kind.name();
        ms.difficultyStage = this.difficultyStage;

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

    // ====== 로드용(팩토리) ======
    public static Monster fromState(game.save.SaveState.MonsterState ms, Image img) {
        if (ms == null) return null;

        MonsterKind k = MonsterKind.valueOf(ms.kind);

        Monster m = new Monster(ms.worldX, ms.worldY, img, k, ms.difficultyStage);

        // 생성자에서 기본값 세팅하므로, 저장된 값으로 덮어쓰기
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

        // 히트박스는 사이즈 기반이라 다시 세팅
        m.setupHitboxByKind();

        return m;
    }
}
