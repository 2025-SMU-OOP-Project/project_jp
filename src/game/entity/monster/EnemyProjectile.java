package game.entity.monster;

import java.awt.*;
import game.entity.player.Player;

public class EnemyProjectile {

    private double x, y;
    private double dx, dy;
    private double speed;
    private int damage;
    private boolean alive = true;

    private int radius = 5;

    public EnemyProjectile(double x, double y, double dx, double dy, double speed, int damage) {
        this.x = x; this.y = y;
        double len = Math.sqrt(dx*dx + dy*dy);
        if (len < 1) len = 1;
        this.dx = dx / len;
        this.dy = dy / len;
        this.speed = speed;
        this.damage = damage;
    }

    public boolean isAlive() { return alive; }
    public int getDamage() { return damage; }

    // GamePanel에서 필요하면 쓸 수 있게 추가
    public Rectangle getBounds() {
        return new Rectangle((int)x - radius, (int)y - radius, radius * 2, radius * 2);
    }

    // GamePanel은 update(player)로 호출해야 함
    public void update(Player player) {
        if (!alive) return;

        x += dx * speed;
        y += dy * speed;

        if (getBounds().intersects(player.getBounds())) {
            player.takeDamage(damage);
            alive = false;
        }

        if (Math.abs(x - player.worldX) > 2000 || Math.abs(y - player.worldY) > 2000) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2, Player player) {
        int sx = (int)x - player.worldX + player.screenX;
        int sy = (int)y - player.worldY + player.screenY;

        g2.setColor(new Color(255, 140, 60, 220));
        g2.fillOval(sx - radius, sy - radius, radius*2, radius*2);
        g2.setColor(new Color(0,0,0,160));
        g2.drawOval(sx - radius, sy - radius, radius*2, radius*2);
    }
}
