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

package de.azapps.material_elements.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AbsSeekBar;
import android.widget.AutoCompleteTextView;
import android.widget.SeekBar;

import java.lang.reflect.Field;

import de.azapps.material_elements.R;
import de.azapps.material_elements.drawable.TextDrawable;
import de.azapps.material_elements.utils.ThemeManager;


public class Slider extends SeekBar implements SeekBar.OnSeekBarChangeListener,
    ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {


    private static final String TAG = "Slider";
    public static final long ANIMATOR_DURATION = 100L;
    @NonNull
    private TextDrawable mThumb;
    @NonNull
    private ShapeDrawable mSmallThumb = new ShapeDrawable( new OvalShape() );;
    @NonNull
    private final Drawable backgroundThumb;
    private final int bubbleSize;
    private final int smallThumbSize;
    private final ValueAnimator popupAnimator;
    private final ValueAnimator vanishAnimator;
    @Nullable
    private OnSeekBarChangeListener listener;
    boolean isThumbShown;
    private int widgetColor;

    public Slider(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.getTheme().obtainStyledAttributes(
                                 attrs,
                                 R.styleable.Slider,
                                 0, 0);

        try {
            widgetColor = a.getColor(R.styleable.Slider_widget_color, ThemeManager.getAccentThemeColor());
        } finally {
            a.recycle();
        }

        bubbleSize = (int) context.getResources().getDimension(R.dimen.bubbleSize);
        smallThumbSize = (int) context.getResources().getDimension(R.dimen.small_thumb_size);

        backgroundThumb = context.getResources().getDrawable(R.drawable.ic_marker);
        mThumb = generateThumbDrawable(bubbleSize, "");


        mSmallThumb.setIntrinsicHeight(smallThumbSize);
        mSmallThumb.setIntrinsicWidth(smallThumbSize);
        mSmallThumb.setColorFilter(widgetColor, PorterDuff.Mode.SRC_IN);
        setThumb(new ColorDrawable(Color.TRANSPARENT));

        super.setOnSeekBarChangeListener(this);

        final Drawable progress = getProgressDrawable();

        progress.setColorFilter(widgetColor, PorterDuff.Mode.SRC_IN);
        setProgressDrawable(progress);
        setPadding(0, 0, 0, 0);
        popupAnimator = new ValueAnimator();
        popupAnimator.setIntValues(0, bubbleSize);
        popupAnimator.setDuration(ANIMATOR_DURATION);
        popupAnimator.setInterpolator(new LinearInterpolator());
        popupAnimator.addUpdateListener(this);
        popupAnimator.addListener(this);

        vanishAnimator = new ValueAnimator();
        vanishAnimator.setIntValues(bubbleSize, 0);
        vanishAnimator.setDuration(ANIMATOR_DURATION);
        vanishAnimator.setInterpolator(new LinearInterpolator());
        vanishAnimator.addUpdateListener(this);
        vanishAnimator.addListener(this);
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        bringToFront();
    }

    public void setBackground(final @NonNull Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.setBackground(background);
        } else {
            setBackgroundDrawable(background);
        }

    }


    private TextDrawable generateThumbDrawable(final int size, final String text) {
        return TextDrawable.builder().beginConfig().height(size)
               .width(3 * size / 4).bold().displace(size / -8).endConfig()
               .buildWithBackground(text, widgetColor, backgroundThumb);
    }


    @Override
    public void setPadding(final int left, final int top, final int right, final int bottom) {
        final int side_offset, top_offset;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            side_offset = (int) (bubbleSize * 0.75 * 0.25);
            if (mSmallThumb != null) {// nullptr because setPadding seems to be called from constructor
                top_offset = bubbleSize / 2;
            } else {
                top_offset = 0;
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            //something is really strange here
            side_offset = (int) (bubbleSize * 0.75 * 0.125);
            top_offset = (bubbleSize / 2) - smallThumbSize;
        } else {
            side_offset = (int) (bubbleSize * 0.75 * 0.5);
            top_offset = (bubbleSize / 2) + (2 * smallThumbSize);
        }
        super.setPadding(left + side_offset, top + top_offset, right + side_offset, bottom);
    }



    @Override
    public void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        listener = l;
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (listener != null) {
            listener.onProgressChanged(seekBar, progress, fromUser);
        }
        mThumb.setNewText(String.valueOf(progress));
        if (isThumbShown) {
            mThumb.invalidateSelf();
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        bringToFront();
        if (listener != null) {
            listener.onStartTrackingTouch(seekBar);
        }
        mThumb.setNewText(String.valueOf(getProgress()));

        mThumb.setHeight(0);
        mThumb.setWidth(0);
        setThumb(new ColorDrawable(Color.TRANSPARENT));

        popupAnimator.start();
        isThumbShown = true;
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        if (listener != null) {
            listener.onStopTrackingTouch(seekBar);
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isThumbShown) {
                    vanishAnimator.start();
                }
            }
        }, ANIMATOR_DURATION);
    }

    @Override
    protected synchronized void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);
        int countOldSdk = 0;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            countOldSdk = canvas.save();
            canvas.translate(0.0F, -0.5F * smallThumbSize);
        }
        if (isThumbShown) {
            setThumbPos(0, mThumb);
            final int count = canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop() - bubbleSize * 0.75F - 0.75F * smallThumbSize);
            mThumb.draw(canvas);
            canvas.restoreToCount(count);
        }
        setThumbPos(0, mSmallThumb);
        final int count = canvas.save();
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) &&
            (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)) {
            canvas.translate(getPaddingRight(), getPaddingTop() - bubbleSize * 0.75F - 0.5F * smallThumbSize);
        } else {
            canvas.translate(getPaddingLeft() - bubbleSize * 0.75F * 0.5F,
                             getPaddingTop() - bubbleSize * 0.75F - 0.5F * smallThumbSize);
        }
        mSmallThumb.draw(canvas);
        canvas.restoreToCount(count);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            canvas.restoreToCount(countOldSdk);
        }
    }

    private double getScale() {
        final int max = getMax();
        return (max > 0) ? (getProgress() / (double) max) : 0.0;
    }


    private void setThumbPos(final int animatedValue, final @NonNull Drawable thumb) {
        int available = getWidth() - getPaddingLeft() - getPaddingRight();
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += getThumbOffset() * 2;

        int thumbPos = (int) (getScale() * available + 0.5f);
        thumbPos += (getScale() - 0.5) * thumbWidth;

        final int top =  (bubbleSize - animatedValue);
        final int bottom = top + thumbHeight;

        final int left;
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) &&
            (getResources().getConfiguration().getLayoutDirection() ==
             View.LAYOUT_DIRECTION_RTL)) {
            left = available - thumbPos;
        } else {
            left = thumbPos + (thumb.equals(mThumb) ? 0 : getPaddingRight());
        }

        final int right = left + thumbWidth;

        final Drawable background = getBackground();
        if (background != null) {
            final int offsetX = getPaddingLeft() - getThumbOffset();
            final int offsetY = getPaddingTop();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                background.setHotspotBounds(left + offsetX, top + offsetY,
                                            right + offsetX, bottom + offsetY);
            }
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, top, right, bottom);
    }

    @Override
    public void onAnimationUpdate(final ValueAnimator animation) {
        final int animatedValue = (Integer)animation.getAnimatedValue();
        mThumb.setWidth((int) (animatedValue * 0.75));
        mThumb.setHeight(animatedValue);
        setThumbPos(animatedValue, mThumb);
        mThumb.setTopPadding((-2 * bubbleSize) + (bubbleSize - animatedValue));
        invalidate();
    }

    @Override
    public void onAnimationStart(final Animator animation) {
        invalidate();
    }

    @Override
    public void onAnimationEnd(final Animator animation) {
        if (mThumb.getIntrinsicHeight() != bubbleSize) {
            isThumbShown = false;
        } else {
            mThumb.setTopPadding(-2 * bubbleSize);
        }
        invalidate();
    }

    @Override
    public void onAnimationCancel(final Animator animation) {
    }

    @Override
    public void onAnimationRepeat(final Animator animation) {

    }
}
