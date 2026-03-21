package com.pallux.extractcore.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds all persistent data for a single player.
 */
public class PlayerData {

    private final UUID uuid;
    private String name;

    // ── Currencies ────────────────────────────
    private long scrap;
    private long screws;
    private long energyCells;
    private long bioSamples;
    private long techShards;

    // ── Leveling ──────────────────────────────
    private int level;
    private long xp;

    // ── Stats ─────────────────────────────────
    private int kills;
    private int deaths;
    private int extractions;
    private int coresDestroyed;
    private long playtimeMinutes;
    private long firstJoinTime;
    private long lastSeenTime;

    // ── Core ──────────────────────────────────
    private String coreWorld;
    private int coreX, coreY, coreZ;
    private boolean corePlaced;
    private int coreSpeedLevel;
    private int coreProductionLevel;
    private long coreStoredScrap;

    // ── Armory Tiers ─────────────────────────
    private int swordTier   = 1;
    private int pickaxeTier = 1;
    private int axeTier     = 1;
    private int helmetTier  = 1;
    private int chestplateTier = 1;
    private int leggingsTier = 1;
    private int bootsTier   = 1;

    // ── Milestones ───────────────────────────
    private final Set<String> completedMilestones = new HashSet<>();
    private final Set<String> claimableMilestones = new HashSet<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.level = 1;
        this.firstJoinTime = System.currentTimeMillis();
    }

    // ── Currency helpers ──────────────────────
    public boolean hasMaterials(long scrap, long screws, long energy, long bio, long tech) {
        return this.scrap >= scrap && this.screws >= screws
                && this.energyCells >= energy && this.bioSamples >= bio && this.techShards >= tech;
    }

    public void deductMaterials(long scrap, long screws, long energy, long bio, long tech) {
        this.scrap      = Math.max(0, this.scrap - scrap);
        this.screws     = Math.max(0, this.screws - screws);
        this.energyCells = Math.max(0, this.energyCells - energy);
        this.bioSamples = Math.max(0, this.bioSamples - bio);
        this.techShards = Math.max(0, this.techShards - tech);
    }

    public void addMaterials(long scrap, long screws, long energy, long bio, long tech) {
        this.scrap       += scrap;
        this.screws      += screws;
        this.energyCells += energy;
        this.bioSamples  += bio;
        this.techShards  += tech;
    }

    // ── Getters & Setters ─────────────────────
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getScrap() { return scrap; }
    public void setScrap(long scrap) { this.scrap = Math.max(0, scrap); }
    public void addScrap(long amount) { this.scrap = Math.max(0, this.scrap + amount); }

    public long getScrews() { return screws; }
    public void setScrews(long screws) { this.screws = Math.max(0, screws); }
    public void addScrews(long amount) { this.screws = Math.max(0, this.screws + amount); }

    public long getEnergyCells() { return energyCells; }
    public void setEnergyCells(long energyCells) { this.energyCells = Math.max(0, energyCells); }
    public void addEnergyCells(long amount) { this.energyCells = Math.max(0, this.energyCells + amount); }

    public long getBioSamples() { return bioSamples; }
    public void setBioSamples(long bioSamples) { this.bioSamples = Math.max(0, bioSamples); }
    public void addBioSamples(long amount) { this.bioSamples = Math.max(0, this.bioSamples + amount); }

    public long getTechShards() { return techShards; }
    public void setTechShards(long techShards) { this.techShards = Math.max(0, techShards); }
    public void addTechShards(long amount) { this.techShards = Math.max(0, this.techShards + amount); }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }
    public void addXp(long amount) { this.xp += amount; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void incrementKills() { kills++; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void incrementDeaths() { deaths++; }

    public int getExtractions() { return extractions; }
    public void setExtractions(int extractions) { this.extractions = extractions; }
    public void incrementExtractions() { extractions++; }

    public int getCoresDestroyed() { return coresDestroyed; }
    public void setCoresDestroyed(int coresDestroyed) { this.coresDestroyed = coresDestroyed; }
    public void incrementCoresDestroyed() { coresDestroyed++; }

    public long getPlaytimeMinutes() { return playtimeMinutes; }
    public void setPlaytimeMinutes(long playtimeMinutes) { this.playtimeMinutes = playtimeMinutes; }
    public void addPlaytimeMinutes(long minutes) { this.playtimeMinutes += minutes; }

    public long getFirstJoinTime() { return firstJoinTime; }
    public void setFirstJoinTime(long firstJoinTime) { this.firstJoinTime = firstJoinTime; }

    public long getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(long lastSeenTime) { this.lastSeenTime = lastSeenTime; }

    public String getCoreWorld() { return coreWorld; }
    public void setCoreWorld(String coreWorld) { this.coreWorld = coreWorld; }

    public int getCoreX() { return coreX; }
    public void setCoreX(int coreX) { this.coreX = coreX; }

    public int getCoreY() { return coreY; }
    public void setCoreY(int coreY) { this.coreY = coreY; }

    public int getCoreZ() { return coreZ; }
    public void setCoreZ(int coreZ) { this.coreZ = coreZ; }

    public boolean isCorePlaced() { return corePlaced; }
    public void setCorePlaced(boolean corePlaced) { this.corePlaced = corePlaced; }

    public int getCoreSpeedLevel() { return coreSpeedLevel; }
    public void setCoreSpeedLevel(int level) { this.coreSpeedLevel = level; }

    public int getCoreProductionLevel() { return coreProductionLevel; }
    public void setCoreProductionLevel(int level) { this.coreProductionLevel = level; }

    public long getCoreStoredScrap() { return coreStoredScrap; }
    public void setCoreStoredScrap(long v) { this.coreStoredScrap = Math.max(0, v); }
    public void addCoreStoredScrap(long v) { this.coreStoredScrap = Math.max(0, this.coreStoredScrap + v); }

    public int getSwordTier() { return swordTier; }
    public void setSwordTier(int tier) { this.swordTier = tier; }

    public int getPickaxeTier() { return pickaxeTier; }
    public void setPickaxeTier(int tier) { this.pickaxeTier = tier; }

    public int getAxeTier() { return axeTier; }
    public void setAxeTier(int tier) { this.axeTier = tier; }

    public int getHelmetTier() { return helmetTier; }
    public void setHelmetTier(int tier) { this.helmetTier = tier; }

    public int getChestplateTier() { return chestplateTier; }
    public void setChestplateTier(int tier) { this.chestplateTier = tier; }

    public int getLeggingsTier() { return leggingsTier; }
    public void setLeggingsTier(int tier) { this.leggingsTier = tier; }

    public int getBootsTier() { return bootsTier; }
    public void setBootsTier(int tier) { this.bootsTier = tier; }

    public Set<String> getCompletedMilestones() { return completedMilestones; }
    public Set<String> getClaimableMilestones() { return claimableMilestones; }

    public double getKD() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
