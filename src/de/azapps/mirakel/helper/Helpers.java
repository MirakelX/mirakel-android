package de.azapps.mirakel.helper;

import android.content.Intent;
import android.util.Log;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.task.Task;

public class Helpers {

	/**
	 * Wrapper-Class
	 * 
	 * @author az
	 * 
	 */
	public interface ExecInterface {
		public void exec();
	}
	public static Task getTaskFromIntent(Intent intent) {
		Task task = null;
		int taskId = (int) intent.getLongExtra(MainActivity.EXTRA_ID, 0);
		Log.e("Blubb", "Task:" + taskId);
		if (taskId != 0) {
			task = Task.get(taskId);
		}
		return task;
	}

}
