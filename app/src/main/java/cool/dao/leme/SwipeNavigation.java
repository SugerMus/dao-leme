package cool.dao.leme;

/** Tracks one horizontal swipe and reports the period navigation direction. */
final class SwipeNavigation {
    static final int NONE = 0;
    static final int PREVIOUS = -1;
    static final int NEXT = 1;

    private final float threshold;
    private float startX;
    private float startY;
    private boolean tracking;
    private boolean horizontal;

    SwipeNavigation(float threshold) {
        this.threshold = Math.max(1f, threshold);
    }

    void begin(float x, float y) {
        startX = x;
        startY = y;
        tracking = true;
        horizontal = false;
    }

    boolean update(float x, float y) {
        if (!tracking || horizontal) return horizontal;
        float dx = x - startX;
        float dy = y - startY;
        horizontal = Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.2f;
        return horizontal;
    }

    int finish(float x, float y) {
        boolean recognized = update(x, y);
        int direction = !recognized ? NONE : x < startX ? NEXT : PREVIOUS;
        cancel();
        return direction;
    }

    void cancel() {
        tracking = false;
        horizontal = false;
    }
}
