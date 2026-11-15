package cool.dao.leme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

/** Samples only the page behind the dock, so navigation labels stay sharp. */
final class GlassDock extends LinearLayout {
    private static final int SAMPLE = 6;
    private static final int RADIUS = 3;
    private final View source;
    private Drawable overlay;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final int[] sourcePosition = new int[2];
    private final int[] dockPosition = new int[2];
    private Bitmap snapshot;
    private int[] pixels;
    private int[] scratch;

    GlassDock(Context context, View source, Drawable overlay) {
        super(context);
        this.source = source;
        this.overlay = overlay;
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getHeight() / 2f);
            }
        });
    }

    void setOverlay(Drawable overlay) {
        this.overlay = overlay;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0 || source.getWidth() == 0) return;
        int width = Math.max(1, getWidth() / SAMPLE);
        int height = Math.max(1, getHeight() / SAMPLE);
        if (snapshot == null || snapshot.getWidth() != width || snapshot.getHeight() != height) {
            if (snapshot != null) snapshot.recycle();
            snapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pixels = new int[width * height];
            scratch = new int[width * height];
        }
        source.getLocationInWindow(sourcePosition);
        getLocationInWindow(dockPosition);
        snapshot.eraseColor(0);
        Canvas capture = new Canvas(snapshot);
        capture.scale(1f / SAMPLE, 1f / SAMPLE);
        capture.translate(sourcePosition[0] - dockPosition[0], sourcePosition[1] - dockPosition[1]);
        source.draw(capture);
        snapshot.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int pass = 0; pass < 3; pass++) {
            blur(pixels, scratch, width, height, true);
            blur(scratch, pixels, width, height, false);
        }
        snapshot.setPixels(pixels, 0, width, 0, 0, width, height);
        canvas.drawBitmap(snapshot, null, new RectF(0, 0, getWidth(), getHeight()), paint);
        if (overlay != null) {
            overlay.setBounds(0, 0, getWidth(), getHeight());
            overlay.draw(canvas);
        }
    }

    private static void blur(int[] input, int[] output, int width, int height, boolean horizontal) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = 0, r = 0, g = 0, b = 0;
                for (int offset = -RADIUS; offset <= RADIUS; offset++) {
                    int xx = horizontal ? Math.min(width - 1, Math.max(0, x + offset)) : x;
                    int yy = horizontal ? y : Math.min(height - 1, Math.max(0, y + offset));
                    int color = input[yy * width + xx];
                    a += color >>> 24;
                    r += (color >> 16) & 255;
                    g += (color >> 8) & 255;
                    b += color & 255;
                }
                int samples = RADIUS * 2 + 1;
                output[y * width + x] = ((a / samples) << 24) | ((r / samples) << 16) | ((g / samples) << 8) | (b / samples);
            }
        }
    }
}
