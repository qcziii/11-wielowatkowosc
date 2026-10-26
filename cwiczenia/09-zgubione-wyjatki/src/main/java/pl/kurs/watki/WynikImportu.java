package pl.kurs.watki;

import java.util.List;

/**
 * @param zaimportowane liczba wierszy zapisanych w cenniku
 * @param bledy         po jednym komunikacie na każdy błędny wiersz, w kolejności wierszy, w formacie
 *                      {@code "wiersz N: opis"} (N liczone od 1)
 */
public record WynikImportu(int zaimportowane, List<String> bledy) {
}
