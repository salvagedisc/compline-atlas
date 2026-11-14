package me.mariz.antique_transport;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Receivers for clientbound payloads. The client fills this in,
 * so payload registration never loads client classes on a dedicated server.
 */
public final class ClientReceivers {
    private static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, IPayloadContext>> RECEIVERS = new HashMap<>();

    private ClientReceivers() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, BiConsumer<T, IPayloadContext> receiver) {
        RECEIVERS.put(type, (payload, context) -> receiver.accept((T) payload, context));
    }

    static void handle(CustomPacketPayload payload, IPayloadContext context) {
        BiConsumer<CustomPacketPayload, IPayloadContext> receiver = RECEIVERS.get(payload.type());
        if (receiver != null) receiver.accept(payload, context);
    }
}
