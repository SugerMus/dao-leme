package cool.dao.leme;

import java.time.YearMonth;

public final class CalendarNavigationRegression {
    public static void main(String[] args) {
        YearMonth december = YearMonth.of(2026, 12);
        equal(YearMonth.of(2027, 1), CalendarNavigation.shift(december, 1), "December swipe to next year");
        equal(YearMonth.of(2026, 11), CalendarNavigation.shift(december, -1), "December swipe to previous month");
        equal(YearMonth.of(1, 1), CalendarNavigation.shift(YearMonth.of(1, 1), -1), "lower calendar bound");
        equal(YearMonth.of(9999, 12), CalendarNavigation.shift(YearMonth.of(9999, 12), 1), "upper calendar bound");
        System.out.println("Calendar boundary regression checks passed.");
    }

    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
}
