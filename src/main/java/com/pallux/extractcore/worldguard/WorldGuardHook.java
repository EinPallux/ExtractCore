package com.pallux.extractcore.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard integration for ExtractCore.
 *
 * Custom flag:
 *   extract-core-place  (StateFlag, default ALLOW)
 *     → Set to DENY on spawn/protected regions to prevent Core placement there.
 *
 * Core bypass:
 *   Core placement and core raiding always bypass WorldGuard's block protection,
 *   so they work even inside arena regions that deny block-place / block-break.
 */
public class WorldGuardHook {

    /**
     * Custom flag: when set to DENY on a region, players cannot place their Core there.
     * Register this BEFORE WorldGuard initialises (i.e. in onLoad()).
     */
    public static StateFlag CORE_PLACE_FLAG;

    private static boolean hooked = false;

    private WorldGuardHook() {}

    /**
     * Call this from your plugin's onLoad() — before WorldGuard loads its regions.
     */
    public static void registerFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("extract-core-place", true /* default ALLOW */);
            registry.register(flag);
            CORE_PLACE_FLAG = flag;
        } catch (FlagConflictException e) {
            // Another plugin (or a previous load) already registered the flag — reuse it.
            Flag<?> existing = registry.get("extract-core-place");
            if (existing instanceof StateFlag sf) {
                CORE_PLACE_FLAG = sf;
            }
        }
    }

    /** Must be called after the server has finished enabling all plugins. */
    public static void init() {
        hooked = isWorldGuardPresent();
    }

    private static boolean isWorldGuardPresent() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isHooked() {
        return hooked;
    }

    // ── Flag queries ─────────────────────────────────────────────────────────

    /**
     * Returns true if the player is allowed to place a Core at the given location.
     * Returns true when WorldGuard is not present (fail-open).
     */
    public static boolean canPlaceCore(Player player, Location location) {
        if (!hooked || CORE_PLACE_FLAG == null) return true;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            com.sk89q.worldguard.LocalPlayer wgPlayer =
                    WorldGuardPlugin.inst().wrapPlayer(player);
            com.sk89q.worldedit.util.Location wgLoc =
                    BukkitAdapter.adapt(location);

            StateFlag.State state = query.queryState(wgLoc, wgPlayer, CORE_PLACE_FLAG);

            // DENY means blocked; ALLOW or null (no flag set) means allowed
            return state != StateFlag.State.DENY;
        } catch (Exception e) {
            // Safety: if anything goes wrong, allow placement
            return true;
        }
    }
}