package pl.kurs.watki;

import java.util.Iterator;

/**
 * Przetwarza strumień zamówień we wzorcu producent-konsument.
 * <ul>
 *     <li>Jeden wątek producenta czyta zamówienia ze źródła i wkłada je do kolejki o pojemności {@code pojemnoscKolejki}.
 *         Gdy kolejka jest pełna, producent czeka (nie czyta źródła na zapas).</li>
 *     <li>{@code liczbaKonsumentow} wątków konsumentów wyjmuje zamówienia z kolejki i woła {@link ObslugaZamowienia#obsluz}.</li>
 *     <li>Wyjątek z obsługi to błąd tego jednego zamówienia: liczy się w raporcie, konsument pracuje dalej.</li>
 *     <li>{@link #przetworz} wraca, gdy wszystkie zamówienia ze źródła są obsłużone i wszystkie wątki
 *         producenta i konsumentów są zakończone.</li>
 * </ul>
 */
public class PrzetwarzaczZamowien {

    private final int liczbaKonsumentow;
    private final int pojemnoscKolejki;
    private final ObslugaZamowienia obsluga;

    public PrzetwarzaczZamowien(int liczbaKonsumentow, int pojemnoscKolejki, ObslugaZamowienia obsluga) {
        this.liczbaKonsumentow = liczbaKonsumentow;
        this.pojemnoscKolejki = pojemnoscKolejki;
        this.obsluga = obsluga;
    }

    /**
     * Przetwarza wszystkie zamówienia ze źródła. Źródło (iterator) nie jest bezpieczne dla wielu wątków:
     * czyta je tylko wątek producenta.
     */
    public Raport przetworz(Iterator<Zamowienie> zrodlo) throws InterruptedException {
        // TODO
        throw new UnsupportedOperationException("TODO: zaimplementuj przetworz");
    }
}
