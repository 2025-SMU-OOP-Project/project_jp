package game.entity;

import java.awt.*;
import game.entity.player.Player;

public class ExpOrb {

    // 월드 좌표 (중심)
    private double x;
    private double y;

    private int baseRadius = 6;   // 기본 크기
    private int value;

    // 흡수 관련
    private static final double ATTRACT_RADIUS = 80.0;
    private static final double PICKUP_RADIUS  = 16.0;
    private static final double BASE_SPEED     = 2.0;

    // 아이콘 이펙트용
    private int tick = 0;

    public ExpOrb(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Rectangle getBounds() {
        int r = baseRadius;
        return new Rectangle((int)(x - r), (int)(y - r), r * 2, r * 2);
    }

    /** true를 리턴하면 플레이어가 먹은 것(제거 + 경험치 지급) */
    public boolean update(Player player) {

        tick = (tick + 1) % 360;

        double px = player.worldX + player.width  / 2.0;
        double py = player.worldY + player.height / 2.0;

        double dx = px - x;
        double dy = py - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= PICKUP_RADIUS) {
            return true;
        }

        if (dist <= ATTRACT_RADIUS) {
            if (dist == 0) dist = 1;
            double t = 1.0 - (dist / ATTRACT_RADIUS); // 0~1
            double speed = BASE_SPEED + t * 4.0;      // 2~6 정도

            double vx = dx / dist * speed;
            double vy = dy / dist * speed;

            x += vx;
            y += vy;
        }

        return false;
    }

    public void draw(Graphics g, Player player) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int screenX = (int) (x - player.worldX + player.screenX);
        int screenY = (int) (y - player.worldY + player.screenY);

        // 펄스(숨 쉬는) 효과
        double pulse = 1.0 + 0.25 * Math.sin(Math.toRadians(tick * 4)); // 0.75~1.25
        int radius   = (int) (baseRadius * pulse);

        // 1) 바깥쪽 부드러운 빛(글로우)
        int glowR = radius + 6;
        g2.setColor(new Color(80, 255, 160, 80));
        g2.fillOval(screenX - glowR, screenY - glowR, glowR * 2, glowR * 2);

        // 2) 메인 구슬 (그라데이션)
        GradientPaint gp = new GradientPaint(
                screenX, screenY - radius,
                new Color(200, 255, 230),
                screenX, screenY + radius,
                new Color(60, 220, 130)
        );
        g2.setPaint(gp);
        g2.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

        // 3) 테두리
        g2.setColor(new Color(20, 100, 70));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

        // 4) 하이라이트 (작은 반달)
        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillOval(screenX - radius / 2, screenY - radius, radius, radius);

        g2.dispose();
    }
}
