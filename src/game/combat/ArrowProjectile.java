package game.combat;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class ArrowProjectile {

    private double x, y;
    private double vx, vy;
    private final double speed;         // ← 레벨에 따라 바뀌는 속도
    private final double maxDistance = 500.0;
    private double traveled = 0.0;

    private final int width = 14;
    private final int height = 4;

    private int damage;
    private int hitsLeft;

    private final GamePanel gp;

    private Set<Monster> hitMonsters = new HashSet<>();

    public ArrowProjectile(GamePanel gp,
                           double startX, double startY,
                           double dirX, double dirY,
                           int damage,
                           int hitsAllowed,
                           double speed) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.hitsLeft = hitsAllowed;
        this.speed = speed;

        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        vx = (dirX / len) * speed;
        vy = (dirY / len) * speed;
    }

    public boolean isAlive() {
        return hitsLeft > 0 && traveled < maxDistance;
    }

    public void update(List<Monster> monsters, Player player) {
        if (!isAlive()) return;

        x += vx;
        y += vy;
        traveled += Math.sqrt(vx * vx + vy * vy);

        Rectangle arrowRect = new Rectangle(
                (int) x - width / 2,
                (int) y - height / 2,
                width, height
        );

        for (Monster m : monsters) {
            if (!m.isAlive()) continue;
            if (hitMonsters.contains(m)) continue;

            if (arrowRect.intersects(m.getBounds())) {
                m.takeDamage(damage);

                int screenX = m.worldX - player.worldX + player.screenX;
                int screenY = m.worldY - player.worldY + player.screenY;
                gp.addDamageText(screenX, screenY, damage);

                hitMonsters.add(m);
                hitsLeft--;
                if (hitsLeft <= 0) break;
            }
        }
    }

    public void draw(Graphics2D g2, Player player) {
        if (!isAlive()) return;

        int screenX = (int) x - player.worldX + player.screenX;
        int screenY = (int) y - player.worldY + player.screenY;

        g2.setColor(new Color(200, 230, 255));
        g2.fillRoundRect(
                screenX - width / 2,
                screenY - height / 2,
                width, height,
                4, 4
        );
    }
}
