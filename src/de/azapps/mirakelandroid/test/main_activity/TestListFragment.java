package de.azapps.mirakelandroid.test.main_activity;

import java.util.List;
import java.util.Random;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.custom_views.TaskSummary;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TestListFragment extends
		ActivityInstrumentationTestCase2<MainActivity> {
	Solo solo;
	private Random randomGenerator;
	private static final String listname = "new list";

	public TestListFragment() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
		randomGenerator = new Random();
		solo.waitForActivity(MainActivity.class);
		solo.drag(0, 100, 50, 100, 100);
		solo.sleep(10);
	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}

	public void testAdd() {
		ListView l = (ListView) solo.getCurrentActivity().findViewById(
				R.id.lists_list);
		int countBegin = l.getChildCount();
		solo.clickOnActionBarItem(R.id.menu_new_list);
		solo.sleep(1000);
		solo.waitForDialogToOpen();
		solo.typeText(0, listname);
		solo.clickOnButton(solo.getString(android.R.string.ok));
		solo.waitForDialogToClose();
		TextView listName = (TextView) l.getChildAt(l.getChildCount() - 1)
				.findViewById(R.id.list_row_name);
		assertEquals("no new list created", countBegin + 1, l.getChildCount());
		assertEquals("name does not equal", listname, listName.getText()
				.toString());
	}

	public void testZDelete() {// strange name to force to be the last test....
		ListMirakel.init(getInstrumentation().getTargetContext());
		ListView l = (ListView) solo.getCurrentActivity().findViewById(
				R.id.lists_list);
		int listcount = ListMirakel.all().size();
		assertEquals("list did change since add test;)", listname,
				((TextView) (l.getChildAt(l.getChildCount() - 1))
						.findViewById(R.id.list_row_name)).getText().toString());
		solo.clickLongOnView(l.getChildAt(l.getChildCount() - 1));
		solo.sleep(100);
		solo.clickOnView(getActivity().findViewById(R.id.menu_delete));
		solo.sleep(100);
		solo.waitForDialogToOpen();
		solo.clickOnButton(solo.getString(android.R.string.ok));
		solo.waitForDialogToClose();
		assertEquals("delete: listcount did not match", listcount - 1,
				ListMirakel.all().size());
	}

	//
	// public void testMove() {
	//
	// }
	//
	// public void testShareList() {
	//
	// }
	//
	public void testEditList() {
		ListView l = (ListView) solo.getCurrentActivity().findViewById(
				R.id.lists_list);
		String listnameIntern = "test list";
		changeListName(l, listnameIntern, listname);
		changeListName(l, listname, listnameIntern);
	}

	private void changeListName(ListView l, String listname, String oldName) {
		assertEquals("list did change since add test;)", oldName,
				((TextView) (l.getChildAt(l.getChildCount() - 1))
						.findViewById(R.id.list_row_name)).getText().toString());
		solo.clickLongOnView(l.getChildAt(l.getChildCount() - 1));
		solo.sleep(100);
		solo.clickOnView(getActivity().findViewById(R.id.edit_list));
		solo.sleep(100);
		solo.waitForDialogToOpen();
		solo.clearEditText(0);
		solo.typeText(0, listname);
		solo.clickOnButton(solo.getString(android.R.string.ok));
		solo.waitForDialogToClose();
		assertEquals("edit: listname did not match", listname,
				((TextView) l.getChildAt(l.getChildCount() - 1).findViewById(R.id.list_row_name)).getText()
						.toString());
	}

	//
	// public void testChangeAccount() {
	//
	// }

	public void testOpenNavDrawer() {
		solo.clickOnScreen(100, 100);
		ListMirakel.init(getInstrumentation().getTargetContext());
		Task.init(getInstrumentation().getTargetContext());
		ListView taskView = (ListView) solo.getCurrentActivity().findViewById(
				R.id.tasks_list);
		for (int i = 0; i < 10; i++) {
			solo.drag(0, 100, 50, 100, 100);
			ListView l = (ListView) solo.getCurrentActivity().findViewById(
					R.id.lists_list);
			int listClicked = randomGenerator.nextInt(l.getChildCount());
			solo.clickOnView(l.getChildAt(listClicked));
			solo.waitForView(taskView);
			TaskSummary t = (TaskSummary) taskView.getChildAt(0);
			List<ListMirakel> lists = ListMirakel.all();
			List<Task> tasks = Task.getTasks(lists.get(listClicked).getId(),
					lists.get(listClicked).getSortBy(), false);
			if (tasks.size() == 0) {
				if (lists.get(listClicked).countTasks() == 0) {
					continue;// There is no task to check
				} else {
					assertFalse("no task found", true);
					break;
				}
			}
			if (t == null) {
				assertFalse("no view found", true);
				break;
			}
			assertEquals("change list: first Taskname is not equal",
					tasks.get(0).getName(),
					((TextView) t.findViewById(R.id.tasks_row_name)).getText()
							.toString());
		}

	}

}
