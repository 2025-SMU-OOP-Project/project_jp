package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class BowWeapon implements Weapon {

    private final int baseDamage = 15;
    private final int maxTargetDistance = 500;

    @Override
    public int getDamage() {
        return baseDamage;
    }

    @Override
    public int getCooldownFrames(Player player) {
        int level = player.getWeaponUpgradeLevel(WeaponType.BOW);
        if (level <= 0) level = 1;
        if (level > 5) level = 5;

        // Lv1:45, Lv2:42, Lv3:39, Lv4:36, Lv5:33
        switch (level) {
            case 1: return 45;
            case 2: return 42;
            case 3: return 39;
            case 4: return 36;
            case 5: return 33;
        }
        return 45;
    }

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {

        int level = player.getWeaponUpgradeLevel(WeaponType.BOW);
        if (level <= 0) level = 1;
        if (level > 5) level = 5;

        // 데미지: 15, 20, 25, 33, 42
        int base;
        switch (level) {
            case 1: base = 15; break;
            case 2: base = 20; break;
            case 3: base = 25; break;
            case 4: base = 33; break;
            case 5: base = 42; break;
            default: base = baseDamage;
        }

        double mul = player.getAttackMultiplier();
        int finalDamage = (int)Math.round(base * mul);

        // 화살 수: 1,2,3,4,5
        int arrowCount = level;

        // 관통 횟수(맞출 수 있는 몬스터 수): 2,3,4,5,6
        int hitsAllowed = 2 + (level - 1);

        // 속도: 12,14,16,18,20
        double speed;
        switch (level) {
            case 1: speed = 12.0; break;
            case 2: speed = 14.0; break;
            case 3: speed = 16.0; break;
            case 4: speed = 18.0; break;
            case 5: speed = 20.0; break;
            default: speed = 12.0;
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
                dist2 <= maxTargetDistance * maxTargetDistance) {
                bestDist2 = dist2;
                target = m;
            }
        }

        if (target == null) return;

        int tx = target.worldX + target.width / 2;
        int ty = target.worldY + target.height / 2;

        double dirX = tx - px;
        double dirY = ty - py;

        double baseAngle = Math.atan2(dirY, dirX);

        // 발수가 많아질수록 살짝만 벌어지도록 감소
        double spread = Math.toRadians(10);

        int midIndex = arrowCount / 2;
        for (int i = 0; i < arrowCount; i++) {
            int offset = i - midIndex;
            double angle = baseAngle + offset * spread;

            double dx = Math.cos(angle);
            double dy = Math.sin(angle);

            gp.spawnArrow(px, py, dx, dy, finalDamage, hitsAllowed, speed);
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        g.setColor(Color.WHITE);
    }
}
