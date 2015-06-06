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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import de.azapps.mirakel.model.list.meta.SpecialListsBooleanProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueExistsProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.settings.R;


public class NegatedPropertyFragment extends BasePropertyFragement<SpecialListsBooleanProperty> {


    public static NegatedPropertyFragment newInstance(final SpecialListsBooleanProperty property) {
        return setInitialArguments(new NegatedPropertyFragment(), property);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_negated_property, container, false);

        CheckBox negated = (CheckBox)rootView.findViewById(R.id.negated_property_checkbox);
        if (property instanceof SpecialListsDoneProperty) {
            negated.setText(R.string.done_text);
        } else if (property instanceof SpecialListsReminderProperty) {
            negated.setText(R.string.reminder_text);
        } else if (property instanceof SpecialListsFileProperty) {
            negated.setText(R.string.file_text);
        } else if (property instanceof SpecialListsDueExistsProperty) {
            negated.setText(R.string.due_exist_text);
        } else {
            negated.setText("Someone implement something wrong????");
        }
        negated.setChecked(property.isSet());
        negated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                property.setIsNegated(isChecked);
            }
        });
        return rootView;
    }

}
