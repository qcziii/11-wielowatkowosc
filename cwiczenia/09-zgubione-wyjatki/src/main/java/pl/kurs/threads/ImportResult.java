package pl.kurs.threads;

import java.util.List;

/**
 * @param imported number of rows saved to the price list
 * @param errors   one message per invalid row, in row order, in the format
 *                 {@code "line N: description"} (N counted from 1)
 */
public record ImportResult(int imported, List<String> errors) {
}
