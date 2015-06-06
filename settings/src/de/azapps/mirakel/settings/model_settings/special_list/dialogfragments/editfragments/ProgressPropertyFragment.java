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
import android.widget.NumberPicker;

import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.settings.R;

public class ProgressPropertyFragment extends BasePropertyFragement<SpecialListsProgressProperty> {

    public static ProgressPropertyFragment newInstance(SpecialListsProgressProperty property) {
        return setInitialArguments(new ProgressPropertyFragment(), property);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.progress_dialog, null);
        NumberPicker operation = (NumberPicker)rootView.findViewById(R.id.progress_op);
        operation.setMinValue(0);
        operation.setMaxValue(2);
        operation.setDisplayedValues(new String[] {"<=", "=", ">="});
        switch (property.getOperation()) {
        case GREATER_THAN:
            operation.setValue(2);
            break;
        case EQUAL:
            operation.setValue(1);
            break;
        case LESS_THAN:
            operation.setValue(0);
            break;
        }
        operation.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                switch (newVal) {
                case 0:
                    property.setOperation(SpecialListsProgressProperty.OPERATION.LESS_THAN);
                    break;
                case 1:
                    property.setOperation(SpecialListsProgressProperty.OPERATION.EQUAL);
                    break;
                case 2:
                    property.setOperation(SpecialListsProgressProperty.OPERATION.GREATER_THAN);
                    break;
                }
            }
        });


        NumberPicker value = (NumberPicker)rootView.findViewById(R.id.progress_value);
        value.setMinValue(0);
        value.setMaxValue(100);
        value.setValue(property.getValue());
        value.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                property.setValue(newVal);
            }
        });

        return rootView;
    }
}
