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
package de.azapps.mirakel.settings.taskfragment;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.main_activity.DragNDropListView;
import de.azapps.mirakel.main_activity.DragNDropListView.RemoveListener;
import de.azapps.mirakelandroid.R;

@SuppressLint("NewApi")
public class TaskFragmentSettingsFragment extends Fragment {
	private final static String TAG = "de.azapps.mirakel.settings.taskfragment.TaskFragmentSettings";
	private DragNDropListView listView;
	private TaskFragmentSettingsAdapter adapter;
	public static final int ADD_KEY=-1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_task_fragment_settings,
				null);
		setupView(view);
		getActivity().getActionBar().setTitle(R.string.settings_task_fragment);
		return view;
	}

	void setupView(View v) {
		final List<Integer> values = MirakelPreferences.getTaskFragmentLayout();
		values.add(ADD_KEY);

		if (adapter != null) {
			adapter.changeData(values);
			adapter.notifyDataSetChanged();
			return;
		}

		adapter = new TaskFragmentSettingsAdapter(getActivity(),
				R.layout.row_taskfragment_settings, values);
		listView = (DragNDropListView) v.findViewById(R.id.taskfragment_list);
		listView.setEnableDrag(true);
		listView.setItemsCanFocus(true);
		listView.setAdapter(adapter);
		listView.requestFocus();
		listView.setDragListener(new DragNDropListView.DragListener() {

			@Override
			public void onStopDrag(View itemView) {
				itemView.setVisibility(View.VISIBLE);

			}

			@Override
			public void onStartDrag(View itemView) {
				itemView.setVisibility(View.INVISIBLE);

			}

			@Override
			public void onDrag(int x, int y, ListView l) {
				// Nothing
			}
		});
		listView.setDropListener(new DragNDropListView.DropListener() {

			@Override
			public void onDrop(int from, int to) {
				if (from != to&&to!=listView.getCount()-1) {
					adapter.onDrop(from, to);
					listView.requestLayout();
				}
				Log.e(TAG, "Drop from:" + from + " to:" + to);

			}
		});

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
			}
		});
		listView.setRemoveListener(new RemoveListener() {
			
			@Override
			public void onRemove(int which) {
				if(which!=adapter.getCount()-1)
					adapter.onRemove(which);				
			}
		});
		listView.allowRemove(true);
	}

}
