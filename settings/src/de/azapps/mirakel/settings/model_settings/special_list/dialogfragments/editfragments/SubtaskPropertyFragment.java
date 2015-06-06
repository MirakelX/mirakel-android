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

import de.azapps.mirakel.model.list.meta.SpecialListsSubtaskProperty;
import de.azapps.mirakel.settings.R;

public class SubtaskPropertyFragment extends BasePropertyFragement<SpecialListsSubtaskProperty> {

    public static SubtaskPropertyFragment newInstance(SpecialListsSubtaskProperty property) {
        return setInitialArguments(new SubtaskPropertyFragment(), property);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.special_list_subtask_edit, null);
        final CheckBox hasSubtasks = (CheckBox)rootView.findViewById(R.id.subtask_has);
        hasSubtasks.setChecked(property.isSet());
        hasSubtasks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                property.setIsNegated(isChecked);
            }
        });
        updateText(hasSubtasks);

        final CheckBox isParent = (CheckBox)rootView.findViewById(R.id.subtask_parent);
        isParent.setChecked(property.isParent());
        isParent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                property.setParent(isChecked);
                updateText(hasSubtasks);
            }
        });
        return rootView;
    }

    private void updateText(CheckBox hasSubtasks) {
        if (property.isParent()) {
            hasSubtasks.setText(R.string.has_subtasks);
        } else {
            hasSubtasks.setText(R.string.has_parent);
        }
    }
}
