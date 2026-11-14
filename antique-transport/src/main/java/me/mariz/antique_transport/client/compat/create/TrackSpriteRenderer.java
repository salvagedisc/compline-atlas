package me.mariz.antique_transport.client.compat.create;

import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import me.mariz.antique_transport.client.AntiqueTransportConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrackSpriteRenderer {
    private static final int COLOR_BALLAST = 0xFF9BC1D6;
    private static final int COLOR_BALLAST_OUTLINE = 0xFF111748;
    private static final float OUTLINE_EXTRA = 0.8f;

    private static final ResourceLocation TEXTURE_ID =
            ResourceLocation.tryBuild("antique_transport", "trackoverlay");

    private static DynamicTexture texture;
    private static double cachedScale = Double.NaN;
    private static ResourceKey<Level> cachedDimension;
    private static double cachedScrollX = Double.MIN_VALUE;
    private static double cachedScrollZ = Double.MIN_VALUE;
    private static int cachedWidth;
    private static int cachedHeight;
    private static float cachedHalfWidth = Float.NaN;

    private TrackSpriteRenderer() {}

    private record EdgeWithGraph(TrackEdge edge, TrackGraph graph) {}

    public static void render(GuiGraphics context, AtlasScreen screen) {
        double scale = screen.getPixelsPerBlock();
        if (scale < 0.05) return;

        int mapWidth = screen.mapWidth;
        int mapHeight = screen.mapHeight;
        ResourceKey<Level> dimension = screen.dim();
        double scrollX = screen.worldXToScreenX(0);
        double scrollZ = screen.worldZToScreenY(0);
        float halfWidth = AntiqueTransportConfig.get().create.trackWidthBlocks / 2.0f;

        boolean dirty = scale != cachedScale
                || !dimension.equals(cachedDimension)
                || scrollX != cachedScrollX
                || scrollZ != cachedScrollZ
                || mapWidth != cachedWidth
                || mapHeight != cachedHeight
                || halfWidth != cachedHalfWidth;

        if (dirty) {
            cachedScale = scale;
            cachedDimension = dimension;
            cachedScrollX = scrollX;
            cachedScrollZ = scrollZ;
            cachedWidth = mapWidth;
            cachedHeight = mapHeight;
            cachedHalfWidth = halfWidth;
            rebuildTexture(screen, scale, dimension, mapWidth, mapHeight, halfWidth);
        }

        if (texture != null) {
            context.blit(TEXTURE_ID, 0, 0, 0, 0, mapWidth, mapHeight, mapWidth, mapHeight);
        }
    }

    private static void rebuildTexture(AtlasScreen screen, double scale, ResourceKey<Level> dimension,
                                       int mapWidth, int mapHeight, float halfWidth) {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture.close();
            texture = null;
        }

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, mapWidth, mapHeight, true);

        int ballastRadius = Math.max(1, (int) (halfWidth * scale));
        int outlineRadius = Math.max(ballastRadius + 1, (int) ((halfWidth + OUTLINE_EXTRA) * scale));

        List<EdgeWithGraph> allEdges = collectEdges(dimension);

        for (EdgeWithGraph edge : allEdges)
            paintEdge(image, screen, scale, edge, outlineRadius, COLOR_BALLAST_OUTLINE, mapWidth, mapHeight);

        for (EdgeWithGraph edge : allEdges)
            paintEdge(image, screen, scale, edge, ballastRadius, COLOR_BALLAST, mapWidth, mapHeight);

        texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
        texture.upload();
    }

    private static List<EdgeWithGraph> collectEdges(ResourceKey<Level> dimension) {
        List<EdgeWithGraph> result = new ArrayList<>();

        for (TrackGraph graph : CreateClient.RAILWAYS.trackNetworks.values()) {
            Set<TrackEdge> visited = Collections.newSetFromMap(new IdentityHashMap<>());

            for (TrackNodeLocation loc : graph.getNodes()) {
                if (!loc.dimension.equals(dimension)) {
                    continue;
                }

                TrackNode node = graph.locateNode(loc);
                Map<TrackNode, TrackEdge> connections = graph.getConnectionsFrom(node);
                if (connections == null) {
                    continue;
                }

                for (TrackEdge edge : connections.values()) {
                    if (!visited.add(edge)) {
                        continue;
                    }
                    if (edge.isInterDimensional()) {
                        continue;
                    }

                    result.add(new EdgeWithGraph(edge, graph));
                }
            }
        }

        return result;
    }

    private static void paintEdge(NativeImage image, AtlasScreen screen, double scale,
                                  EdgeWithGraph e, int radius, int color, int mapWidth, int mapHeight) {
        double length = e.edge.getLength();
        if (length < 0.001) return;

        double stepInBlocks = Math.max(0.15, 0.5 / scale);
        int steps = Math.min(Math.max(8, (int) Math.ceil(length / stepInBlocks)), 2048);

        double previousX = Double.NaN;
        double previousZ = Double.NaN;

        for (int i = 0; i <= steps; i++) {
            Vec3 pos = e.edge.getPosition(e.graph, (double) i / steps);
            double screenX = toScreenX(screen, pos.x);
            double screenZ = toScreenZ(screen, pos.z);

            if (!Double.isNaN(previousX)) {
                paintLine(image, previousX, previousZ, screenX, screenZ, radius, color, mapWidth, mapHeight);
            }

            previousX = screenX;
            previousZ = screenZ;
        }
    }
    private static void paintLine(NativeImage image, double x0, double y0, double x1, double y1,
                                      int radius, int color, int mapWidth, int mapHeight) {
        int minX = Math.max(0, (int) Math.floor(Math.min(x0, x1) - radius - 1));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, y1) - radius - 1));
        int maxX = Math.min(mapWidth - 1, (int) Math.ceil(Math.max(x0, x1) + radius + 1));
        int maxY = Math.min(mapHeight - 1, (int) Math.ceil(Math.max(y0, y1) + radius + 1));

        double dx = x1 - x0, dy = y1 - y0;
        double lengthSq = dx * dx + dy * dy;
        double radiusSq = (double) radius * radius;

        if (lengthSq < 0.0001) {
            paintCircle(image, x0, y0, radius, color, mapWidth, mapHeight);
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5, py = y + 0.5;
                double t = Math.clamp(((px - x0) * dx + (py - y0) * dy) / lengthSq, 0.0, 1.0);
                double cx = x0 + t * dx, cy = y0 + t * dy;
                double distX = px - cx, distY = py - cy;
                if (distX * distX + distY * distY <= radiusSq) {
                    image.setPixelRGBA(x, y, color);
                }
            }
        }
    }
    private static void paintCircle(NativeImage image, double cx, double cy, int radius,
                                   int color, int mapWidth, int mapHeight) {
        int minX = Math.max(0, (int) Math.floor(cx - radius));
        int minY = Math.max(0, (int) Math.floor(cy - radius));
        int maxX = Math.min(mapWidth - 1, (int) Math.ceil(cx + radius));
        int maxY = Math.min(mapHeight - 1, (int) Math.ceil(cy + radius));
        double radiusSq = (double) radius * radius;
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                double dx = (x + 0.5) - cx, dy = (y + 0.5) - cy;
                if (dx * dx + dy * dy <= radiusSq) image.setPixelRGBA(x, y, color);
            }
    }

    private static int toScreenX(AtlasScreen screen, double worldX) {
        int floorX = (int) Math.floor(worldX);
        return (int) (
                screen.worldXToScreenX(floorX) - screen.getGuiX()
                        + (worldX - floorX) * screen.getPixelsPerBlock()
        );
    }

    private static int toScreenZ(AtlasScreen screen, double worldZ) {
        int floorZ = (int) Math.floor(worldZ);
        return (int) (
                screen.worldZToScreenY(floorZ) - screen.getGuiY()
                        + (worldZ - floorZ) * screen.getPixelsPerBlock()
        );
    }
}