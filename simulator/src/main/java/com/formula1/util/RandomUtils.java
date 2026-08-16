package com.formula1.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtils {

    private RandomUtils() {
    }

    public static double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static boolean randomBoolean(double probabilidad) {
        return ThreadLocalRandom.current().nextDouble() < probabilidad;
    }

    public static <T> T pickRandom(List<T> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(options.size());
        return options.get(index);
    }
}
