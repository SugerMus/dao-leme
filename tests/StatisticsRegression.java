package cool.dao.leme;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatisticsRegression {
    public static void main(String[] args) {
        LocalDate today = LocalDate.parse("2026-11-14");
        Map<String, Statistics.Entry> records = new LinkedHashMap<>();
        records.put("2026-11-09", new Statistics.Entry("导了", "晚间"));
        records.put("2026-11-10", new Statistics.Entry("没导", ""));
        records.put("2026-11-14", new Statistics.Entry("导了", "晚间"));
        records.put("2026-11-15", new Statistics.Entry("没导", ""));
        records.put("2026-11-02", new Statistics.Entry("导了", "早晨"));
        records.put("2025-06-01", new Statistics.Entry("没导", ""));
        records.put("2021-04-01", new Statistics.Entry("导了", "早晨"));

        Statistics.Counts week = Statistics.count(records, Statistics.range("week", today, null, today), today);
        equal(6, week.elapsedDays, "week elapsed days stop at today");
        equal(2, week.done, "week done count");
        equal(1, week.missed, "week missed count");
        equal(3, week.pending, "week pending count");
        equal(2, week.types.get("晚间"), "week type count");

        Statistics.Counts month = Statistics.count(records, Statistics.range("month", today, null, today), today);
        equal(14, month.elapsedDays, "month elapsed days stop at today");
        equal(10, month.pending, "month pending count");

        LocalDate firstRecord = Statistics.firstRecordDate(records);
        Statistics.Counts total = Statistics.count(records, Statistics.range("total", today, firstRecord, today), today);
        equal(4, total.done, "total done count");
        equal(2, total.missed, "total missed count excludes future records");
        equal((int) java.time.temporal.ChronoUnit.DAYS.between(firstRecord, today) + 1 - 6, total.pending, "total pending count");

        java.util.List<Statistics.ComparisonBucket> months = Statistics.recentComparison(records, "month", today);
        equal(6, months.size(), "six-month comparison window");
        equal("11月", months.get(5).label, "monthly comparison ends at current month");
        equal(4, months.get(5).counts.recorded(), "current-month recorded-day total");
        java.util.List<Statistics.ComparisonBucket> weeks = Statistics.recentComparison(records, "week", today);
        equal(6, weeks.size(), "six-week comparison window");
        equal("11/9–11/15", weeks.get(5).label, "weekly comparison uses Monday start and Sunday end");
        equal(2, weeks.get(5).counts.done, "current-week done count excludes future records");
        java.util.List<Statistics.ComparisonBucket> years = Statistics.recentComparison(records, "year", today);
        equal(6, years.size(), "six-year comparison window");
        equal("2021年", years.get(0).label, "year comparison starts six calendar years back");
        equal(318, years.get(5).counts.elapsedDays, "current year comparison stops at today");
        equal(0, Statistics.recentComparison(records, "total", today).size(), "total period has no misleading six-month comparison");
        System.out.println("Statistics regression checks passed.");
    }

    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
