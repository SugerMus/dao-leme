package cool.dao.leme;

import java.time.YearMonth;

public final class CalendarSwipeRegression {
    public static void main(String[] args) {
        SwipeNavigation swipe = new SwipeNavigation(48f);
        swipe.begin(720f, 800f);
        if (!swipe.update(600f, 806f)) throw new AssertionError("left swipe should be recognized");
        int direction = swipe.finish(600f, 806f);
        if (direction != SwipeNavigation.NEXT) throw new AssertionError("left swipe should move to next month");
        YearMonth next = CalendarNavigation.shift(YearMonth.of(2026, 12), direction);
        if (!YearMonth.of(2027, 1).equals(next)) throw new AssertionError("December swipe should cross into January");

        swipe.begin(600f, 800f);
        if (!swipe.update(720f, 806f)) throw new AssertionError("right swipe should be recognized");
        direction = swipe.finish(720f, 806f);
        if (direction != SwipeNavigation.PREVIOUS) throw new AssertionError("right swipe should move to previous month");
        if (!YearMonth.of(2026, 12).equals(CalendarNavigation.shift(next, direction))) {
            throw new AssertionError("right swipe should return from January to December");
        }
        System.out.println("Calendar swipe regression checks passed.");
    }
}
