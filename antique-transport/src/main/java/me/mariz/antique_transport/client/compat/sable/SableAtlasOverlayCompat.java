package me.mariz.antique_transport.client.compat.sable;

import folk.sisby.antique_atlas.gui.AtlasOverlay;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import me.mariz.antique_transport.client.AntiqueTransportConfig;
import me.mariz.antique_transport.client.compat.ModCompat;
import me.mariz.antique_transport.client.compat.simulated.ShipDiagramRenderer;
import me.mariz.antique_transport.server.ShipDataPacket;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;

public final class SableAtlasOverlayCompat {
    private SableAtlasOverlayCompat() {
    }

    public static void onScreenRender(AtlasOverlay.AtlasScreenRenderContext context) {
        if (!AntiqueTransportConfig.get().sable.showShips) return;
        if (!(Minecraft.getInstance().screen instanceof AtlasScreen)) return;
        ShipRenderer.renderShips(context);
    }

    public static boolean handleShipClick(AtlasScreen screen, int mouseX, int mouseY) {
        if (ShipNameModal.isOpenOn(screen)) return false;

        int mx = mouseX - screen.getGuiX();
        int my = mouseY - screen.getGuiY();
        float ppb = (float) screen.getPixelsPerBlock();

        UUID closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Map.Entry<UUID, int[]> entry : ShipCache.shipScreenPositions.entrySet()) {
            UUID uuid = entry.getKey();
            int[] pos = entry.getValue();
            int sx = pos[0], sy = pos[1];

            // size of sublevel
            float[] frustum = ModCompat.SIMULATED
                    ? ShipDiagramRenderer.getShipFrustumSize(uuid)
                    : null;
            boolean hasDiagram = ModCompat.SIMULATED
                    && ShipDiagramRenderer.hasDiagramContent(uuid)
                    && frustum != null && frustum[0] > 0
                    && ShipCache.isDiagramEnabled(uuid)
                    && ppb >= ShipCache.getDiagramMinPpb(uuid) * 2;

            boolean hit;
            if (hasDiagram) {
                int dispW = Math.max(1, Math.round(frustum[0] * ppb));
                int dispH = Math.max(1, Math.round(frustum[1] * ppb));
                // rotation of sublevel
                float rad = (float) Math.toRadians(
                        -ShipCache.lastMovingYaw.getOrDefault(uuid, 0f));
                float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
                float localX = cos * (mx - sx) - sin * (my - sy);
                float localZ = sin * (mx - sx) + cos * (my - sy);
                hit = Math.abs(localX) <= dispW / 2f && Math.abs(localZ) <= dispH / 2f;
            } else {
                // just by icon
                hit = Math.abs(mx - sx) <= 16 && Math.abs(my - sy) <= 16;
            }

            if (hit) {
                double dist = Math.hypot(mx - sx, my - sy);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = uuid;
                }
            }
        }

        if (closest != null) {
            screen.addChild(new ShipNameModal(closest));
            return true;
        } else {
            for (Map.Entry<UUID, ShipDataPacket.ShipEntry> entry : ShipCache.lastKnownPositions.entrySet()) {
                UUID uuid = entry.getKey();
                if (ShipCache.shipNames.containsKey(uuid)) continue;

                int sx = (int) screen.worldXToScreenX(entry.getValue().x()) - screen.getGuiX();
                int sy = (int) screen.worldZToScreenY(entry.getValue().z()) - screen.getGuiY();

                if (Math.abs(mx - sx) <= 16 && Math.abs(my - sy) <= 16) {
                    double dist = Math.hypot(mx - sx, my - sy);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = uuid;
                    }
                }
            }
            if (closest != null) {
                screen.addChild(new ShipNameModal(closest));
                return true;
            }
        }
        return false;
    }
}