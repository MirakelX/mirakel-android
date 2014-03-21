package de.azapps.mirakel.main_activity;

import java.util.List;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakelandroid.R;

public class BackgroundTasks {
	protected static MainActivityBroadcastReceiver mSyncReciver;

	static void run(final MainActivity context) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				ReminderAlarm.updateAlarms(context);
				NotificationService.updateNotificationAndWidget(context);

				if (!MirakelCommonPreferences.containsHighlightSelected()) {
					final SharedPreferences.Editor editor = MirakelPreferences
							.getEditor();
					editor.putBoolean("highlightSelected",
							MirakelCommonPreferences.isTablet());
					editor.commit();
				}

				if (!MirakelCommonPreferences.containsStartupAllLists()) {
					final SharedPreferences.Editor editor = MirakelPreferences
							.getEditor();
					editor.putBoolean("startupAllLists", false);
					editor.putString("startupList", ""
							+ ListMirakel.first().getId());
					editor.commit();
				}
				// We should remove this in the future, nobody uses such old
				// versions (hopefully)
				if (MainActivity.updateTasksUUID) {
					final List<Task> tasks = Task.all();
					for (final Task t : tasks) {
						t.setUUID(java.util.UUID.randomUUID().toString());
						t.safeSave();
					}
				}
				mSyncReciver = new MainActivityBroadcastReceiver(context);
				context.registerReceiver(mSyncReciver, new IntentFilter(
						DefinitionsHelper.SYNC_FINISHED));
				if (DefinitionsHelper.freshInstall) {
					final String[] lists = context.getResources()
							.getStringArray(R.array.demo_lists);
					for (final String list : lists) {
						ListMirakel.newList(list);
					}
					if (MirakelCommonPreferences.isDemoMode()) {
						final String[] tasks = context.getResources()
								.getStringArray(R.array.demo_tasks);
						final String[] task_lists = { lists[1], lists[1],
								lists[0], lists[2], lists[2], lists[2] };
						final int[] priorities = { 2, -1, 1, 2, 0, 0 };
						int i = 0;
						for (final String task : tasks) {
							final Task t = Semantic.createTask(task,
									ListMirakel.findByName(task_lists[i]),
									true, context);
							t.setPriority(priorities[i]);
							t.safeSave();
							i++;
						}
					}
				}

			}
		}).run();
	}

	static void onDestroy(final MainActivity context) {
		context.unregisterReceiver(mSyncReciver);
	}
}
