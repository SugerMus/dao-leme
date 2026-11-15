package cool.dao.leme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class ComparisonRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Statistics.Counts counts;
    private int accent;
    private int negative;
    private int track;
    private int text;

    ComparisonRingView(Context context, Statistics.Counts counts, int accent, int negative, int track, int text, String label) {
        super(context);
        this.counts = counts;
        this.accent = accent;
        this.negative = negative;
        this.track = track;
        this.text = text;
        setContentDescription(label + "：导了 " + counts.done + " 天，没导 " + counts.missed + " 天");
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float stroke = 6 * density;
        float diameter = Math.min(getWidth(), getHeight()) - stroke - 2 * density;
        float left = (getWidth() - diameter) / 2f + stroke / 2f;
        float top = (getHeight() - diameter) / 2f + stroke / 2f;
        RectF oval = new RectF(left, top, left + diameter, top + diameter);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(track);
        canvas.drawArc(oval, 0, 360, false, paint);

        int recorded = counts.recorded();
        if (recorded > 0) {
            float doneSweep = 360f * counts.done / recorded;
            paint.setColor(accent);
            if (counts.done > 0) canvas.drawArc(oval, -90, doneSweep, false, paint);
            paint.setColor(negative);
            if (counts.missed > 0) canvas.drawArc(oval, -90 + doneSweep, 360f - doneSweep, false, paint);
        }

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        String doneCount = String.valueOf(counts.done);
        float textSize = 21 * density;
        float maxTextWidth = Math.max(1f, diameter - stroke * 2 - 8 * density);
        paint.setTextSize(textSize);
        while (paint.measureText(doneCount) > maxTextWidth && textSize > 14 * density) {
            textSize -= density;
            paint.setTextSize(textSize);
        }
        paint.setColor(text);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(doneCount, centerX, baseline, paint);
    }
}
