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

package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;

public class TextViewWithCircularIndicator extends TextView {
    private final int mCircleColor;
    Paint mCirclePaint = new Paint();
    private boolean mDrawCircle;
    private final String mItemIsSelectedText;
    private int mSeclectedTextColor;
    private int mUnselectedTextColor;

    public TextViewWithCircularIndicator(final Context context,
                                         final AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCircleColor = ThemeManager.getPrimaryThemeColor();
        this.mSeclectedTextColor = ThemeManager.getAccentThemeColor();
        this.mItemIsSelectedText = context.getResources().getString(
                                       R.string.item_is_selected);
        this.mUnselectedTextColor = ThemeManager.getColor(R.attr.colorTextBlack);
        init();
    }

    public void drawIndicator(final boolean drawIndicator) {
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
        this.mCirclePaint.setAlpha(70);
    }

    @Override
    public void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawCircle) {
            final int width = getWidth();
            final int heigth = getHeight();
            final int radius = Math.min(width, heigth) / 2;
            canvas.drawCircle(width / 2.0F, heigth / 2.0F, radius, this.mCirclePaint);
            setTextColor(this.mSeclectedTextColor, true);
        } else {
            setTextColor(this.mUnselectedTextColor, true);
        }
    }

    @Override
    public void setTextColor(final int color) {
        setTextColor(color, false);
    }

    private void setTextColor(final int color, final boolean intern) {
        super.setTextColor(color);
        if (!intern) {
            this.mSeclectedTextColor = color;
            this.mUnselectedTextColor = color;
        }
    }
}