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

package de.azapps.mirakel.helper;

import java.util.List;

import de.azapps.mirakel.custom_views.TaskDetailView.TYPE;

public class MirakelViewPreferences extends MirakelCommonPreferences {

    public static List<Integer> getTaskFragmentLayout() {
        final List<Integer> items = MirakelCommonPreferences
                                    .loadIntArray("task_fragment_adapter_settings");
        if (items.size() == 0) {// should not be, add all
            items.add(TYPE.HEADER);
            items.add(TYPE.DUE);
            items.add(TYPE.REMINDER);
            items.add(TYPE.CONTENT);
            items.add(TYPE.PROGRESS);
            items.add(TYPE.SUBTASK);
            items.add(TYPE.FILE);
            items.add(TYPE.TAGS);
            setTaskFragmentLayout(items);
        }
        return items;
    }

}
