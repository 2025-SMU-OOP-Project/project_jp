package game.effects;

import java.awt.*;

public class DamageText {

    private int x, y;
    private int value;

    private float alpha = 1.0f;     // 투명도 (1 → 0)
    private int life = 40;          // 40프레임 = 약 0.6초
    private int riseSpeed = 1;      // 위로 떠오르는 속도

    public DamageText(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public boolean update() {
        y -= riseSpeed;         // 위로 이동
        alpha -= 0.03f;         // 점점 투명하게
        life--;

        return (life <= 0 || alpha <= 0);
    }

    public void draw(Graphics2D g2) {
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha));

        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        g2.drawString(String.valueOf(value), x, y);

        // 원래 투명도로 복구
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1f));
    }
}
