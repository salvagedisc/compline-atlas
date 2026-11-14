package me.mariz.antique_transport.client.compat.create;

import folk.sisby.antique_atlas.gui.AtlasScreen;
import me.mariz.antique_transport.client.AntiqueTransportConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CreateClientCompat {
    private CreateClientCompat() {
    }

    public static void init() {
        registerTickHandlers();
    }

    public static void syncStations(boolean show) {
        StationLandmarkSync.sync(show);
    }

    private static void registerTickHandlers() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) {
                return;
            }

            boolean atlasOpen = client.screen instanceof AtlasScreen;
            if (!atlasOpen) {
                com.simibubi.create.compat.trainmap.TrainMapSyncClient.stopRequesting();
                return;
            }

            if (AntiqueTransportConfig.get().create.showTracks) {
                com.simibubi.create.compat.trainmap.TrainMapSyncClient.requestData();
            }
        });
    }
}