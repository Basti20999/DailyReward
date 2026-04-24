package com.basti20999.dailyreward.reward;

import com.basti20999.dailyreward.config.PluginConfig;

import java.util.concurrent.ThreadLocalRandom;

public final class RewardCalculator {

    private RewardCalculator() {}

    public static RewardResult calculate(PluginConfig cfg, int streakAfterClaim) {
        int mMin, mMax, keyAmount, gems;
        String keyType;
        int day;

        if (cfg.weeklyEnabled && !cfg.weeklyRotation.isEmpty()) {
            day = ((Math.max(1, streakAfterClaim) - 1) % 7) + 1;
            PluginConfig.DayReward dr = cfg.weeklyRotation.get(day);
            if (dr == null) {
                mMin = cfg.moneyMin;
                mMax = cfg.moneyMax;
                keyType = cfg.keyType;
                keyAmount = cfg.keyAmount;
                gems = cfg.gems;
            } else {
                mMin = dr.moneyMin();
                mMax = dr.moneyMax();
                keyType = dr.keyType();
                keyAmount = dr.keyAmount();
                gems = dr.gems();
            }
        } else {
            day = 0;
            mMin = cfg.moneyMin;
            mMax = cfg.moneyMax;
            keyType = cfg.keyType;
            keyAmount = cfg.keyAmount;
            gems = cfg.gems;
        }

        int money = mMin >= mMax ? mMin : ThreadLocalRandom.current().nextInt(mMin, mMax + 1);

        if (cfg.streakEnabled && !cfg.streakBonuses.isEmpty()) {
            PluginConfig.StreakBonus applicable = null;
            for (var entry : cfg.streakBonuses.entrySet()) {
                if (streakAfterClaim >= entry.getKey()
                        && (applicable == null || entry.getKey() > applicable.threshold())) {
                    applicable = entry.getValue();
                }
            }
            if (applicable != null) {
                money = (int) Math.round(money * applicable.moneyMultiplier());
                keyAmount += applicable.extraKeys();
                gems += applicable.bonusGems();
            }
        }

        return new RewardResult(money, keyAmount, keyType, gems, day);
    }

    public static int computeNextStreak(PluginConfig cfg, long lastClaimed, int previousStreak) {
        if (lastClaimed <= 0L) return 1;
        long now = System.currentTimeMillis();
        long diff = now - lastClaimed;
        long grace = (long) (com.basti20999.dailyreward.util.CooldownUtil.cooldownMillis(cfg.cooldownHours)
                * cfg.streakGraceMultiplier);
        if (diff <= grace) return Math.max(1, previousStreak) + 1;
        return 1;
    }
}
