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

package de.azapps.widgets;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import de.azapps.mirakel.settings.R;

public class DueDialog extends AlertDialog {
    private final Context ctx;
    protected VALUE dayYear = VALUE.DAY;
    protected int count;
    private final View dialogView;
    protected String[] s;

    public enum VALUE {
        MINUTE, HOUR, DAY, MONTH, YEAR,;

        public int getInt() {
            switch (this) {
            case DAY:
                return 0;
            case MONTH:
                return 1;
            case YEAR:
                return 2;
            case MINUTE:
                return 3;
            case HOUR:
                return 4;
            default:
                break;
            }
            return 0;
        }
    }

    public void setNegativeButton(final int textId,
                                  final OnClickListener onCancel) {
        setButton(BUTTON_NEGATIVE, this.ctx.getString(textId), onCancel);
    }

    public void setPositiveButton(final int textId,
                                  final OnClickListener onCancel) {
        setButton(BUTTON_POSITIVE, this.ctx.getString(textId), onCancel);
    }

    public void setNeutralButton(final int textId,
                                 final OnClickListener onCancel) {
        setButton(BUTTON_NEUTRAL, this.ctx.getString(textId), onCancel);
    }

    @SuppressLint("NewApi")
    public DueDialog(final Context context, final boolean minuteHour) {
        super(context);
        this.ctx = context;
        this.s = new String[100];
        for (int i = 0; i < this.s.length; i++) {
            this.s[i] = (i > 10 ? "+" : "") + (i - 10) + "";
        }
        this.dialogView = getNumericPicker();

        final NumberPicker pickerDay = (NumberPicker) this.dialogView
                                       .findViewById(R.id.due_day_year);
        final NumberPicker pickerValue = (NumberPicker) this.dialogView
                                         .findViewById(R.id.due_val);
        final String dayYearValues[] = getDayYearValues(0, minuteHour);
        pickerDay.setDisplayedValues(dayYearValues);
        pickerDay.setMaxValue(dayYearValues.length - 1);
        pickerValue.setMaxValue(this.s.length - 1);
        pickerValue.setValue(10);
        pickerValue.setMinValue(0);
        pickerValue.setDisplayedValues(this.s);
        pickerValue
        .setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        pickerDay
        .setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        pickerValue.setWrapSelectorWheel(false);
        pickerValue
        .setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(final NumberPicker picker,
                                      final int oldVal, final int newVal) {
                pickerDay.setDisplayedValues(getDayYearValues(
                                                 newVal - 10, minuteHour));
                DueDialog.this.count = newVal - 10;
            }
        });
        pickerDay
        .setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(final NumberPicker picker,
                                      final int oldVal, final int newVal) {
                switch (newVal) {
                case 0:
                    DueDialog.this.dayYear = VALUE.DAY;
                    break;
                case 1:
                    DueDialog.this.dayYear = VALUE.MONTH;
                    break;
                case 2:
                    DueDialog.this.dayYear = VALUE.YEAR;
                    break;
                default:
                    break;
                }
            }
        });
        setView(this.dialogView);
    }

    protected String updateDayYear() {
        switch (this.dayYear) {
        case MINUTE:
            return this.ctx.getResources().getQuantityString(
                       R.plurals.due_minute, this.count);
        case HOUR:
            return this.ctx.getResources().getQuantityString(
                       R.plurals.due_hour, this.count);
        case DAY:
            return this.ctx.getResources().getQuantityString(R.plurals.due_day,
                    this.count);
        case MONTH:
            return this.ctx.getResources().getQuantityString(
                       R.plurals.due_month, this.count);
        case YEAR:
            return this.ctx.getResources().getQuantityString(
                       R.plurals.due_year, this.count);
        default:
            break;
        }
        return "";
    }

    protected String[] getDayYearValues(final int newVal,
                                        final boolean minutesHour) {
        final int size = minutesHour ? 5 : 3;
        int i = 0;
        final String[] ret = new String[size];
        if (minutesHour) {
            ret[i++] = this.ctx.getResources().getQuantityString(
                           R.plurals.due_minute, newVal);
            ret[i++] = this.ctx.getResources().getQuantityString(
                           R.plurals.due_hour, newVal);
        }
        ret[i++] = this.ctx.getResources().getQuantityString(R.plurals.due_day,
                   newVal);
        ret[i++] = this.ctx.getResources().getQuantityString(
                       R.plurals.due_month, newVal);
        ret[i] = this.ctx.getResources().getQuantityString(R.plurals.due_year,
                 newVal);
        return ret;
    }

    protected View getNumericPicker() {
        return getLayoutInflater().inflate(R.layout.due_dialog, null);
    }

    public void setValue(final int val) {
        final int _day = this.dayYear.getInt();
        ((NumberPicker) this.dialogView.findViewById(R.id.due_day_year))
        .setValue(_day);
        ((NumberPicker) this.dialogView.findViewById(R.id.due_val))
        .setValue(val + 10);
    }

    public int getValue() {
        return this.count;
    }

    public VALUE getDayYear() {
        return this.dayYear;
    }

}
