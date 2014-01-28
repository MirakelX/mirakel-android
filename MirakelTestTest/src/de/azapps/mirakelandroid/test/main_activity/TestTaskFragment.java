package de.azapps.mirakelandroid.test.main_activity;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakelandroid.R;

public class TestTaskFragment extends
		ActivityInstrumentationTestCase2<MainActivity> {
	Solo solo;

	public TestTaskFragment() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
		solo.clickInList(0);
	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}

	public void testName() {
		// Get Task name
		TextView taskName = (TextView) solo.getView(R.id.task_name);
		solo.clickOnView(taskName);
		solo.sendKey(Solo.ENTER);
	}

	public void testDone() {

	}

	public void testPriority() {
		solo.clickOnView(solo.getView(R.id.task_prio));
		assertTrue(solo.searchText(getActivity()
				.getString(R.string.task_change_prio_title)));
		solo.clickInList(0);
		//solo.goBack();

	}

	public void testDue() {

	}

	public void testReminder() {

	}

	public void testFile() {
		// Sound
		// Cam
		// Add
		// Delete
		// Click
	}

	public void testNote() {

	}

	public void testProgress() {

	}

	public void testSubtask() {
		// Add
		// Delete
		// Click
	}

	public void testDeleteTask() {

	}

	public void testMoveTask() {

	}

	public void testShare() {

	}

	public void testDuplicate() {

	}

	public void testAddAsSubtask() {

	}
}
