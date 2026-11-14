package me.mariz.antique_transport.server;

import me.mariz.antique_transport.ClientReceivers;
import me.mariz.antique_transport.client.compat.sable.ShipCache;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Registers client-side packet handlers for ship synchronization.
 * Call {@link #register()} in client initializer.
 */
public class ShipNetworking {

    public static void register() {

        // Sublevel positions from server (SERVER_MOD mode)
        ClientReceivers.register(ShipDataPacket.TYPE, (payload, ctx) ->
                Minecraft.getInstance().execute(() -> ShipCache.updateFromServer(payload.ships())));

        // Sublevel name/icon sync from server
        ClientReceivers.register(ShipSyncPacket.TYPE, (payload, ctx) ->
                Minecraft.getInstance().execute(() -> {
            UUID id = payload.shipId();
            if (payload.removed()) {
                ShipCache.deleteShip(id);
            } else {
                if (payload.name() != null) ShipCache.shipNames.put(id, payload.name());
                if (payload.iconId() != null) {
                    ResourceLocation rl = ResourceLocation.tryParse(payload.iconId());
                    if (rl != null) ShipCache.shipIcons.put(id, rl);
                }
            }
        }));

        // JOIN/DISCONNECT events
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) ->
                Minecraft.getInstance().execute(ShipCache::load)
        );

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
                ShipCache.onDisconnect()
        );
    }

    /**
     * Sends sublevel name/icon update to server from ShipNameModal.
     */
    public static void sendUpdate(UUID shipId, String name, String iconId) {
        if (!canSendToServer()) return;
        PacketDistributor.sendToServer(new ShipUpdatePacket(shipId, name, iconId));
    }

    /**
     * Sends ship removal signal to server.
     */
    public static void sendRemove(UUID shipId) {
        if (!canSendToServer()) return;
        PacketDistributor.sendToServer(new ShipUpdatePacket(shipId, null, null));
    }

    /**
     * Checks if server connection is ready for sending packets.
     */
    private static boolean canSendToServer() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return false;
        return connection.isAcceptingMessages()
                && connection.hasChannel(ShipUpdatePacket.TYPE);
    }
}