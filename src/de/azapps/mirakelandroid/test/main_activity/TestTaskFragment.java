package de.azapps.mirakelandroid.test.main_activity;

import java.util.GregorianCalendar;
import java.util.Random;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.robotium.solo.Solo;

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

	@SuppressLint("NewApi")
	protected void swipe(final Direction direction) {
		final Point size = new Point();
		getActivity().getWindowManager().getDefaultDisplay().getSize(size);
		final int width = size.x;
		final float xStart = direction == Direction.Left ? width - 10 : 10;
		final float xEnd = direction == Direction.Left ? 10 : width - 10;

		// The value for y doesn't change, as we want to swipe straight across
		this.solo.drag(xStart, xEnd, size.y / 2, size.y / 2, 1);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.randomGenerator = new Random();
		this.solo = new Solo(getInstrumentation(), getActivity());
		this.solo.waitForActivity(MainActivity.class);
		final View row = this.solo.getView(R.id.tasks_row);
		this.solo.clickOnView(row);
		assertTrue("faild to setup", this.solo.waitForView(R.id.task_content));

	}

	@Override
	protected void tearDown() throws Exception {
		this.solo.finishOpenedActivities();
	}

	public void testName() {
		// Get Task name
		final TextView taskName = (TextView) this.solo.getView(R.id.task_name);
		this.solo.clickOnView(taskName);
		final String newName = "testText " + this.randomGenerator.nextInt(1000);
		this.solo.waitForView(R.id.edit_name);
		final EditText edittext = (EditText) this.solo.getView(R.id.edit_name);
		this.solo.clearEditText(edittext);
		this.solo.enterText(edittext, newName);
		// solo.enterText(R.id.edit_name, newName);
		final Runnable runnable = new Runnable()

		{

			@Override
			public void run()

			{
				edittext.onEditorAction(EditorInfo.IME_ACTION_DONE);

			}
		}; // Use Solo to get the current activity, and pass our runnable to the
			// UI // thread.

		this.solo.getCurrentActivity().runOnUiThread(runnable);

		this.solo.waitForView(R.id.task_name);
		assertEquals("Change Name failed", taskName.getText().toString(),
				newName);
	}

	//
	public void testDone() {
		final CheckBox doneBox = (CheckBox) this.solo.getView(R.id.task_done);
		boolean state = doneBox.isChecked();
		this.solo.clickOnView(doneBox);
		this.solo.sleep(100);
		state = !state;
		assertEquals("Done does not work", doneBox.isChecked(), state);
	}

	public void testPriority() {
		final TextView prio = (TextView) this.solo.getView(R.id.task_prio);
		this.solo.clickOnView(prio);
		this.solo.waitForDialogToOpen();
		assertTrue(this.solo.searchText(getActivity().getString(
				R.string.task_change_prio_title)));
		final int newPrio = this.randomGenerator.nextInt(4) - 2;
		Log.d(TAG, "newPrio:" + newPrio);
		this.solo.clickOnText(newPrio + "");
		this.solo.waitForDialogToClose();
		Log.wtf(TAG, newPrio + "  " + prio.getText());
		assertEquals("Prio does not work", prio.getText().toString(), newPrio
				+ "");
	}

	public void testReminder() {
		// TODO set custom date and time
		final TextView reminder = (TextView) this.solo
				.getView(R.id.task_reminder);
		if (!reminder.getText().toString()
				.equals(this.solo.getString(R.string.no_reminder))) {
			clearReminder(reminder);
		}

		this.solo.clickOnView(reminder);
		this.solo.waitForDialogToOpen();
		this.solo.clickOnButton(this.solo.getString(android.R.string.ok));
		this.solo.waitForDialogToClose();
		assertEquals("Change reminder failed", DateTimeHelper.formatReminder(
				getActivity(), new GregorianCalendar()), reminder.getText()
				.toString());

		clearReminder(reminder);
	}

	public void testDue() {
		final TextView due = (TextView) this.solo.getView(R.id.task_due);
		if (!due.getText().toString()
				.equals(this.solo.getString(R.string.no_date))) {
			clearDue(due);
		}

		this.solo.clickOnView(due);
		this.solo.waitForDialogToOpen();
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
		this.solo.clickOnButton(this.solo.getString(android.R.string.ok));
		this.solo.waitForDialogToClose();
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

	private void clearDue(final TextView due) {
		this.solo.clickOnView(due);
		this.solo.waitForDialogToOpen();
		this.solo.clickOnButton(this.solo.getString(R.string.no_date));
		this.solo.waitForDialogToClose();
		assertEquals("Clear due failed", this.solo.getString(R.string.no_date),
				due.getText().toString());
	}

	private void clearReminder(final TextView reminder) {
		this.solo.clickOnView(reminder);
		this.solo.waitForDialogToOpen();
		this.solo.clickOnButton(this.solo.getString(R.string.no_date));
		this.solo.waitForDialogToClose();
		assertEquals("Clear reminder failed",
				this.solo.getString(R.string.no_reminder), reminder.getText()
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
		final String text = "testcontent\n"
				+ this.randomGenerator.nextInt(1000);
		// test save..
		this.solo.clickOnView(this.solo.getView(R.id.edit_content));
		this.solo.waitForView(R.id.task_content_edit);
		this.solo.clearEditText((EditText) this.solo
				.getView(R.id.task_content_edit));
		this.solo.enterText(
				(EditText) this.solo.getView(R.id.task_content_edit), text);
		Log.d(TAG, "enterd " + text);
		this.solo.sleep(100);
		this.solo.clickOnView(this.solo.getView(R.id.edit_content));
		this.solo.waitForView(R.id.task_content);
		TextView content = (TextView) this.solo.getView(R.id.task_content);
		assertEquals("Change Content failed", text, content.getText()
				.toString());

		// Abort...
		this.solo.clickOnView(this.solo.getView(R.id.edit_content));
		this.solo.waitForView(R.id.task_content_edit);
		this.solo.clearEditText((EditText) this.solo
				.getView(R.id.task_content_edit));
		this.solo.typeText(
				(EditText) this.solo.getView(R.id.task_content_edit), "foo");
		this.solo.clickOnActionBarItem(R.id.cancel);
		this.solo.waitForView(R.id.task_content);
		content = (TextView) this.solo.getView(R.id.task_content);
		assertEquals("Abbort Change Content failed", text, content.getText()
				.toString());
	}

	//
	public void testProgress() {
		final int progress = this.randomGenerator.nextInt(100);
		final SeekBar prograssBar = (SeekBar) this.solo
				.getView(R.id.task_progress_seekbar);
		this.solo.setProgressBar(prograssBar, progress);
		this.solo.sleep(100);
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
