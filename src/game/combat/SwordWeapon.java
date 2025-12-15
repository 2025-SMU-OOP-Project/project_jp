package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class SwordWeapon implements Weapon {

    private final int baseDamage = 20;
    private final int baseRange  = 60;

    @Override
    public int getDamage() {
        return baseDamage;
    }

    @Override
    public int getCooldownFrames(Player player) {
        int level = player.getWeaponUpgradeLevel(WeaponType.SWORD);
        if (level <= 0) level = 1;

        // Lv1:60, Lv2:45, Lv3:32, Lv4:24, Lv5:18
        switch (level) {
            case 1: return 60;
            case 2: return 45;
            case 3: return 32;
            case 4: return 24;
            case 5: return 18;
        }
        return 60;
    }

    private int effectTimer = 0;
    private final int EFFECT_DURATION = 8;

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {

        int level = player.getWeaponUpgradeLevel(WeaponType.SWORD);
        if (level <= 0) level = 1;
        if (level > 5) level = 5;

        // 데미지: 20, 30, 40, 55, 70
        int base;
        switch (level) {
            case 1: base = 20; break;
            case 2: base = 30; break;
            case 3: base = 40; break;
            case 4: base = 55; break;
            case 5: base = 70; break;
            default: base = baseDamage;
        }

        // 범위: 60, 90, 130, 170, 210
        int range;
        switch (level) {
            case 1: range = baseRange; break;
            case 2: range = 90; break;
            case 3: range = 130; break;
            case 4: range = 170; break;
            case 5: range = 210; break;
            default: range = baseRange;
        }

        double mul = player.getAttackMultiplier();
        int finalDamage = (int)Math.round(base * mul);

        Rectangle atkArea = new Rectangle(
                player.worldX - range,
                player.worldY - range,
                player.width + range * 2,
                player.height + range * 2
        );

        for (Monster m : monsters) {
            if (!m.isAlive()) continue;

            if (atkArea.intersects(m.getBounds())) {
                m.takeDamage(finalDamage);
                int screenX = m.worldX - player.worldX + player.screenX;
                int screenY = m.worldY - player.worldY + player.screenY;
                gp.addDamageText(screenX, screenY, finalDamage);
            }
        }

        effectTimer = EFFECT_DURATION;
    }

    @Override
    public void draw(Graphics g, Player player) {
        if (effectTimer <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        int px = player.screenX;
        int py = player.screenY;

        int level = player.getWeaponUpgradeLevel(WeaponType.SWORD);
        if (level <= 0) level = 1;
        if (level > 5) level = 5;

        int range;
        switch (level) {
            case 1: range = baseRange; break;
            case 2: range = 90; break;
            case 3: range = 130; break;
            case 4: range = 170; break;
            case 5: range = 210; break;
            default: range = baseRange;
        }

        int sizeW = player.width + range;
        int sizeH = player.height + range;

        g2.setColor(new Color(255, 255, 0, 120));
        g2.fillOval(px - range / 2, py - range / 2, sizeW, sizeH);

        g2.setColor(new Color(255, 200, 0, 180));
        g2.drawOval(px - range / 2, py - range / 2, sizeW, sizeH);

        g2.dispose();
        effectTimer--;
    }
}
