package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class StaffWeapon implements Weapon {

    private final int damage = 10;
    private final int cooldownFrames = 90; // 1.5초
    private final int radius = 120;

    private int effectTimer = 0;
    private final int EFFECT_DURATION = 12;

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

        int cx = player.worldX + player.width / 2;
        int cy = player.worldY + player.height / 2;

        int r2 = radius * radius;

        for (Monster m : monsters) {
            if (!m.isAlive()) continue;

            int mx = m.worldX + m.width / 2;
            int my = m.worldY + m.height / 2;
            int dx = mx - cx;
            int dy = my - cy;

            if (dx * dx + dy * dy <= r2) {
                m.takeDamage(damage);
                System.out.println("Staff hit " + m + " for " + damage);
            }
        }

        effectTimer = EFFECT_DURATION;
    }

    @Override
    public void draw(Graphics g, Player player) {
        if (effectTimer <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();

        int cx = player.screenX + player.width / 2;
        int cy = player.screenY + player.height / 2;
        int d = radius * 2;

        g2.setColor(new Color(100, 200, 255, 80));
        g2.fillOval(cx - radius, cy - radius, d, d);

        g2.setColor(new Color(150, 220, 255, 180));
        g2.drawOval(cx - radius, cy - radius, d, d);

        g2.dispose();
        effectTimer--;
    }
}
