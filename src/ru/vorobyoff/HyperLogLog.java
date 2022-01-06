package ru.vorobyoff;

import java.io.IOException;
import java.nio.file.Path;

import static java.lang.Integer.numberOfLeadingZeros;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;
import static java.util.Objects.nonNull;

public final class HyperLogLog {

    public static void main(String... args) {
        if (args.length == 0) return;

        final var hyperLogLog = new HyperLogLog(15);

        final var path = Path.of(args[0]);
        try (final var reader = newBufferedReader(path, UTF_8)) {
            var line = reader.readLine();
            while (nonNull(line)) {
                hyperLogLog.update(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Unique words = " + (int) hyperLogLog.get());
    }


    private static final long Pow2_32 = 1L << 32;

    private final double alphaM;
    private final byte[] M;
    private final int b;
    private final int m;

    public HyperLogLog(int b) {
        assert b >= 4 && b <= 16;
        this.b = b;
        this.m = 1 << b;
        this.alphaM =
                b == 4 ? 0.673 // m == 16
                        : b == 5 ? 0.697 // m == 32
                        : b == 6 ? 0.709 // m == 64
                        : 0.7213 / (1 + 1.079 / m);
        this.M = new byte[m];
    }

    public void update(String value) {
        int x = hash(value);
        int j = x >>> (Integer.SIZE - b);
        M[j] = (byte) max(M[j], rank((x << b) | (1 << (b - 1)) + 1));
    }


    public int hash(String value) {
        int hash = 0;

        for (int i = 0, l = value.length(); i < l; i++) {
            hash += value.charAt(i);
            hash += hash << 10;
            hash ^= hash >> 6;
        }

        hash += hash << 3;
        hash ^= hash >> 6;
        hash += hash << 16;

        return hash;
    }

    public double get() {
        double Z = 0.0;
        for (int i = 0; i < m; ++i)
            Z += 1.0 / (1 << M[i]);

        double E = alphaM * m * m / Z;

        if (E <= (5.0 / 2.0) * m) {
            int V = 0;
            for (int v : M)
                if (v == 0) V++;
            return V == 0 ? E : m * log((float) m / V);
        } else if (E <= Pow2_32 / 30.0) {
            return E;
        } else {
            return -1 * Pow2_32 * log(1.0 - E / Pow2_32);
        }
    }

    int rank(int w) {
        return w == 0 ? 0 : 1 + numberOfLeadingZeros(w);
    }
}