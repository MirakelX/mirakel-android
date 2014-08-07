/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.helper;

import java.util.Calendar;

import org.joda.time.LocalDate;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class TaskHelper {

    public static Task getTaskFromIntent(final Intent intent) {
        Task task = null;
        if (intent == null) {
            return task;
        }
        long taskId = intent.getLongExtra(DefinitionsHelper.EXTRA_ID, 0);
        if (taskId == 0) {
            // ugly fix for show Task from Widget
            taskId = intent.getIntExtra(DefinitionsHelper.EXTRA_ID, 0);
        }
        if (taskId != 0) {
            task = Task.get(taskId);
        }
        return task;
    }

    /**
     * Helper for the share-functions
     *
     * @param ctx
     * @param t
     * @return
     */
    static String getTaskName(final Context ctx, final Task t) {
        String subject;
        if (t.getDue() == null) {
            subject = ctx.getString(R.string.share_task_title, t.getName());
        } else {
            subject = ctx.getString(
                          R.string.share_task_title_with_date,
                          t.getName(),
                          DateTimeHelper.formatDate(t.getDue(),
                                                    ctx.getString(R.string.dateFormat)));
        }
        return subject;
    }

    /**
     * Returns the ID of the Color–Resource for a Due–Date
     *
     * @param origDue
     *            The Due–Date
     * @param isDone
     *            Is the Task done?
     * @return ID of the Color–Resource
     */
    public static int getTaskDueColor(final Context context, final Calendar origDue,
                                      final boolean isDone) {
        int colorResource;
        if (origDue == null) {
            colorResource = R.color.Grey;
        } else {
            final LocalDate today = new LocalDate();
            final LocalDate nextWeek = new LocalDate().plusDays(7);
            final LocalDate due = new LocalDate(origDue);
            final int cmpr = today.compareTo(due);
            if (isDone) {
                colorResource = R.color.Grey;
            } else if (cmpr > 0) {
                colorResource = R.color.due_overdue;
            } else if (cmpr == 0) {
                colorResource = R.color.due_today;
            } else if (nextWeek.compareTo(due) >= 0) {
                colorResource = R.color.due_next;
            } else {
                colorResource = R.color.due_future;
            }
        }
        return context.getResources().getColor(colorResource);
    }

    public static int getPrioColor(final int priority) {
        final int[] PRIO_COLOR = { Color.parseColor("#669900"),
                                   Color.parseColor("#99CC00"), Color.parseColor("#33B5E5"),
                                   Color.parseColor("#FFBB33"), Color.parseColor("#FF4444")
                                 };
        final int[] DARK_PRIO_COLOR = { Color.parseColor("#008000"),
                                        Color.parseColor("#00c400"), Color.parseColor("#3377FF"),
                                        Color.parseColor("#FF7700"), Color.parseColor("#FF3333")
                                      };
        if (MirakelCommonPreferences.isDark()) {
            return DARK_PRIO_COLOR[priority + 2];
        }
        return PRIO_COLOR[priority + 2];
    }

    public static void setPrio(final TextView taskPrio, final Task task) {
        taskPrio.setText("" + task.getPriority());
        final GradientDrawable bg = (GradientDrawable) taskPrio.getBackground();
        bg.setColor(TaskHelper.getPrioColor(task.getPriority()));
    }

}
