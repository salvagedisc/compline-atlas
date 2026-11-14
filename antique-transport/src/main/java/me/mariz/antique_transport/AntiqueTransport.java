package me.mariz.antique_transport;

import me.mariz.antique_transport.client.compat.ModCompat;
import me.mariz.antique_transport.server.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AntiqueTransport.ID)
public class AntiqueTransport {
    public static final String ID = "antique_transport";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    public AntiqueTransport(IEventBus modBus) {
        modBus.addListener(AntiqueTransport::registerPayloads);
        registerPersistence();
        registerJoinSync();

        if (ModCompat.SABLE) {
            NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> AntiqueTransportServer.tick(event.getServer()));
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(ShipDataPacket.TYPE, ShipDataPacket.CODEC, ClientReceivers::handle);
        registrar.playToClient(ShipSyncPacket.TYPE, ShipSyncPacket.CODEC, ClientReceivers::handle);
        registrar.playToServer(ShipUpdatePacket.TYPE, ShipUpdatePacket.CODEC, (payload, ctx) ->
                AntiqueTransportServer.handleShipUpdate(ctx.player().getServer(), payload));
    }

    private static void registerPersistence() {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> AntiqueTransportServer.loadPositionCache(event.getServer()));
        // Every level posts this event when the server saves; only react to the overworld, so the cache is written once.
        NeoForge.EVENT_BUS.addListener((LevelEvent.Save event) -> {
            if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
                AntiqueTransportServer.savePositionCache(level.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> AntiqueTransportServer.savePositionCache(event.getServer()));
    }

    private static void registerJoinSync() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) AntiqueTransportServer.syncAllKnownShipsTo(player);
        });
    }
}
