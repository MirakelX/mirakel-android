/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.main_activity.TaskFragmentAdapter.TYPE;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TaskFragment extends Fragment {
	private static final String TAG = "TaskActivity";

	public TaskFragmentAdapter adapter;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		MainActivity main = (MainActivity) getActivity();
		View view = inflater.inflate(R.layout.task_fragment, container, false);
		ListView listView = (ListView) view.findViewById(R.id.taskFragment);
		List<Pair<Integer,Integer>> data = generateData(main.getCurrentTask());
		Log.d(TAG, data.size() + " entries");
		adapter = new TaskFragmentAdapter(main, R.layout.task_head_line, data,
				main.getCurrentTask());
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		Log.d(TAG, "created");
		return view;
	}

	public static List<Pair<Integer,Integer>> generateData(Task task) {
		//TODO implement Subtasks
		List<Pair<Integer,Integer>> data = new ArrayList<Pair<Integer,Integer>>();
		data.add(new Pair<Integer, Integer>(TYPE.HEADER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.DUE, 0));
		data.add(new Pair<Integer, Integer>(TYPE.REMINDER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.CONTENT, 0));
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, 0));
		// for(task.)
		// data.add(TYPE.SUBTASK);
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, 1));
		int fileCount=FileMirakel.getFileCount(task);
		for (int i=0;i<fileCount;i++)
			data.add(new Pair<Integer, Integer>(TYPE.FILE, i));
		return data;
	}

	public void update(Task t) {
		if (adapter != null) {
			adapter.setData(generateData(t), t);
			adapter.notifyDataSetChanged();
		}
	}

}
