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

package de.azapps.mirakel.main_activity;

import android.content.SharedPreferences;

import java.util.List;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.services.NotificationService;

public class BackgroundTasks {

    static void run (final MainActivity context) {
        new Thread (new Runnable () {
            @Override
            public void run () {
                NotificationService.updateServices (context);
                if (!MirakelCommonPreferences.containsHighlightSelected ()) {
                    final SharedPreferences.Editor editor = MirakelPreferences
                                                            .getEditor ();
                    editor.putBoolean ("highlightSelected",
                                       MirakelCommonPreferences.isTablet ());
                    editor.commit ();
                }
                if (!MirakelCommonPreferences.containsStartupList()) {
                    final SharedPreferences.Editor editor = MirakelPreferences
                                                            .getEditor ();
                    editor.putString ("startupList", ""
                                      + ListMirakel.safeFirst().getId ());
                    editor.commit ();
                }
                // We should remove this in the future, nobody uses such old
                // versions (hopefully)
                if (MainActivity.updateTasksUUID) {
                    final List<Task> tasks = Task.all ();
                    for (final Task t : tasks) {
                        t.setUUID (java.util.UUID.randomUUID ().toString ());
                        t.save ();
                    }
                }
            }
        }).run ();
    }

}
