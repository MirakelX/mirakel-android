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

package de.azapps.mirakel.dashclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.list.ListMirakel;

public class SettingsHelper {
    private static SharedPreferences settings;
    public static void init(Context context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Optional<ListMirakel> getList() {
        final long list_id = Long.parseLong(settings.getString("startupList", "-1000"));
        return ListMirakel.get(list_id);
    }
    public static void setList(final ListMirakel listMirakel) {
        final SharedPreferences.Editor editor = settings.edit();
        editor.putString("startupList", String.valueOf(listMirakel.getId()));
        editor.apply();
    }
    public static int getMaxTasks() {
        return settings.getInt("showTaskNumber", 1);
    }

    public static void setMaxTasks(final int maxTasks) {
        final SharedPreferences.Editor editor = settings
                                                .edit();
        editor.putInt("showTaskNumber", maxTasks);
        editor.apply();
    }

    public static boolean showEmpty() {
        return settings.getBoolean("showEmpty", false);
    }

    public static boolean showDue() {
        return settings.getBoolean("showDueDate", true);
    }

}
