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

package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;

import java.net.InterfaceAddress;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskOverview;
import de.azapps.mirakel.new_ui.views.PriorityView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.TaskNameView;
import de.azapps.mirakelandroid.R;

public class TaskAdapter extends
    MultiSelectCursorAdapter<TaskAdapter.TaskViewHolder, TaskOverview> {

    private final LayoutInflater mInflater;
    private final TaskAdapterCallbacks taskAdapterCallbacks;


    public interface TaskAdapterCallbacks {
        void toggleShowDoneTasks();
        boolean shouldShowDone();
        boolean shouldShowDoneToggle();
    }

    public TaskAdapter(final Context context, final Cursor cursor,
                       final OnItemClickedListener<TaskOverview> itemClickListener,
                       final MultiSelectCallbacks<TaskOverview> multiSelectCallbacks,
                       final TaskAdapterCallbacks taskAdapterCallbacks) {
        super(context, cursor, itemClickListener, multiSelectCallbacks);
        mInflater = LayoutInflater.from(context);
        this.taskAdapterCallbacks = taskAdapterCallbacks;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public TaskOverview fromCursor(@NonNull Cursor cursor) {
        return new TaskOverview(cursor);
    }

    @Override
    public TaskViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int i) {
        final View view = mInflater.inflate(R.layout.row_task, viewGroup, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final TaskViewHolder holder, final Cursor cursor, int position) {
        if (position == getItemCount() - 1) {
            holder.viewSwitcher.setDisplayedChild(1);
            if (taskAdapterCallbacks.shouldShowDoneToggle()) {
                holder.showDoneTasks.setVisibility(View.VISIBLE);
                if (taskAdapterCallbacks.shouldShowDone()) {
                    holder.showDoneTasks.setText(R.string.hide_done_tasks);
                } else {
                    // if no tasks are displayed it would be useless to not show the done tasks
                    if (getItemCount() == 1 && taskAdapterCallbacks.shouldShowDoneToggle()) {
                        taskAdapterCallbacks.toggleShowDoneTasks();
                    }
                    holder.showDoneTasks.setText(R.string.show_done_tasks);
                }
            } else {
                holder.showDoneTasks.setVisibility(View.GONE);
            }
        } else {
            holder.viewSwitcher.setDisplayedChild(0);
            final TaskOverview task = new TaskOverview(cursor);
            holder.task = task;
            holder.name.setText(task.getName());
            holder.name.setStrikeThrough(task.isDone());
            if (task.getDue().isPresent()) {
                holder.due.setVisibility(View.VISIBLE);
                holder.due.setText(DateTimeHelper.formatDate(mContext,
                                   task.getDue()));
                holder.due.setTextColor(TaskHelper.getTaskDueColor(task.getDue(),
                                        task.isDone()));
            } else {
                holder.due.setVisibility(View.GONE);
            }
            holder.list.setText(task.getListName());
            holder.priority.setPriority(task.getPriority());
            // otherwise the OnCheckedChangeListener will be called
            holder.progressDone.setOnCheckedChangeListener(null);
            holder.progressDone.setChecked(task.isDone());
            holder.progressDone.setProgress(task.getProgress());
            holder.progressDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    final Optional<Task> taskOptional = task.getTask();
                    if (taskOptional.isPresent()) {
                        final Task task = taskOptional.get();
                        task.setDone(isChecked);
                        task.save();
                    }
                }
            });

            if (selectedItems.get(position)) {
                holder.card.setCardBackgroundColor(ThemeManager.getColor(R.attr.colorSelectedRow));
            } else {
                holder.card.setCardBackgroundColor(ThemeManager.getColor(R.attr.colorTaskCard));
            }
        }
    }



    public class TaskViewHolder extends MultiSelectCursorAdapter.MultiSelectViewHolder {
        @InjectView(R.id.task_name)
        TaskNameView name;
        @InjectView(R.id.task_due)
        TextView due;
        @InjectView(R.id.task_list)
        TextView list;
        @InjectView(R.id.task_progress_done)
        ProgressDoneView progressDone;
        @InjectView(R.id.task_card)
        CardView card;
        @InjectView(R.id.priority)
        PriorityView priority;
        @InjectView(R.id.show_done_tasks)
        TextView showDoneTasks;
        @InjectView(R.id.view_switcher)
        ViewSwitcher viewSwitcher;
        TaskOverview task;

        public TaskViewHolder(final View view) {
            super(view);
            view.setOnClickListener(this);
            ButterKnife.inject(this, view);
        }

        @OnClick(R.id.show_done_tasks)
        public void toggleShowDone() {
            taskAdapterCallbacks.toggleShowDoneTasks();
        }
    }

    // This is a dirty workaround to enable „footer” views s.t. we can show the toggle button

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    @Override
    public void onBindViewHolder(final TaskViewHolder viewHolder, final int position) {
        if (position == getItemCount() - 1) {
            onBindViewHolder(viewHolder, null, position);
        } else {
            super.onBindViewHolder(viewHolder, position);
        }
    }
}
