package com.raishxn.gtoa.factory;

import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.io.IOException;

public final class FactoryConfig {
    private static FactoryBalance balance;
    private FactoryConfig() {}
    public static synchronized FactoryBalance get() {
        if (balance != null) return balance;
        var path = FMLPaths.CONFIGDIR.get().resolve("gtoa/balance/universal_factory.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(path)) {
                try (var reader = Files.newBufferedReader(path)) { balance = gson.fromJson(reader, FactoryBalance.class); }
            } else {
                balance = new FactoryBalance();
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(balance) + "\n");
            }
            if (balance == null) throw new IllegalArgumentException("Empty Universal Factory config: " + path);
            balance.validate();
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load Universal Factory config: " + path, error);
        }
        return balance;
    }
}
