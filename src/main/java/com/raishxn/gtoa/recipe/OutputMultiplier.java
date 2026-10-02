package com.raishxn.gtoa.recipe;

import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentInner;
import java.util.ArrayList;
import java.util.List;

public final class OutputMultiplier {
    private OutputMultiplier() {}
    public static int limit(int tier) { return BoostMath.limit(tier); }
    public static long amount(long amount, int multiplier) { return BoostMath.amount(amount, multiplier); }
    public static <T extends ContentInner> List<Content<T>> copy(List<Content<T>> source, int multiplier) {
        var result = new ArrayList<Content<T>>(source.size());
        for (var content : source) result.add(new Content<>(content, amount(content.amount, multiplier)));
        return result;
    }
}
