package de.azapps.mirakelandroid.test.main_activity;

import java.util.List;
import java.util.Random;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;
import com.robotium.solo.Solo;

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
		this.solo = new Solo(getInstrumentation(), getActivity());
		this.randomGenerator = new Random();
		this.solo.waitForActivity(MainActivity.class);
		this.solo.drag(0, 100, 50, 100, 100);
		this.solo.sleep(10);
	}

	@Override
	protected void tearDown() throws Exception {
		this.solo.finishOpenedActivities();
	}

	public void testAdd() {
		// ListView l = (ListView) solo.getCurrentActivity().findViewById(
		// R.id.lists_list);
		final ListView l = findListView(R.id.lists_list);
		final int countBegin = l.getChildCount();
		this.solo.clickOnActionBarItem(R.id.menu_new_list);
		this.solo.sleep(1000);
		this.solo.waitForDialogToOpen();
		this.solo.typeText(0, listname);
		this.solo.clickOnButton(this.solo.getString(android.R.string.ok));
		this.solo.waitForDialogToClose();
		final TextView listName = (TextView) l
				.getChildAt(l.getChildCount() - 1).findViewById(
						R.id.list_row_name);
		assertTrue(this.solo.searchText(listname));
		assertEquals("no new list created", countBegin + 1, l.getChildCount());
		assertEquals("name does not equal", listname, listName.getText()
				.toString());
	}

	public void testZDelete() {// strange name to force to be the last test....
		ListMirakel.init(getInstrumentation().getTargetContext());
		final ListView l = findListView(R.id.lists_list);
		final int listcount = ListMirakel.all().size();
		assertEquals(
				"list did change since add test;)",
				listname,
				((TextView) l.getChildAt(l.getChildCount() - 1).findViewById(
						R.id.list_row_name)).getText().toString());
		this.solo.clickLongOnView(l.getChildAt(l.getChildCount() - 1));
		this.solo.sleep(100);
		this.solo.clickOnView(getActivity().findViewById(R.id.menu_delete));
		this.solo.sleep(100);
		this.solo.waitForDialogToOpen();
		this.solo.clickOnButton(this.solo.getString(android.R.string.ok));
		this.solo.waitForDialogToClose();
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
		final ListView l = findListView(R.id.lists_list);
		final String listnameIntern = "test list";
		changeListName(l, listnameIntern, listname);
		changeListName(l, listname, listnameIntern);
	}

	ListView findListView(final int id) {
		if (id == R.id.lists_list) {
			return this.solo.getView(DragSortListView.class, 0);
		}
		final List<ListView> l = this.solo.getCurrentViews(ListView.class);
		for (final ListView v : l) {
			if (v.getId() == id) {
				return v;
			}
		}
		throw new NullPointerException("No list list found");
	}

	private void changeListName(final ListView l, final String listname,
			final String oldName) {
		assertEquals(
				"list did change since add test;)",
				oldName,
				((TextView) l.getChildAt(l.getChildCount() - 1).findViewById(
						R.id.list_row_name)).getText().toString());
		this.solo.clickLongOnView(l.getChildAt(l.getChildCount() - 1));
		this.solo.sleep(100);
		this.solo.clickOnView(getActivity().findViewById(R.id.edit_list));
		this.solo.sleep(100);
		this.solo.waitForDialogToOpen();
		this.solo.clearEditText(0);
		this.solo.typeText(0, listname);
		this.solo.clickOnButton(this.solo.getString(android.R.string.ok));
		this.solo.waitForDialogToClose();
		assertEquals(
				"edit: listname did not match",
				listname,
				((TextView) l.getChildAt(l.getChildCount() - 1).findViewById(
						R.id.list_row_name)).getText().toString());
	}

	//
	// public void testChangeAccount() {
	//
	// }

	public void testOpenNavDrawer() {
		this.solo.clickOnScreen(100, 100);
		ListMirakel.init(getInstrumentation().getTargetContext());
		Task.init(getInstrumentation().getTargetContext());
		final ListView taskView = findListView(R.id.tasks_list);
		for (int i = 0; i < 10; i++) {
			this.solo.drag(0, 100, 50, 100, 100);
			final ListView l = findListView(R.id.lists_list);
			final int listClicked = this.randomGenerator.nextInt(l
					.getChildCount());
			this.solo.clickOnView(l.getChildAt(listClicked));
			this.solo.waitForView(taskView);
			final TaskSummary t = (TaskSummary) taskView.getChildAt(0);
			final List<ListMirakel> lists = ListMirakel.all();
			final List<Task> tasks = Task.getTasks(lists.get(listClicked)
					.getId(), lists.get(listClicked).getSortBy(), false);
			if (tasks.size() == 0) {
				if (lists.get(listClicked).countTasks() == 0) {
					continue;// There is no task to check
				}
				assertFalse("no task found", true);
				break;
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
