package com.raishxn.gtoa;

/** Independent of machine definitions: checking this must never trigger late registration. */
public final class MachineRegistrationState {
    private static volatile boolean registered;

    private MachineRegistrationState() {}
    static void complete() { registered = true; }

    public static void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException("GTO-Additions machines were not registered in GTOMachines.<clinit>. Check the coremod hook.");
        }
    }
}
