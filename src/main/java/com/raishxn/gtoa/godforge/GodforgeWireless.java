package com.raishxn.gtoa.godforge;

import com.hepdd.gtmthings.api.misc.WirelessEnergyContainer;
import com.hepdd.gtmthings.utils.TeamUtil;

import java.math.BigInteger;
import java.util.UUID;

/** The owner's GTMThings wireless EU network, as GTO's own Eye of Harmony uses it. */
public final class GodforgeWireless {

    private GodforgeWireless() {}

    public static WirelessEnergyContainer container(UUID owner) {
        return WirelessEnergyContainer.getOrCreateContainer(TeamUtil.getTeamUUID(owner));
    }

    public static BigInteger stored(UUID owner) {
        return container(owner).getStorage();
    }

    /** Takes {@code amount} EU if the network holds it. */
    public static boolean consume(UUID owner, BigInteger amount) {
        var container = container(owner);
        BigInteger storage = container.getStorage();
        if (storage.compareTo(amount) < 0) return false;
        container.setStorage(storage.subtract(amount));
        return true;
    }
}
