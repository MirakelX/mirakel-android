package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.views.PriorityDoneView;

public class TaskAdapter extends CursorAdapter {

	private LayoutInflater mInflater;

	public TaskAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.row_task, null);
		ViewHolder viewHolder = new ViewHolder(view);
		view.setTag(viewHolder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		Task task = new Task(cursor);
		viewHolder.name.setText(task.getName());

		if (task.getDue() != null) {
			viewHolder.due.setVisibility(View.VISIBLE);
			viewHolder.due.setText(DateTimeHelper.formatDate(context,
					task.getDue()));
			viewHolder.due.setTextColor(context.getResources().getColor(
					TaskHelper.getTaskDueColor(task.getDue(),
							task.isDone())));
		} else {
			viewHolder.due.setVisibility(View.GONE);
		}

		viewHolder.list.setText(task.getList().getName());
		viewHolder.priorityDone.setDone(task.isDone());
		viewHolder.priorityDone.setProgress(task.getProgress());
	}

	static class ViewHolder {
		TextView name;
		TextView due;
		TextView list;
		PriorityDoneView priorityDone;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.task_name);
			due = (TextView) view.findViewById(R.id.task_due);
			list = (TextView) view.findViewById(R.id.task_list);
			priorityDone = (PriorityDoneView) view.findViewById(R.id.task_priority_done);
		}
	}
}
