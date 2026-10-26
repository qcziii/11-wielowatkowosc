package pl.kurs.threads;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Parses log lines in the format {@code "2026-03-14 09:26:53 INFO message"}.
 * Files have millions of lines, so parsing runs in parallel.
 */
public class LogParser {

    // One format object for the whole application, so that we don't create it for every line.
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public List<LogEntry> parseAll(List<String> lines) {
        return lines.parallelStream()
                .map(LogParser::parseLine)
                .toList();
    }

    static LogEntry parseLine(String line) {
        String[] parts = line.split(" ", 4);
        try {
            Date date = FORMAT.parse(parts[0] + " " + parts[1]);
            LocalDateTime time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            return new LogEntry(time, parts[2], parts[3]);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid log line: " + line, e);
        }
    }
}
