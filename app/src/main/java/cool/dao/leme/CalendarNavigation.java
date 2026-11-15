package cool.dao.leme;

import java.time.YearMonth;

final class CalendarNavigation {
    private CalendarNavigation() { }

    static YearMonth shift(YearMonth current, int months) {
        if (months == 0) return current;
        try {
            YearMonth shifted = current.plusMonths(months);
            return shifted.getYear() < 1 || shifted.getYear() > 9999 ? current : shifted;
        }
        catch (RuntimeException outOfRange) { return current; }
    }
}
