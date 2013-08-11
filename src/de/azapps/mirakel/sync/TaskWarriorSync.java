package de.azapps.mirakel.sync;

import java.io.File;
import java.nio.charset.MalformedInputException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.taskwarrior.Msg;

public class TaskWarriorSync {

	public static final String TYPE = "TaskWarrior";
	private static final String TAG = "TaskWarroirSync";
	private Context mContext;

	// Outgoing.
	static int _debug_level = 0;
	static int _limit = (1024 * 1024);
	static String _host = "localhost";
	static String _port = "6544";
	static String _org = "";
	static String _user = "";
	static String _key = "";
	static File _cert;
	static List<Task> _local_tasks;
	private AccountManager accountManager;
	private Account account;

	public TaskWarriorSync(Context ctx) {
		mContext = ctx;
	}

	public void sync(Account account) {
		Log.w(TAG, "Not implemented yet");
		accountManager = AccountManager.get(mContext);
		this.account = account;
		init();

		_local_tasks = Task.getTasksToSync();

		Msg sync = new Msg();
		String payload = "";
		for (Task t : _local_tasks) {
			payload += taskToJson(t) + "\n";
		}
		// Build sync-request
		sync.set("protocol", "v1");
		sync.set("type", "sync");
		sync.set("org", _org);
		sync.set("user", _user);
		sync.set("key", _key);
		sync.setPayload(payload);

		// TODO Send sync request
		// TODO Get server response
		String response = "";

		Msg remotes = new Msg();
		try {
			remotes.parse(response);
		} catch (MalformedInputException e) {
			// TODO do something
		}
	}

	/**
	 * Initialize the variables
	 */
	private void init() {
		String server = accountManager.getUserData(account,
				SyncAdapter.BUNDLE_SERVER_URL);
		String srv[] = server.split(":");
		if (srv.length != 2) {
			error("port", 1376235889);
		}
		String key = accountManager.getPassword(account);
		if (key.length() != 0 && key.length() != 36) {
			error("key", 1376235890);
		}
		File cert = new File(accountManager.getUserData(account,
				SyncAdapter.BUNDLE_CERT));
		if (!cert.exists() || !cert.canRead()) {
			error("cert", 1376235891);
		}
		_host = srv[0];
		_port = srv[1];
		_user = account.name;
		_org = accountManager.getUserData(account, SyncAdapter.BUNDLE_ORG);
		_key = key;
		_cert = cert;
	}

	/**
	 * Converts a task to the json-format we need
	 * 
	 * @param t
	 * @return
	 */
	private String taskToJson(Task t) {

		String status = "pending";
		if (t.getSync_state() == Network.SYNC_STATE.DELETE)
			status = "deleted";
		else if (t.isDone())
			status = "completed";
		Log.e(TAG, "Status waiting / recurring is not implemented now");
		// TODO

		String priority = null;
		switch (t.getPriority()) {
		case -2:
		case -1:
			priority = "L";
			break;
		case 1:
			priority = "M";
			break;
		case 2:
			priority = "H";
			break;
		}

		String json = "{";
		json += "\"uuid\":\"" + t.getUUID() + "\",";
		json += "\"status\":\"" + status + "\",";
		json += "\"entry\":\"" + formatCal(t.getCreated_at()) + "\",";
		json += "\"description\":\"" + t.getName() + "\",";
		json += "\"due\":\"" + formatCal(t.getDue()) + "\",";
		json += "\"project\":\"" + t.getList().getName() + "\",";
		if (priority != null)
			json += "\"priority\":\"" + priority + "\",";
		json += "\"modification\":\"" + formatCal(t.getUpdated_at()) + "\",";
		json += "\"content\":\"" + t.getContent() + "\",";
		json += "\"reminder\":\"" + formatCal(t.getReminder()) + "\"";
		json += "}";
		return json;
	}

	/**
	 * Format a Calendar to the taskwarrior-date-format
	 * 
	 * @param c
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private String formatCal(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddTkkmmssZ");
		return df.format(c.getTime());
	}

	/**
	 * Handle an error
	 * 
	 * @param what
	 * @param code
	 */
	private void error(String what, int code) {
		Log.e(TAG, what + " (Code: " + code + ")");
		Toast.makeText(mContext, what, Toast.LENGTH_SHORT).show();
	}

}
