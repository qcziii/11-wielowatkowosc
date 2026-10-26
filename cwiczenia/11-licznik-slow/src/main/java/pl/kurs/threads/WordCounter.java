package pl.kurs.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts word occurrences in a text. Lines are split between threads, all threads write to one map.
 */
public class WordCounter {

    public Map<String, Long> count(List<String> lines, int threadCount) throws InterruptedException {
        Map<String, Long> occurrences = new ConcurrentHashMap<>();
        List<Thread> threads = new ArrayList<>();

        for (int w = 0; w < threadCount; w++) {
            final int threadIdx = w;
            Thread t = new Thread(() -> {
                for (int i = threadIdx; i < lines.size(); i += threadCount) {
                    for (String word : lines.get(i).toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                        if (word.isEmpty()) {
                            continue;
                        }
                        Long count = occurrences.get(word);
                        if (count == null) {
                            occurrences.put(word, 1L);
                        } else {
                            occurrences.put(word, count + 1);
                        }
                    }
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        return occurrences;
    }
}
