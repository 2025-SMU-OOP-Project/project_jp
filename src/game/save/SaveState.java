package game.save;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import game.combat.WeaponType;

public class SaveState implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean valid = false;

    // ---- 런/시간 ----
    public int killCount = 0;
    public int elapsedPlaySec = 0;

    // ---- 스폰/보스 상태 ----
    public int eliteTimer = 0;
    public int nonBossKillCount = 0;
    public boolean bossAlive = false;
    public int bossKillThreshold = 50;
    public int spawnTimer = 0;

    // ---- 플레이어 ----
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

    // ---- 몬스터 ----
    public List<MonsterState> monsters = new ArrayList<>();

    public static class MonsterState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int worldX, worldY;
        public int width, height;

        public String kind;        // MonsterKind name()
        public int difficultyStage;

        // 추가: 등급(엘리트/보스)
        public boolean elite;
        public boolean boss;

        public int currentHp;
        public int maxHp;

        public int damage;
        public int speed;

        public int dashCooldown;
        public int dashTimer;
        public int dashDirX, dashDirY;

        public int shootCooldown;
    }

    // ---- 경험치 구슬(ExpOrb) ----
    public static class ExpOrbSave implements Serializable {
        private static final long serialVersionUID = 1L;
        public int worldX, worldY;
        public int value;
    }
    public List<ExpOrbSave> expOrbs = new ArrayList<>();
}
