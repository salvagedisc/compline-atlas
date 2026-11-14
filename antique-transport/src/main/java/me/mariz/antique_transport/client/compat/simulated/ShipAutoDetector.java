package me.mariz.antique_transport.client.compat.simulated;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import me.mariz.antique_transport.client.AntiqueTransportConfig;
import me.mariz.antique_transport.client.compat.sable.ShipCache;
import me.mariz.antique_transport.client.compat.sable.ShipUtils;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Automatically marks ships as visible on the atlas if they:
 * - have a name set in Sable (sublevel display name), or
 * - contain at least one entity diagram block (scanned once, result cached forever).
 * Runs every autoDetectIntervalSeconds*20 ticks, only for live (loaded) sublevels.
 */
public final class ShipAutoDetector {

    private ShipAutoDetector() {}

    private static final Set<UUID> confirmedHasDiagram = new HashSet<>();

    public static void tick() {
        int intervalSeconds = AntiqueTransportConfig.get().simulated.autoDetectIntervalSeconds;
        if (intervalSeconds == 0) return;

        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        long intervalTicks = (long) intervalSeconds * 20;
        if (level.getGameTime() % intervalTicks != 0) return;

        var ships = SableCompanion.INSTANCE.getAllIntersecting(level, ShipUtils.worldBounds(level));

        for (SubLevelAccess ship : ships) {
            UUID id = ship.getUniqueId();
            String name = null;
            try { name = ship.getName(); } catch (Exception ignored) {}

            // Condition 1: named sublevel
            if (!ShipCache.shipNames.containsKey(id)) {
                if (name != null && !name.isBlank()) {
                    ShipCache.shipNames.put(id, name);
                }
            }

            // Condition 2: diagram entity
            if (!confirmedHasDiagram.contains(id)) {
                if (ship instanceof ClientSubLevel sub) {
                    boolean found = hasDiagramEntity(sub);
                    if (found) {
                        confirmedHasDiagram.add(id);
                        ShipCache.shipNames.putIfAbsent(id, "");
                    }
                }
            }
        }
    }
    // Returns true if the sublevel contains at least one entity diagram block
    private static boolean hasDiagramEntity(SubLevelAccess ship) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return false;

        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof DiagramEntity)) continue;
            SubLevel containing = Sable.HELPER.getContaining(entity);
            if (containing != null && containing.getUniqueId().equals(ship.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    // Call on disconnect to avoid stale data across sessions
    public static void onDisconnect() {
        confirmedHasDiagram.clear();
    }
}