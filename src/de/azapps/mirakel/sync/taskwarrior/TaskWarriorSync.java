package de.azapps.mirakel.sync.taskwarrior;

import java.io.File;
import java.nio.charset.MalformedInputException;
import java.text.SimpleDateFormat;
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

import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakelandroid.R;

public class TaskWarriorSync {

	public static final String TYPE = "TaskWarrior";
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
		longInfo(payload);

		TLSClient client=new TLSClient();
		client.init(root);
		client.connect(_host, _port);
		client.send(sync.serialize());
		
		String response = client.recv();
		longInfo(response);
		

		Msg remotes = new Msg();
		try {
			remotes.parse(response);
		} catch (MalformedInputException e) {
			// TODO do something
		}

		// parse tasks
		String tasksString[] = remotes.getPayload().split("\n");
		for (String taskString : tasksString) {
			JsonObject taskObject = new JsonParser().parse(taskString)
					.getAsJsonObject();
			Task server_task = Task.parse_json(taskObject);
			Task local_task = Task.getByUUID(server_task.getUUID());
			if (server_task.getSync_state() == Network.SYNC_STATE.DELETE) {
				if (local_task != null)
					local_task.delete(true);
			} else if (local_task == null) {
				server_task.create();
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
		// delete tasks, which are marked as deleted locally
		Task.deleteTasksPermanently();

		String message = remotes.get("message");
		if (message != null && message != "") {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_from_server, message),
					Toast.LENGTH_LONG).show();
			Log.v(TAG, "Message from Server: " + message);
		}
		String error_code = remotes.get("code");
		String error = remotes.get("error");
		// TODO do something with the errors
	}
	
	public static void longInfo(String str) {
	    if(str.length() > 4000) {
	        Log.i(TAG,str.substring(0, 4000));
	        longInfo(str.substring(4000));
	    } else
	        Log.i(TAG,str);
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
		
		File root;
		// TODO FIXIT!!!
		if (accountManager.getUserData(account, SyncAdapter.BUNDLE_CERT) == null) {
			root = new File("/data/data/de.azapps.mirakelandroid/ca.cert.pem");
		} else {
			//TODO Fix this
			root=null;
		}
		if (!root.exists() || !root.canRead()) {
			error("cert", 1376235891);
		}
		//_host = srv[0];
		//TODO get this from somewhere else, do not hardcode userdata!!
		_host="192.168.10.24";
		_port = Integer.parseInt(srv[1]);
		_port=6544;
		_user = "test";//account.name;
		_org = "TEST";//accountManager.getUserData(account, SyncAdapter.BUNDLE_ORG);
		_key = "3d331137-dadc-484e-986b-4e3c25e0eab9";//key;
		TaskWarriorSync.root=root;
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
