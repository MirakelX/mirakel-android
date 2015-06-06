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

package de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;

import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.settings.R;

public class DuePropertyFragment extends BasePropertyFragement<SpecialListsDueProperty> {

    public static DuePropertyFragment newInstance(SpecialListsDueProperty property) {
        return setInitialArguments(new DuePropertyFragment(), property);
    }

    protected VALUE dayYear = VALUE.DAY;
    protected int count;
    private View dialogView;
    boolean minuteHour = false;
    protected final String[] s;

    public DuePropertyFragment() {
        super();
        this.s = new String[100];
        for (int i = 0; i < this.s.length; i++) {
            this.s[i] = (i > 10 ? "+" : "") + (i - 10) + "";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        dialogView = inflater.inflate(R.layout.due_dialog, null);
        final NumberPicker pickerDay = (NumberPicker) this.dialogView
                                       .findViewById(R.id.due_day_year);
        final NumberPicker pickerValue = (NumberPicker) this.dialogView
                                         .findViewById(R.id.due_val);
        final CheckBox negated = (CheckBox)dialogView.findViewById(R.id.due_dialog_negated);
        negated.setVisibility(View.VISIBLE);
        negated.setChecked(property.isSet());
        negated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                property.setIsNegated(isChecked);
            }
        });
        final String dayYearValues[] = getDayYearValues(0, minuteHour);
        pickerDay.setDisplayedValues(dayYearValues);
        pickerDay.setMaxValue(dayYearValues.length - 1);
        pickerValue.setMaxValue(this.s.length - 1);
        pickerValue.setValue(10 + property.getLength());
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
                DuePropertyFragment.this.count = newVal - 10;
                property.setLength(count);
            }
        });
        pickerDay
        .setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(final NumberPicker picker,
                                      final int oldVal, final int newVal) {
                switch (newVal) {
                case 0:
                    DuePropertyFragment.this.dayYear = VALUE.DAY;
                    property.setUnit(SpecialListsDueProperty.Unit.DAY);
                    break;
                case 1:
                    DuePropertyFragment.this.dayYear = VALUE.MONTH;
                    property.setUnit(SpecialListsDueProperty.Unit.MONTH);
                    break;
                case 2:
                    DuePropertyFragment.this.dayYear = VALUE.YEAR;
                    property.setUnit(SpecialListsDueProperty.Unit.YEAR);
                    break;
                default:
                    break;
                }
            }
        });
        switch (property.getUnit()) {
        case DAY:
            pickerDay.setValue(0);
            break;
        case MONTH:
            pickerDay.setValue(1);
            break;
        case YEAR:
            pickerDay.setValue(2);
            break;
        }
        return dialogView;
    }

    public enum VALUE {
        MINUTE, HOUR, DAY, MONTH, YEAR;

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


    protected String updateDayYear() {
        switch (this.dayYear) {
        case MINUTE:
            return getActivity().getResources().getQuantityString(
                       R.plurals.due_minute, this.count);
        case HOUR:
            return getActivity().getResources().getQuantityString(
                       R.plurals.due_hour, this.count);
        case DAY:
            return getActivity().getResources().getQuantityString(R.plurals.due_day,
                    this.count);
        case MONTH:
            return getActivity().getResources().getQuantityString(
                       R.plurals.due_month, this.count);
        case YEAR:
            return getActivity().getResources().getQuantityString(
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
            ret[i++] = getActivity().getResources().getQuantityString(
                           R.plurals.due_minute, newVal);
            ret[i++] = getActivity().getResources().getQuantityString(
                           R.plurals.due_hour, newVal);
        }
        ret[i++] = getActivity().getResources().getQuantityString(R.plurals.due_day,
                   newVal);
        ret[i++] = getActivity().getResources().getQuantityString(
                       R.plurals.due_month, newVal);
        ret[i] = getActivity().getResources().getQuantityString(R.plurals.due_year,
                 newVal);
        return ret;
    }

    public void setValue(final int val, final VALUE day) {
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


