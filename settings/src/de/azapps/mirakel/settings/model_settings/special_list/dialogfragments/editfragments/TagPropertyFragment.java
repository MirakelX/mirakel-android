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

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import de.azapps.mirakel.model.list.meta.SpecialListsTagProperty;
import de.azapps.mirakel.model.tags.Tag;

public class TagPropertyFragment extends SetPropertyFragment<SpecialListsTagProperty> {

    public static TagPropertyFragment newInstance(SpecialListsTagProperty property) {
        return setInitialArguments(new TagPropertyFragment(), property);
    }

    @NonNull
    @Override
    protected Map<String, Integer> getElements() {
        Map<String, Integer> lists = new HashMap<>();
        for (Tag l : Tag.all()) {
            lists.put(l.getName(), (int)l.getId());
        }
        return lists;
    }
}
