package com.raishxn.gtoa.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Committed input batches: a blocked completed entry retains its output without another energy tick. */
public final class FactoryWorkQueue<R> {
    public static final class Entry<R> {
        public final R recipe;
        public final int duration;
        public final long parallel;
        public int progress;
        public Entry(R recipe, int duration, long parallel, int progress) {
            if (recipe == null || duration < 1 || parallel < 1) throw new IllegalArgumentException("Invalid recipe lane");
            this.recipe = recipe;
            this.duration = duration;
            this.parallel = parallel;
            this.progress = Math.clamp(progress, 0, duration);
        }
    }
    private final List<Entry<R>> entries = new ArrayList<>();
    public List<Entry<R>> entries() { return Collections.unmodifiableList(entries); }
    public int size() { return entries.size(); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public long occupied() {
        long result = 0;
        for (var entry : entries) result = Math.addExact(result, entry.parallel);
        return result;
    }
    public boolean canAccept(int threadLimit, long operationLimit, long parallel) {
        return parallel > 0 && entries.size() < threadLimit && parallel <= Math.max(0, operationLimit - occupied());
    }
    public void restore(Entry<R> entry) { entries.add(entry); }
    public void clear() { entries.clear(); }
    public boolean tick(Predicate<R> advance, Predicate<R> outputReady, Consumer<R> outputCommit) {
        boolean worked = false;
        for (var iterator = entries.iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (entry.progress < entry.duration && advance.test(entry.recipe)) {
                entry.progress++;
                worked = true;
            }
            if (entry.progress >= entry.duration && outputReady.test(entry.recipe)) {
                // The commit is serialized with other entries; simulation sees earlier outputs.
                outputCommit.accept(entry.recipe);
                iterator.remove();
                worked = true;
            }
        }
        Collections.rotate(entries, 1); // Prevent a continuously refilled first lane monopolizing EU.
        return worked;
    }
}
