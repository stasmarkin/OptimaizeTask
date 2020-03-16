package me.stasmarkin;

import jdk.internal.jline.internal.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentStat implements Stat {

    private final AtomicReference<Accumulator> acc = new AtomicReference<>(null);

    @Override
    public void onNumber(int number) {
        while (true) {
            Accumulator acc = this.acc.get();
            Accumulator newAcc = acc == null ? new Accumulator(number) :
                    acc.applyNewNumber(number);
            if (this.acc.compareAndSet(acc, newAcc))
                break;
        }
    }

    @Override
    public Optional<Integer> getSmallestEncountered() {
        Accumulator acc = this.acc.get();
        if (acc == null)
            return Optional.empty();
        return Optional.of(acc.smallest);
    }

    @Override
    public Optional<Integer> getLargestEncountered() {
        Accumulator acc = this.acc.get();
        if (acc == null)
            return Optional.empty();
        return Optional.of(acc.largest);
    }

    @Override
    public Optional<Double> getAverageOfAllEncountered() {
        Accumulator acc = this.acc.get();
        if (acc == null)
            return Optional.empty();
        return Optional.of(acc.avg());
    }

    @Nullable
    protected Accumulator getAccumulator() {
        return this.acc.get();
    }

    protected static class Accumulator {
        public final int smallest;
        public final int largest;
        public final int counter;
        public final long sum;

        private Accumulator(int smallest, int largest, int counter, long sum) {
            this.smallest = smallest;
            this.largest = largest;
            this.counter = counter;
            this.sum = sum;
        }

        private Accumulator(int number) {
            this.smallest = number;
            this.largest = number;
            this.counter = 1;
            this.sum = number;
        }

        public Accumulator applyNewNumber(int number) {
            if (counter == Integer.MAX_VALUE)
                //there are plenty thing you could do to avoid that overflow
                //1) Store counter in long, dismiss int sum and store avg in double instead.
                //   You will lose some precision
                //2) Add List<Double> avgs, after you hit counter == Integer.MAX_VALUE, add sum/counter to avgs.
                //   You will lose a little precision after 2^31 numbers applied
                //3) Both (1) and (2) methods has limits of 2^63 numbers applied.
                //   Once you hit that, you may create sort of List<List<Double>>, where every inner List<Double> represents an avg of previous list.
                //   E.g. [ [1, 5] , [ -1] , [3, 7] ] means that there are:
                //   2^31 elements that gives 1 in average,
                //   2^31 elements with avg 5
                //   2^31 * 2^31 = 2^62 elements with avg -1
                //   2^62 * 2^31 = 2^93 elements with avg 3
                //   2^93 elements with avg 7
                //   So, this method limit would be (2^31)^31 = 2^961 numbers
                throw new RuntimeException("Can't accumulate anymore numbers");

            return new Accumulator(
                    Math.min(smallest, number),
                    Math.max(largest, number),
                    counter + 1,
                    sum + number
            );
        }

        public double avg() {
            return 1.0 * sum / counter;
        }
    }
}
