package pl.kurs.watki;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CheckTest {

    @RepeatedTest(value = 3, name = "(próba {currentRepetition} z {totalRepetitions})")
    @DisplayName("1) Po zatrzymaj() wątek generatora kończy się w ciągu 2 s")
    void zatrzymuje() throws Exception {
        Generator g = new Generator();
        Thread t = new Thread(g, "generator");
        t.setDaemon(true); // gdyby wątek zawisł, nie blokuje końca testów
        t.start();
        Thread.sleep(500); // generator chwilę pracuje (JIT zdąży skompilować pętlę)
        g.zatrzymaj();
        t.join(2000);
        assertFalse(t.isAlive(), "Wątek generatora dalej działa 2 s po zatrzymaj(), stan: " + t.getState());
        assertTrue(g.kroki() > 0, "Generator nie wykonał żadnego kroku");
    }
}
