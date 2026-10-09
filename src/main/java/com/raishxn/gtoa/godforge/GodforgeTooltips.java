package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.raishxn.gtoa.godforge.api.GodforgeModuleStats.Type;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * GTO-style machine tooltips for the Forge of Gods: animated highlight, coloured key facts, a blinking warning and the
 * recipe types at the end (GTO's {@code addRecipeTypeTooltips} order) instead of GTCEu's "Machine Type" line on top.
 */
public final class GodforgeTooltips {

    private GodforgeTooltips() {}

    private static final String P = "gtoa.godforge.tooltip.";

    public static BiConsumer<ItemStack, List<Component>> forge() {
        return (stack, lines) -> {
            lines.add(rainbow(Component.translatable(P + "forge.highlight").getString()));
            story(lines, "forge_of_gods", 15);
            separator(lines);
            lines.add(fact("modules", ChatFormatting.LIGHT_PURPLE, Component.literal("8 / 12 / 16")
                    .withStyle(ChatFormatting.GOLD)));
            lines.add(fact("fuel", ChatFormatting.AQUA));
            lines.add(fact("wireless", ChatFormatting.AQUA));
            lines.add(fact("upgrades", ChatFormatting.GOLD, Component.literal("31").withStyle(ChatFormatting.YELLOW)));
            lines.add(fact("rings", ChatFormatting.GREEN));
            lines.add(blink(Component.translatable(P + "forge.warning")));
            separator(lines);
            lines.add(Component.translatable(P + "machine_type",
                    Component.translatable(P + "stellar_forge").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY));
            source(lines);
        };
    }

    public static BiConsumer<ItemStack, List<Component>> module(String id, Type type, int lines,
                                                              Supplier<GTRecipeType[]> recipeTypes) {
        String key = type.name().toLowerCase(java.util.Locale.ROOT);
        return (stack, list) -> {
            list.add(rainbow(Component.translatable(P + key + ".highlight").getString()));
            story(list, id, lines);
            separator(list);
            list.add(fact("base_parallel", ChatFormatting.GOLD,
                    Component.literal(String.valueOf(switch (type) {
                        case SMELTING -> 1024;
                        case MOLTEN, PLASMA -> 512;
                        case EXOTIC -> 64;
                    })).withStyle(ChatFormatting.YELLOW)));
            list.add(fact(key + ".specialty", ChatFormatting.LIGHT_PURPLE));
            list.add(fact("wireless", ChatFormatting.AQUA));
            if (type != Type.EXOTIC) {
                list.add(fact("threads", ChatFormatting.GREEN));
                list.add(fact("heat", ChatFormatting.RED));
            } else {
                list.add(fact("exotic.random", ChatFormatting.DARK_PURPLE));
            }
            list.add(blink(Component.translatable(P + "module.warning")));
            separator(list);
            GTRecipeType[] types = recipeTypes.get();
            if (types == null || types.length == 0 || type == Type.EXOTIC) {
                list.add(Component.translatable(P + "machine_type",
                        Component.translatable(P + "exotic_matter").withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent names = Component.empty();
                for (int i = 0; i < types.length; i++) {
                    if (i > 0) names.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                    names.append(Component.translatable(types[i].registryName.toLanguageKey())
                            .withStyle(ChatFormatting.YELLOW));
                }
                list.add(Component.translatable(P + "machine_type", names).withStyle(ChatFormatting.GRAY));
            }
            source(list);
        };
    }

    private static void story(List<Component> lines, String id, int count) {
        for (int i = 0; i < count; i++) lines.add(Component.translatable("gtoa.machine." + id + ".tooltip." + i));
    }

    private static void separator(List<Component> lines) {
        lines.add(Component.literal("§8§m                                                            "));
    }

    private static Component fact(String key, ChatFormatting bullet, Object... args) {
        return Component.literal("▶ ").withStyle(bullet)
                .append(Component.translatable(P + key, args).withStyle(ChatFormatting.GRAY));
    }

    private static void source(List<Component> lines) {
        lines.add(Component.translatable(P + "source").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** Blinks between red and dark red twice a second. */
    private static Component blink(Component text) {
        boolean on = (System.currentTimeMillis() / 500) % 2 == 0;
        return text.copy().withStyle(on ? ChatFormatting.RED : ChatFormatting.DARK_RED, ChatFormatting.BOLD);
    }

    /** Each letter takes a hue that scrolls with time. */
    private static Component rainbow(String text) {
        MutableComponent out = Component.empty();
        long time = System.currentTimeMillis();
        for (int i = 0; i < text.length(); i++) {
            float hue = ((time / 20 + i * 12) % 360) / 360f;
            int rgb = net.minecraft.util.Mth.hsvToRgb(hue, 0.7f, 1f);
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)));
        }
        return out;
    }
}
