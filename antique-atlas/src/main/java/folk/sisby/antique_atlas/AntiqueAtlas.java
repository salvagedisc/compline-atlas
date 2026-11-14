package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.reloader.TileTextures;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod(value = AntiqueAtlas.ID, dist = Dist.CLIENT)
public class AntiqueAtlas {
	public static final String ID = "antique_atlas";
	public static final String NAME = "Antique Atlas";

	public static final Logger LOGGER = LogManager.getLogger(NAME);

	public static final AntiqueAtlasConfig CONFIG = AntiqueAtlasConfig.createToml(FMLPaths.CONFIGDIR.get(), "", "antique-atlas", AntiqueAtlasConfig.class);
	public static final ScreenState<AtlasScreen> lastState = new ScreenState<>();

	public static final ModelResourceLocation ATLAS_MODEL = ModelResourceLocation.standalone(AntiqueAtlas.id("item/atlas"));

	public static final List<String> ATLAS_NAMES = List.of(
		"Antique Atlas"
	);

	public static ResourceLocation id(String path) {
		return path.contains(":") ? ResourceLocation.tryParse(path) : ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	public static ItemStack getHandheldAtlas() {
		ItemStack stack = Items.BOOK.getDefaultInstance().copy();
		stack.set(DataComponents.ITEM_NAME, Component.translatable("item.antique_atlas.atlas"));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.translatable("item.antique_atlas.atlas.lore").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)),
			Component.translatable("item.antique_atlas.atlas.hint", Component.translatable("item.antique_atlas.atlas")).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false))
		)));
		return stack;
	}

	public static AtlasScreen openAtlasScreen() {
		if (Minecraft.getInstance().screen == null && (!AntiqueAtlas.CONFIG.requireItem || (Minecraft.getInstance().player != null && AntiqueAtlas.hasHandheldAtlas(Minecraft.getInstance().player)))) {
			AtlasScreen screen = new AtlasScreen();
			screen.init();
			screen.prepareToOpen();
			screen.tick();
			Minecraft.getInstance().setScreen(screen);
			return screen;
		}
		return null;
	}

	public static boolean isHandheldAtlas(ItemStack stack) {
		return stack.is(Items.BOOK) && ATLAS_NAMES.stream().anyMatch(n -> stack.getHoverName().getString().toLowerCase().contains(n.toLowerCase()));
	}

	public static boolean hasHandheldAtlas(Player player) {
		if (isHandheldAtlas(player.getOffhandItem())) return true;
		for (ItemStack itemStack : player.getInventory().items) {
			if (isHandheldAtlas(itemStack)) {
				return true;
			}
		}
		return false;
	}

	public static Map<UUID, PlayerSummary> getOrderedFriends() {
		Map<UUID, PlayerSummary> friends = SurveyorClient.getFriends();
		PlayerSummary playerSummary = friends.remove(SurveyorClient.getClientUuid());
		Map<UUID, PlayerSummary> orderedFriends = new LinkedHashMap<>(friends);
		if (playerSummary != null) orderedFriends.put(SurveyorClient.getClientUuid(), playerSummary);
		return orderedFriends;
	}

	private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
		ItemStack map = Items.MAP.getDefaultInstance();
		if (event.getParentEntries().contains(map) && event.getSearchEntries().contains(map)) {
			event.insertAfter(map, getHandheldAtlas(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		} else {
			event.accept(getHandheldAtlas());
		}
	}

	public AntiqueAtlas(IEventBus modBus) {
		AntiqueAtlasKeybindings.init(modBus);
		modBus.addListener((RegisterClientReloadListenersEvent e) -> {
			// Applied in registration order: the textures first, then the providers that reference them.
			e.registerReloadListener(TileTextures.getInstance());
			e.registerReloadListener(MarkerTextures.getInstance());
			e.registerReloadListener(BiomeTileProviders.getInstance());
			e.registerReloadListener(StructureTileProviders.getInstance());
		});
		modBus.addListener((ModelEvent.RegisterAdditional e) -> e.register(ATLAS_MODEL));
		modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> ItemProperties.register(Items.BOOK, AntiqueAtlas.id("atlas"), ((stack, world, entity, seed) -> isHandheldAtlas(stack) ? 1.0F : 0.0F))));
		modBus.addListener(AntiqueAtlas::addToCreativeTab);

		SurveyorClientEvents.Register.terrainUpdated(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onTerrainUpdated(s, k));
		SurveyorClientEvents.Register.structuresAdded(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onStructuresAdded(s, k));
		SurveyorClientEvents.Register.landmarksAdded(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksAdded(s, k));
		SurveyorClientEvents.Register.landmarksRemoved(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksRemoved(s, k));
		NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
			if (e.getLevel() instanceof ClientLevel) SurveyorClient.getSummaries(Minecraft.getInstance().getConnection()).values().forEach(s -> WorldAtlasData.getOrCreate(s.dimension()).tick(s));
		});
		NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent e) -> BiomeTileProviders.getInstance().registerFallbacks(e.getRegistryAccess().registryOrThrow(Registries.BIOME)));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
			BiomeTileProviders.getInstance().clearFallbacks();
			WorldAtlasData.WORLDS.clear();
		});

		WorldSummary.enableTerrain();
		WorldSummary.enableStructures();
		WorldSummary.enableLandmarks();
	}
}
