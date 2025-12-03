package game.combat;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

public class SwordWeapon implements Weapon {

    private final int damage = 20;
    private final int cooldownFrames = 30;   // 0.5초마다 한 번
    private final int range = 60;           // 플레이어 주변 사거리(px)

    // 간단한 이펙트 표시용
    private int effectTimer = 0;
    private final int EFFECT_DURATION = 8;

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

        // 플레이어를 중심으로 한 사각형 범위
        Rectangle atkArea = new Rectangle(
                player.worldX - range,
                player.worldY - range,
                player.width + range * 2,
                player.height + range * 2
        );

        for (Monster m : monsters) {
            if (!m.isAlive()) continue;

            if (atkArea.intersects(m.getBounds())) {
                m.takeDamage(damage);
                int screenX = m.worldX - player.worldX + player.screenX;
                int screenY = m.worldY - player.worldY + player.screenY;
                gp.addDamageText(screenX, screenY, damage);
                
                System.out.println("Sword hit " + m + " for " + damage);
            }
        }

        // 이펙트 표시 시작
        effectTimer = EFFECT_DURATION;
    }

    @Override
    public void draw(Graphics g, Player player) {
        if (effectTimer <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        int px = player.screenX;
        int py = player.screenY;

        int sizeW = player.width + range;
        int sizeH = player.height + range;

        g2.setColor(new Color(255, 255, 0, 120));
        g2.fillOval(px - range / 2, py - range / 2, sizeW, sizeH);

        g2.setColor(new Color(255, 200, 0, 180));
        g2.drawOval(px - range / 2, py - range / 2, sizeW, sizeH);

        g2.dispose();

        // 프레임마다 감소 (GamePanel이 한 프레임 그릴 때마다 호출됨)
        effectTimer--;
    }
}
