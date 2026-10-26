package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 15, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    private final PrzetwarzanieRownolegle p = new PrzetwarzanieRownolegle();

    /** Funkcja testowa: chwilę „pracuje” i zapisuje, ile wywołań działa naraz i w jakich wątkach. */
    static class Obserwator<T, R> implements Function<T, R> {
        final Function<T, R> f;
        final long msPracy;
        final AtomicInteger teraz = new AtomicInteger();
        final AtomicInteger maks = new AtomicInteger();
        final Set<Thread> watki = ConcurrentHashMap.newKeySet();

        Obserwator(long msPracy, Function<T, R> f) {
            this.msPracy = msPracy;
            this.f = f;
        }

        @Override
        public R apply(T t) {
            watki.add(Thread.currentThread());
            maks.accumulateAndGet(teraz.incrementAndGet(), Math::max);
            try {
                Thread.sleep(msPracy);
                return f.apply(t);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("przerwano", e);
            } finally {
                teraz.decrementAndGet();
            }
        }

        void sprawdzZeWatkiZakonczone() throws InterruptedException {
            for (Thread t : watki) {
                t.join(1000);
                assertFalse(t.isAlive(), "Wątek puli " + t.getName() + " dalej działa po powrocie z przetworz() (pula nie zamknięta?)");
            }
        }
    }

    @Test
    @DisplayName("1) Wyniki w kolejności danych wejściowych")
    void kolejnosc() {
        List<Integer> dane = IntStream.rangeClosed(1, 50).boxed().toList();
        // Wcześniejsze elementy pracują dłużej, więc kończą się później.
        Function<Integer, String> f = i -> {
            try {
                Thread.sleep(60 - i);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "#" + i;
        };
        List<String> wynik = p.przetworz(dane, f, 4);
        assertEquals(dane.stream().map(i -> "#" + i).toList(), wynik);
    }

    @Test
    @DisplayName("2) Równolegle, ale nie więcej niż liczbaWatkow naraz (24 elementy po 100 ms, 4 wątki)")
    void rownolegle() {
        Obserwator<Integer, Integer> f = new Obserwator<>(100, i -> i * i);
        long start = System.nanoTime();
        List<Integer> wynik = p.przetworz(IntStream.range(0, 24).boxed().toList(), f, 4);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(IntStream.range(0, 24).map(i -> i * i).boxed().toList(), wynik);
        assertEquals(4, f.maks.get(), "Naraz działało maksymalnie " + f.maks.get() + " wywołań, a miały 4");
        assertTrue(ms < 1500, "24 x 100 ms na 4 wątkach to ok. 600 ms, trwało " + ms + " ms");
    }

    @Test
    @DisplayName("3) Po powrocie nie zostają działające wątki puli")
    void watkiZakonczone() throws Exception {
        Obserwator<Integer, Integer> f = new Obserwator<>(10, i -> i);
        p.przetworz(IntStream.range(0, 20).boxed().toList(), f, 3);
        f.sprawdzZeWatkiZakonczone();
    }

    @Test
    @DisplayName("4) Wyjątek z funkcji: PrzetwarzanieException z oryginalną przyczyną, pula też zamknięta")
    void wyjatek() throws Exception {
        Obserwator<Integer, Integer> f = new Obserwator<>(10, i -> {
            if (i == 7) {
                throw new IllegalArgumentException("zły element 7");
            }
            return i;
        });
        PrzetwarzanieException e = assertThrows(PrzetwarzanieException.class,
                () -> p.przetworz(IntStream.range(0, 20).boxed().toList(), f, 3));
        assertInstanceOf(IllegalArgumentException.class, e.getCause(),
                "Przyczyną ma być oryginalny wyjątek z funkcji, a jest: " + e.getCause());
        assertEquals("zły element 7", e.getCause().getMessage());
        f.sprawdzZeWatkiZakonczone();
    }

    @Test
    @DisplayName("5) Pusta lista: pusty wynik")
    void pusta() {
        assertEquals(List.of(), p.przetworz(List.<Integer>of(), i -> i, 4));
    }
}
