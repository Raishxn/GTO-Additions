package com.raishxn.gtoa.godforge;

import com.raishxn.gtoa.godforge.api.GodforgeModuleStats;

import java.util.UUID;

/** A Forge of Gods module controller placed in one of the forge's module slots. */
public interface IGodforgeModule {

    GodforgeModuleStats godforgeStats();

    /** Owner of the wireless EU network the module draws from; set by the forge. */
    void setNetworkOwner(UUID owner);

    void connect();

    void disconnect();

    /** Mirrors the forge-pushed stats to the client GUI. */
    default void syncStats() {}
}
