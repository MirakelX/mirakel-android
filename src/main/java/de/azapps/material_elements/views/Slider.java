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
import android.graphics.PorterDuff;
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
import android.widget.SeekBar;

import java.lang.reflect.Field;

import de.azapps.material_elements.R;
import de.azapps.material_elements.drawable.TextDrawable;
import de.azapps.material_elements.utils.ThemeManager;


public class Slider extends SeekBar implements SeekBar.OnSeekBarChangeListener, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {


    private static final String TAG = "Slider";
    public static final long ANIMATOR_DURATION = 100L;
    @NonNull
    private TextDrawable mThumb;
    @NonNull
    private Drawable mNoThumb;
    @NonNull
    private ShapeDrawable mSmallThumb;
    @NonNull
    private final Drawable backgroundThumb;
    private final int bubbleSize;
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
        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Slider,
                0, 0);

        try {
            widgetColor=a.getColor(R.styleable.Slider_widget_color,ThemeManager.getAccentThemeColor());
        } finally {
            a.recycle();
        }

        bubbleSize = (int) context.getResources().getDimension(R.dimen.bubbleSize);


        backgroundThumb=context.getResources().getDrawable(R.drawable.ic_marker);
        mThumb= generateThumbDrawable(bubbleSize, "");
        mThumb.setTopPadding(-1 * bubbleSize);
        try {
            final Field originalThumb = AbsSeekBar.class.getDeclaredField("mThumb");
            originalThumb.setAccessible(true);
            mNoThumb= (Drawable) originalThumb.get(this);

        } catch (final NoSuchFieldException e) {
            Log.wtf(TAG,"mThumb not found",e);
        } catch (final IllegalAccessException e) {
            Log.wtf(TAG,"could not access mThumb",e);
        }
        mNoThumb.setColorFilter(widgetColor, PorterDuff.Mode.SRC_IN);
        setThumb(mNoThumb);
        mSmallThumb=new ShapeDrawable( new OvalShape() );
        mSmallThumb.setIntrinsicHeight(mNoThumb.getIntrinsicHeight()/3);
        mSmallThumb.setIntrinsicWidth(mNoThumb.getIntrinsicWidth()/3);
        mSmallThumb.setColorFilter(widgetColor,PorterDuff.Mode.SRC_IN);

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

        bringToFront();
    }

    private TextDrawable generateThumbDrawable(final int size,final String text) {
        return TextDrawable.builder().beginConfig().height(size)
                .width(3 * size / 4).bold().displace(size / -8).endConfig()
                .buildWithBackground(text, widgetColor, backgroundThumb);
    }


    @Override
    public void setPadding(final int left, final int top, final int right, final int bottom) {
        mThumb.setTopPadding(top - bubbleSize / 2-mSmallThumb.getIntrinsicHeight());
        mSmallThumb.setPadding(0,top-bubbleSize,0,0);
        super.setPadding(left+bubbleSize/2, top + bubbleSize/2+7*mSmallThumb.getIntrinsicHeight()/6, right+bubbleSize/2, bottom);
    }


    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = getMeasuredWidth();
    }

    @Override
    public void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        listener=l;
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if(listener!=null){
            listener.onProgressChanged(seekBar, progress, fromUser);
        }
        mThumb.setNewText(String.valueOf(progress));
        final double scale= getScale() -0.5;
        mThumb.setLeft((int) (((scale * mThumb.getIntrinsicWidth()) / 2.0) + (((-1.0 * Math.abs(scale)) + 0.5) * ((scale < 0) ? -1 : 1))));
        if(isThumbShown) {
            mThumb.invalidateSelf();
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        bringToFront();
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
                if (isThumbShown) {
                    vanishAnimator.start();
                }
                isThumbShown = false;
            }
        }, 100L);
    }

    @Override
    protected synchronized void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);
        if(isThumbShown){
            setThumbPos(0, mSmallThumb);
            mSmallThumb.draw(canvas);
        }

    }

    private double getScale() {
        final int max = getMax();
        return (max > 0) ? (getProgress() / (double) max) : 0.0;
    }


    private void setThumbPos(final int animatedValue,final @NonNull Drawable thumb){
        int available = getWidth() - getPaddingLeft() - getPaddingRight();
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += getThumbOffset() * 2;

        int thumbPos = (int) (getScale() * available + 0.5f);
        thumbPos+=(getScale()-0.5)*thumbWidth;

        final int top =  (bubbleSize - animatedValue);
        final int bottom =top + thumbHeight;

        final int left;
        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) && (getResources().getConfiguration().getLayoutDirection() ==
                View.LAYOUT_DIRECTION_RTL)){
            left= available-thumbPos;
        }else {
            left = thumbPos+(thumb.equals(mThumb)?0:getPaddingRight());
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
        thumb.setBounds(left, top, right, bottom);
    }

    @Override
    public void onAnimationUpdate(final ValueAnimator animation) {
        final int animatedValue = (Integer)animation.getAnimatedValue();
        mThumb.setWidth((int) (animatedValue * 0.75));
        mThumb.setHeight(animatedValue);
        setThumbPos(animatedValue, mThumb);
        mThumb.setLeft(0);
        mThumb.invalidateSelf();
    }

    @Override
    public void onAnimationStart(final Animator animation) {

    }

    @Override
    public void onAnimationEnd(final Animator animation) {
        if(!isThumbShown){
            setThumb(mNoThumb);
        }
        mThumb.setLeft(0);
    }

    @Override
    public void onAnimationCancel(final Animator animation) {
    }

    @Override
    public void onAnimationRepeat(final Animator animation) {

    }
}
