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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

import de.azapps.material_elements.R;
import de.azapps.material_elements.drawable.TextDrawable;
import de.azapps.material_elements.utils.ThemeManager;


public class Slider extends SeekBar implements SeekBar.OnSeekBarChangeListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {


    private static final String TAG = "Slider";
    public static final long ANIMATOR_DURATION = 100L;
    @NonNull
    private TextDrawable mThumb;
    @NonNull
    private final ColorDrawable mNoThumb;
    @NonNull
    private final Drawable backgroundThumb;
    private final int bubbleSize;
    private final int paddingOffset;
    private final ValueAnimator popupAnimator;
    private final ValueAnimator vanishAnimator;
    @Nullable
    private OnSeekBarChangeListener listener;
    boolean isThumbShown;
    private int width;
    private int height;
    private int widgetColor;

    public Slider(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Slider,
                0, 0);

        try {
            widgetColor=a.getColor(R.styleable.Slider_widget_color,ThemeManager.getAccentThemeColor());
        } finally {
            a.recycle();
        }

        bubbleSize = (int) context.getResources().getDimension(R.dimen.bubbleSize);
        paddingOffset=21*bubbleSize/20;


        backgroundThumb=context.getResources().getDrawable(R.drawable.ic_marker);
        mThumb= generateThumbDrawable(bubbleSize,"");
        mNoThumb=new ColorDrawable(Color.TRANSPARENT);

        setThumb(mNoThumb);
        super.setOnSeekBarChangeListener(this);

        final Drawable progress=getProgressDrawable();

        progress.setColorFilter(widgetColor, PorterDuff.Mode.SRC_IN);
        setProgressDrawable(progress);
        setPadding(0,0,0,0);
        popupAnimator = new ValueAnimator();
        popupAnimator.setIntValues(0, bubbleSize);
        popupAnimator.setDuration(ANIMATOR_DURATION);
        popupAnimator.setInterpolator(new LinearInterpolator());
        popupAnimator.addUpdateListener(this);
        popupAnimator.addListener(this);

        vanishAnimator = new ValueAnimator();
        vanishAnimator.setIntValues(bubbleSize,0);
        vanishAnimator.setDuration(ANIMATOR_DURATION);
        vanishAnimator.setInterpolator(new LinearInterpolator());
        vanishAnimator.addUpdateListener(this);
        vanishAnimator.addListener(this);
    }

    private TextDrawable generateThumbDrawable(final int size,final String text) {
        return TextDrawable.builder().beginConfig().height(size)
                .width(3 * size / 4).bold().displace(size / -8).endConfig()
                .buildWithBackground(text, widgetColor, backgroundThumb);
    }


    @Override
    public void setPadding(final int left, final int top, final int right, final int bottom) {
        super.setPadding(left, top, right, bottom-paddingOffset);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = (int) ((getMeasuredHeight() + (2 * bubbleSize)) - (isThumbShown ? (0.70 * mThumb.getHeight()) : 0));
        setMeasuredDimension(width, height);
    }

    @Override
    public void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        listener=l;
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if(listener!=null){
            listener.onProgressChanged(seekBar,progress,fromUser);
        }
        mThumb.setNewText(String.valueOf(progress));
        if(isThumbShown) {
            mThumb.invalidateSelf();
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        if(listener!=null){
            listener.onStartTrackingTouch(seekBar);
        }
        mThumb.setNewText(String.valueOf(getProgress()));

        mThumb.setHeight(0);
        mThumb.setWidth(0);
        setThumb(mThumb);

        popupAnimator.start();
        isThumbShown=true;
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        if(listener!=null){
            listener.onStopTrackingTouch(seekBar);
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isThumbShown){
                    vanishAnimator.start();
                }
                isThumbShown=false;
            }
        }, 100L);
    }


    private float getScale() {
        final int max = getMax();
        return (max > 0) ? (getProgress() / (float) max) : 0.0F;
    }


    private void setThumbPos(int animatedValue) {
        int available = width - getPaddingLeft() - getPaddingRight();
        final int thumbWidth = mThumb.getWidth();
        final int thumbHeight = mThumb.getHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += getThumbOffset() * 2;

        final int thumbPos = (int) (getScale() * available + 0.5f);

        final int top =  (bubbleSize - animatedValue);
        final int bottom =top+ thumbHeight;

        final int left;
        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) && (getResources().getConfiguration().getLayoutDirection() ==
                View.LAYOUT_DIRECTION_RTL)){
            left= available-thumbPos;
        }else {
            left = thumbPos;
        }

        final int right = left + thumbWidth;

        final Drawable background = getBackground();
        if (background != null) {
            final int offsetX = getPaddingLeft() - getThumbOffset();
            final int offsetY = getPaddingTop();
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                background.setHotspotBounds(left + offsetX, top + offsetY,
                        right + offsetX, bottom + offsetY);
            }
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        mThumb.setBounds(left, top, right, bottom);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        final int animatedValue = (Integer)animation.getAnimatedValue();
        mThumb.setWidth((int) (animatedValue*0.75));
        mThumb.setHeight(animatedValue);
        setThumbPos(animatedValue);
        mThumb.invalidateSelf();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if(!isThumbShown){
            setThumb(mNoThumb);
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
