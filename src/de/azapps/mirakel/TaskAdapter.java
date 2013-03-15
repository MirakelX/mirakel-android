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

	public TaskAdapter(Context context, int layoutResourceId, List<Task> data,
			OnClickListener clickCheckbox) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.context = context;
		this.clickCheckbox = clickCheckbox;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TaskHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
			holder.taskRowName = (TextView) row
					.findViewById(R.id.tasks_row_name);
			holder.taskRowPriority = (TextView) row
					.findViewById(R.id.tasks_row_priority);

			row.setTag(holder);
		} else {
			holder = (TaskHolder) row.getTag();
		}

		Task task = data.get(position);
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setOnClickListener(clickCheckbox);
		holder.taskRowDone.setTag(task);
		holder.taskRowName.setText(task.getName());
		holder.taskRowPriority.setText("" + task.getPriority());
		holder.taskRowPriority.setBackgroundColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);
		// TODO implement onClickListener
		//holder.taskRowPriority.setOnClickListener();
		return row;
	}

	static class TaskHolder {
		CheckBox taskRowDone;
		TextView taskRowName;
		TextView taskRowPriority;
	}

}
