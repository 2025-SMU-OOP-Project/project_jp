package game.entity.monster;

import java.awt.*;

import game.entity.player.Player;

public class Monster {

    // ===== 위치 / 이동 =====
    public int worldX, worldY;      // 맵 상의 절대 좌표
    public int speed = 1;
    public int width = 30, height = 30;

    // ===== 그래픽 =====
    public Image image;

    // ===== 체력 =====
    private int maxHp = 50;
    private int currentHp = maxHp;

    // ----------------------------------------------------
    // 생성자
    // ----------------------------------------------------
    public Monster(int x, int y, Image image) {
        this.worldX = x;
        this.worldY = y;
        this.image   = image;
    }

    // ----------------------------------------------------
    // AI : 플레이어를 향해 이동
    // ----------------------------------------------------
    public void update(int playerWorldX, int playerWorldY) {
        if (!isAlive()) return;

        if (worldX < playerWorldX) worldX += speed;
        if (worldX > playerWorldX) worldX -= speed;
        if (worldY < playerWorldY) worldY += speed;
        if (worldY > playerWorldY) worldY -= speed;
    }

    // ----------------------------------------------------
    // 그리기 (플레이어 기준 카메라 변환 + HP 바)
    // ----------------------------------------------------
    public void draw(Graphics g, Player player) {
        if (!isAlive()) return;

        // 월드좌표 -> 화면좌표
        int screenX = worldX - player.worldX + player.screenX;
        int screenY = worldY - player.worldY + player.screenY;

        // 1) 몬스터 이미지
        g.drawImage(image, screenX, screenY, width, height, null);

        // 2) HP 바 (몬스터 머리 위)
        int barWidth  = width;
        int barHeight = 4;
        int barX = screenX;
        int barY = screenY - 6; // 머리 위쪽

        // 배경 바
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);

        // 현재 HP 비율
        double ratio = (double) currentHp / maxHp;
        int hpFill = (int) (barWidth * ratio);

        // 남은 HP (초록)
        g.setColor(new Color(0, 220, 0));
        g.fillRect(barX, barY, hpFill, barHeight);

        // 테두리
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }

    // ----------------------------------------------------
    // 충돌 범위
    // ----------------------------------------------------
    public Rectangle getBounds() {
        return new Rectangle(worldX, worldY, width, height);
    }

    // ----------------------------------------------------
    // 전투 관련
    // ----------------------------------------------------
    public int getDamage() {
        return 10;
    }

    // 공격 당했을 때
    public void takeDamage(int damage) {
        if (!isAlive()) return;

        currentHp -= damage;
        if (currentHp < 0) currentHp = 0;
    }

    // 무기/게임 로직에서 쓰는 상태 메서드
    public boolean isDead()  { return currentHp <= 0; }
    public boolean isAlive() { return currentHp > 0; }

    public int getCurrentHp() { return currentHp; }
    public int getMaxHp()     { return maxHp;    }
}
