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

    // 기본 Stats (레벨 1 기준)
    private final int baseDamage = 20;
    private final int baseRange  = 60;   // 지금은 안 써도 놔둬도 됨

    @Override
    public int getDamage() {
        return baseDamage;
    }

    @Override
    public int getCooldownFrames(Player player) {
        int level = player.getWeaponUpgradeLevel(WeaponType.SWORD);
        if (level <= 0) level = 1;

        // Lv1: 60, Lv2: 40, Lv3: 25 프레임 (느리 → 빠르게)
        switch (level) {
            case 1: return 60;
            case 2: return 40;
            case 3: return 25;
        }
        return 60;
    }

    // 이펙트 표시용
    private int effectTimer = 0;
    private final int EFFECT_DURATION = 8;

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {

        int level = player.getWeaponUpgradeLevel(WeaponType.SWORD);
        if (level <= 0) level = 1;

        // 데미지: 20, 30, 40
        int base = baseDamage + (level - 1) * 10;

        // ✅ 범위 크게: Lv1 120, Lv2 170, Lv3 230
        int range;
        switch (level) {
            case 1: range = baseRange; break;
            case 2: range = 90; break;
            case 3: range = 130; break;
            default: range = 120;
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

                System.out.println("Sword(Lv " + level + ") hit " + m + " for " + finalDamage);
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

        //  이펙트 원도 같은 range 사용
        int range;
        switch (level) {
            case 1: range = baseRange; break;
            case 2: range = 90; break;
            case 3: range = 130; break;
            default: range = 120;
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
