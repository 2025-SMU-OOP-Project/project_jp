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
        // 쿨타임은 큰 변화 없이 유지 (원하면 레벨에 따라 조금 줄여도 됨)
        return 45;
    }

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {

        // 1. 레벨 기반 스탯 계산
        int level = player.getWeaponUpgradeLevel(WeaponType.BOW);
        if (level <= 0) level = 1;

        int base = baseDamage + (level - 1) * 5;      // 15,20,25
        double mul = player.getAttackMultiplier();
        int finalDamage = (int)Math.round(base * mul);

        int arrowCount = level;                       // Lv1=1, Lv2=2, Lv3=3
        int hitsAllowed = 2 + (level - 1);            // 2,3,4
        double speed;
        if      (level == 1) speed = 12.0;
        else if (level == 2) speed = 14.0;
        else                 speed = 16.0;

        // 2. 타겟 방향 (기본 중심 방향)
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

        if (target == null) {
            return;
        }

        int tx = target.worldX + target.width / 2;
        int ty = target.worldY + target.height / 2;

        double dirX = tx - px;
        double dirY = ty - py;

        // 3. 여러 발 발사 (각도 스프레드)
        double baseAngle = Math.atan2(dirY, dirX);
        double spread = Math.toRadians(10); // 화살 간 각도

        int midIndex = arrowCount / 2;
        for (int i = 0; i < arrowCount; i++) {
            int offset = i - midIndex; // -1,0,1 ...
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
