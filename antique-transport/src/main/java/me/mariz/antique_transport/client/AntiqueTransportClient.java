package me.mariz.antique_transport.client;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import folk.sisby.antique_atlas.gui.AtlasRenderer;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import me.mariz.antique_transport.client.compat.ModCompat;
import me.mariz.antique_transport.client.compat.create.CreateClientCompat;
import me.mariz.antique_transport.client.compat.sable.SableClientCompat;
import me.mariz.antique_transport.client.compat.sable.ShipCache;
import me.mariz.antique_transport.client.compat.sable.ShipUtils;
import me.mariz.antique_transport.client.compat.simulated.ShipDiagramRenderer;
import me.mariz.antique_transport.AntiqueTransport;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = AntiqueTransport.ID, dist = Dist.CLIENT)
public class AntiqueTransportClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("antiquetrains");

    public static final ResourceLocation OVERLAY_ID =
            ResourceLocation.tryBuild("antiquetrains", "tracks");

    public AntiqueTransportClient(IEventBus modBus) {
        LOGGER.info("Initializing Antique Trains client");

        AntiqueTransportConfig.register();
        AtlasRenderer.registerOverlay(OVERLAY_ID, new AtlasOverlay());

        if (ModCompat.CREATE) {
            CreateClientCompat.init();
        }

        if (ModCompat.SABLE) {
            SableClientCompat.init(modBus);
        }
        if(ModCompat.SIMULATED) {
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
                    Minecraft.getInstance().execute(() -> {
                        ShipDiagramRenderer.freeAll();
                        ShipCache.onDisconnect();
                    }));
            NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
                if (!(Minecraft.getInstance().screen instanceof AtlasScreen)) return;
                var level = Minecraft.getInstance().level;
                if (level == null) return;
                float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
                for (SubLevelAccess ship : SableCompanion.INSTANCE.getAllIntersecting(level, ShipUtils.worldBounds(level))) {
                    if (ship instanceof ClientSubLevel sub) {
                        ShipDiagramRenderer.renderDiagram(sub.getUniqueId(), sub, pt);
                    }
                }
            });
        }
    }
}