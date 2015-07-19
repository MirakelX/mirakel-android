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

package com.android.calendar.recurrencepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;

public class WeekButton extends android.widget.ToggleButton {

    public WeekButton(final Context context) {
        super(context);
        setTheme();
    }

    private void setTheme() {
        setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.oval_filled));
        setTextAppearance(getContext(), R.style.TextAppearance_Bold);
        setGravity(Gravity.CENTER);
        setSingleLine(true);
    }



    public WeekButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setTheme();
    }

    public WeekButton(final Context context, final AttributeSet attrs,
                      final int defStyle) {
        super(context, attrs, defStyle);
        setTheme();
    }


    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (isChecked()) {
            setTextAppearance(getContext(), R.style.TextAppearance_Bold);
        } else {
            setTextAppearance(getContext(), R.style.TextAppearance_Regular);
        }
        setTextColor(new ColorStateList(new int[][] {new int[]{android.R.attr.state_checked}, new int[]{}},
        new int[] {ThemeManager.getPrimaryThemeColor(), ThemeManager.getColor(R.attr.colorTextGrey)}));
    }

    @SuppressLint("NewAPI")
    @Override
    protected void onMeasure(final int widthMeasureSpec,
                             final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int h = getMeasuredHeight();
        int w = getMeasuredWidth();
        if (h > 0 && w > 0) {
            if (w < h) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    h = w;
                } else {
                    if (View.MeasureSpec.getMode(getMeasuredHeightAndState()) != MeasureSpec.EXACTLY) {
                        h = w;
                    }
                }
            } else if (h < w) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    w = h;
                } else {
                    if (View.MeasureSpec.getMode(getMeasuredWidthAndState()) != MeasureSpec.EXACTLY) {
                        w = h;
                    }
                }
            }
        }
        setMeasuredDimension(w, h);
    }
}
