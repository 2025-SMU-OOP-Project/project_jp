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

        // Lv1:100, Lv2:90, Lv3:80
        switch (level) {
            case 1: return 100;
            case 2: return 90;
            case 3: return 80;
        }
        return 100;
    }

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {

        int level = player.getWeaponUpgradeLevel(WeaponType.STAFF);
        if (level <= 0) level = 1;

        int base = baseDamage + (level - 1) * 10; // 30,40,50
        double mul = player.getAttackMultiplier();
        int finalDamage = (int)Math.round(base * mul);

        // 반경: 60 → 100 → 150 (눈에 띄게)
        int radius;
        if      (level == 1) radius = baseRadius;
        else if (level == 2) radius = 100;
        else                 radius = 150;

        // 1. 사거리 내 가장 가까운 몬스터 탐색
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

        if (target == null) {
            return;
        }

        int tx = target.worldX + target.width / 2;
        int ty = target.worldY + target.height / 2;

        double dirX = tx - px;
        double dirY = ty - py;

        gp.spawnFireball(px, py, dirX, dirY, finalDamage, radius);
    }

    @Override
    public void draw(Graphics g, Player player) {
        // 파이어볼/폭발 그래픽은 FireballProjectile이 담당
        g.setColor(Color.WHITE);
    }
}
