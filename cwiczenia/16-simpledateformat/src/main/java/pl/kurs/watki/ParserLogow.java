package pl.kurs.watki;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Parsuje linie logu w formacie {@code "2026-03-14 09:26:53 INFO treść"}.
 * Pliki mają miliony linii, więc parsowanie idzie równolegle.
 */
public class ParserLogow {

    // Jeden obiekt formatu na całą aplikację, żeby nie tworzyć go dla każdej linii.
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public List<Wpis> parsuj(List<String> linie) {
        return linie.parallelStream()
                .map(ParserLogow::parsujLinie)
                .toList();
    }

    static Wpis parsujLinie(String linia) {
        String[] czesci = linia.split(" ", 4);
        try {
            Date data = FORMAT.parse(czesci[0] + " " + czesci[1]);
            LocalDateTime czas = LocalDateTime.ofInstant(data.toInstant(), ZoneId.systemDefault());
            return new Wpis(czas, czesci[2], czesci[3]);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Zła linia logu: " + linia, e);
        }
    }
}
