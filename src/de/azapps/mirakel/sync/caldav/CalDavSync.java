package de.azapps.mirakel.sync.caldav;

import java.util.List;
import java.util.UUID;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.DataDownloadCommand;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.sync.Network.HttpMode;
import android.accounts.Account;
import android.content.Context;

public class CalDavSync {
	private static final String TAG = "CalDavSync";
	private Context ctx;

	public CalDavSync(Context mContext) {
		this.ctx = mContext;
	}

	public void sync(Account account) {
		Log.d(TAG, "sync hear");
		List<Task> tasks = Task.all();
		// Some Headers

		String content = "BEGIN:VCALENDAR\n";
		// content+="PRODID:-//Ximian//NONSGML Evolution Calendar//EN
		content += "VERSION:2.0\n";
		content += "METHOD:PUBLISH\n";
		for (Task t : tasks) {
			// Add Taskcontent
			content += parseTask(t);
		}
		content += "END:VCALENDAR";
		// TODO get url from somwhere else
		String url = "http://192.168.10.168:5232/foo/"
				+ UUID.randomUUID().toString() + ".ics";

		Network n = new Network(new DataDownloadCommand() {

			@Override
			public void after_exec(String result) {
				Log.d(TAG, result);
			}
		}, HttpMode.PUT, content, ctx);
		n.execute(url);

	}

	// Generate VTODO string
	private String parseTask(Task t) {
		String ret = "BEGIN:VTODO\n";
		ret += "UID:" + t.getUUID() + "\n";
		ret += "SUMMARY:" + t.getName() + "\n";
		ret += "PRIORITY:" + t.getPriority() + "\n";
		ret += "CREATED:" + DateTimeHelper.formateCalDav(t.getCreated_at())
				+ "\n";
		ret += "LAST-MODIFIED:"
				+ DateTimeHelper.formateCalDav(t.getUpdated_at()) + "\n";
		if (t.getContent() != null && t.getContent().equals("")) {
			ret += "DESCRIPTION:" + t.getContent() + "\n";
		}
		if (t.getDue() != null)
			ret += "DUE;VALUE=DATE:"
					+ DateTimeHelper.formateCalDavDue(t.getDue()) + "\n";
		ret += "CLASS:" + t.getList().getName() + "\n";
		ret += "STATUS:" + (t.isDone() ? "COMPLETED" : "NEEDS-ACTION") + "\n";
		return ret + "END:VTODO\n";
	}

}
