package de.azapps.mirakel.main_activity;

import java.util.List;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;

public class BackgroundTasks {
    protected static MainActivityBroadcastReceiver mSyncReciver;

    static void run (final MainActivity context) {
        new Thread (new Runnable () {
            @Override
            public void run () {
                ReminderAlarm.updateAlarms (context);
                NotificationService.updateServices (context, true);
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
                                      + ListMirakel.first ().getId ());
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
                mSyncReciver = new MainActivityBroadcastReceiver (context);
                context.registerReceiver (mSyncReciver, new IntentFilter (
                                              DefinitionsHelper.SYNC_FINISHED));
            }
        }).run ();
    }

    static void onDestroy (final MainActivity context) {
        context.unregisterReceiver (mSyncReciver);
    }
}
