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

package de.azapps.mirakel.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.google.common.base.Optional;

import org.joda.time.DateTime;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class TaskService extends Service {
    public static final String TASK_DONE = "de.azapps.mirakel.services.TaskService.TASK_DONE";
    public static final String TASK_LATER = "de.azapps.mirakel.services.TaskService.TASK_LATER";

    @Override
    public IBinder onBind(final Intent intent) {
        // nothing
        return null;
    }

    private void handleCommand(final Intent intent) {
        if ((intent == null) || (intent.getAction() == null)) {
            return;
        }
        final Optional<Task> taskOptional = TaskHelper.getTaskFromIntent(intent);
        if (!taskOptional.isPresent()) {
            return;
        }
        //reload the task to get the current version
        final Task task = Task.get(taskOptional.get().getId()).orNull();
        if (task == null) {
            return;
        }
        switch (intent.getAction()) {
        case TASK_DONE:
            task.setDone(true);
            task.save();
            Toast.makeText(this,
                           getString(R.string.reminder_notification_done_confirm),
                           Toast.LENGTH_LONG).show();
            break;
        case TASK_LATER:
            final DateTime reminder = new DateTime();
            final int addMinutes = MirakelCommonPreferences.getAlarmLater();
            reminder.plusMinutes(addMinutes);
            task.setReminder(Optional.of(reminder));
            task.save();
            Toast.makeText(
                this,
                getString(R.string.reminder_notification_later_confirm,
                          addMinutes), Toast.LENGTH_LONG).show();
            break;
        }
        stopSelf();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags,
                              final int startId) {
        handleCommand(intent);
        return START_STICKY;
    }

}
