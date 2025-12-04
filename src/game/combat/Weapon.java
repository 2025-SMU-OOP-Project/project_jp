package game.combat;

import java.awt.Graphics;
import java.util.List;

import game.entity.monster.Monster;
import game.entity.player.Player;
import game.main.GamePanel;

/**
 * 무기 공통 인터페이스
 */
public interface Weapon {

    /** (기본) 한 번 공격했을 때 입히는 데미지 값 (레벨/패시브 전) */
    int getDamage();

    /** 
     * 공격 쿨타임(프레임 단위, 60이면 1초) 
     * - Player를 받아서 무기 레벨에 따라 쿨타임을 다르게 계산
     */
    int getCooldownFrames(Player player);

    /** 자동 공격 로직 */
    void attack(GamePanel gp, Player player, List<Monster> monsters);

    /** 무기 이펙트 그리기 */
    void draw(Graphics g, Player player);
}
