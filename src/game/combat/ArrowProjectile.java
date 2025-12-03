package game.combat;

import java.awt.*;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class ArrowProjectile {

    private double x, y;          // 월드 좌표
    private double vx, vy;        // 속도 (프레임당 이동량)
    private final double speed = 12.0;
    private final double maxDistance = 500.0;
    private double traveled = 0.0;

    private final int width = 14;
    private final int height = 4;

    private int damage;
    private int hitsLeft;         // 관통 가능 횟수 (2면 2마리까지)

    private final GamePanel gp;

    public ArrowProjectile(GamePanel gp,
                           double startX, double startY,
                           double dirX, double dirY,
                           int damage,
                           int hitsAllowed) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.hitsLeft = hitsAllowed;

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

            if (arrowRect.intersects(m.getBounds())) {
                m.takeDamage(damage);

                // 데미지 텍스트
                int screenX = m.worldX - player.worldX + player.screenX;
                int screenY = m.worldY - player.worldY + player.screenY;
                gp.addDamageText(screenX, screenY, damage);

                hitsLeft--;          // 한 마리 맞춤
                if (hitsLeft <= 0) break; // 관통 횟수 다 쓰면 종료
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
