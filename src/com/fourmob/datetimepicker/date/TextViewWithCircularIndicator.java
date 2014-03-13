package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;

public class TextViewWithCircularIndicator extends TextView {
	private final int mCircleColor;
	Paint mCirclePaint = new Paint();
	private final boolean mDark;
	private boolean mDrawCircle;
	private final String mItemIsSelectedText;
	private int mSeclectedTextColor;
	private int mUnselectedTextColor;

	public TextViewWithCircularIndicator(Context context,
			AttributeSet attributeSet) {
		super(context, attributeSet);
		Resources localResources = context.getResources();
		this.mDark = MirakelCommonPreferences.isDark();
		this.mCircleColor = localResources.getColor(this.mDark ? R.color.Red
				: R.color.blue);
		this.mSeclectedTextColor = localResources
				.getColor(this.mDark ? R.color.Red : R.color.clock_blue);
		this.mItemIsSelectedText = context.getResources().getString(
				R.string.item_is_selected);
		this.mUnselectedTextColor = context.getResources().getColor(
				this.mDark ? R.color.white : R.color.dialog_dark_gray);
		init();
	}

	public void drawIndicator(boolean drawIndicator) {
		this.mDrawCircle = drawIndicator;
	}

	@Override
	public CharSequence getContentDescription() {
		CharSequence text = getText();
		if (this.mDrawCircle) {
			text = String.format(this.mItemIsSelectedText, text);
		}
		return text;
	}

	private void init() {
		this.mCirclePaint.setFakeBoldText(true);
		this.mCirclePaint.setAntiAlias(true);
		this.mCirclePaint.setColor(this.mCircleColor);
		this.mCirclePaint.setTextAlign(Paint.Align.CENTER);
		this.mCirclePaint.setStyle(Paint.Style.FILL);
		this.mCirclePaint.setAlpha(this.mDark ? 80 : 60);
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (this.mDrawCircle) {
			int width = getWidth();
			int heigth = getHeight();
			int radius = Math.min(width, heigth) / 2;
			canvas.drawCircle(width / 2, heigth / 2, radius, this.mCirclePaint);
			setTextColor(this.mSeclectedTextColor, true);
		} else {
			setTextColor(this.mUnselectedTextColor, true);
		}
	}

	@Override
	public void setTextColor(int color) {
		setTextColor(color, false);
	}

	private void setTextColor(int color, boolean intern) {
		super.setTextColor(color);
		if (!intern) {
			this.mSeclectedTextColor = color;
			this.mUnselectedTextColor = color;
		}
	}
}