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
import android.util.Log;
import android.view.View;

import com.nineoldandroids.animation.Keyframe;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;

/**
 * View to show what number is selected. This will draw a blue circle over the
 * number, with a blue line coming from the center of the main circle to the
 * edge of the blue selection.
 */
public class RadialSelectorView extends View {
    /**
     * We'll need to invalidate during the animation.
     */
    private class InvalidateUpdateListener implements
        ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
            RadialSelectorView.this.invalidate();
        }
    }

    private static final String TAG = "RadialSelectorView";

    private float mAmPmCircleRadiusMultiplier;
    private float mAnimationRadiusMultiplier;

    private int mCircleRadius;
    private float mCircleRadiusMultiplier;
    private boolean mDrawValuesReady;
    private boolean mForceDrawDot;
    private boolean mHasInnerCircle;
    private float mInnerNumbersRadiusMultiplier;
    private InvalidateUpdateListener mInvalidateUpdateListener;
    private boolean mIs24HourMode;

    private boolean mIsInitialized;
    private int mLineLength;
    private float mNumbersRadiusMultiplier;
    private float mOuterNumbersRadiusMultiplier;
    private final Paint mPaint = new Paint();
    private int mSelectionDegrees;
    private double mSelectionRadians;
    private int mSelectionRadius;

    private float mSelectionRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;

    private int mXCenter;

    private int mYCenter;

    public RadialSelectorView(final Context context) {
        super(context);
        this.mIsInitialized = false;
    }

    public int getDegreesFromCoords(final float pointX, final float pointY,
                                    final boolean forceLegal, final Boolean[] isInnerCircle) {
        if (!this.mDrawValuesReady) {
            return -1;
        }
        final double hypotenuse = Math.sqrt((pointY - this.mYCenter)
                                            * (pointY - this.mYCenter) + (pointX - this.mXCenter)
                                            * (pointX - this.mXCenter));
        // Check if we're outside the range
        if (this.mHasInnerCircle) {
            if (forceLegal) {
                // If we're told to force the coordinates to be legal, we'll set
                // the isInnerCircle
                // boolean based based off whichever number the coordinates are
                // closer to.
                final int innerNumberRadius = (int) (this.mCircleRadius * this.mInnerNumbersRadiusMultiplier);
                final int distanceToInnerNumber = (int) Math.abs(hypotenuse
                                                  - innerNumberRadius);
                final int outerNumberRadius = (int) (this.mCircleRadius * this.mOuterNumbersRadiusMultiplier);
                final int distanceToOuterNumber = (int) Math.abs(hypotenuse
                                                  - outerNumberRadius);
                isInnerCircle[0] = distanceToInnerNumber <= distanceToOuterNumber;
            } else {
                // Otherwise, if we're close enough to either number (with the
                // space between the
                // two allotted equally), set the isInnerCircle boolean as the
                // closer one.
                // appropriately, but otherwise return -1.
                final int minAllowedHypotenuseForInnerNumber = (int) (this.mCircleRadius *
                        this.mInnerNumbersRadiusMultiplier)
                        - this.mSelectionRadius;
                final int maxAllowedHypotenuseForOuterNumber = (int) (this.mCircleRadius *
                        this.mOuterNumbersRadiusMultiplier)
                        + this.mSelectionRadius;
                final int halfwayHypotenusePoint = (int) (this.mCircleRadius * ((this.mOuterNumbersRadiusMultiplier
                                                   + this.mInnerNumbersRadiusMultiplier) / 2.0F));
                if (hypotenuse >= minAllowedHypotenuseForInnerNumber
                    && hypotenuse <= halfwayHypotenusePoint) {
                    isInnerCircle[0] = true;
                } else if (hypotenuse <= maxAllowedHypotenuseForOuterNumber
                           && hypotenuse >= halfwayHypotenusePoint) {
                    isInnerCircle[0] = false;
                } else {
                    return -1;
                }
            }
        } else {
            // If there's just one circle, we'll need to return -1 if:
            // we're not told to force the coordinates to be legal, and
            // the coordinates' distance to the number is within the allowed
            // distance.
            if (!forceLegal) {
                final int distanceToNumber = (int) Math.abs(hypotenuse
                                             - this.mLineLength);
                // The max allowed distance will be defined as the distance from
                // the center of the
                // number to the edge of the circle.
                final int maxAllowedDistance = (int) (this.mCircleRadius * (1.0F - this.mNumbersRadiusMultiplier));
                if (distanceToNumber > maxAllowedDistance) {
                    return -1;
                }
            }
        }
        final float opposite = Math.abs(pointY - this.mYCenter);
        final double radians = Math.asin(opposite / hypotenuse);
        int degrees = (int) (radians * 180.0D / Math.PI);
        // Now we have to translate to the correct quadrant.
        final boolean rightSide = pointX > this.mXCenter;
        final boolean topSide = pointY < this.mYCenter;
        if (rightSide && topSide) {
            degrees = 90 - degrees;
        } else if (rightSide) {
            degrees = 90 + degrees;
        } else if (!topSide) {
            degrees = 270 - degrees;
        } else {
            degrees = 270 + degrees;
        }
        return degrees;
    }

    public ObjectAnimator getDisappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady) {
            Log.e(TAG, "RadialSelectorView was not ready for animation.");
            return null;
        }
        Keyframe kf0 = Keyframe.ofFloat(0.0F, 1.0F);
        final float midwayPoint = 0.2F;
        Keyframe kf1 = Keyframe
                       .ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
        final Keyframe kf2 = Keyframe.ofFloat(1.0F, this.mTransitionEndRadiusMultiplier);
        final PropertyValuesHolder radiusDisappear = PropertyValuesHolder
                .ofKeyframe("animationRadiusMultiplier", kf0, kf1, kf2);
        kf0 = Keyframe.ofFloat(0.0F, 1.0F);
        kf1 = Keyframe.ofFloat(1.0F, 0.0F);
        final PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe(
                "alpha", kf0, kf1);
        final int duration = 500;
        final ObjectAnimator disappearAnimator = ObjectAnimator
                .ofPropertyValuesHolder(this, radiusDisappear, fadeOut)
                .setDuration(duration);
        disappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
        return disappearAnimator;
    }

    public ObjectAnimator getReappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady) {
            Log.e(TAG, "RadialSelectorView was not ready for animation.");
            return null;
        }
        float midwayPoint = 0.2f;
        final int duration = 500;
        // The time points are half of what they would normally be, because this
        // animation is
        // staggered against the disappear so they happen seamlessly. The
        // reappear starts
        // halfway into the disappear.
        final float delayMultiplier = 0.25F;
        final float transitionDurationMultiplier = 1.0F;
        final float totalDurationMultiplier = transitionDurationMultiplier
                                              + delayMultiplier;
        final int totalDuration = (int) (duration * totalDurationMultiplier);
        final float delayPoint = delayMultiplier * duration / totalDuration;
        midwayPoint = 1.0F - midwayPoint * (1.0F - delayPoint);
        Keyframe kf0 = Keyframe.ofFloat(0.0F, this.mTransitionEndRadiusMultiplier);
        Keyframe kf1 = Keyframe.ofFloat(delayPoint, this.mTransitionEndRadiusMultiplier);
        Keyframe kf2 = Keyframe
                       .ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
        final Keyframe kf3 = Keyframe.ofFloat(1.0F, 1.0F);
        final PropertyValuesHolder radiusReappear = PropertyValuesHolder
                .ofKeyframe("animationRadiusMultiplier", kf0, kf1, kf2, kf3);
        kf0 = Keyframe.ofFloat(0.0F, 0.0F);
        kf1 = Keyframe.ofFloat(delayPoint, 0.0F);
        kf2 = Keyframe.ofFloat(1.0F, 1.0F);
        final PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe(
                                                "alpha", kf0, kf1, kf2);
        final ObjectAnimator reappearAnimator = ObjectAnimator
                                                .ofPropertyValuesHolder(this, radiusReappear, fadeIn)
                                                .setDuration(totalDuration);
        reappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
        return reappearAnimator;
    }

    /**
     * Allows for smoother animations.
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Initialize this selector with the state of the picker.
     *
     * @param context
     *            Current context.
     * @param is24HourMode
     *            Whether the selector is in 24-hour mode, which will tell us
     *            whether the circle's center is moved up slightly to make room
     *            for the AM/PM circles.
     * @param hasInnerCircle
     *            Whether we have both an inner and an outer circle of numbers
     *            that may be selected. Should be true for 24-hour mode in the
     *            hours circle.
     * @param disappearsOut
     *            Whether the numbers' animation will have them disappearing out
     *            or disappearing in.
     * @param selectionDegrees
     *            The initial degrees to be selected.
     * @param isInnerCircle
     *            Whether the initial selection is in the inner or outer circle.
     *            Will be ignored when hasInnerCircle is false.
     */
    public void initialize(final Context context, final boolean is24HourMode,
                           final boolean hasInnerCircle, final boolean disappearsOut,
                           final int selectionDegrees, final boolean isInnerCircle) {
        if (this.mIsInitialized) {
            Log.e(TAG, "This RadialSelectorView may only be initialized once.");
            return;
        }
        final Resources res = context.getResources();
        final int selectorColor = ThemeManager.getPrimaryThemeColor();
        this.mPaint.setColor(selectorColor);
        this.mPaint.setAntiAlias(true);
        // Calculate values for the circle radius size.
        this.mIs24HourMode = is24HourMode;
        if (is24HourMode) {
            this.mCircleRadiusMultiplier = Float.parseFloat(res
                                           .getString(R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(res
                                           .getString(R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res
                                               .getString(R.string.ampm_circle_radius_multiplier));
        }
        // Calculate values for the radius size(s) of the numbers circle(s).
        this.mHasInnerCircle = hasInnerCircle;
        if (hasInnerCircle) {
            this.mInnerNumbersRadiusMultiplier = Float.parseFloat(res
                                                 .getString(R.string.numbers_radius_multiplier_inner));
            this.mOuterNumbersRadiusMultiplier = Float.parseFloat(res
                                                 .getString(R.string.numbers_radius_multiplier_outer));
        } else {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res
                                            .getString(R.string.numbers_radius_multiplier_normal));
        }
        this.mSelectionRadiusMultiplier = Float.parseFloat(res
                                          .getString(R.string.selection_radius_multiplier));
        // Calculate values for the transition mid-way states.
        this.mAnimationRadiusMultiplier = 1.0F;
        this.mTransitionMidRadiusMultiplier = 1.0f + 0.05f * (disappearsOut ? -1
                                              : 1);
        this.mTransitionEndRadiusMultiplier = 1.0f + 0.3f * (disappearsOut ? 1
                                              : -1);
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        setSelection(selectionDegrees, isInnerCircle, false);
        this.mIsInitialized = true;
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
            this.mCircleRadius = (int) (Math.min(this.mXCenter, this.mYCenter) * this.mCircleRadiusMultiplier);
            if (!this.mIs24HourMode) {
                // We'll need to draw the AM/PM circles, so the main circle will
                // need to have
                // a slightly higher center. To keep the entire view centered
                // vertically, we'll
                // have to push it up by half the radius of the AM/PM circles.
                final int amPmCircleRadius = (int) (this.mCircleRadius * this.mAmPmCircleRadiusMultiplier);
                this.mYCenter -= amPmCircleRadius / 2;
            }
            this.mSelectionRadius = (int) (this.mCircleRadius * this.mSelectionRadiusMultiplier);
            this.mDrawValuesReady = true;
        }
        // Calculate the current radius at which to place the selection circle.
        this.mLineLength = (int) (this.mCircleRadius
                                  * this.mNumbersRadiusMultiplier * this.mAnimationRadiusMultiplier);
        int pointX = this.mXCenter
                     + (int) (this.mLineLength * Math.sin(this.mSelectionRadians));
        int pointY = this.mYCenter
                     - (int) (this.mLineLength * Math.cos(this.mSelectionRadians));
        // Draw the selection circle.
        this.mPaint.setAlpha(75);
        canvas.drawCircle(pointX, pointY, this.mSelectionRadius, this.mPaint);
        if (this.mForceDrawDot | this.mSelectionDegrees % 30 != 0) {
            // We're not on a direct tick (or we've been told to draw the dot
            // anyway).
            this.mPaint.setAlpha(255);
            canvas.drawCircle(pointX, pointY, this.mSelectionRadius * 2 / 7,
                              this.mPaint);
        } else {
            // We're not drawing the dot, so shorten the line to only go as far
            // as the edge of the
            // selection circle.
            int lineLength = this.mLineLength;
            lineLength -= this.mSelectionRadius;
            pointX = this.mXCenter
                     + (int) (lineLength * Math.sin(this.mSelectionRadians));
            pointY = this.mYCenter
                     - (int) (lineLength * Math.cos(this.mSelectionRadians));
        }
        // Draw the line from the center of the circle.
        this.mPaint.setAlpha(255);
        this.mPaint.setStrokeWidth(1.0F);
        canvas.drawLine(this.mXCenter, this.mYCenter, pointX, pointY,
                        this.mPaint);
    }

    /**
     * Set the multiplier for the radius. Will be used during animations to move
     * in/out.
     */
    public void setAnimationRadiusMultiplier(
        final float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier = animationRadiusMultiplier;
    }

    /**
     * Set the selection.
     *
     * @param selectionDegrees
     *            The degrees to be selected.
     * @param isInnerCircle
     *            Whether the selection should be in the inner circle or outer.
     *            Will be ignored if hasInnerCircle was initialized to false.
     * @param forceDrawDot
     *            Whether to force the dot in the center of the selection circle
     *            to be drawn. If false, the dot will be drawn only when the
     *            degrees is not a multiple of 30, i.e. the selection is not on
     *            a visible number.
     */
    public void setSelection(final int selectionDegrees,
                             final boolean isInnerCircle, final boolean forceDrawDot) {
        this.mSelectionDegrees = selectionDegrees;
        this.mSelectionRadians = selectionDegrees * Math.PI / 180.0D;
        this.mForceDrawDot = forceDrawDot;
        if (this.mHasInnerCircle) {
            if (isInnerCircle) {
                this.mNumbersRadiusMultiplier = this.mInnerNumbersRadiusMultiplier;
            } else {
                this.mNumbersRadiusMultiplier = this.mOuterNumbersRadiusMultiplier;
            }
        }
    }
}