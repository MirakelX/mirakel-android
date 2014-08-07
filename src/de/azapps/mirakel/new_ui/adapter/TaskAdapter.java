package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;

public class TaskAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private View.OnClickListener itemClickListener;

    public TaskAdapter(Context context, Cursor c, int flags, View.OnClickListener itemClickListener) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemClickListener = itemClickListener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.row_task, null);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        view.setOnClickListener(itemClickListener);
        Task task = new Task(cursor);
        viewHolder.task = task;
        viewHolder.name.setText(task.getName());
        if (task.getDue() != null) {
            viewHolder.due.setVisibility(View.VISIBLE);
            viewHolder.due.setText(DateTimeHelper.formatDate(context,
                                   task.getDue()));
            viewHolder.due.setTextColor(TaskHelper.getTaskDueColor(context, task.getDue(),
                                        task.isDone()));
        } else {
            viewHolder.due.setVisibility(View.GONE);
        }
        viewHolder.list.setText(task.getList().getName());
        viewHolder.priorityDone.setChecked(task.isDone());
        viewHolder.priorityDone.setProgress(task.getProgress());
        viewHolder.priorityDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO implement this
                Toast.makeText(context, "Check", Toast.LENGTH_LONG).show();
            }
        });
    }

    public static class ViewHolder {
        final TextView name;
        final TextView due;
        final TextView list;
        final ProgressDoneView priorityDone;
        private Task task;

        public ViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.task_name);
            due = (TextView) view.findViewById(R.id.task_due);
            list = (TextView) view.findViewById(R.id.task_list);
            priorityDone = (ProgressDoneView) view.findViewById(R.id.task_priority_done);
        }

        public Task getTask() {
            return task;
        }
    }
}
