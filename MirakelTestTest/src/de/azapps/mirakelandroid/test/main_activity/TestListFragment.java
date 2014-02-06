package de.azapps.mirakelandroid.test.main_activity;

import java.util.List;
import java.util.Random;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.custom_views.TaskSummary;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TestListFragment extends
		ActivityInstrumentationTestCase2<MainActivity> {
	Solo solo;
	private Random randomGenerator;

	public TestListFragment() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
		randomGenerator = new Random();
	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}
//
//	public void testClick() {
//
//	}
//
//	public void testAdd() {
//
//	}
//
//	public void testDelete() {
//
//	}
//
//	public void testMove() {
//
//	}
//
//	public void testShareList() {
//
//	}
//
//	public void testEditList() {
//
//	}
//
//	public void testChangeAccount() {
//
//	}
	
	public void testOpenNavDrawer(){
		ListMirakel.init(getInstrumentation().getTargetContext());
		Task.init(getInstrumentation().getTargetContext());
		ListView taskView=(ListView)solo.getCurrentActivity().findViewById(R.id.tasks_list);
		for(int i=0;i<25;i++){
			solo.drag(0, 100, 50, 100, 100);
			ListView l = (ListView) solo.getCurrentActivity().findViewById(R.id.lists_list);
			int listClicked=randomGenerator.nextInt(l.getChildCount());
			solo.clickOnView(l.getChildAt(listClicked));
			solo.waitForView(taskView);
			TaskSummary t=(TaskSummary) taskView.getChildAt(0);
			List<ListMirakel> lists=ListMirakel.all();
			List<Task> tasks=Task.getTasks(lists.get(listClicked).getId(), lists.get(listClicked).getSortBy(), false);
			if(tasks.size()==0){
				if(lists.get(listClicked).countTasks()==0){
					continue;//There is no task to check
				}else{
					assertFalse("no task found", true);
					break;
				}
			}
			if(t==null){
				assertFalse("no view found", true);
				break;
			}
			assertEquals("change list: first Taskname is not equal",tasks.get(0).getName(),((TextView)t.findViewById(R.id.tasks_row_name)).getText().toString());
		}
			
	}

}
