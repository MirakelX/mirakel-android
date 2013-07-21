package de.azapps.mirakel.helper;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.joda.time.LocalDate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.Toast;
import de.azapps.mirakelandroid.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

public class Helpers {
	private static String TAG = "Helpers";

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
		long taskId = intent.getLongExtra(MainActivity.EXTRA_ID, 0);
		if (taskId == 0) {
			// ugly fix for show Task from Widget
			taskId = (long) intent.getIntExtra(MainActivity.EXTRA_ID, 0);
		}
		if (taskId != 0) {
			task = Task.get(taskId);
		}
		return task;
	}

	/**
	 * Share a Task as text with other apps
	 * 
	 * @param ctx
	 * @param t
	 */
	public static void share(Context ctx, Task t) {
		String subject = getTaskName(ctx, t);
		share(ctx, subject, t.getContent());
	}

	/**
	 * Share a list of Tasks from a List with other apps
	 * 
	 * @param ctx
	 * @param l
	 */
	public static void share(Context ctx, ListMirakel l) {
		String subject = ctx.getString(R.string.share_list_title, l.getName(),
				l.countTasks());
		String body = "";
		for (Task t : l.tasks()) {
			if (t.isDone()) {
				// body += "* ";
				continue;
			} else {
				body += "* ";
			}
			body += getTaskName(ctx, t) + "\n";
		}
		share(ctx, subject, body);
	}

	/**
	 * Helper for the share-functions
	 * 
	 * @param ctx
	 * @param t
	 * @return
	 */
	private static String getTaskName(Context ctx, Task t) {
		String subject;
		if (t.getDue() == null)
			subject = ctx.getString(R.string.share_task_title, t.getName());
		else
			subject = ctx.getString(R.string.share_task_title_with_date,
					t.getName(),
					formatDate(t.getDue(), ctx.getString(R.string.dateFormat)));
		return subject;
	}

	/**
	 * Share something
	 * 
	 * @param context
	 * @param subject
	 * @param shareBody
	 */
	private static void share(Context context, String subject, String shareBody) {
		shareBody += "\n\n" + context.getString(R.string.share_footer);
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

		Intent ci = Intent.createChooser(sharingIntent, context.getResources()
				.getString(R.string.share_using));
		ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(ci);
	}

	/**
	 * Format a Date for showing it in the app
	 * 
	 * @param date
	 *            Date
	 * @param format
	 *            Format–String (like dd.MM.YY)
	 * @return The formatted Date as String
	 */
	public static String formatDate(GregorianCalendar date, String format) {
		if (date == null)
			return "";
		else {
			return new SimpleDateFormat(format, Locale.getDefault())
					.format(date.getTime());
		}
	}

	/**
	 * Returns the ID of the Color–Resource for a Due–Date
	 * 
	 * @param origDue
	 *            The Due–Date
	 * @param isDone
	 *            Is the Task done?
	 * @return ID of the Color–Resource
	 */
	public static int getTaskDueColor(GregorianCalendar origDue, boolean isDone) {
		if (origDue == null)
			return R.color.Grey;
		LocalDate today = new LocalDate();
		LocalDate nextWeek = new LocalDate().plusDays(7);
		LocalDate due = new LocalDate(origDue);
		int cmpr = today.compareTo(due);
		int color;
		if (isDone) {
			color = R.color.Grey;
		} else if (cmpr > 0) {
			color = R.color.Red;
		} else if (cmpr == 0) {
			color = R.color.Orange;
		} else if (nextWeek.compareTo(due) >= 0) {
			color = R.color.Yellow;
		} else {
			color = R.color.Green;
		}
		return color;
	}

	public static void contact(Context context) {

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL,
				new String[] { context.getString(R.string.contact_email) });
		i.putExtra(Intent.EXTRA_SUBJECT,
				context.getString(R.string.contact_subject));
		String mirakelVersion = "unknown";
		try {
			mirakelVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "could not get version name from manifest!");
			e.printStackTrace();
		}
		i.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.contact_text,
				mirakelVersion, android.os.Build.VERSION.SDK_INT,
				android.os.Build.DEVICE));
		try {
			Intent ci = Intent.createChooser(i,
					context.getString(R.string.contact_chooser));
			ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(ci);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(context,
					context.getString(R.string.contact_no_client),
					Toast.LENGTH_SHORT).show();
		}
	}
}
