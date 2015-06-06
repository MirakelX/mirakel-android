package com.sleepbot.datetimepicker.time;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;

import com.nineoldandroids.animation.Keyframe;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;

/**
 * A view to show a series of numbers in a circular pattern.
 */
public class RadialTextsView extends View {
    private final static String TAG = "RadialTextsView";

    private final Paint mPaint = new Paint();

    private boolean mDrawValuesReady;
    private boolean mIsInitialized;

    private Typeface mTypefaceLight;
    private Typeface mTypefaceRegular;
    private String[] mTexts;
    private String[] mInnerTexts;
    private boolean mIs24HourMode;
    private boolean mHasInnerCircle;
    private float mCircleRadiusMultiplier;
    private float mAmPmCircleRadiusMultiplier;
    private float mNumbersRadiusMultiplier;
    private float mInnerNumbersRadiusMultiplier;
    private float mTextSizeMultiplier;
    private float mInnerTextSizeMultiplier;

    private int mXCenter;
    private int mYCenter;
    private float mCircleRadius;
    private boolean mTextGridValuesDirty;
    private float mTextSize;
    private float mInnerTextSize;
    private float[] mTextGridHeights;
    private float[] mTextGridWidths;
    private float[] mInnerTextGridHeights;
    private float[] mInnerTextGridWidths;

    private float mAnimationRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    ObjectAnimator mDisappearAnimator;
    ObjectAnimator mReappearAnimator;
    private InvalidateUpdateListener mInvalidateUpdateListener;

    public RadialTextsView(final Context context) {
        super(context);
        this.mIsInitialized = false;
    }

    public void initialize(final Resources res, final String[] texts,
                           final String[] innerTexts, final boolean is24HourMode,
                           final boolean disappearsOut) {
        if (this.mIsInitialized) {
            Log.e(TAG, "This RadialTextsView may only be initialized once.");
            return;
        }
        // Set up the paint.
        final int numbersTextColor = ThemeManager.getColor(R.attr.colorTextBlack);
        this.mPaint.setColor(numbersTextColor);
        final String typefaceFamily = res
                                      .getString(R.string.radial_numbers_typeface);
        this.mTypefaceLight = Typeface.create(typefaceFamily, Typeface.NORMAL);
        final String typefaceFamilyRegular = res.getString(R.string.sans_serif);
        this.mTypefaceRegular = Typeface.create(typefaceFamilyRegular,
                                                Typeface.NORMAL);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextAlign(Align.CENTER);
        this.mTexts = texts;
        this.mInnerTexts = innerTexts;
        this.mIs24HourMode = is24HourMode;
        this.mHasInnerCircle = innerTexts != null;
        // Calculate the radius for the main circle.
        if (is24HourMode) {
            this.mCircleRadiusMultiplier = Float.parseFloat(res
                                           .getString(R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(res
                                           .getString(R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res
                                               .getString(R.string.ampm_circle_radius_multiplier));
        }
        // Initialize the widths and heights of the grid, and calculate the
        // values for the numbers.
        this.mTextGridHeights = new float[7];
        this.mTextGridWidths = new float[7];
        if (this.mHasInnerCircle) {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res
                                            .getString(R.string.numbers_radius_multiplier_outer));
            this.mTextSizeMultiplier = Float.parseFloat(res
                                       .getString(R.string.text_size_multiplier_outer));
            this.mInnerNumbersRadiusMultiplier = Float.parseFloat(res
                                                 .getString(R.string.numbers_radius_multiplier_inner));
            this.mInnerTextSizeMultiplier = Float.parseFloat(res
                                            .getString(R.string.text_size_multiplier_inner));
            this.mInnerTextGridHeights = new float[7];
            this.mInnerTextGridWidths = new float[7];
        } else {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res
                                            .getString(R.string.numbers_radius_multiplier_normal));
            this.mTextSizeMultiplier = Float.parseFloat(res
                                       .getString(R.string.text_size_multiplier_normal));
        }
        this.mAnimationRadiusMultiplier = 1.0F;
        this.mTransitionMidRadiusMultiplier = 1.0F + 0.05F * (disappearsOut ? -1
                                              : 1);
        this.mTransitionEndRadiusMultiplier = 1.0F + 0.3F * (disappearsOut ? 1
                                              : -1);
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        this.mTextGridValuesDirty = true;
        this.mIsInitialized = true;
    }

    /**
     * Allows for smoother animation.
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Used by the animation to move the numbers in and out.
     */
    public void setAnimationRadiusMultiplier(
        final float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier = animationRadiusMultiplier;
        this.mTextGridValuesDirty = true;
    }

    @Override
    public void onDraw(final Canvas canvas) {
        final int viewWidth = getWidth();
        if (viewWidth == 0 || !this.mIsInitialized) {
            return;
        }
        if (!this.mDrawValuesReady) {
            this.mXCenter = getWidth() / 2;
            this.mYCenter = getHeight() / 2;
            this.mCircleRadius = Math.min(this.mXCenter, this.mYCenter)
                                 * this.mCircleRadiusMultiplier;
            if (!this.mIs24HourMode) {
                // We'll need to draw the AM/PM circles, so the main circle will
                // need to have
                // a slightly higher center. To keep the entire view centered
                // vertically, we'll
                // have to push it up by half the radius of the AM/PM circles.
                final float amPmCircleRadius = this.mCircleRadius
                                               * this.mAmPmCircleRadiusMultiplier;
                this.mYCenter -= amPmCircleRadius / 2.0F;
            }
            this.mTextSize = this.mCircleRadius * this.mTextSizeMultiplier;
            if (this.mHasInnerCircle) {
                this.mInnerTextSize = this.mCircleRadius
                                      * this.mInnerTextSizeMultiplier;
            }
            // Because the text positions will be static, pre-render the
            // animations.
            renderAnimations();
            this.mTextGridValuesDirty = true;
            this.mDrawValuesReady = true;
        }
        // Calculate the text positions, but only if they've changed since the
        // last onDraw.
        if (this.mTextGridValuesDirty) {
            final float numbersRadius = this.mCircleRadius
                                        * this.mNumbersRadiusMultiplier
                                        * this.mAnimationRadiusMultiplier;
            // Calculate the positions for the 12 numbers in the main circle.
            calculateGridSizes(numbersRadius, this.mXCenter, this.mYCenter,
                               this.mTextSize, this.mTextGridHeights, this.mTextGridWidths);
            if (this.mHasInnerCircle) {
                // If we have an inner circle, calculate those positions too.
                final float innerNumbersRadius = this.mCircleRadius
                                                 * this.mInnerNumbersRadiusMultiplier
                                                 * this.mAnimationRadiusMultiplier;
                calculateGridSizes(innerNumbersRadius, this.mXCenter,
                                   this.mYCenter, this.mInnerTextSize,
                                   this.mInnerTextGridHeights, this.mInnerTextGridWidths);
            }
            this.mTextGridValuesDirty = false;
        }
        // Draw the texts in the pre-calculated positions.
        drawTexts(canvas, this.mTextSize, this.mTypefaceLight, this.mTexts,
                  this.mTextGridWidths, this.mTextGridHeights);
        if (this.mHasInnerCircle) {
            drawTexts(canvas, this.mInnerTextSize, this.mTypefaceRegular,
                      this.mInnerTexts, this.mInnerTextGridWidths,
                      this.mInnerTextGridHeights);
        }
    }

    /**
     * Using the trigonometric Unit Circle, calculate the positions that the
     * text will need to be drawn at based on the specified circle radius. Place
     * the values in the textGridHeights and textGridWidths parameters.
     */
    private void calculateGridSizes(final float numbersRadius,
                                    final float xCenter, float yCenter, final float textSize,
                                    final float[] textGridHeights, final float[] textGridWidths) {
        /*
         * The numbers need to be drawn in a 7x7 grid, representing the points
         * on the Unit Circle.
         */
        // cos(30) = a / r => r * cos(30) = a => r * √3/2 = a
        final float offset2 = numbersRadius * (float) Math.sqrt(3.0) / 2.0F;
        // sin(30) = o / r => r * sin(30) = o => r / 2 = a
        final float offset3 = numbersRadius / 2.0F;
        this.mPaint.setTextSize(textSize);
        // We'll need yTextBase to be slightly lower to account for the text's
        // baseline.
        yCenter -= (this.mPaint.descent() + this.mPaint.ascent()) / 2.0F;
        textGridHeights[0] = yCenter - numbersRadius;
        textGridWidths[0] = xCenter - numbersRadius;
        textGridHeights[1] = yCenter - offset2;
        textGridWidths[1] = xCenter - offset2;
        textGridHeights[2] = yCenter - offset3;
        textGridWidths[2] = xCenter - offset3;
        textGridHeights[3] = yCenter;
        textGridWidths[3] = xCenter;
        textGridHeights[4] = yCenter + offset3;
        textGridWidths[4] = xCenter + offset3;
        textGridHeights[5] = yCenter + offset2;
        textGridWidths[5] = xCenter + offset2;
        textGridHeights[6] = yCenter + numbersRadius;
        textGridWidths[6] = xCenter + numbersRadius;
    }

    /**
     * Draw the 12 text values at the positions specified by the textGrid
     * parameters.
     */
    private void drawTexts(final Canvas canvas, final float textSize,
                           final Typeface typeface, final String[] texts,
                           final float[] textGridWidths, final float[] textGridHeights) {
        this.mPaint.setTextSize(textSize);
        this.mPaint.setTypeface(typeface);
        canvas.drawText(texts[0], textGridWidths[3], textGridHeights[0],
                        this.mPaint);
        canvas.drawText(texts[1], textGridWidths[4], textGridHeights[1],
                        this.mPaint);
        canvas.drawText(texts[2], textGridWidths[5], textGridHeights[2],
                        this.mPaint);
        canvas.drawText(texts[3], textGridWidths[6], textGridHeights[3],
                        this.mPaint);
        canvas.drawText(texts[4], textGridWidths[5], textGridHeights[4],
                        this.mPaint);
        canvas.drawText(texts[5], textGridWidths[4], textGridHeights[5],
                        this.mPaint);
        canvas.drawText(texts[6], textGridWidths[3], textGridHeights[6],
                        this.mPaint);
        canvas.drawText(texts[7], textGridWidths[2], textGridHeights[5],
                        this.mPaint);
        canvas.drawText(texts[8], textGridWidths[1], textGridHeights[4],
                        this.mPaint);
        canvas.drawText(texts[9], textGridWidths[0], textGridHeights[3],
                        this.mPaint);
        canvas.drawText(texts[10], textGridWidths[1], textGridHeights[2],
                        this.mPaint);
        canvas.drawText(texts[11], textGridWidths[2], textGridHeights[1],
                        this.mPaint);
    }

    /**
     * Render the animations for appearing and disappearing.
     */
    private void renderAnimations() {
        float midwayPoint = 0.2F;
        // Set up animator for disappearing.
        Keyframe kf0 = Keyframe.ofFloat(0.0F, 1.0F);
        Keyframe kf1 = Keyframe
                       .ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
        Keyframe kf2 = Keyframe.ofFloat(1.0F, this.mTransitionEndRadiusMultiplier);
        final PropertyValuesHolder radiusDisappear = PropertyValuesHolder
                .ofKeyframe("animationRadiusMultiplier", kf0, kf1, kf2);
        kf0 = Keyframe.ofFloat(0.0F, 1.0F);
        kf1 = Keyframe.ofFloat(1.0F, 0.0F);
        final PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe(
                "alpha", kf0, kf1);
        final int duration = 500;
        this.mDisappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this,
                                  radiusDisappear, fadeOut).setDuration(duration);
        this.mDisappearAnimator
        .addUpdateListener(this.mInvalidateUpdateListener);
        // Set up animator for reappearing.
        final float delayMultiplier = 0.25F;
        final float transitionDurationMultiplier = 1.0F;
        final float totalDurationMultiplier = transitionDurationMultiplier
                                              + delayMultiplier;
        final int totalDuration = (int) (duration * totalDurationMultiplier);
        final float delayPoint = delayMultiplier * duration / totalDuration;
        midwayPoint = 1.0F - midwayPoint * (1.0F - delayPoint);
        kf0 = Keyframe.ofFloat(0.0F, this.mTransitionEndRadiusMultiplier);
        kf1 = Keyframe.ofFloat(delayPoint, this.mTransitionEndRadiusMultiplier);
        kf2 = Keyframe
              .ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
        final Keyframe kf3 = Keyframe.ofFloat(1.0F, 1.0F);
        final PropertyValuesHolder radiusReappear = PropertyValuesHolder
                .ofKeyframe("animationRadiusMultiplier", kf0, kf1, kf2, kf3);
        kf0 = Keyframe.ofFloat(0.0F, 0.0F);
        kf1 = Keyframe.ofFloat(delayPoint, 0.0F);
        kf2 = Keyframe.ofFloat(1.0F, 1.0F);
        final PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe(
                                                "alpha", kf0, kf1, kf2);
        this.mReappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this,
                                 radiusReappear, fadeIn).setDuration(totalDuration);
        this.mReappearAnimator
        .addUpdateListener(this.mInvalidateUpdateListener);
    }

    public ObjectAnimator getDisappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady
            || this.mDisappearAnimator == null) {
            Log.e(TAG, "RadialTextView was not ready for animation.");
            return null;
        }
        return this.mDisappearAnimator;
    }

    public ObjectAnimator getReappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady
            || this.mReappearAnimator == null) {
            Log.e(TAG, "RadialTextView was not ready for animation.");
            return null;
        }
        return this.mReappearAnimator;
    }

    private class InvalidateUpdateListener implements
        ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
            RadialTextsView.this.invalidate();
        }
    }
}