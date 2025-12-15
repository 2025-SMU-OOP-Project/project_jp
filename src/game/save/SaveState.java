package game.save;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import game.combat.WeaponType;

public class SaveState implements Serializable {
    private static final long serialVersionUID = 1L;

    // 유효한 세이브인지 체크용
    public boolean valid = false;

    // 진행
    public int killCount;
    public int elapsedPlaySec;

    public int eliteTimer;
    public int nonBossKillCount;
    public boolean bossAlive;
    public int bossKillThreshold;
    public int spawnTimer;

    // 플레이어
    public PlayerState player = new PlayerState();

    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int worldX, worldY;

        public int currentHp;
        public int maxHp;

        public int level;
        public int currentExp;
        public int expToNextLevel;

        public int attackLevel;
        public int speedLevel;
        public int maxHpLevel;

        public List<WeaponState> weapons = new ArrayList<>();
    }

    public static class WeaponState implements Serializable {
        private static final long serialVersionUID = 1L;

        public WeaponType type;
        public int level;
    }
}
