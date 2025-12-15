package game.combat;

import java.awt.*;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class FireballProjectile {

    private double x, y;
    private double vx, vy;
    private final double speed = 8.0;
    private final double maxDistance = 300.0;

    private double traveled = 0.0;

    private int damage;
    private int radius;

    private boolean exploded = false;
    private boolean finished = false;
    private int explosionTimer = 0;
    private final int EXPLOSION_DURATION = 12;

    private final GamePanel gp;

    private static final int FIREBALL_SIZE = 18;

    public FireballProjectile(GamePanel gp,
                              double startX, double startY,
                              double dirX, double dirY,
                              int damage, int radius) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.radius = radius;

        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        vx = (dirX / len) * speed;
        vy = (dirY / len) * speed;
    }

    public boolean isAlive() {
        return !finished;
    }

    public void update(List<Monster> monsters, Player player) {
        if (finished) return;

        if (!exploded) {
            x += vx;
            y += vy;
            traveled += Math.sqrt(vx * vx + vy * vy);

            Rectangle myRect = getHitBounds();
            for (Monster m : monsters) {
                if (m == null || !m.isAlive()) continue;

                if (myRect.intersects(m.getBounds())) {
                    explode(monsters, player);
                    return;
                }
            }

            if (traveled >= maxDistance) {
                explode(monsters, player);
            }
        } else {
            explosionTimer++;
            if (explosionTimer > EXPLOSION_DURATION) {
                finished = true;
            }
        }
    }

    private Rectangle getHitBounds() {
        return new Rectangle(
                (int) (x - FIREBALL_SIZE / 2.0),
                (int) (y - FIREBALL_SIZE / 2.0),
                FIREBALL_SIZE,
                FIREBALL_SIZE
        );
    }

    private void explode(List<Monster> monsters, Player player) {
        if (exploded) return;
        exploded = true;

        int cx = (int) x;
        int cy = (int) y;
        int r2 = radius * radius;

        for (Monster m : monsters) {
            if (m == null || !m.isAlive()) continue;

            Rectangle hb = m.getBounds();
            int mx = hb.x + hb.width / 2;
            int my = hb.y + hb.height / 2;

            int dx = mx - cx;
            int dy = my - cy;

            if (dx * dx + dy * dy <= r2) {
                m.takeDamage(damage);

                int screenX = m.worldX - player.worldX + player.screenX;
                int screenY = m.worldY - player.worldY + player.screenY;
                gp.addDamageText(screenX, screenY, damage);
            }
        }
    }

    public void draw(Graphics2D g2, Player player) {
        if (finished) return;

        int screenX = (int) x - player.worldX + player.screenX;
        int screenY = (int) y - player.worldY + player.screenY;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (!exploded) {
            g2.setColor(new Color(255, 180, 0));
            g2.fillOval(screenX - FIREBALL_SIZE / 2, screenY - FIREBALL_SIZE / 2, FIREBALL_SIZE, FIREBALL_SIZE);
            g2.setColor(new Color(255, 80, 0));
            g2.drawOval(screenX - FIREBALL_SIZE / 2, screenY - FIREBALL_SIZE / 2, FIREBALL_SIZE, FIREBALL_SIZE);
        } else {
            float t = explosionTimer / (float) EXPLOSION_DURATION;
            int alpha = (int) ((1.0f - t) * 180);
            int currentR = (int) (radius * (0.8 + 0.4 * t));

            g2.setColor(new Color(255, 200, 50, Math.max(0, alpha)));
            g2.fillOval(screenX - currentR, screenY - currentR,
                    currentR * 2, currentR * 2);

            g2.setColor(new Color(255, 120, 0, Math.max(0, alpha + 40)));
            g2.drawOval(screenX - currentR, screenY - currentR,
                    currentR * 2, currentR * 2);
        }
    }
}
