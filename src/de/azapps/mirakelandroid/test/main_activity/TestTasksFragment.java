package de.azapps.mirakelandroid.test.main_activity;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakelandroid.R;

public class TestTasksFragment extends
		ActivityInstrumentationTestCase2<MainActivity> {

	Solo solo;

	public TestTasksFragment() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	protected void tearDown() throws Exception {
		this.solo.finishOpenedActivities();
	}

	/**
	 * Create new task and check if it was correctly inserted
	 * 
	 * @param message
	 * @param task
	 * @param expected
	 */
	private void insertAndTestNewTask(final String message, final String task,
			final String expected) {
		final EditText newTask = (EditText) this.solo.getView(R.id.tasks_new);
		this.solo.clearEditText(newTask);
		this.solo.enterText(newTask, task);
		this.solo.clickOnView(this.solo.getView(R.id.btnEnter));
		final boolean actual = this.solo.searchText(expected);
		assertTrue(message, actual);
	}

	public void testAddTask() {
		// First a simple task
		final String taskName = "tassssk";
		insertAndTestNewTask("Simple task not inserted", taskName, taskName);
		// Now a semantic
		insertAndTestNewTask("Semantic task wrong inserted (Today)", "today "
				+ taskName, taskName);
	}

	public void testChangePriority() {

	}

	public void testChangeDone() {

	}

	public void testDeleteList() {

	}

	public void testDeleteTasks() {

	}

	public void testSearch() {

	}

	public void testShare() {

	}

	public void testSort() {

	}
}
