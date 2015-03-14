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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Optional;

import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

import static com.google.common.base.Optional.absent;

public class DatesView extends LinearLayout {
    private OnClickListener dueEditListener;
    private OnClickListener listEditListener;
    private OnClickListener reminderEditListener;
    @InjectView(R.id.dates_due)
    TextView dueText;
    @InjectView(R.id.dates_due_title)
    TextView dueTitleText;
    @InjectView(R.id.dates_list)
    TextView listText;
    @InjectView(R.id.dates_list_title)
    TextView listTitleText;
    @InjectView(R.id.dates_reminder)
    TextView reminderText;
    @InjectView(R.id.dates_reminder_title)
    TextView reminderTitleText;

    private Optional<Calendar> due = absent();
    private String listMirakel;
    private Optional<Calendar> reminder = absent();
    private boolean isDone;


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
        final Drawable listIcon = ThemeManager.getColoredIcon(R.drawable.ic_list_white_18dp,
                                  ThemeManager.getColor(R.attr.colorTextGrey));
        listTitleText.setCompoundDrawablesWithIntrinsicBounds(listIcon, null, null, null);
    }

    private void rebuildLayout() {
        final int dueColor, reminderColor;
        // Set data
        if (isInEditMode()) {
            dueText.setText("today");
            reminderText.setText("today at 6 pm");
            listText.setText("At home");
            dueColor = ThemeManager.getColor(R.attr.colorDisabled);
            reminderColor = ThemeManager.getColor(R.attr.colorDisabled);
        } else {
            if (due.isPresent()) {
                dueText.setText(DateTimeHelper.formatDate(getContext(), due));
                dueColor = TaskHelper.getTaskDueColor(getContext(), due, isDone);
            } else {
                dueText.setText(getContext().getString(R.string.no_date));
                dueColor = ThemeManager.getColor(R.attr.colorDisabled);
            }
            listText.setText(listMirakel);
            if (reminder.isPresent()) {
                reminderText.setText(DateTimeHelper.formatReminder(getContext(), reminder.get()));
                reminderColor = TaskHelper.getTaskDueColor(getContext(), reminder, false);
            } else {
                reminderText.setText(getContext().getString(R.string.no_reminder));
                reminderColor = ThemeManager.getColor(R.attr.colorDisabled);
            }
        }
        final Drawable dueIcon = ThemeManager.getColoredIcon(R.drawable.ic_calendar_white_18dp, dueColor);
        dueTitleText.setCompoundDrawablesWithIntrinsicBounds(dueIcon, null, null, null);
        dueTitleText.setTextColor(dueColor);
        dueText.setTextColor(dueColor);
        final Drawable reminderIcon = ThemeManager.getColoredIcon(R.drawable.ic_alarm_white_18dp,
                                      reminderColor);
        reminderTitleText.setCompoundDrawablesWithIntrinsicBounds(reminderIcon, null, null, null);
        reminderTitleText.setTextColor(reminderColor);
        reminderText.setTextColor(reminderColor);
        invalidate();
        requestLayout();
    }

    public void setData(final Task task) {
        this.due = task.getDue();
        this.listMirakel = task.getList().getName();
        this.reminder = task.getReminder();
        this.isDone = task.isDone();
        rebuildLayout();
    }

    public void setListeners(final OnClickListener dueEditListener,
                             final OnClickListener listEditListener,
                             final OnClickListener reminderEditListener) {
        this.dueEditListener = dueEditListener;
        this.listEditListener = listEditListener;
        this.reminderEditListener = reminderEditListener;
        rebuildLayout();
    }

    @OnClick(R.id.dates_due_wrapper)
    void onDueClick() {
        if (dueEditListener != null) {
            dueEditListener.onClick(dueText);
        }
    }

    @OnClick(R.id.list_wrapper)
    void onListClick() {
        if (listEditListener != null) {
            listEditListener.onClick(listText);
        }
    }

    @OnClick(R.id.reminder_wrapper)
    void onReminderClick() {
        if (reminderEditListener != null) {
            reminderEditListener.onClick(reminderText);
        }
    }

}
