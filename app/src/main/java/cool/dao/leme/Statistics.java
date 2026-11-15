package cool.dao.leme;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class Statistics {
    private Statistics() { }

    public static final class Entry {
        public final String state;
        public final String typeName;

        public Entry(String state, String typeName) {
            this.state = state == null ? "" : state;
            this.typeName = typeName == null ? "" : typeName;
        }
    }

    public static final class Range {
        public final LocalDate start;
        public final LocalDate end;

        public Range(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    public static final class Counts {
        public final int done;
        public final int missed;
        public final int pending;
        public final int elapsedDays;
        public final Map<String, Integer> types;

        public Counts(int done, int missed, int pending, int elapsedDays, Map<String, Integer> types) {
            this.done = done;
            this.missed = missed;
            this.pending = pending;
            this.elapsedDays = elapsedDays;
            this.types = types;
        }

        public int recorded() { return done + missed; }
        public int completionPercent() { return elapsedDays == 0 ? 0 : Math.round(recorded() * 100f / elapsedDays); }
    }

    public static final class ComparisonBucket {
        public final String label;
        public final Counts counts;

        public ComparisonBucket(String label, Counts counts) {
            this.label = label;
            this.counts = counts;
        }
    }

    public static Range range(String period, LocalDate focus, LocalDate firstRecord, LocalDate today) {
        if ("week".equals(period)) {
            LocalDate start = focus.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new Range(start, start.plusDays(6));
        }
        if ("month".equals(period)) {
            YearMonth month = YearMonth.from(focus);
            return new Range(month.atDay(1), month.atEndOfMonth());
        }
        if ("year".equals(period)) {
            return new Range(focus.withDayOfYear(1), focus.withDayOfYear(focus.lengthOfYear()));
        }
        return firstRecord == null ? null : new Range(firstRecord, today);
    }

    public static Counts count(Map<String, Entry> records, Range range, LocalDate today) {
        int done = 0;
        int missed = 0;
        int elapsed = 0;
        TreeMap<String, Integer> types = new TreeMap<>();
        if (range == null || range.start.isAfter(today)) return new Counts(0, 0, 0, 0, types);
        LocalDate end = range.end.isAfter(today) ? today : range.end;
        for (LocalDate date = range.start; !date.isAfter(end); date = date.plusDays(1)) {
            elapsed++;
            Entry entry = records.get(date.toString());
            if (entry == null) continue;
            if ("导了".equals(entry.state)) {
                done++;
                if (!entry.typeName.isEmpty()) types.put(entry.typeName, types.getOrDefault(entry.typeName, 0) + 1);
            } else if ("没导".equals(entry.state)) {
                missed++;
            }
        }
        return new Counts(done, missed, Math.max(0, elapsed - done - missed), elapsed, types);
    }

    public static List<ComparisonBucket> recentComparison(Map<String, Entry> records, String period, LocalDate today) {
        List<ComparisonBucket> result = new ArrayList<>();
        if ("total".equals(period)) return result;
        if ("week".equals(period)) {
            LocalDate current = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int offset = 5; offset >= 0; offset--) {
                LocalDate start = current.minusWeeks(offset);
                LocalDate end = start.plusDays(6);
                DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("M/d", Locale.ROOT);
                String label = start.format(labelFormat) + "–" + end.format(labelFormat);
                result.add(new ComparisonBucket(label, count(records, new Range(start, end), today)));
            }
        } else if ("year".equals(period)) {
            int current = today.getYear();
            for (int year = current - 5; year <= current; year++) {
                LocalDate start = LocalDate.of(year, 1, 1);
                LocalDate end = LocalDate.of(year, 12, 31);
                result.add(new ComparisonBucket(year + "年", count(records, new Range(start, end), today)));
            }
        } else {
            YearMonth current = YearMonth.from(today);
            for (int offset = 5; offset >= 0; offset--) {
                YearMonth month = current.minusMonths(offset);
                result.add(new ComparisonBucket(month.getMonthValue() + "月", count(records, new Range(month.atDay(1), month.atEndOfMonth()), today)));
            }
        }
        return result;
    }

    public static LocalDate firstRecordDate(Map<String, Entry> records) {
        return records.keySet().stream().map(LocalDate::parse).min(Comparator.naturalOrder()).orElse(null);
    }
}
