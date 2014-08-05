package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import de.azapps.mirakel.new_ui.R;

public class ProgressDoneView extends RelativeLayout {
	private int progress;
	private boolean done;
	private int progressColor, progressBackgroundColor;
	private int size=dpToPx(40);

	private CheckBox checkBox;

	public ProgressDoneView(Context context) {
		this(context, null);
	}

	public ProgressDoneView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ProgressDoneView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		inflate(context, R.layout.priority_done, this);

		checkBox=(CheckBox) findViewById(R.id.priority_done_checkbox);

		TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PriorityDone, 0,0);
		try {
			progress = attributes.getInt(R.styleable.PriorityDone_progress, 0);
			done = attributes.getBoolean(R.styleable.PriorityDone_done, false);
			progressColor = attributes.getInt(R.styleable.PriorityDone_progress_color, 0);
			progressBackgroundColor = attributes.getInt(R.styleable.PriorityDone_progress_background_color, 0);
		} finally {
			attributes.recycle();
		}
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDone(!done);
			}
		});
	}

	private int dpToPx(int px) {
		return (int) getResources().getDisplayMetrics().density * px;
	}

	@Override
	public void dispatchDraw(Canvas canvas) {
		checkBox.setChecked(done);
		drawProgress(canvas);
		super.dispatchDraw(canvas);
	}

	private void drawProgress(Canvas canvas) {
		// Background circle
		Paint backgroundPaint = new Paint();
		backgroundPaint.setColor(progressBackgroundColor);
		backgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		canvas.drawCircle(size / 2, size / 2, size / 2, backgroundPaint);

		// Foreground arc
		RectF oval = new RectF(0,0,size,size);
		Paint paint = new Paint();
		paint.setColor(progressColor);
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		float sweep = (float) ((360.0/100.0) * progress);
		canvas.drawArc(oval, 270, sweep, true, paint);
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
		invalidate();
		requestLayout();
	}

	public boolean isDone() {
		return done;
	}

	private void rebuildLayout() {
		invalidate();
		requestLayout();
	}

	public void setDone(boolean done) {
		this.done = done;
		rebuildLayout();
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

	public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
		this.checkBox.setOnCheckedChangeListener(listener);
		rebuildLayout();
	}
}
