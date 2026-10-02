package com.raishxn.gtoa.factory;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class FactoryWorkQueueTest {
    @Test void blockedCompletedOutputConsumesNoMoreEnergyAndCommitsExactlyOnce() {
        var queue = new FactoryWorkQueue<String>();
        queue.restore(new FactoryWorkQueue.Entry<>("bender", 2, 4, 0));
        var energy = new AtomicInteger();
        var output = new ArrayList<String>();
        for (int i = 0; i < 10; i++) queue.tick(r -> { energy.incrementAndGet(); return true; }, r -> false, output::add);
        assertEquals(2, energy.get());
        assertEquals(4, queue.occupied());
        assertTrue(output.isEmpty());
        queue.tick(r -> { fail("Completed lane must not draw energy"); return false; }, r -> true, output::add);
        queue.tick(r -> true, r -> true, output::add);
        assertEquals(java.util.List.of("bender"), output);
        assertTrue(queue.isEmpty());
    }
    @Test void independentRecipesAdvanceTogetherAndOutputChecksObserveEarlierCommits() {
        var queue = new FactoryWorkQueue<String>();
        queue.restore(new FactoryWorkQueue.Entry<>("loom", 1, 2, 0));
        queue.restore(new FactoryWorkQueue.Entry<>("laminator", 1, 3, 0));
        var output = new ArrayList<String>();
        queue.tick(r -> true, r -> output.isEmpty(), output::add);
        assertEquals(1, output.size());
        assertEquals(3, queue.occupied());
        output.clear();
        queue.tick(r -> { fail("Blocked completed lane must retain progress"); return false; }, r -> true, output::add);
        assertEquals(java.util.List.of("laminator"), output);
    }
    @Test void limitedEnergyRotatesBetweenLanesWithoutRegressingProgress() {
        var queue = new FactoryWorkQueue<String>();
        queue.restore(new FactoryWorkQueue.Entry<>("a", 4, 1, 0));
        queue.restore(new FactoryWorkQueue.Entry<>("b", 4, 1, 0));
        var output = new ArrayList<String>();
        for (int tick = 0; tick < 8; tick++) {
            var energy = new AtomicInteger(1);
            queue.tick(r -> energy.getAndDecrement() > 0, r -> true, output::add);
        }
        assertEquals(2, output.size());
        assertTrue(queue.isEmpty());
    }
    @Test void restoringAnInFlightBatchKeepsItsOccupiedBudgetAndProgress() {
        var queue = new FactoryWorkQueue<String>();
        queue.restore(new FactoryWorkQueue.Entry<>("welder", 20, 6, 19));
        assertFalse(queue.canAccept(2, 8, 3));
        assertTrue(queue.canAccept(2, 8, 2));
        assertFalse(queue.canAccept(1, 8, 1));
        assertFalse(queue.canAccept(2, 4, 1));
        var energy = new AtomicInteger();
        var output = new ArrayList<String>();
        queue.tick(r -> { energy.incrementAndGet(); return true; }, r -> true, output::add);
        assertEquals(1, energy.get());
        assertEquals(java.util.List.of("welder"), output);
        assertEquals(0, queue.occupied());
    }
}
