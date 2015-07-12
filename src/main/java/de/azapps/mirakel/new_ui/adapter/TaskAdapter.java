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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.query_builder.CursorGetter;
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
        ListMirakelInterface.ShowDoneCases shouldShowDoneToggle();
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
    public TaskOverview fromCursor(@NonNull final Cursor cursor) {
        return new TaskOverview(CursorGetter.unsafeGetter(cursor));
    }

    @Override
    public TaskViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = mInflater.inflate(R.layout.row_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final TaskViewHolder viewHolder, final Cursor cursor,
                                 final int position) {
        if (position == 0) {
            final int normal = (int) mContext.getResources().getDimension(R.dimen.card_margin);
            final int bottom = (int) mContext.getResources().getDimension(R.dimen.card_margin_bottom);
            final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(normal, normal, normal, bottom);
            viewHolder.card.setLayoutParams(params);
        } else {
            final int normal = (int) mContext.getResources().getDimension(R.dimen.card_margin);
            final int top = (int) mContext.getResources().getDimension(R.dimen.card_margin_top);
            final int bottom = (int) mContext.getResources().getDimension(R.dimen.card_margin_bottom);
            final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(normal, top, normal, bottom);
            viewHolder.card.setLayoutParams(params);
        }
        if (ViewHelper.isRTL(mContext)) {
            final int size = (int) mContext.getResources().getDimension(R.dimen.priorityDoneSize);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins((int) mContext.getResources().getDimension(R.dimen.margin_progress_done), 0, 0,
                              0);
            params.gravity = Gravity.CENTER_VERTICAL;
            viewHolder.progressDone.setLayoutParams(params);

            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                   ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins((int) mContext.getResources().getDimension(R.dimen.task_due_margin), 0, 0, 0);
            viewHolder.due.setLayoutParams(params);
        }
        if (position == (getItemCount() - 1)) {
            viewHolder.viewSwitcher.setDisplayedChild(1);
            final ListMirakelInterface.ShowDoneCases status = taskAdapterCallbacks.shouldShowDoneToggle();
            if (status != ListMirakelInterface.ShowDoneCases.BOTH) {
                viewHolder.showDoneTasks.setVisibility(View.GONE);
                if ((status == ListMirakelInterface.ShowDoneCases.ONLY_DONE) &&
                    !taskAdapterCallbacks.shouldShowDone()) {
                    taskAdapterCallbacks.toggleShowDoneTasks();
                }
            } else {
                viewHolder.showDoneTasks.setVisibility(View.VISIBLE);
                if (taskAdapterCallbacks.shouldShowDone()) {
                    viewHolder.showDoneTasks.setText(R.string.hide_done_tasks);
                } else {
                    viewHolder.showDoneTasks.setText(R.string.show_done_tasks);
                }
            }
        } else {
            viewHolder.viewSwitcher.setDisplayedChild(0);
            final TaskOverview task = new TaskOverview(CursorGetter.unsafeGetter(cursor));
            viewHolder.task = task;
            viewHolder.name.setText(task.getName());
            viewHolder.name.setStrikeThrough(task.isDone());
            if (task.getDue().isPresent()) {
                viewHolder.due.setVisibility(View.VISIBLE);
                viewHolder.due.setText(DateTimeHelper.formatDate(mContext,
                                       task.getDue()));
                viewHolder.due.setTextColor(TaskHelper.getTaskDueColor(task.getDue(),
                                            task.isDone()));
            } else {
                viewHolder.due.setVisibility(View.GONE);
            }
            viewHolder.list.setText(task.getListName());
            viewHolder.priority.setPriority(task.getPriority());
            // otherwise the OnCheckedChangeListener will be called
            viewHolder.progressDone.setOnCheckedChangeListener(null);
            viewHolder.progressDone.setChecked(task.isDone());
            viewHolder.progressDone.setProgress(task.getProgress());
            viewHolder.progressDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

            if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
                if (selectedItems.get(position)) {
                    viewHolder.card.setBackgroundResource(R.drawable.background_task_selected);
                } else {
                    viewHolder.card.setBackgroundResource(R.drawable.background_task);
                }
            } else {
                if (selectedItems.get(position)) {
                    viewHolder.card.setCardBackgroundColor(ThemeManager.getColor(R.attr.colorSelectedRow));
                } else {
                    viewHolder.card.setCardBackgroundColor(ThemeManager.getColor(R.attr.colorTaskCard));
                }
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
    protected boolean isSelectable(int pos) {
        if (pos < getCursor().getCount()) {
            return true;
        }
        return false;
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
