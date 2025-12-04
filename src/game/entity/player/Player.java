package game.entity.player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import game.combat.BowWeapon;
import game.combat.StaffWeapon;
import game.combat.SwordWeapon;
import game.combat.Weapon;
import game.combat.WeaponType;
import game.main.GamePanel;
import game.main.KeyHandler;

public class Player {

    // ====== 위치 / 그래픽 ======
    public GamePanel gp;
    public KeyHandler keyH;

    public int worldX, worldY;
    public int screenX, screenY;
    public int width = 40, height = 40;

    private final int baseSpeed = 4;
    public int speed = baseSpeed;

    private Image image;

    // ====== 체력 ======
    private final int baseMaxHp = 100;
    private int maxHp = baseMaxHp;
    private int currentHp = baseMaxHp;

    private boolean isInvincible = false;
    private int invincibleCounter = 0;
    private final int INVINCIBLE_TIME = 60;

    // ====== 레벨 / 경험치 ======
    private int level = 1;
    private final int maxLevel = 20;
    private int currentExp = 0;
    private int expToNextLevel = 50;

    // ====== 패시브 스택 ======
    private int attackLevel = 0; // 공격력 증가
    private int speedLevel  = 0; // 이동속도 증가
    private int maxHpLevel  = 0; // 최대 체력 증가

    // ====== 보유 무기(액티브) ======
    public static class OwnedWeapon {
        public WeaponType type;
        public Weapon weapon;
        public int level = 1;          // 1~3
        public int cooldownCounter = 0;
    }

    private List<OwnedWeapon> ownedWeapons = new ArrayList<>();

    // ----------------------------------------------------
    // 생성자
    // ----------------------------------------------------
    public Player(GamePanel gp, KeyHandler keyH, WeaponType initialWeaponType) {
        this.gp = gp;
        this.keyH = keyH;

        worldX = 0;
        worldY = 0;

        updateScreenCenter();
        recalcStats();

        loadImage();

        // 시작 무기(선택 화면에서 null로 만들면 나중에 추가)
        if (initialWeaponType != null) {
            addOrUpgradeWeapon(initialWeaponType);
        }
    }

    private void loadImage() {
        image = new ImageIcon(
                getClass().getResource("/images/character_1.png")
        ).getImage();
    }

    // 화면 정가운데에 위치
    public void updateScreenCenter() {
        screenX = (gp.getWidth() / 2) - (width / 2);
        screenY = (gp.getHeight() / 2) - (height / 2);
    }

    // ----------------------------------------------------
    // 매 프레임 업데이트
    // ----------------------------------------------------
    public void update() {
        updateScreenCenter();  // 창 크기 바뀌어도 항상 가운데

        if (keyH.upPressed)    worldY -= speed;
        if (keyH.downPressed)  worldY += speed;
        if (keyH.leftPressed)  worldX -= speed;
        if (keyH.rightPressed) worldX += speed;

        if (isInvincible) {
            invincibleCounter++;
            if (invincibleCounter > INVINCIBLE_TIME) {
                isInvincible = false;
                invincibleCounter = 0;
            }
        }
    }

    public void draw(Graphics g) {
        g.drawImage(image, screenX, screenY, width, height, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(worldX, worldY, width, height);
    }

    // ----------------------------------------------------
    // 체력 / 데미지
    // ----------------------------------------------------
    public void takeDamage(int damage) {
        if (!isInvincible) {
            currentHp -= damage;
            if (currentHp < 0) currentHp = 0;
            isInvincible = true;
        }
    }

    public int getCurrentHp() { return currentHp; }
    public int getMaxHp()     { return maxHp;     }

    // ----------------------------------------------------
    // 레벨 / 경험치
    // ----------------------------------------------------
    private int calcExpToNextLevel() {
        // 레벨이 오를수록 조금씩 증가
        return 50 + (level - 1) * 15;
    }

    /** 경험치를 얻고, 레벨업이 일어나면 true 리턴 */
    public boolean gainExp(int amount) {
        if (level >= maxLevel) return false;

        currentExp += amount;
        boolean leveledUp = false;

        while (currentExp >= expToNextLevel && level < maxLevel) {
            currentExp -= expToNextLevel;
            level++;
            expToNextLevel = calcExpToNextLevel();
            leveledUp = true;
        }
        return leveledUp;
    }

    public int getLevel()           { return level; }
    public int getMaxLevel()        { return maxLevel; }
    public int getCurrentExp()      { return currentExp; }
    public int getExpToNextLevel()  { return expToNextLevel; }

    // ----------------------------------------------------
    // 패시브(공/이속/체력) 업그레이드
    // ----------------------------------------------------
    public void upgradeAttack() {
        attackLevel++;
    }

    public void upgradeSpeed() {
        speedLevel++;
        recalcStats();
    }

    public void upgradeMaxHp() {
    	int oldMax = maxHp;     // 기존 최대 체력 저장
        maxHpLevel++;
        recalcStats();
        
        int increase = maxHp - oldMax;   // 증가한 체력량 계산
        // 증가한 만큼 체력 회복 (최대 체력 넘지 않도록)
        currentHp = Math.min(currentHp + increase, maxHp);
    }

    private void recalcStats() {
        speed = baseSpeed + speedLevel;
        maxHp = baseMaxHp + maxHpLevel * 20;
        if (currentHp > maxHp) currentHp = maxHp;
    }

    public double getAttackMultiplier() {
        return 1.0 + attackLevel * 0.2; // 1스택당 +20%
    }

    public int getAttackLevel() { return attackLevel; }
    public int getSpeedLevel()  { return speedLevel; }
    public int getMaxHpLevel()  { return maxHpLevel; }

    // ----------------------------------------------------
    // 무기(액티브) 관리
    // ----------------------------------------------------
    public List<OwnedWeapon> getOwnedWeapons() {
        return ownedWeapons;
    }

    public boolean hasWeapon(WeaponType type) {
        for (OwnedWeapon ow : ownedWeapons) {
            if (ow.type == type) return true;
        }
        return false;
    }

    public boolean canUpgradeWeapon(WeaponType type) {
        for (OwnedWeapon ow : ownedWeapons) {
            if (ow.type == type) {
                return ow.level < 3; // 최대 3단계
            }
        }
        // 아직 없으면 얻을 수 있음
        return true;
    }

    public int getWeaponUpgradeLevel(WeaponType type) {
        for (OwnedWeapon ow : ownedWeapons) {
            if (ow.type == type) return ow.level;
        }
        return 0;
    }

    /** 새 무기를 얻거나, 이미 있다면 강화(최대 3단계) */
    public void addOrUpgradeWeapon(WeaponType type) {
        for (OwnedWeapon ow : ownedWeapons) {
            if (ow.type == type) {
                if (ow.level < 3) {
                    ow.level++;
                }
                return;
            }
        }

        // 여기까지 왔으면 새 무기 획득
        OwnedWeapon newW = new OwnedWeapon();
        newW.type = type;
        newW.level = 1;
        newW.cooldownCounter = 0;

        switch (type) {
            case SWORD:
                newW.weapon = new SwordWeapon();
                break;
            case BOW:
                newW.weapon = new BowWeapon();
                break;
            case STAFF:
                newW.weapon = new StaffWeapon();
                break;
        }
        ownedWeapons.add(newW);
    }

    // 기존 Weapon getter는 첫 번째 무기를 반환 (호환용)
    public Weapon getWeapon() {
        if (ownedWeapons.isEmpty()) return null;
        return ownedWeapons.get(0).weapon;
    }

    // UI 표시용: 가지고 있는 무기 리스트 문자열
    public String getWeaponStatusString() {
        if (ownedWeapons.isEmpty()) return "None";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ownedWeapons.size(); i++) {
            OwnedWeapon ow = ownedWeapons.get(i);
            if (i > 0) sb.append(", ");
            String name = ow.type.name().substring(0,1).toUpperCase()
                        + ow.type.name().substring(1).toLowerCase();
            sb.append(name).append(" Lv").append(ow.level);
        }
        return sb.toString();
    }

    // ----------------------------------------------------
    // 기타
    // ----------------------------------------------------
    public GamePanel getGamePanel() {
        return gp;
    }
}
