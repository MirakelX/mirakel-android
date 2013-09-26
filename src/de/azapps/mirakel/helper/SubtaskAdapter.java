package de.azapps.mirakel.helper;

import java.util.List;

import de.azapps.mirakel.model.task.Task;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SubtaskAdapter extends ArrayAdapter<Task> {
	
	private List<Task> data;
	private Context context;
	private boolean[] checked;

	public SubtaskAdapter(Context context, int textViewResourceId,
			List<Task> objects) {
		super(context, textViewResourceId, objects);
		this.data=objects;
		this.context=context;
		checked=new boolean[data.size()];
	}
	
	@Override
	public int getCount() {
		return data.size();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		CheckBox c=new CheckBox(context);
		c.setChecked(checked[position]);
		c.setText(data.get(position).getName());
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {		
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				checked[position]=isChecked;				
			}
		});
		return c;
	}
	
	public boolean[] getChecked(){
		return checked;
	}
	
	public List<Task> getData(){
		return data;
	}
	
	public void setData(List<Task> newData){
		data=newData;
		checked=new boolean[data.size()];
		notifyDataSetChanged();
	}

}
