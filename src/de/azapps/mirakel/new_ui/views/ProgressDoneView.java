/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *  Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.CheckBox;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.new_ui.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class ProgressDoneView extends CheckBox implements Runnable {
    private int progress;
    private int progressColor;
    private int progressBackgroundColor;

    private final Paint backgroundPaint = new Paint();
    private final Paint circlePaint = new Paint();
    private final Paint checkBoxPaint = new Paint();

    private Drawable mButtonDrawable;
    private static Optional<Integer> checkboxLeft = absent();
    private static Optional<Integer> checkboxTop = absent();
    private static Optional<Integer> widthCheckbox = absent();
    private static Optional<Integer> heightCheckbox = absent();

    private static final boolean isJellyBeanMR1 = Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.JELLY_BEAN_MR1;

    public ProgressDoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PriorityDone,
                                0, 0);
        try {
            progress = attributes.getInt(R.styleable.PriorityDone_progress, 0);
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

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }


    @SuppressLint("NewApi")
    @Override
    public void onDraw(@NonNull final Canvas canvas) {
        final Drawable buttonDrawable = mButtonDrawable;
        if (buttonDrawable != null) {
            final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            final int drawableHeight = buttonDrawable.getIntrinsicHeight();
            final int drawableWidth = buttonDrawable.getIntrinsicWidth();
            int top;
            switch (verticalGravity) {
            case Gravity.BOTTOM:
                top = getHeight() - drawableHeight;
                break;
            case Gravity.CENTER_VERTICAL:
                top = (getHeight() - drawableHeight) / 2;
                break;
            default:
                top = 0;
                break;
            }
            //top+=dpToPx(5);
            final int bottom = top + drawableHeight;
            int left = (isJellyBeanMR1 &&
                        (getLayoutDirection() == LAYOUT_DIRECTION_RTL)) ? (getWidth() - drawableWidth) : 0;
            int right = (isJellyBeanMR1 &&
                         (getLayoutDirection() == LAYOUT_DIRECTION_RTL)) ? getWidth() : drawableWidth;
            left += dpToPx(5);
            right += dpToPx(5);
            final int x = (checkboxLeft.or(0) + (widthCheckbox.or(heightCheckbox.or(0)) / 2)) + left;
            final int y = (checkboxTop.or(0) + (heightCheckbox.or(widthCheckbox.or(0)) / 2)) + top;
            final int size = Math.max(widthCheckbox.or(0), heightCheckbox.or(0)) + dpToPx(3);
            // Background circle
            canvas.drawCircle(x, y, size, backgroundPaint);
            // Foreground arc
            final RectF oval = new RectF(x - size, y - size, x + size, y + size);
            final float sweep = (float) ((360.0 / 100.0) * progress);
            canvas.drawArc(oval, 270.0F, sweep, true, circlePaint);
            //white background for checkbox
            final RectF box = new RectF(checkboxLeft.or(0) + left, checkboxTop.or(0) + top,
                                        widthCheckbox.or(heightCheckbox.or(0)) + left + checkboxLeft.or(0),
                                        heightCheckbox.or(widthCheckbox.or(0)) + top + checkboxTop.or(0));
            canvas.drawRect(box, checkBoxPaint);
            //checkbox
            buttonDrawable.setBounds(left, top, right, bottom);
            buttonDrawable.draw(canvas);
        }
    }


    @Override
    public void setButtonDrawable(final Drawable d) {
        super.setButtonDrawable(d);
        if (d != null) {
            if (mButtonDrawable != null) {
                mButtonDrawable.setCallback(null);
            }
            d.setCallback(this);
            d.setVisible(getVisibility() == VISIBLE, false);
            mButtonDrawable = d;
            //calculate the offset for further drawing
            new Thread(this).start();
        }
    }


    public void run() {
        final Bitmap b = drawableToBitmap(mButtonDrawable);
        final int x = b.getWidth() / 2;
        final int y = b.getHeight() / 2;
        final int startColorTop = b.getPixel(x, 0);
        final int startColorLeft = b.getPixel(0, y);
        final int startColorBottom = b.getPixel(x, (2 * y) - 1);
        final int startColorRight = b.getPixel((2 * x) - 1, y);
        List<Integer> boarderTop = new ArrayList<>();
        List<Integer> boarderLeft = new ArrayList<>();
        List<Integer> boarderBottom = new ArrayList<>();
        List<Integer> boarderRight = new ArrayList<>();
        for (int i = 1; (i <= x) && (i <= y); i++) {
            if ((i < (2 * x)) && (b.getPixel(i, y) != startColorTop)) {
                boarderTop.add(i);
            } else if (!boarderTop.isEmpty()) {
                if (!checkboxTop.isPresent()) {
                    checkboxTop = of(getAVG(boarderTop) + dpToPx(3));
                    if (heightCheckbox.isPresent()) {
                        heightCheckbox = of(heightCheckbox.get() - checkboxTop.get());
                    }
                }
                boarderTop = new ArrayList<>(0);
            }
            if ((i < (2 * y)) && (b.getPixel(x, i) != startColorLeft)) {
                boarderLeft.add(i);
            } else if (!boarderLeft.isEmpty()) {
                if (!checkboxLeft.isPresent()) {
                    checkboxLeft = of(getAVG(boarderLeft) + dpToPx(3));
                    if (widthCheckbox.isPresent()) {
                        widthCheckbox = of(widthCheckbox.get() - checkboxLeft.get());
                    }
                }
                boarderLeft = new ArrayList<>(0);
            }
            if ((((2 * x) - i) > x) && (b.getPixel((2 * x) - i, y) != startColorBottom)) {
                boarderBottom.add((2 * x) - i);
            } else if (!boarderBottom.isEmpty()) {
                if (!heightCheckbox.isPresent()) {
                    if (checkboxTop.isPresent()) {
                        heightCheckbox = of(getAVG(boarderBottom) - checkboxTop.get() - dpToPx(3));
                    } else {
                        heightCheckbox = of(getAVG(boarderBottom));
                    }
                }
                boarderBottom = new ArrayList<>(0);
            }
            if ((((2 * y) - i) > y) && (b.getPixel(x, (2 * y) - i) != startColorRight)) {
                boarderRight.add((2 * y) - i);
            } else if (!boarderRight.isEmpty()) {
                if (!widthCheckbox.isPresent()) {
                    if (checkboxLeft.isPresent()) {
                        widthCheckbox = of(getAVG(boarderRight) - checkboxLeft.get() - dpToPx(3));
                    } else {
                        widthCheckbox = of(getAVG(boarderRight));
                    }
                }
                boarderRight = new ArrayList<>(0);
            }
            if (checkboxLeft.isPresent() && checkboxTop.isPresent() && widthCheckbox.isPresent() &&
                heightCheckbox.isPresent()) {
                break;
            }

        }
        invalidate();
    }

    private int getAVG(final List<Integer> boarderY) {
        int sum = 0;
        for (final int s : boarderY) {
            sum += s;
        }
        return sum / boarderY.size();
    }

    private static Bitmap drawableToBitmap(final Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                              drawable.getIntrinsicHeight(),
                              Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(final int progress) {
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

    public void setProgressColor(final int progressColor) {
        this.progressColor = progressColor;
        rebuildLayout();
    }

    public int getProgressBackgroundColor() {
        return progressBackgroundColor;
    }

    public void setProgressBackgroundColor(final int progressBackgroundColor) {
        this.progressBackgroundColor = progressBackgroundColor;
        rebuildLayout();
    }
}
