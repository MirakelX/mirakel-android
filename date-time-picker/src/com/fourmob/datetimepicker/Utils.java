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

package com.fourmob.datetimepicker;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.nineoldandroids.animation.Keyframe;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;

import org.joda.time.DateTimeConstants;

public class Utils {

    public static int getDaysInMonth(final int month, final int year) {

        switch (month) {
        default:
            throw new IllegalArgumentException("Invalid Month");
        case DateTimeConstants.JANUARY:
        case DateTimeConstants.MARCH:
        case DateTimeConstants.MAY:
        case DateTimeConstants.JULY:
        case DateTimeConstants.AUGUST:
        case DateTimeConstants.OCTOBER:
        case DateTimeConstants.DECEMBER:
            return 31;
        case DateTimeConstants.APRIL:
        case DateTimeConstants.JUNE:
        case DateTimeConstants.SEPTEMBER:
        case DateTimeConstants.NOVEMBER:
            return 30;
        case DateTimeConstants.FEBRUARY:
            if ((year % 4) == 0) {
                return 29;
            }
            return 28;
        }
    }

    public static ObjectAnimator getPulseAnimator(final View view,
            final float animVal1, final float animVal2) {
        final Keyframe keyframe1 = Keyframe.ofFloat(0.0F, 1.0F);
        final Keyframe keyframe2 = Keyframe.ofFloat(0.275F, animVal1);
        final Keyframe keyframe3 = Keyframe.ofFloat(0.69F, animVal2);
        final Keyframe keyframe4 = Keyframe.ofFloat(1.0F, 1.0F);
        final ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                                            view,
                                            new PropertyValuesHolder[] {
                                                PropertyValuesHolder.ofKeyframe("scaleX",
                                                        new Keyframe[] { keyframe1, keyframe2,
                                                                keyframe3, keyframe4
                                                                       }),
                                                PropertyValuesHolder.ofKeyframe("scaleY",
                                                        new Keyframe[] { keyframe1, keyframe2,
                                                                keyframe3, keyframe4
                                                                       })
                                            });
        animator.setDuration(544L);
        return animator;
    }

    public static void tryAccessibilityAnnounce(final Object obj,
            final Object announcement) {
        // TODO
    }

    @SuppressLint("NewApi")
    public static boolean isTouchExplorationEnabled(
        final AccessibilityManager accessibilityManager) {
        if (Build.VERSION.SDK_INT >= 14) {
            return accessibilityManager.isTouchExplorationEnabled();
        } else {
            return false;
        }
    }
}
