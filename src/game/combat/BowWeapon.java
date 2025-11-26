package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class BowWeapon implements Weapon {

    private final int damage = 15;
    private final int cooldownFrames = 45; // 약 0.75초

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
        // 가장 가까운 몬스터 하나만 맞추는 간단한 구현
        Monster target = null;
        double bestDist2 = Double.MAX_VALUE;

        for (Monster m : monsters) {
            if (!m.isAlive()) continue;
            double dx = m.worldX - player.worldX;
            double dy = m.worldY - player.worldY;
            double dist2 = dx * dx + dy * dy;
            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                target = m;
            }
        }

        if (target != null) {
            target.takeDamage(damage);
            System.out.println("Bow hit " + target + " for " + damage);
        }
    }

    @Override
    public void draw(Graphics g, Player player) {
        // 나중에 화살 이펙트 넣고 싶으면 여기 구현
        // 일단은 안 그려도 됨
        g.setColor(Color.WHITE);
    }
}
