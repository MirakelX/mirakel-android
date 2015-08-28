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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public final class TaskHelper {

    @NonNull
    public static Optional<Task> getTaskFromIntent(final Intent intent) {
        if (intent == null) {
            return absent();
        }
        final Bundle b = intent.getBundleExtra(DefinitionsHelper.BUNDLE_WRAPPER);
        if (b != null) {
            return fromNullable((Task) b.getParcelable(DefinitionsHelper.EXTRA_TASK));
        } else if (intent.hasExtra(DefinitionsHelper.EXTRA_TASK)) {
            return fromNullable((Task) intent.getParcelableExtra(DefinitionsHelper.EXTRA_TASK));
        } else if (intent.hasExtra(DefinitionsHelper.EXTRA_TASK_REMINDER)) {
            return fromNullable((Task) intent.getParcelableExtra(DefinitionsHelper.EXTRA_TASK_REMINDER));
        } else {
            return absent();
        }
    }

    /**
     * Helper for the share-functions
     *
     * @param ctx
     * @param t
     * @return
     */
    static String getTaskName(final Context ctx, final Task t) {
        final String subject;
        if (!t.getDue().isPresent()) {
            subject = ctx.getString(R.string.share_task_title, t.getName());
        } else {
            subject = ctx.getString(
                          R.string.share_task_title_with_date,
                          t.getName(),
                          DateTimeHelper.formatDate(t.getDue().get(),
                                                    ctx.getString(R.string.dateFormat)));
        }
        return subject;
    }

    /**
     * Returns the ID of the Color–Resource for a Due–Date
     *
     * @param due The Due–Date
     * @param isDone  Is the Task done?
     * @return ID of the Color–Resource
     */
    public static int getTaskDueColor(final Optional<DateTime> due,
                                      final boolean isDone) {
        final int colorResource;
        if (!due.isPresent()) {
            colorResource = R.attr.colorTextGrey;
        } else {
            final DateTime today = new LocalDate().toDateTimeAtStartOfDay();
            final DateTime tomorrow = today.plusDays(1);
            final DateTime nextWeek = today.plusDays(7);
            if (isDone) {
                colorResource = R.attr.colorTextGrey;
            } else if (today.isAfter(due.get())) {
                colorResource = R.attr.colorDueOverdue;
            } else if (tomorrow.isAfter(due.get())) {
                colorResource = R.attr.colorDueToday;
            } else if (nextWeek.isAfter(due.get())) {
                colorResource = R.attr.colorDueNext;
            } else {
                colorResource = R.attr.colorDueFuture;
            }
        }
        return ThemeManager.getColor(colorResource);
    }

    public static int getPrioColor(final int priority) {
        final int[] PRIO_COLOR = {R.attr.colorPrio_2, R.attr.colorPrio_1, R.attr.colorPrio0, R.attr.colorPrio1, R.attr.colorPrio2};
        return ThemeManager.getColor(PRIO_COLOR[priority + 2]);
    }

}
