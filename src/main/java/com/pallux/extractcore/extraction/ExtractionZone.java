package com.pallux.extractcore.extraction;

public class ExtractionZone {
    private final String name;
    private final String world;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final long rewardScrap, rewardScrews, rewardEnergy, rewardBio, rewardTech;

    public ExtractionZone(String name, String world,
                          int minX, int minY, int minZ,
                          int maxX, int maxY, int maxZ,
                          long rewardScrap, long rewardScrews, long rewardEnergy,
                          long rewardBio, long rewardTech) {
        this.name = name; this.world = world;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.rewardScrap = rewardScrap; this.rewardScrews = rewardScrews;
        this.rewardEnergy = rewardEnergy; this.rewardBio = rewardBio; this.rewardTech = rewardTech;
    }

    public String getName() { return name; }
    public String getWorld() { return world; }
    public int getMinX() { return minX; } public int getMinY() { return minY; } public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; } public int getMaxY() { return maxY; } public int getMaxZ() { return maxZ; }
    public long getRewardScrap()  { return rewardScrap; }
    public long getRewardScrews() { return rewardScrews; }
    public long getRewardEnergy() { return rewardEnergy; }
    public long getRewardBio()    { return rewardBio; }
    public long getRewardTech()   { return rewardTech; }

    public int getCenterX() { return (minX + maxX) / 2; }
    public int getCenterZ() { return (minZ + maxZ) / 2; }
}
