package de.azapps.mirakel.settings.taskfragment;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.main_activity.DragNDropListView;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter;
import de.azapps.mirakelandroid.R;

public class TaskFragmentSettings extends Activity {
	private final static String TAG = "de.azapps.mirakel.settings.taskfragment.TaskFragmentSettings";
	private DragNDropListView listView;
	private TaskFragmentSettingsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// The activity is being created.
		if (MirakelPreferences.isDark())
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(R.layout.activity_task_fragment_settings);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		final List<Pair<Integer, Boolean>> values = TaskFragmentAdapter
				.getValuesForConfig(this);

		if (adapter != null) {
			adapter.changeData(values);
			adapter.notifyDataSetChanged();
			return;
		}

		adapter = new TaskFragmentSettingsAdapter(this,
				R.layout.row_taskfragment_settings, values);
		listView = (DragNDropListView) findViewById(R.id.taskfragment_list);
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
			public void onDrag(int x, int y, ListView listView) {
				// Nothing
			}
		});
		listView.setDropListener(new DragNDropListView.DropListener() {

			@Override
			public void onDrop(int from, int to) {
				if (from != to) {
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
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Another activity is taking focus (this activity is about to be
		// "paused").
	}

}
