package game.entity.player;

import java.awt.*;
import javax.swing.ImageIcon;

import game.combat.SwordWeapon;
import game.combat.BowWeapon;
import game.combat.StaffWeapon;
import game.combat.Weapon;
import game.combat.WeaponType;
import game.main.GamePanel;
import game.main.KeyHandler;

public class Player {

    public GamePanel gp;
    public KeyHandler keyH;

    public int worldX, worldY;
    public int speed;

    public int screenX;
    public int screenY;

    public int width = 40, height = 40;

    private int maxHp = 100;
    private int currentHp = 100;

    private boolean isInvincible = false;
    private int invincibleCounter = 0;
    private final int INVINCIBLE_TIME = 60;

    private Image image;

    // 현재 장착 무기
    private Weapon weapon;

    public Player(GamePanel gp, KeyHandler keyH, WeaponType weaponType) {
        this.gp = gp;
        this.keyH = keyH;
        
        updateScreenCenter();

        worldX = 0;
        worldY = 0;
        speed = 4;

        loadImage();

        // 기본 무기 : Sword  (무기 타입에 따라 결정)
        setInitialWeapon(weaponType);
    }
    
 // 화면 크기에 맞춰 캐릭터를 가운데로 위치시키는 메서드
    public void updateScreenCenter() {
        screenX = (gp.getWidth()  / 2) - (width  / 2);
        screenY = (gp.getHeight() / 2) - (height / 2);
    }
    
    private void setInitialWeapon(WeaponType type) {
        if (type == null) {
            weapon = new SwordWeapon();
            return;
        }

        switch (type) {
            case BOW:
                weapon = new BowWeapon();
                break;
            case STAFF:
                weapon = new StaffWeapon();
                break;
            case SWORD:
            default:
                weapon = new SwordWeapon();
        }
    }

    private void loadImage() {
        image = new ImageIcon(getClass().getResource("/images/character_1.png")).getImage();
    }

    public void update() {
        // 창 크기 바뀔 때에도 항상 중앙
    	updateScreenCenter();

        if (keyH.upPressed) worldY -= speed;
        if (keyH.downPressed) worldY += speed;
        if (keyH.leftPressed) worldX -= speed;
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

    public void takeDamage(int damage) {
        if (!isInvincible) {
            currentHp -= damage;
            isInvincible = true;
            if (currentHp <= 0) currentHp = 0;
        }
    }

    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    
 // === GamePanel 접근용 Getter (몬스터가 화면 크기 알 때 사용) ===
    public GamePanel getGamePanel() {
        return gp;
    }

    // 무기 관련
    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        if (weapon != null) {
            this.weapon = weapon;
        }
    }
}
