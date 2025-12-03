package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class BowWeapon implements Weapon {

    // 중간 쿨타임, 중간 데미지
    private final int damage = 18;
    private final int cooldownFrames = 45; // 약 0.75초
    private final int maxTargetDistance = 500; // 중거리 제한

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public int getCooldownFrames() {
        return cooldownFrames;
    }

    @Override
    public void attack(GamePanel gp, Player player, List<Monster> monsters) {
        // 가장 가까운 몬스터를 찾아 그 방향으로 화살 발사
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

            if (dist2 < bestDist2 && dist2 <= maxTargetDistance * maxTargetDistance) {
                bestDist2 = dist2;
                target = m;
            }
        }

        if (target == null) {
            // 사거리 안에 몬스터가 없으면 발사 안 함
            return;
        }

        int tx = target.worldX + target.width / 2;
        int ty = target.worldY + target.height / 2;

        double dirX = tx - px;
        double dirY = ty - py;

        // 관통 1마리 → 총 2마리까지 맞출 수 있도록 hitsAllowed = 2
        gp.spawnArrow(px, py, dirX, dirY, damage, 2);
    }

    @Override
    public void draw(Graphics g, Player player) {
        // 화살 그래픽은 ArrowProjectile에서 그림
        g.setColor(Color.WHITE);
    }
}
