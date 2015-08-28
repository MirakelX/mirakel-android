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

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.google.common.base.Optional;

import org.joda.time.DateTime;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

import static com.google.common.base.Optional.absent;

public class DatesView extends LinearLayout {
    @Nullable
    private OnClickListener dueEditListener;
    @Nullable
    private OnClickListener listEditListener;
    @Nullable
    private OnClickListener reminderEditListener;
    @Nullable
    private OnClickListener dueRecurrenceEditListener;

    @InjectView(R.id.dates_due)
    KeyValueView dueView;
    @InjectView(R.id.dates_due_recurrence)
    KeyValueView dueRecurrenceView;

    @InjectView(R.id.dates_reminder)
    KeyValueView reminderView;



    @InjectView(R.id.dates_list)
    KeyValueView listView;

    private Optional<DateTime> due = absent();
    private String listMirakel;
    private Optional<DateTime> reminder = absent();
    private boolean isDone;
    private Optional<Recurring> dueRecurrence;



    public DatesView(final Context context) {
        this(context, null);
    }

    public DatesView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatesView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_dates, this);
        ButterKnife.inject(this, this);
        dueView.setup(R.drawable.ic_calendar_white_18dp, context.getString(R.string.task_due_header));
        dueRecurrenceView.setup(R.drawable.ic_history_white_18dp,
                                context.getString(R.string.task_due_recurrence_header));
        reminderView.setup(R.drawable.ic_alarm_white_18dp,
                           context.getString(R.string.task_reminder_header));

        listView.setup(R.drawable.ic_list_white_18dp, context.getString(R.string.task_list_header));
        listView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));
    }

    private void rebuildLayout() {
        // Set data
        if (isInEditMode()) {
            dueView.setValue("today");
            dueView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));
            dueRecurrenceView.setValue("today");
            dueRecurrenceView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));

            reminderView.setValue("today at 6 pm");
            reminderView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));
            listView.setValue("At home");
        } else {
            if (due.isPresent()) {
                dueView.setValue(DateTimeHelper.formatDate(getContext(), due));
                dueView.setColor(TaskHelper.getTaskDueColor(due, isDone));
                dueRecurrenceView.setVisibility(VISIBLE);
                if (dueRecurrence.isPresent()) {
                    dueRecurrenceView.setValue(dueRecurrence.get().generateDescription());
                    dueRecurrenceView.setColor(ThemeManager.getColor(R.attr.colorTextBlack));
                } else {
                    dueRecurrenceView.setNoValue(getContext().getString(R.string.none_recurring));
                    dueRecurrenceView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));
                }
            } else {
                dueView.setNoValue(getContext().getString(R.string.due_date));
                dueView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));
                dueRecurrenceView.setVisibility(GONE);
            }
            listView.setValue(listMirakel);
            if (reminder.isPresent()) {
                reminderView.setValue(DateTimeHelper.formatReminder(getContext(), reminder.get()));
                reminderView.setColor(TaskHelper.getTaskDueColor(reminder, false));
            } else {
                reminderView.setNoValue(getContext().getString(R.string.reminder));
                reminderView.setColor(ThemeManager.getColor(R.attr.colorTextGrey));
            }
        }
        invalidate();
        requestLayout();
    }

    public void setData(final Task task) {
        this.due = task.getDue();
        this.listMirakel = task.getList().getName();
        this.reminder = task.getReminder();
        this.isDone = task.isDone();
        this.dueRecurrence = task.getRecurrence();
        rebuildLayout();
    }

    public void setListeners(final OnClickListener dueEditListener,
                             final OnClickListener listEditListener,
                             final OnClickListener reminderEditListener,
                             final OnClickListener dueRecurrenceEditListener) {
        this.dueEditListener = dueEditListener;
        this.listEditListener = listEditListener;
        this.reminderEditListener = reminderEditListener;
        this.dueRecurrenceEditListener = dueRecurrenceEditListener;
        rebuildLayout();
    }

    @OnClick(R.id.dates_due)
    void onDueClick() {
        if (dueEditListener != null) {
            dueEditListener.onClick(dueView);
        }
    }

    @OnClick(R.id.dates_due_recurrence)
    void onDueRecurrenceClick() {
        if ((dueRecurrenceEditListener != null) && due.isPresent()) {
            dueRecurrenceEditListener.onClick(dueRecurrenceView);
        }
    }

    @OnClick(R.id.dates_list)
    void onListClick() {
        if (listEditListener != null) {
            listEditListener.onClick(listView);
        }
    }

    @OnClick(R.id.dates_reminder)
    void onReminderClick() {
        if (reminderEditListener != null) {
            reminderEditListener.onClick(reminderView);
        }
    }
}
