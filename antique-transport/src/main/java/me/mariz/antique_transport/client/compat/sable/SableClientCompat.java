package me.mariz.antique_transport.client.compat.sable;

import me.mariz.antique_transport.client.compat.ModCompat;
import me.mariz.antique_transport.client.compat.simulated.ShipAutoDetector;
import me.mariz.antique_transport.server.ShipNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Comparator;
import java.util.UUID;

public final class SableClientCompat {
    private SableClientCompat() {
    }

    public static void init(IEventBus modBus) {
        ShipNetworking.register();
        registerShipTextureReload(modBus);
        registerCommands();
        registerTick();
    }
    private static void registerTick() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            ShipCache.tick();
            if (ModCompat.SIMULATED) {
                ShipAutoDetector.tick();
            }
        });
    }

    private static void registerShipTextureReload(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) SableClientCompat::reloadShipTextures));
    }

    private static void reloadShipTextures(ResourceManager manager) {
        ShipCache.SHIP_TEXTURES.clear();

        manager.listResources("textures/gui/ship_textures", path -> path.toString().endsWith(".png"))
                .keySet()
                .stream()
                .filter(rl -> rl.getNamespace().equals("antique_transport"))
                .sorted(Comparator.comparing(ResourceLocation::getPath))
                .forEach(ShipCache.SHIP_TEXTURES::add);
    }

    private static void registerCommands() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            var root = Commands.literal("antique_transport");
            root.then(Commands.literal("markship").executes(ctx -> {
                var player = Minecraft.getInstance().player;
                if (player == null) {
                    return 0;
                }

                var level = Minecraft.getInstance().level;
                if (level == null) {
                    return 0;
                }

                UUID nearestId = SableShipLocator.findNearestShipId(player, level);
                if (nearestId == null) {
                    ctx.getSource().sendSystemMessage(Component.literal("Antique Transport: No ship found nearby!"));
                    return 0;
                }

                Minecraft.getInstance().execute(() -> {
                    var current = Minecraft.getInstance().screen;
                    if (current instanceof folk.sisby.antique_atlas.gui.AtlasScreen atlasScreen && !ShipNameModal.isOpenOn(atlasScreen)) {
                        atlasScreen.addChild(new ShipNameModal(nearestId));
                    } else {
                        ShipCache.pendingModalShipId = nearestId;
                        var screen = new folk.sisby.antique_atlas.gui.AtlasScreen();
                        screen.prepareToOpen();
                        Minecraft.getInstance().setScreen(screen);
                    }
                });

                return 1;
            }));

            event.getDispatcher().register(root);
        });
    }
}