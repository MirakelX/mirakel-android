package de.azapps.mirakel.sync.taskwarrior;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;

public class TaskWarriorSync {

	public static final String TYPE = "TaskWarrior";
	public static final String CA_FILE = Mirakel.MIRAKEL_DIR + "ca.cert.pem";
	public static final String CLIENT_CERT_FILE = Mirakel.MIRAKEL_DIR
			+ "client.cert.pem";
	private static final String TAG = "TaskWarroirSync";
	private Context mContext;

	// Outgoing.
	static int _debug_level = 0;
	static int _limit = (1024 * 1024);
	static String _host = "localhost";
	static int _port = 6544;
	static String _org = "";
	static String _user = "";
	static String _key = "";
	static File root;
	static File user_ca;
	private AccountManager accountManager;
	private Account account;

	public enum TW_ERRORS {
		CANNOT_CREATE_SOCKET, CANNOT_PARSE_MESSAGE, MESSAGE_ERRORS, TRY_LATER, ACCESS_DENIED, ACCOUNT_SUSPENDED, NO_ERROR
	}

	public TaskWarriorSync(Context ctx) {
		mContext = ctx;
	}

	public TW_ERRORS sync(Account account) {
		Log.w(TAG, "very unstable yet");
		accountManager = AccountManager.get(mContext);
		this.account = account;
		init();

		Msg sync = new Msg();
		String payload = "";
		List<Task> local_tasks = Task.getTasksToSync();
		sync.set("protocol", "v1");
		sync.set("type", "sync");
		sync.set("org", _org);
		sync.set("user", _user);
		sync.set("key", _key);
		// split big sync into smaller pieces
		short taskNumber = 5;
		int parts = local_tasks.size() / taskNumber < 1 ? 1 : local_tasks
				.size() / taskNumber;
		for (int i = 0; i < parts; i++) {
			String old_key = accountManager.getUserData(account,
					SyncAdapter.TASKWARRIOR_KEY);
			if (old_key != null && !old_key.equals("")) {
				payload += old_key + "\n";
			}
			List<Task> syncedTasksId = new ArrayList<Task>();
			for (int j = i * taskNumber; j < local_tasks.size()
					&& j < (i + 1) * taskNumber; j++) {
				Task task = local_tasks.get(j);
				syncedTasksId.add(task);
				payload += taskToJson(task) + "\n";
			}
			// Build sync-request

			sync.setPayload(payload);
			TW_ERRORS error = doSync(account, sync);
			if (error == TW_ERRORS.NO_ERROR) {
				Task.resetSyncState(syncedTasksId);
			} else
				return error;
		}
		return TW_ERRORS.NO_ERROR;
	}

	private TW_ERRORS doSync(Account account, Msg sync) {

		longInfo(sync.getPayload());

		TLSClient client = new TLSClient();
		client.init(root, user_ca);
		try {
			client.connect(_host, _port);
		} catch (IOException e) {
			Log.e(TAG, "cannot create Socket");
			return TW_ERRORS.CANNOT_CREATE_SOCKET;
		}
		client.send(sync.serialize());

		String response = client.recv();
		longInfo(response);

		Msg remotes = new Msg();
		try {
			remotes.parse(response);
		} catch (MalformedInputException e) {
			Log.e(TAG, "cannot parse Message");
			return TW_ERRORS.CANNOT_PARSE_MESSAGE;
		}
		int code = Integer.parseInt(remotes.get("code"));
		switch (code) {
		case 200:
			Log.d(TAG, "Success");
			break;
		case 201:
			Log.d(TAG, "No change");
			break;
		case 300:
			Log.d(TAG,
					"Deprecated message type\n"
							+ "This message will not be supported in future task server releases.");
			break;
		case 301:
			Log.d(TAG,
					"Redirect\n"
							+ "Further requests should be made to the specified server/port.");
			// TODO
			break;
		case 302:
			Log.d(TAG,
					"Retry\n"
							+ "The client is requested to wait and retry the same request.  The wait\n"
							+ "time is not specified, and further retry responses are possible.");
			return TW_ERRORS.TRY_LATER;
		case 400:
			Log.e(TAG, "Malformed data");
			return TW_ERRORS.MESSAGE_ERRORS;
		case 401:
			Log.e(TAG, "Unsupported encoding");
			return TW_ERRORS.MESSAGE_ERRORS;
		case 420:
			Log.e(TAG, "Server temporarily unavailable");
			return TW_ERRORS.TRY_LATER;
		case 421:
			Log.e(TAG, "Server shutting down at operator request");
			return TW_ERRORS.TRY_LATER;
		case 430:
			Log.e(TAG, "Access denied");
			return TW_ERRORS.ACCESS_DENIED;
		case 431:
			Log.e(TAG, "Account suspended");
			return TW_ERRORS.ACCOUNT_SUSPENDED;
		case 432:
			Log.e(TAG, "Account terminated");
			return TW_ERRORS.ACCOUNT_SUSPENDED;
		case 500:
			Log.e(TAG, "Syntax error in request");
			return TW_ERRORS.MESSAGE_ERRORS;
		case 501:
			Log.e(TAG, "Syntax error, illegal parameters");
			return TW_ERRORS.MESSAGE_ERRORS;
		case 502:
			Log.e(TAG, "Not implemented");
			return TW_ERRORS.MESSAGE_ERRORS;
		case 503:
			Log.e(TAG, "Command parameter not implemented");
			return TW_ERRORS.MESSAGE_ERRORS;
		case 504:
			Log.e(TAG, "Request too big");
			return TW_ERRORS.MESSAGE_ERRORS;

		}
		if (remotes.get("status").equals("Client sync key not found.")) {
			Log.d(TAG, "reset sync-key");
			accountManager.setUserData(account, SyncAdapter.TASKWARRIOR_KEY,
					null);
			sync(account);
		}

		// parse tasks
		if (remotes.getPayload() == null || remotes.getPayload().equals("")) {
			Log.i(TAG, "there is no Payload");
		} else {
			String tasksString[] = remotes.getPayload().split("\n");
			for (String taskString : tasksString) {
				if (taskString.charAt(0) != '{') {
					Log.d(TAG, "Key: " + taskString);
					accountManager.setUserData(account,
							SyncAdapter.TASKWARRIOR_KEY, taskString);
					continue;
				}
				JsonObject taskObject;
				Task local_task;
				Task server_task;
				try {
					taskObject = new JsonParser().parse(taskString)
							.getAsJsonObject();
					server_task = Task.parse_json(taskObject);
					local_task = Task.getByUUID(server_task.getUUID());
				} catch (Exception e) {
					Log.d(TAG, Log.getStackTraceString(e));
					Log.e(TAG, "malformed JSON");
					continue;
				}

				if (server_task.getSyncState() == SYNC_STATE.DELETE) {
					if (local_task != null)
						local_task.destroy(true);
				} else if (local_task == null) {
					try {
						server_task.create();
					} catch (NoSuchListException e) {
						Log.wtf(TAG, "List vanish");
						Toast.makeText(mContext, R.string.no_lists,
								Toast.LENGTH_LONG).show();
					}
				} else {
					server_task.setId(local_task.getId());
					try {
						server_task.save();
					} catch (NoSuchListException e) {
						// Should not happen, because the list should be created
						// while parsing the task
					}
				}
			}
		}
		String message = remotes.get("message");
		if (message != null && message != "") {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_from_server, message),
					Toast.LENGTH_LONG).show();
			Log.v(TAG, "Message from Server: " + message);
		}
		client.close();
		return TW_ERRORS.NO_ERROR;
	}

	public static void longInfo(String str) {
		if (str.length() > 4000) {
			Log.i(TAG, str.substring(0, 4000));
			longInfo(str.substring(4000));
		} else
			Log.i(TAG, str);
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

		File root, user;
		// TODO FIXIT!!!
		if (accountManager.getUserData(account, SyncAdapter.BUNDLE_CERT) == null) {
			root = new File(CA_FILE);
			user = new File(CLIENT_CERT_FILE);
		} else {
			// TODO Fix this
			root = null;
			user = null;
		}
		if (!root.exists() || !root.canRead() || !user.exists()
				|| !user.canRead()) {
			error("cert", 1376235891);
		}
		_host = srv[0];
		_port = Integer.parseInt(srv[1]);
		_user = account.name;
		_org = accountManager.getUserData(account, SyncAdapter.BUNDLE_ORG);
		_key = accountManager.getPassword(account);
		TaskWarriorSync.root = root;
		TaskWarriorSync.user_ca = user;
	}

	/**
	 * Converts a task to the json-format we need
	 * 
	 * @param t
	 * @return
	 */
	private String taskToJson(Task t) {

		String status = "pending";
		if (t.getSyncState() == SYNC_STATE.DELETE)
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
		json += "\"entry\":\"" + formatCal(t.getCreatedAt()) + "\",";
		json += "\"description\":\"" + t.getName() + "\",";
		if (t.getDue() != null)
			json += "\"due\":\"" + formatCal(t.getDue()) + "\",";
		json += "\"project\":\"" + t.getList().getName() + "\",";
		if (priority != null)
			json += "\"priority\":\"" + priority + "\",";
		json += "\"modification\":\"" + formatCal(t.getUpdated_at()) + "\",";
		if (t.getReminder() != null)
			json += "\"reminder\":\"" + formatCal(t.getReminder()) + "\",";

		// Annotations
		json += "\"annotations\":[";
		/*
		 * An annotation in taskd is a line of content in Mirakel!
		 */
		String annotations[] = t.getContent().split("\n");
		boolean first = true;
		for (String a : annotations) {
			if (first)
				first = false;
			else
				json += ",";
			json += "{\"entry\":\"" + formatCal(t.getUpdated_at()) + "\",";
			json += "\"description\":\"" + a + "\"}";
		}
		json += "]";

		if (t.getAdditionalEntries() != null) {
			Map<String, String> additionalEntries = t.getAdditionalEntries();
			for (String key : additionalEntries.keySet()) {
				json += ",\"" + key + "\":\"" + additionalEntries.get(key)
						+ "\"";
			}
		}
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
		SimpleDateFormat df = new SimpleDateFormat(
				mContext.getString(R.string.TWDateFormat));
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
		// Toast.makeText(mContext, what, Toast.LENGTH_SHORT).show();
	}

}
