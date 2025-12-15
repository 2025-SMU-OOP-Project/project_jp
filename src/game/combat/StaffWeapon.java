package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class StaffWeapon implements Weapon {

    private final int baseDamage = 30;
    private final int baseRadius = 60;
    private final int maxCastDistance = 600;

    @Override
    public int getDamage() {
        return baseDamage;
    }

    @Override
    public int getCooldownFrames(Player player) {
        int level = player.getWeaponUpgradeLevel(WeaponType.STAFF);
        if (level <= 0) level = 1;

        // Lv1:100, Lv2:90, Lv3:80, Lv4:70, Lv5:60
        switch (level) {
            case 1: return 100;
            case 2: return 90;
            case 3: return 80;
            case 4: return 70;
            case 5: return 60;
        }
        return 100;
    }

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {

        int level = player.getWeaponUpgradeLevel(WeaponType.STAFF);
        if (level <= 0) level = 1;
        if (level > 5) level = 5;

        // 데미지: 30, 40, 50, 65, 80
        int base;
        switch (level) {
            case 1: base = 30; break;
            case 2: base = 40; break;
            case 3: base = 50; break;
            case 4: base = 65; break;
            case 5: base = 80; break;
            default: base = baseDamage;
        }

        double mul = player.getAttackMultiplier();
        int finalDamage = (int)Math.round(base * mul);

        // 반경: 60, 100, 150, 200, 260
        int radius;
        switch (level) {
            case 1: radius = baseRadius; break;
            case 2: radius = 100; break;
            case 3: radius = 140; break;
            case 4: radius = 180; break;
            case 5: radius = 230; break;
            default: radius = baseRadius;
        }

        Monster target = null;
        double bestDist2 = Double.MAX_VALUE;

        int px = player.worldX + player.width / 2;
        int py = player.worldY + player.height / 2;

        for (Monster m : monsters) {
            if (!m.isAlive()) continue;

            int mx = m.worldX + m.width / 2;
            int my = m.worldY + m.height / 2;

            double dx = mx - px;
            double dy = my - py;
            double dist2 = dx * dx + dy * dy;

            if (dist2 < bestDist2 &&
                dist2 <= maxCastDistance * maxCastDistance) {
                bestDist2 = dist2;
                target = m;
            }
        }

        if (target == null) return;

        int tx = target.worldX + target.width / 2;
        int ty = target.worldY + target.height / 2;

        double dirX = tx - px;
        double dirY = ty - py;

        gp.spawnFireball(px, py, dirX, dirY, finalDamage, radius);
    }

    @Override
    public void draw(Graphics g, Player player) {
        g.setColor(Color.WHITE);
    }
}
