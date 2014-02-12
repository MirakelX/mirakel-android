package de.azapps.mirakelandroid.test.main_activity;

import java.util.GregorianCalendar;
import java.util.Random;

import android.graphics.Point;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakelandroid.R;

public class TestTaskFragment extends
		ActivityInstrumentationTestCase2<MainActivity> {
	private static final String TAG = "TEST_MIRAKEL";
	Solo solo;
	private Random randomGenerator;

	public TestTaskFragment() {
		super(MainActivity.class);
	}

	public enum Direction {
		Left, Right;
	}

	protected void swipe(final Direction direction) {
		Point size = new Point();
		getActivity().getWindowManager().getDefaultDisplay().getSize(size);
		int width = size.x;
		float xStart = ((direction == Direction.Left) ? (width - 10) : 10);
		float xEnd = ((direction == Direction.Left) ? 10 : (width - 10));

		// The value for y doesn't change, as we want to swipe straight across
		solo.drag(xStart, xEnd, size.y / 2, size.y / 2, 1);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		randomGenerator = new Random();
		solo = new Solo(getInstrumentation(), getActivity());
		solo.waitForActivity(MainActivity.class);
		View row = solo.getView(R.id.tasks_row);
		solo.clickOnView(row);
		assertTrue("faild to setup", solo.waitForView(R.id.task_content));

	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}

	public void testName() {
		// Get Task name
		TextView taskName = (TextView) solo.getView(R.id.task_name);
		solo.clickOnView(taskName);
		String newName = "testText " + randomGenerator.nextInt(1000);
		solo.waitForView(R.id.edit_name);
		final EditText edittext = (EditText) solo.getView(R.id.edit_name);
		solo.clearEditText(edittext);
		solo.enterText(edittext, newName);
		// solo.enterText(R.id.edit_name, newName);
		Runnable runnable = new Runnable()

		{

			public void run()

			{
				edittext.onEditorAction(EditorInfo.IME_ACTION_DONE);

			}
		}; // Use Solo to get the current activity, and pass our runnable to the
			// UI // thread.

		solo.getCurrentActivity().runOnUiThread(runnable);

		solo.waitForView(R.id.task_name);
		assertEquals("Change Name failed", taskName.getText().toString(),
				newName);
	}

	//
	public void testDone() {
		CheckBox doneBox = (CheckBox) solo.getView(R.id.task_done);
		boolean state = doneBox.isChecked();
		solo.clickOnView(doneBox);
		solo.sleep(100);
		state = !state;
		assertEquals("Done does not work", doneBox.isChecked(), state);
	}

	public void testPriority() {
		TextView prio = (TextView) solo.getView(R.id.task_prio);
		solo.clickOnView(prio);
		solo.waitForDialogToOpen();
		assertTrue(solo.searchText(getActivity().getString(
				R.string.task_change_prio_title)));
		int newPrio = randomGenerator.nextInt(4) - 2;
		Log.d(TAG, "newPrio:" + newPrio);
		solo.clickOnText(newPrio + "");
		solo.waitForDialogToClose();
		Log.wtf(TAG, newPrio + "  " + prio.getText());
		assertEquals("Prio does not work", prio.getText().toString(), newPrio
				+ "");
	}

	public void testReminder() {
		// TODO set custom date and time
		TextView reminder = (TextView) solo.getView(R.id.task_reminder);
		if (!reminder.getText().toString()
				.equals(solo.getString(R.string.no_reminder))) {
			clearReminder(reminder);
		}

		solo.clickOnView(reminder);
		solo.waitForDialogToOpen();
		solo.clickOnButton(solo.getString(android.R.string.ok));
		solo.waitForDialogToClose();
		assertEquals("Change reminder failed", DateTimeHelper.formatReminder(
				getActivity(), new GregorianCalendar()), reminder.getText()
				.toString());

		clearReminder(reminder);
	}

	public void testDue() {
		TextView due = (TextView) solo.getView(R.id.task_due);
		if (!due.getText().toString().equals(solo.getString(R.string.no_date))) {
			clearDue(due);
		}

		solo.clickOnView(due);
		solo.waitForDialogToOpen();
		// TODO Fix here to set other due
		// Calendar then = new GregorianCalendar();
		// then.add(Calendar.MONTH, 1);
		// then.add(Calendar.DAY_OF_MONTH, 1);
		//
		// // solo.setDatePicker(R.id.date_picker, now.getYear(),
		// now.getMonth(),
		// // now.getDay());
		// String text = DateUtils.formatDateRange(getActivity(),
		// then.getTimeInMillis(), then.getTimeInMillis(), 52).toString();
		// Log.d(TAG, text);
		// Log.d(TAG, new SimpleDateFormat("MM").format(then.getTime()));
		// for (int i = then.get(Calendar.MONTH)
		// - (new GregorianCalendar()).get(Calendar.MONTH); i > 0; i--) {
		// solo.scrollDown();
		// }
		// solo.clickOnText(then.get(Calendar.DAY_OF_MONTH) + "");
		// Log.d(TAG, then.get(Calendar.DAY_OF_MONTH) + "");
		// solo.clickOnText(then.get(Calendar.YEAR) + "");
		solo.clickOnButton(solo.getString(android.R.string.ok));
		solo.waitForDialogToClose();
		// if (((CheckBox) solo.getView(R.id.task_done)).isChecked()) {
		// Log.d(TAG,"done");
		// assertEquals("Change due failed",
		// DateTimeHelper.formatDate(new GregorianCalendar()), due
		// .getText().toString());
		// } else {
		// Log.d(TAG,"not done");
		assertEquals("Change due failed", DateTimeHelper.formatDate(
				getActivity(), new GregorianCalendar()), due.getText()
				.toString());
		// }

		clearDue(due);

	}

	private void clearDue(TextView due) {
		solo.clickOnView(due);
		solo.waitForDialogToOpen();
		solo.clickOnButton(solo.getString(R.string.no_date));
		solo.waitForDialogToClose();
		assertEquals("Clear due failed", solo.getString(R.string.no_date), due
				.getText().toString());
	}

	private void clearReminder(TextView reminder) {
		solo.clickOnView(reminder);
		solo.waitForDialogToOpen();
		solo.clickOnButton(solo.getString(R.string.no_date));
		solo.waitForDialogToClose();
		assertEquals("Clear reminder failed",
				solo.getString(R.string.no_reminder), reminder.getText()
						.toString());
	}

	//
	// public void testReminder() {
	//
	// }
	//
	// public void testFile() {
	// // Sound
	// // Cam
	// // Add
	// // Delete
	// // Click
	// }
	//
	public void testNote() {
		// ImageButton edit=(ImageButton) solo.getView(R.id.edit_content);
		String text = "testcontent\n" + randomGenerator.nextInt(1000);
		// test save..
		solo.clickOnView(solo.getView(R.id.edit_content));
		solo.waitForView(R.id.task_content_edit);
		solo.clearEditText((EditText) solo.getView(R.id.task_content_edit));
		solo.enterText((EditText) solo.getView(R.id.task_content_edit), text);
		Log.d(TAG, "enterd " + text);
		solo.sleep(100);
		solo.clickOnView(solo.getView(R.id.edit_content));
		solo.waitForView(R.id.task_content);
		TextView content = (TextView) solo.getView(R.id.task_content);
		assertEquals("Change Content failed", text, content.getText()
				.toString());

		// Abort...
		solo.clickOnView(solo.getView(R.id.edit_content));
		solo.waitForView(R.id.task_content_edit);
		solo.clearEditText((EditText) solo.getView(R.id.task_content_edit));
		solo.typeText((EditText) solo.getView(R.id.task_content_edit), "foo");
		solo.clickOnActionBarItem(R.id.cancel);
		solo.waitForView(R.id.task_content);
		content = (TextView) solo.getView(R.id.task_content);
		assertEquals("Abbort Change Content failed", text, content.getText()
				.toString());
	}

	//
	public void testProgress() {
		int progress = randomGenerator.nextInt(100);
		SeekBar prograssBar = (SeekBar) solo
				.getView(R.id.task_progress_seekbar);
		solo.setProgressBar(prograssBar, progress);
		solo.sleep(100);
		assertTrue("Progress does not work",
				prograssBar.getProgress() == progress);
	}
	//
	// public void testSubtask() {
	// // Add
	// // Delete
	// // Click
	// }
	//
	// public void testDeleteTask() {
	//
	// }
	//
	// public void testMoveTask() {
	//
	// }
	//
	// public void testShare() {
	//
	// }
	//
	// public void testDuplicate() {
	//
	// }
	//
	// public void testAddAsSubtask() {
	//
	// }
}
