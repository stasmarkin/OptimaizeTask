package me.stasmarkin;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class ConcurrentStatTest {
    private final double EPS = 0.0001;

    @Test
    public void sanityTest() {
        Stat stat = new ConcurrentStat();
        assertEquals(Optional.empty(), stat.getSmallestEncountered());
        assertEquals(Optional.empty(), stat.getLargestEncountered());
        assertEquals(Optional.empty(), stat.getAverageOfAllEncountered());

        stat.onNumber(10);
        assertEquals(Optional.of(10), stat.getSmallestEncountered());
        assertEquals(Optional.of(10), stat.getLargestEncountered());
        assertEquals(10.0, stat.getAverageOfAllEncountered().get(), EPS);

        stat.onNumber(-10);
        assertEquals(Optional.of(-10), stat.getSmallestEncountered());
        assertEquals(Optional.of(10), stat.getLargestEncountered());
        assertEquals(0.0, stat.getAverageOfAllEncountered().get(), EPS);

        stat.onNumber(3);
        stat.onNumber(4);
        stat.onNumber(5);
        stat.onNumber(5);
        assertEquals(Optional.of(-10), stat.getSmallestEncountered());
        assertEquals(Optional.of(10), stat.getLargestEncountered());
        assertEquals(2.833333333, stat.getAverageOfAllEncountered().get(), EPS);
    }

    @Test
    public void maxIntNumbersDoesntCauseOverflowTest() {
        final Stat stat = new ConcurrentStat();

        final int NUMBERS = Integer.MAX_VALUE;
        for (int i = 0; i < NUMBERS; i++) {
            stat.onNumber(i);
        }
    }

    @Test(expected = RuntimeException.class)
    public void maxIntPlusOneNumbersCauseOverflowTest() {
        final Stat stat = new ConcurrentStat();

        final int NUMBERS = Integer.MAX_VALUE;
        for (int i = 0; i < NUMBERS; i++) {
            stat.onNumber(i);
        }

        stat.onNumber(0); //exception happens here
    }


    @Test
    public void concurrentTest() {
        final ConcurrentStat stat = new ConcurrentStat();

        final int THREADS = 32;
        final int NUMBERS = Math.min(1, (Integer.MAX_VALUE / THREADS) - 1);
        final int OFFSET = -20;

        final CyclicBarrier barrier = new CyclicBarrier(THREADS + 1);
        final ExecutorService ex = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final int threadNo = i;
            ex.submit(() -> {
                System.out.println("Thread " + threadNo + " waits");
                await(barrier);

                List<Integer> numbers = new ArrayList<>(NUMBERS);
                for (int j = 0; j < NUMBERS; j++) {
                    numbers.add(j, j + OFFSET);
                }
                Collections.shuffle(numbers);

                for (Integer number : numbers) {
                    stat.onNumber(number);
                }

                System.out.println("Thread " + threadNo + " completed");
                await(barrier);
            });
        }

        await(barrier);
        System.out.println("All threads started");

        await(barrier);
        System.out.println("All threads completed");

        int smallestExpected = OFFSET;
        int largestExpected = NUMBERS - 1 + OFFSET;
        double avgExpected = (1.0 * (NUMBERS - 1) / 2) + OFFSET;

        assertEquals(Optional.of(smallestExpected), stat.getSmallestEncountered());
        assertEquals(Optional.of(largestExpected), stat.getLargestEncountered());
        assertEquals(avgExpected, stat.getAverageOfAllEncountered().get(), EPS);

        ConcurrentStat.Accumulator acc = stat.getAccumulator();
        assertEquals(acc.counter, NUMBERS * THREADS);

    }


    private void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}