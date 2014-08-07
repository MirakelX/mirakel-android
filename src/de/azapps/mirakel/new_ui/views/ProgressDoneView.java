package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.CheckBox;

import de.azapps.mirakel.new_ui.R;

public class ProgressDoneView extends CheckBox {
    private int progress;
    private boolean done;
    private int progressColor, progressBackgroundColor;
    private int size = dpToPx(40);

    private final Paint backgroundPaint = new Paint();
    private final Paint circlePaint = new Paint();
    private final Paint checkBoxPaint = new Paint();

    public ProgressDoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PriorityDone,
                                0, 0);
        try {
            progress = attributes.getInt(R.styleable.PriorityDone_progress, 0);
            done = attributes.getBoolean(R.styleable.PriorityDone_done, false);
            progressColor = attributes.getInt(R.styleable.PriorityDone_progress_color, 0);
            progressBackgroundColor = attributes.getInt(R.styleable.PriorityDone_progress_background_color, 0);
        } finally {
            attributes.recycle();
        }
        //setup paints here because its better for performance
        backgroundPaint.setColor(progressBackgroundColor);
        backgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(progressColor);
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        checkBoxPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        checkBoxPaint.setColor(Color.WHITE);
    }


    private int dpToPx(int px) {
        return (int) getResources().getDisplayMetrics().density * px;
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawProgress(canvas);
        canvas.translate(dpToPx(4), 0);
        super.onDraw(canvas);
    }

    private void drawProgress(Canvas canvas) {
        // Background circle
        canvas.drawCircle(size / 2, size / 2, size / 2, backgroundPaint);
        // Foreground arc
        RectF oval = new RectF(0, 0, size, size);
        float sweep = (float) ((360.0 / 100.0) * progress);
        canvas.drawArc(oval, 270, sweep, true, circlePaint);
        //white background for checkbox
        RectF box = new RectF(dpToPx(12), dpToPx(12), dpToPx(28), dpToPx(28));
        canvas.drawRect(box, checkBoxPaint);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
        requestLayout();
    }

    private void rebuildLayout() {
        invalidate();
        requestLayout();
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
        rebuildLayout();
    }

    public int getProgressBackgroundColor() {
        return progressBackgroundColor;
    }

    public void setProgressBackgroundColor(int progressBackgroundColor) {
        this.progressBackgroundColor = progressBackgroundColor;
        rebuildLayout();
    }
}
