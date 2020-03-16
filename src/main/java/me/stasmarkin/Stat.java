package me.stasmarkin;

import java.util.Optional;

public interface Stat {

    void onNumber(int number);

    Optional<Integer> getSmallestEncountered();

    Optional<Integer> getLargestEncountered();

    Optional<Double> getAverageOfAllEncountered();
}
