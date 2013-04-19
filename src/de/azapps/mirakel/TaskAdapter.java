package de.azapps.mirakel;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class TaskAdapter extends ArrayAdapter<Task> {
	Context context;
	int layoutResourceId;
	List<Task> data = null;
	OnClickListener clickCheckbox;
	OnClickListener clickPrio;

	public TaskAdapter(Context context, int layoutResourceId, List<Task> data,
			OnClickListener clickCheckbox, OnClickListener click_prio) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.context = context;
		this.clickCheckbox = clickCheckbox;
		this.clickPrio = click_prio;

	}

	/**
	 * Add a task to the head of the List
	 * 
	 * @param task
	 */
	void addToHead(Task task) {
		data.add(0, task);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TaskHolder holder = null;

		if (row == null) {
			// Initialize the View
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
			holder.taskRowName = (TextView) row
					.findViewById(R.id.tasks_row_name);
			holder.taskRowPriority = (TextView) row
					.findViewById(R.id.tasks_row_priority);
			holder.taskRowDue = (TextView) row.findViewById(R.id.tasks_row_due);

			row.setTag(holder);
		} else {
			holder = (TaskHolder) row.getTag();
		}

		Task task = data.get(position);

		// Done
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setOnClickListener(clickCheckbox);
		holder.taskRowDone.setTag(task);

		// Name
		holder.taskRowName.setText(task.getName());
		int nameColor;
		if (task.isDone()) {
			nameColor = R.color.Grey;
		} else {
			nameColor = R.color.Black;
		}
		holder.taskRowName.setTextColor(row.getResources().getColor(nameColor));

		// Priority
		holder.taskRowPriority.setText("" + task.getPriority());
		holder.taskRowPriority.setBackgroundColor(Mirakel.PRIO_COLOR[task
				.getPriority() + 2]);
		holder.taskRowPriority.setOnClickListener(clickPrio);
		holder.taskRowPriority.setTag(task);

		// Due
		holder.taskRowDue.setText(MirakelHelper.formatDate(task.getDue(),
				context.getString(R.string.dateFormat)));
		holder.taskRowDue.setTextColor(row.getResources().getColor(
				MirakelHelper.getTaskDueColor(task.getDue(), task.isDone())));

		return row;
	}

	/**
	 * The class, holding the Views of the Row
	 * 
	 * @author az
	 * 
	 */
	static class TaskHolder {
		CheckBox taskRowDone;
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue;
	}

}
