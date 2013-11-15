package de.azapps.mirakel.adapter;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.azapps.mirakel.model.task.Task;

public class SubtaskAdapter extends ArrayAdapter<Task> {

	private List<Task> data;
	private Context context;
	private boolean[] checked;
	private Task task;
	private boolean asSubtask;

	public SubtaskAdapter(Context context, int textViewResourceId,
			List<Task> objects, Task task, boolean asSubtask) {
		super(context, textViewResourceId, objects);
		this.data = objects;
		this.context = context;
		this.task = task;
		checked = new boolean[data.size()];
		this.asSubtask = asSubtask;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		if (position >= data.size()) {
			return new View(context);
		}
		CheckBox c = new CheckBox(context);
		if (!asSubtask) {
			checked[position] = data.get(position).isSubtaskOf(task);
		} else {
			checked[position] = task.isSubtaskOf(data.get(position));
		}
		c.setChecked(checked[position]);
		c.setText(data.get(position).getName());
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				checked[position] = isChecked;
			}
		});
		return c;
	}

	public boolean[] getChecked() {
		return checked;
	}

	public List<Task> getData() {
		return data;
	}

	final Handler mHandler = new Handler();

	public void setData(List<Task> newData) {
		data = newData;
		checked = new boolean[data.size()];
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});

	}

}
