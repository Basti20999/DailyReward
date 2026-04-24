package com.basti20999.dailyreward;

public final class PlayerData {

    private long lastClaimed;
    private int streak;
    private int totalClaims;

    public PlayerData(long lastClaimed, int streak, int totalClaims) {
        this.lastClaimed = lastClaimed;
        this.streak = streak;
        this.totalClaims = totalClaims;
    }

    public static PlayerData empty() {
        return new PlayerData(0L, 0, 0);
    }

    public long lastClaimed() {
        return lastClaimed;
    }

    public int streak() {
        return streak;
    }

    public int totalClaims() {
        return totalClaims;
    }

    public void setLastClaimed(long lastClaimed) {
        this.lastClaimed = lastClaimed;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public void setTotalClaims(int totalClaims) {
        this.totalClaims = totalClaims;
    }

    public boolean hasEverClaimed() {
        return lastClaimed > 0;
    }
}
