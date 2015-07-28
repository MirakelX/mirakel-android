/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakelandroid.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class ProgressDoneView extends AppCompatCheckBox implements Runnable {
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


    public ProgressDoneView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                                      R.styleable.ProgressDone,
                                      0, 0);
        try {
            progress = attributes.getInt(R.styleable.ProgressDone_progress_value, 0);
            progressColor = attributes.getInt(R.styleable.ProgressDone_progress_color, 0);
            progressBackgroundColor = attributes.getInt(R.styleable.ProgressDone_progress_background_color, 0);
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



    @SuppressLint("NewApi")
    @Override
    public void onDraw(@NonNull final Canvas canvas) {
        final Drawable buttonDrawable = mButtonDrawable;
        if (buttonDrawable != null) {
            final int save = canvas.save();
            int x = getWidth() / 2;
            int y = getHeight() / 2;
            final int size = Math.min(x, y);
            // Background circle
            canvas.drawCircle(y, x, size, backgroundPaint);
            // Foreground arc
            final RectF oval = new RectF(x - size, y - size, x + size, y + size);
            final float sweep = (float) ((360.0 / 100.0) * progress);
            canvas.drawArc(oval, 270.0F, sweep, true, circlePaint);
            //white background for checkbox
            final int h = heightCheckbox.or(widthCheckbox.or(0)) / 2 + 1;
            final int w = widthCheckbox.or(heightCheckbox.or(0)) / 2 + 1;
            final RectF box = new RectF(x - w, y - h, w + x, h + y);
            canvas.drawRect(box, checkBoxPaint);

            //checkbox
            canvas.translate(x - buttonDrawable.getIntrinsicWidth() / 2,
                             y - buttonDrawable.getIntrinsicHeight() / 2);
            buttonDrawable.draw(canvas);
            canvas.restoreToCount(save);
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
        final int w = b.getWidth();
        final int h = b.getHeight();
        final int x = w / 2;
        final int y = h / 2;
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
                    checkboxTop = of(getAVG(boarderTop) + (int)ViewHelper.dpToPx(getContext(), 3.0F));
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
                    checkboxLeft = of(getAVG(boarderLeft) + (int)ViewHelper.dpToPx(getContext(), 3.0F));
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
                        heightCheckbox = of(getAVG(boarderBottom) - checkboxTop.get() - (int)ViewHelper.dpToPx(getContext(),
                                            3.0F));
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
                        widthCheckbox = of(getAVG(boarderRight) - checkboxLeft.get() - (int)ViewHelper.dpToPx(getContext(),
                                           3.0F
                                                                                                             ));
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
        post(new Runnable() {
            @Override
            public void run() {
                if (checkboxLeft.isPresent()) {
                    checkboxLeft = of(checkboxLeft.get() - 1);
                }
                if (checkboxTop.isPresent()) {
                    checkboxTop = of(checkboxTop.get() - 1);
                }
                widthCheckbox = of(widthCheckbox.or(0) + 1);
                heightCheckbox = of(heightCheckbox.or(0) + 1);
                invalidate();
            }
        });
    }

    private static int getAVG(final List<Integer> boarderY) {
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
