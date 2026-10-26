package pl.kurs.threads;

/**
 * Sums the elements of an array by splitting it into parts. Each part is summed by a separate thread.
 */
public class ParallelSum {

    public long sum(long[] array, int threadCount) {
        long[] partSums = new long[threadCount];
        Thread[] threads = new Thread[threadCount];
        int partSize = array.length / threadCount;

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            final int from = i * partSize;
            final int toExclusive = from + partSize;
            threads[i] = new Thread(() -> {
                long s = 0;
                for (int j = from; j < toExclusive; j++) {
                    s += array[j];
                }
                partSums[idx] = s;
            }, "sum-" + i);
            threads[i].start();
        }

        long result = 0;
        for (long s : partSums) {
            result += s;
        }
        return result;
    }
}
