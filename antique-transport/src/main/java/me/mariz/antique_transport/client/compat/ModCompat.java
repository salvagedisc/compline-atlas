package me.mariz.antique_transport.client.compat;

import net.neoforged.fml.ModList;

public final class ModCompat {
    public static final boolean CREATE = ModList.get().isLoaded("create");
    public static final boolean SABLE = ModList.get().isLoaded("sable");
    public static final boolean SIMULATED = ModList.get().isLoaded("simulated");
    private ModCompat() {
    }
}