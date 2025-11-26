package game.combat;

import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

/**
 * 무기 공통 인터페이스
 *  - 자동 공격용 attack(...)
 *  - 화면에 효과를 그리는 draw(...)
 */
public interface Weapon {

    /** 한 번 공격했을 때 입히는 데미지 */
    int getDamage();

    /** 공격 쿨타임(프레임 단위, 60이면 1초) */
    int getCooldownFrames();

    /**
     * 자동 공격 로직
     *  - GamePanel에서 쿨타임마다 호출
     */
    void attack(GamePanel gp, Player player, List<Monster> monsters);

    /**
     *  무기 이펙트 그리기 (필요 없으면 빈 구현 가능)
     */
    void draw(Graphics g, Player player);
}
