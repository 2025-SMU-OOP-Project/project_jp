package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class StaffWeapon implements Weapon {

    private final int damage = 25;          // 폭발 데미지 (검보다 조금 쎄게)
    private final int cooldownFrames = 90;  // 느린 쿨
    private final int explosionRadius = 60; // 폭발 반경
    private final int maxCastDistance = 400; // 타겟까지 최대 거리

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
        // 가장 가까운 몬스터 방향으로 파이어볼 발사
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

            if (dist2 < bestDist2 && dist2 <= maxCastDistance * maxCastDistance) {
                bestDist2 = dist2;
                target = m;
            }
        }

        if (target == null) {
            // 사거리 내 몬스터 없으면 발사 안 함
            return;
        }

        int tx = target.worldX + target.width / 2;
        int ty = target.worldY + target.height / 2;

        double dirX = tx - px;
        double dirY = ty - py;

        gp.spawnFireball(px, py, dirX, dirY, damage, explosionRadius);
    }

    @Override
    public void draw(Graphics g, Player player) {
        // 파이어볼/폭발 그래픽은 FireballProjectile에서 처리하므로 여기선 할 일 없음
        g.setColor(Color.WHITE);
    }
}
