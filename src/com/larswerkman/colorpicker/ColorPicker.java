/*
 * Copyright 2012 Lars Werkman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larswerkman.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import de.azapps.mirakel.colorpicker.R;

/**
 * Displays a holo-themed color picker.
 *
 * <p>
 * Use {@link #getColor()} to retrieve the selected color. <br>
 * Use {@link #addSVBar(SVBar)} to add a Saturation/Value Bar. <br>
 * Use {@link #addOpacityBar(OpacityBar)} to add a Opacity Bar.
 * </p>
 */
public class ColorPicker extends View {
    /*
     * Constants used to save/restore the instance state.
     */
    private static final String STATE_PARENT = "parent";
    private static final String STATE_ANGLE = "angle";
    private static final String STATE_OLD_COLOR = "color";

    /**
     * Colors to construct the color wheel using {@link SweepGradient}.
     *
     * <p>
     * Note: The algorithm in {@link #normalizeColor(int)} highly depends on
     * these exact values. Be aware that {@link #setColor(int)} might break if
     * you change this array.
     * </p>
     */
    private static final int[] COLORS = new int[] { 0xFFFF0000, 0xFFFF00FF,
            0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000
                                                  };

    /**
     * {@code Paint} instance used to draw the color wheel.
     */
    private Paint mColorWheelPaint;

    /**
     * {@code Paint} instance used to draw the pointer's "halo".
     */
    private Paint mPointerHaloPaint;

    /**
     * {@code Paint} instance used to draw the pointer (the selected color).
     */
    private Paint mPointerColor;

    /**
     * The width of the color wheel thickness.
     */
    private int mColorWheelThickness;

    /**
     * The radius of the color wheel.
     */
    private int mColorWheelRadius;
    private int mPreferredColorWheelRadius;

    /**
     * The radius of the center circle inside the color wheel.
     */
    private int mColorCenterRadius;
    private int mPreferredColorCenterRadius;

    /**
     * The radius of the halo of the center circle inside the color wheel.
     */
    private int mColorCenterHaloRadius;
    private int mPreferredColorCenterHaloRadius;

    /**
     * The radius of the pointer.
     */
    private int mColorPointerRadius;

    /**
     * The radius of the halo of the pointer.
     */
    private int mColorPointerHaloRadius;

    /**
     * The rectangle enclosing the color wheel.
     */
    private final RectF mColorWheelRectangle = new RectF();

    /**
     * The rectangle enclosing the center inside the color wheel.
     */
    private final RectF mCenterRectangle = new RectF();

    /**
     * {@code true} if the user clicked on the pointer to start the move mode. <br>
     * {@code false} once the user stops touching the screen.
     *
     * @see #onTouchEvent(MotionEvent)
     */
    private boolean mUserIsMovingPointer = false;

    /**
     * The ARGB value of the currently selected color.
     */
    private int mColor;

    /**
     * The ARGB value of the center with the old selected color.
     */
    private int mCenterOldColor;

    /**
     * The ARGB value of the center with the new selected color.
     */
    private int mCenterNewColor;

    /**
     * Number of pixels the origin of this view is moved in X- and Y-direction.
     *
     * <p>
     * We use the center of this (quadratic) View as origin of our internal
     * coordinate system. Android uses the upper left corner as origin for the
     * View-specific coordinate system. So this is the value we use to translate
     * from one coordinate system to the other.
     * </p>
     *
     * <p>
     * Note: (Re)calculated in {@link #onMeasure(int, int)}.
     * </p>
     *
     * @see #onDraw(Canvas)
     */
    private float mTranslationOffset;

    /**
     * The pointer's position expressed as angle (in rad).
     */
    private float mAngle;

    /**
     * {@code Paint} instance used to draw the center with the old selected
     * color.
     */
    private Paint mCenterOldPaint;

    /**
     * {@code Paint} instance used to draw the center with the new selected
     * color.
     */
    private Paint mCenterNewPaint;

    /**
     * {@code Paint} instance used to draw the halo of the center selected
     * colors.
     */
    private Paint mCenterHaloPaint;

    /**
     * An array of floats that can be build into a {@code Color} <br>
     * Where we can extract the Saturation and Value from.
     */
    private final float[] mHSV = new float[3];

    /**
     * {@code SVBar} instance used to control the Saturation/Value bar.
     */
    private SVBar mSVbar = null;

    /**
     * {@code OpacityBar} instance used to control the Opacity bar.
     */
    private OpacityBar mOpacityBar = null;

    /**
     * {@code SaturationBar} instance used to control the Saturation bar.
     */
    private SaturationBar mSaturationBar = null;

    /**
     * {@code ValueBar} instance used to control the Value bar.
     */
    private ValueBar mValueBar = null;

    /**
     * {@code onColorChangedListener} instance of the onColorChangedListener
     */
    private OnColorChangedListener onColorChangedListener;

    public ColorPicker(final Context context) {
        super(context);
        init(null, 0);
    }

    public ColorPicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ColorPicker(final Context context, final AttributeSet attrs,
                       final int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * An interface that is called whenever the color is changed. Currently it
     * is always called when the color is changes.
     *
     * @author lars
     *
     */
    public interface OnColorChangedListener {
        public void onColorChanged(final int color);
    }

    /**
     * Set a onColorChangedListener
     *
     * @param {@code OnColorChangedListener}
     */
    public void setOnColorChangedListener(final OnColorChangedListener listener) {
        this.onColorChangedListener = listener;
    }

    /**
     * Gets the onColorChangedListener
     *
     * @return {@code OnColorChangedListener}
     */
    public OnColorChangedListener getOnColorChangedListener() {
        return this.onColorChangedListener;
    }

    private void init(final AttributeSet attrs, final int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                             R.styleable.ColorPicker, defStyle, 0);
        final Resources b = getContext().getResources();
        this.mColorWheelThickness = a.getDimensionPixelSize(
                                        R.styleable.ColorPicker_color_wheel_thickness,
                                        b.getDimensionPixelSize(R.dimen.color_wheel_thickness));
        this.mColorWheelRadius = a.getDimensionPixelSize(
                                     R.styleable.ColorPicker_color_wheel_radius,
                                     b.getDimensionPixelSize(R.dimen.color_wheel_radius));
        this.mPreferredColorWheelRadius = this.mColorWheelRadius;
        this.mColorCenterRadius = a.getDimensionPixelSize(
                                      R.styleable.ColorPicker_color_center_radius,
                                      b.getDimensionPixelSize(R.dimen.color_center_radius));
        this.mPreferredColorCenterRadius = this.mColorCenterRadius;
        this.mColorCenterHaloRadius = a.getDimensionPixelSize(
                                          R.styleable.ColorPicker_color_center_halo_radius,
                                          b.getDimensionPixelSize(R.dimen.color_center_halo_radius));
        this.mPreferredColorCenterHaloRadius = this.mColorCenterHaloRadius;
        this.mColorPointerRadius = a.getDimensionPixelSize(
                                       R.styleable.ColorPicker_color_pointer_radius,
                                       b.getDimensionPixelSize(R.dimen.color_pointer_radius));
        this.mColorPointerHaloRadius = a.getDimensionPixelSize(
                                           R.styleable.ColorPicker_color_pointer_halo_radius,
                                           b.getDimensionPixelSize(R.dimen.color_pointer_halo_radius));
        a.recycle();
        this.mAngle = (float) (-Math.PI / 2);
        final Shader s = new SweepGradient(0, 0, COLORS, null);
        this.mColorWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mColorWheelPaint.setShader(s);
        this.mColorWheelPaint.setStyle(Paint.Style.STROKE);
        this.mColorWheelPaint.setStrokeWidth(this.mColorWheelThickness);
        this.mPointerHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPointerHaloPaint.setColor(Color.BLACK);
        this.mPointerHaloPaint.setAlpha(0x50);
        this.mPointerColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPointerColor.setColor(calculateColor(this.mAngle));
        this.mCenterNewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mCenterNewPaint.setColor(calculateColor(this.mAngle));
        this.mCenterNewPaint.setStyle(Paint.Style.FILL);
        this.mCenterOldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mCenterOldPaint.setColor(calculateColor(this.mAngle));
        this.mCenterOldPaint.setStyle(Paint.Style.FILL);
        this.mCenterHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mCenterHaloPaint.setColor(Color.BLACK);
        this.mCenterHaloPaint.setAlpha(0x00);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        // All of our positions are using our internal coordinate system.
        // Instead of translating
        // them we let Canvas do the work for us.
        canvas.translate(this.mTranslationOffset, this.mTranslationOffset);
        // Draw the color wheel.
        canvas.drawOval(this.mColorWheelRectangle, this.mColorWheelPaint);
        final float[] pointerPosition = calculatePointerPosition(this.mAngle);
        // Draw the pointer's "halo"
        canvas.drawCircle(pointerPosition[0], pointerPosition[1],
                          this.mColorPointerHaloRadius, this.mPointerHaloPaint);
        // Draw the pointer (the currently selected color) slightly smaller on
        // top.
        canvas.drawCircle(pointerPosition[0], pointerPosition[1],
                          this.mColorPointerRadius, this.mPointerColor);
        // Draw the halo of the center colors.
        canvas.drawCircle(0, 0, this.mColorCenterHaloRadius,
                          this.mCenterHaloPaint);
        // Draw the old selected color in the center.
        canvas.drawArc(this.mCenterRectangle, 90, 180, true,
                       this.mCenterOldPaint);
        // Draw the new selected color in the center.
        canvas.drawArc(this.mCenterRectangle, 270, 180, true,
                       this.mCenterNewPaint);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec,
                             final int heightMeasureSpec) {
        final int intrinsicSize = 2 * (this.mPreferredColorWheelRadius + this.mColorPointerHaloRadius);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width;
        int height;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(intrinsicSize, widthSize);
        } else {
            width = intrinsicSize;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(intrinsicSize, heightSize);
        } else {
            height = intrinsicSize;
        }
        final int min = Math.min(width, height);
        setMeasuredDimension(min, min);
        this.mTranslationOffset = min * 0.5f;
        // fill the rectangle instances.
        this.mColorWheelRadius = min / 2 - this.mColorWheelThickness
                                 - this.mColorPointerHaloRadius;
        this.mColorWheelRectangle.set(-this.mColorWheelRadius,
                                      -this.mColorWheelRadius, this.mColorWheelRadius,
                                      this.mColorWheelRadius);
        this.mColorCenterRadius = (int) (this.mPreferredColorCenterRadius * ((
                                             float) this.mColorWheelRadius / (float) this.mPreferredColorWheelRadius));
        this.mColorCenterHaloRadius = (int) (this.mPreferredColorCenterHaloRadius * ((
                float) this.mColorWheelRadius / (float) this.mPreferredColorWheelRadius));
        this.mCenterRectangle.set(-this.mColorCenterRadius,
                                  -this.mColorCenterRadius, this.mColorCenterRadius,
                                  this.mColorCenterRadius);
    }

    private static int ave(final int s, final int d, final float p) {
        return s + java.lang.Math.round(p * (d - s));
    }

    /**
     * Calculate the color using the supplied angle.
     *
     * @param angle
     *            The selected color's position expressed as angle (in rad).
     *
     * @return The ARGB value of the color on the color wheel at the specified
     *         angle.
     */
    private int calculateColor(final float angle) {
        float unit = (float) (angle / (2 * Math.PI));
        if (unit < 0) {
            unit += 1;
        }
        if (unit <= 0) {
            this.mColor = COLORS[0];
            return COLORS[0];
        }
        if (unit >= 1) {
            this.mColor = COLORS[COLORS.length - 1];
            return COLORS[COLORS.length - 1];
        }
        float p = unit * (COLORS.length - 1);
        final int i = (int) p;
        p -= i;
        final int c0 = COLORS[i];
        final int c1 = COLORS[i + 1];
        final int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        final int r = ave(Color.red(c0), Color.red(c1), p);
        final int g = ave(Color.green(c0), Color.green(c1), p);
        final int b = ave(Color.blue(c0), Color.blue(c1), p);
        this.mColor = Color.argb(a, r, g, b);
        return Color.argb(a, r, g, b);
    }

    /**
     * Get the currently selected color.
     *
     * @return The ARGB value of the currently selected color.
     */
    public int getColor() {
        return this.mCenterNewColor;
    }

    /**
     * Set the color to be highlighted by the pointer. </br> </br> If the
     * instances {@code SVBar} and the {@code OpacityBar} aren't null the color
     * will also be set to them
     *
     * @param color
     *            The RGB value of the color to highlight. If this is not a
     *            color displayed on the color wheel a very simple algorithm is
     *            used to map it to the color wheel. The resulting color often
     *            won't look close to the original color. This is especially
     *            true for shades of grey. You have been warned!
     */
    public void setColor(final int color) {
        this.mAngle = colorToAngle(color);
        this.mPointerColor.setColor(calculateColor(this.mAngle));
        // check of the instance isn't null
        if (this.mOpacityBar != null) {
            // set the value of the opacity
            this.mOpacityBar.setColor(this.mColor);
            this.mOpacityBar.setOpacity(Color.alpha(color));
        }
        // check if the instance isn't null
        if (this.mSVbar != null) {
            // the array mHSV will be filled with the HSV values of the color.
            Color.colorToHSV(color, this.mHSV);
            this.mSVbar.setColor(this.mColor);
            // because of the design of the Saturation/Value bar,
            // we can only use Saturation or Value every time.
            // Here will be checked which we shall use.
            if (this.mHSV[1] < this.mHSV[2]) {
                this.mSVbar.setSaturation(this.mHSV[1]);
            } else { // if (mHSV[1] > mHSV[2]) {
                this.mSVbar.setValue(this.mHSV[2]);
            }
        }
        if (this.mSaturationBar != null) {
            Color.colorToHSV(color, this.mHSV);
            this.mSaturationBar.setColor(this.mColor);
            this.mSaturationBar.setSaturation(this.mHSV[1]);
        }
        if (this.mValueBar != null && this.mSaturationBar == null) {
            Color.colorToHSV(color, this.mHSV);
            this.mValueBar.setColor(this.mColor);
            this.mValueBar.setValue(this.mHSV[2]);
        } else if (this.mValueBar != null) {
            Color.colorToHSV(color, this.mHSV);
            this.mValueBar.setValue(this.mHSV[2]);
        }
        invalidate();
    }

    /**
     * Convert a color to an angle.
     *
     * @param color
     *            The RGB value of the color to "find" on the color wheel.
     *
     * @return The angle (in rad) the "normalized" color is displayed on the
     *         color wheel.
     */
    private static float colorToAngle(final int color) {
        final float[] colors = new float[3];
        Color.colorToHSV(color, colors);
        return (float) Math.toRadians(-colors[0]);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        // Convert coordinates to our internal coordinate system
        final float x = event.getX() - this.mTranslationOffset;
        final float y = event.getY() - this.mTranslationOffset;
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            // Check whether the user pressed on the pointer.
            final float[] pointerPosition = calculatePointerPosition(this.mAngle);
            if (x >= pointerPosition[0] - this.mColorPointerHaloRadius
                && x <= pointerPosition[0] + this.mColorPointerHaloRadius
                && y >= pointerPosition[1] - this.mColorPointerHaloRadius
                && y <= pointerPosition[1] + this.mColorPointerHaloRadius) {
                this.mUserIsMovingPointer = true;
                invalidate();
            }
            // Check whether the user pressed on the center.
            else if (x >= -this.mColorCenterRadius
                     && x <= this.mColorCenterRadius
                     && y >= -this.mColorCenterRadius
                     && y <= this.mColorCenterRadius) {
                this.mCenterHaloPaint.setAlpha(0x50);
                setColor(getOldCenterColor());
                this.mCenterNewPaint.setColor(getOldCenterColor());
                invalidate();
            }
            // If user did not press pointer or center, report event not handled
            else {
                getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (this.mUserIsMovingPointer) {
                this.mAngle = (float) java.lang.Math.atan2(y, x);
                this.mPointerColor.setColor(calculateColor(this.mAngle));
                setNewCenterColor(this.mCenterNewColor = calculateColor(this.mAngle));
                if (this.mOpacityBar != null) {
                    this.mOpacityBar.setColor(this.mColor);
                }
                if (this.mValueBar != null) {
                    this.mValueBar.setColor(this.mColor);
                }
                if (this.mSaturationBar != null) {
                    this.mSaturationBar.setColor(this.mColor);
                }
                if (this.mSVbar != null) {
                    this.mSVbar.setColor(this.mColor);
                }
                invalidate();
            }
            // If user did not press pointer or center, report event not handled
            else {
                getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
            break;
        case MotionEvent.ACTION_UP:
            this.mUserIsMovingPointer = false;
            this.mCenterHaloPaint.setAlpha(0x00);
            invalidate();
            break;
        default:
            break;
        }
        return true;
    }

    /**
     * Calculate the pointer's coordinates on the color wheel using the supplied
     * angle.
     *
     * @param angle
     *            The position of the pointer expressed as angle (in rad).
     *
     * @return The coordinates of the pointer's center in our internal
     *         coordinate system.
     */
    private float[] calculatePointerPosition(final float angle) {
        final float x = (float) (this.mColorWheelRadius * Math.cos(angle));
        final float y = (float) (this.mColorWheelRadius * Math.sin(angle));
        return new float[] { x, y };
    }

    /**
     * Add a Saturation/Value bar to the color wheel.
     *
     * @param bar
     *            The instance of the Saturation/Value bar.
     */
    public void addSVBar(final SVBar bar) {
        this.mSVbar = bar;
        // Give an instance of the color picker to the Saturation/Value bar.
        this.mSVbar.setColorPicker(this);
        this.mSVbar.setColor(this.mColor);
    }

    /**
     * Add a Opacity bar to the color wheel.
     *
     * @param bar
     *            The instance of the Opacity bar.
     */
    public void addOpacityBar(final OpacityBar bar) {
        this.mOpacityBar = bar;
        // Give an instance of the color picker to the Opacity bar.
        this.mOpacityBar.setColorPicker(this);
        this.mOpacityBar.setColor(this.mColor);
    }

    public void addSaturationBar(final SaturationBar bar) {
        this.mSaturationBar = bar;
        this.mSaturationBar.setColorPicker(this);
        this.mSaturationBar.setColor(this.mColor);
    }

    public void addValueBar(final ValueBar bar) {
        this.mValueBar = bar;
        this.mValueBar.setColorPicker(this);
        this.mValueBar.setColor(this.mColor);
    }

    /**
     * Change the color of the center which indicates the new color.
     *
     * @param color
     *            int of the color.
     */
    public void setNewCenterColor(final int color) {
        this.mCenterNewColor = color;
        this.mCenterNewPaint.setColor(color);
        if (this.mCenterOldColor == 0) {
            this.mCenterOldColor = color;
            this.mCenterOldPaint.setColor(color);
        }
        if (this.onColorChangedListener != null) {
            this.onColorChangedListener.onColorChanged(color);
        }
        invalidate();
    }

    /**
     * Change the color of the center which indicates the old color.
     *
     * @param color
     *            int of the color.
     */
    public void setOldCenterColor(final int color) {
        this.mCenterOldColor = color;
        this.mCenterOldPaint.setColor(color);
        invalidate();
    }

    public int getOldCenterColor() {
        return this.mCenterOldColor;
    }

    /**
     * Used to change the color of the {@code OpacityBar} used by the
     * {@code SVBar} if there is an change in color.
     *
     * @param color
     *            int of the color used to change the opacity bar color.
     */
    public void changeOpacityBarColor(final int color) {
        if (this.mOpacityBar != null) {
            this.mOpacityBar.setColor(color);
        }
    }

    /**
     * Used to change the color of the {@code SaturationBar}.
     *
     * @param color
     *            int of the color used to change the opacity bar color.
     */
    public void changeSaturationBarColor(final int color) {
        if (this.mSaturationBar != null) {
            this.mSaturationBar.setColor(color);
        }
    }

    /**
     * Used to change the color of the {@code ValueBar}.
     *
     * @param color
     *            int of the color used to change the opacity bar color.
     */
    public void changeValueBarColor(final int color) {
        if (this.mValueBar != null) {
            this.mValueBar.setColor(color);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final Bundle state = new Bundle();
        state.putParcelable(STATE_PARENT, superState);
        state.putFloat(STATE_ANGLE, this.mAngle);
        state.putInt(STATE_OLD_COLOR, this.mCenterOldColor);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        final Bundle savedState = (Bundle) state;
        final Parcelable superState = savedState.getParcelable(STATE_PARENT);
        super.onRestoreInstanceState(superState);
        this.mAngle = savedState.getFloat(STATE_ANGLE);
        setOldCenterColor(savedState.getInt(STATE_OLD_COLOR));
        final int currentColor = calculateColor(this.mAngle);
        this.mPointerColor.setColor(currentColor);
        setNewCenterColor(currentColor);
    }
}
