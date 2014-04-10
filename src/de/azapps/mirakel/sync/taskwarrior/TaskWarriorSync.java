package de.azapps.mirakel.sync.taskwarrior;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.R;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TaskWarriorSync {

	private static final String TW_PROTOCOL_VERSION = "v1";

	public enum TW_ERRORS {
		ACCESS_DENIED, ACCOUNT_SUSPENDED, CANNOT_CREATE_SOCKET, CANNOT_PARSE_MESSAGE, CONFIG_PARSE_ERROR, MESSAGE_ERRORS, NO_ERROR, NOT_ENABLED, TRY_LATER;
		public static TW_ERRORS getError(final int code) {
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
			default:
				Log.d(TAG, "Unkown code: " + code);
				break;

			}
			return NO_ERROR;
		}
	}

	// Outgoing.
	// private static int _debug_level = 0;
	// private static int _limit = (1024 * 1024);
	private static String _host = "localhost";
	private static String _key = "";
	private static String sync_key = "";
	private static String _org = "";
	private static int _port = 6544;
	private static String _user = "";

	public static final String NO_PROJECT = "NO_PROJECT";
	private static String root;
	private static final String TAG = "TaskWarroirSync";
	public static final String TYPE = "TaskWarrior";
	private static String user_ca;
	private static String user_key;

	private static String escape(final String string) {
		return string.replace("\"", "\\\"");
	}

	public static void longInfo(final String str) {
		if (str.length() > 4000) {
			Log.i(TAG, str.substring(0, 4000));
			longInfo(str.substring(4000));
		} else {
			Log.i(TAG, str);
		}
	}

	private Account account;

	private AccountManager accountManager;

	private HashMap<String, String[]> dependencies;

	private final Context mContext;

	public TaskWarriorSync(final Context ctx) {
		this.mContext = ctx;
	}

	private TW_ERRORS doSync(final Account a, final Msg sync) {
		final AccountMirakel accountMirakel = AccountMirakel.get(this.account);
		longInfo(sync.getPayload());

		final TLSClient client = new TLSClient();
		try {
			client.init(root, user_ca, user_key);
		} catch (final ParseException e) {
			Log.e(TAG, "cannot open certificate");
			return TW_ERRORS.CONFIG_PARSE_ERROR;
		} catch (final CertificateException e) {
			Log.e(TAG, "general problem with init");
			return TW_ERRORS.CONFIG_PARSE_ERROR;
		}
		try {
			client.connect(_host, _port);
		} catch (final IOException e) {
			Log.e(TAG, "cannot create Socket");
			return TW_ERRORS.CANNOT_CREATE_SOCKET;
		}
		client.send(sync.serialize());

		final String response = client.recv();
		if (MirakelCommonPreferences.isEnabledDebugMenu()
				&& MirakelCommonPreferences.isDumpTw()) {
			try {
				FileUtils.writeToFile(new File(FileUtils.getLogDir(), getTime()
						+ ".tw_down.log"), response);
			} catch (final IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// longInfo(response);
		final Msg remotes = new Msg();
		try {
			remotes.parse(response);
		} catch (final MalformedInputException e) {
			Log.e(TAG, "cannot parse Message");
			return TW_ERRORS.CANNOT_PARSE_MESSAGE;
		} catch (final NullPointerException e) {
			Log.wtf(TAG, "remotes.pars throwed NullPointer");
			return TW_ERRORS.CANNOT_PARSE_MESSAGE;
		}
		final int code = Integer.parseInt(remotes.get("code"));
		final TW_ERRORS error = TW_ERRORS.getError(code);
		if (error != TW_ERRORS.NO_ERROR) {
			return error;
		}

		if (remotes.get("status").equals("Client sync key not found.")) {
			Log.d(TAG, "reset sync-key");
			this.accountManager.setUserData(a, SyncAdapter.TASKWARRIOR_KEY,
					null);
			sync(a);
		}

		// parse tasks
		if (remotes.getPayload() == null || remotes.getPayload().equals("")) {
			Log.i(TAG, "there is no Payload");
		} else {
			final String tasksString[] = remotes.getPayload().split("\n");
			for (final String taskString : tasksString) {
				if (taskString.charAt(0) != '{') {
					Log.d(TAG, "Key: " + taskString);
					accountMirakel.setSyncKey(taskString);
					accountMirakel.save();
					continue;
				}
				JsonObject taskObject;
				Task local_task;
				Task server_task;
				try {
					taskObject = new JsonParser().parse(taskString)
							.getAsJsonObject();
					Log.i(TAG, taskString);
					server_task = Task.parse_json(taskObject, accountMirakel,
							true);
					if (server_task.getList() == null
							|| server_task.getList().getAccount().getId() != accountMirakel
									.getId()) {
						final ListMirakel list = ListMirakel
								.getInboxList(accountMirakel);
						server_task.setList(list, false);
						Log.d(TAG, "no list");
						server_task.addAdditionalEntry(NO_PROJECT, "true");
					}
					this.dependencies.put(server_task.getUUID(),
							server_task.getDependencies());
					local_task = Task.getByUUID(server_task.getUUID());
				} catch (final Exception e) {
					Log.d(TAG, Log.getStackTraceString(e));
					Log.e(TAG, "malformed JSON");
					Log.e(TAG, taskString);
					continue;
				}

				if (server_task.getSyncState() == SYNC_STATE.DELETE) {
					Log.d(TAG, "destroy " + server_task.getName());
					if (local_task != null) {
						local_task.destroy(true);
					}
				} else if (local_task == null) {
					try {
						server_task.create(false);
						Log.d(TAG, "create " + server_task.getName());
					} catch (final NoSuchListException e) {
						Log.wtf(TAG, "List vanish");
					}
				} else {
					server_task.setId(local_task.getId());
					Log.d(TAG, "update " + server_task.getName());
					server_task.safeSave();
				}
			}
		}
		final String message = remotes.get("message");
		if (message != null && message != "") {
			Log.v(TAG, "Message from Server: " + message);
		}
		client.close();
		return TW_ERRORS.NO_ERROR;
	}

	/**
	 * Format a Calendar to the taskwarrior-date-format
	 * 
	 * @param c
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private String formatCal(final Calendar c) {
		final SimpleDateFormat df = new SimpleDateFormat(
				this.mContext.getString(R.string.TWDateFormat));
		return df.format(c.getTime());
	}

	/**
	 * Initialize the variables
	 * 
	 * @param aMirakel
	 */
	private boolean init(final AccountMirakel aMirakel) {
		final String server = this.accountManager.getUserData(this.account,
				SyncAdapter.BUNDLE_SERVER_URL);
		final String srv[] = server.trim().split(":");
		if (srv.length != 2) {
			Log.wtf(TAG, "cannot determine serveradress");
			return false;
		}
		sync_key = aMirakel.getSyncKey();
		_host = srv[0];
		_port = Integer.parseInt(srv[1]);
		_user = this.account.name;
		_org = this.accountManager.getUserData(this.account,
				SyncAdapter.BUNDLE_ORG);
		TaskWarriorSync.root = this.accountManager.getUserData(this.account,
				DefinitionsHelper.BUNDLE_CERT);
		TaskWarriorSync.user_ca = this.accountManager.getUserData(this.account,
				DefinitionsHelper.BUNDLE_CERT_CLIENT);
		final String[] pwds = this.accountManager.getPassword(this.account)
				.split(":");
		if (pwds.length != 2) {
			Log.wtf(TAG, "cannot split pwds");
			return false;
		}

		TaskWarriorSync.user_key = pwds[0].trim();
		_key = pwds[1].trim();
		if (_key.length() != 0 && _key.length() != 36) {
			Log.wtf(TAG, "no valid key ");
			return false;
		}
		return true;
	}

	private void setDependencies() {
		for (final String uuid : this.dependencies.keySet()) {
			final Task parent = Task.getByUUID(uuid);
			if (uuid == null || this.dependencies == null) {
				continue;
			}
			final String[] childs = this.dependencies.get(uuid);
			if (childs == null) {
				continue;
			}
			for (final String childUuid : childs) {
				final Task child = Task.getByUUID(childUuid);
				if (child == null) {
					continue;
				}
				if (child.isSubtaskOf(parent)) {
					continue;
				}
				try {
					parent.addSubtask(child);
				} catch (final Exception e) {
					// eat it
				}
			}

		}
	}

	public String getTime() {
		return new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss",
				Helpers.getLocal(this.mContext)).format(new Date());
	}

	public TW_ERRORS sync(final Account a) {
		this.accountManager = AccountManager.get(this.mContext);
		this.account = a;
		final AccountMirakel aMirakel = AccountMirakel.get(a);
		if (!aMirakel.isEnabeld()) {
			return TW_ERRORS.NOT_ENABLED;
		}

		if (!init(aMirakel)) {
			return TW_ERRORS.CONFIG_PARSE_ERROR;
		}

		final Msg sync = new Msg();
		sync.set("protocol", TW_PROTOCOL_VERSION);
		sync.set("type", "sync");
		sync.set("org", _org);
		sync.set("user", _user);
		sync.set("key", _key);
		String payload = sync_key != null ? sync_key + "\n" : "";
		final List<Task> local_tasks = Task.getTasksToSync(a);
		for (final Task task : local_tasks) {
			payload += taskToJson(task) + "\n";
		}
		// Format: {UUID:[UUID]}
		this.dependencies = new HashMap<String, String[]>();
		// for (int i = 0; i < parts; i++) {
		final String old_key = this.accountManager.getUserData(a,
				SyncAdapter.TASKWARRIOR_KEY);
		if (old_key != null && !old_key.equals("")) {
			payload += old_key + "\n";
		}

		// Build sync-request

		sync.setPayload(payload);
		if (MirakelCommonPreferences.isDumpTw()) {
			try {
				final FileWriter f = new FileWriter(new File(
						FileUtils.getLogDir(), getTime() + ".tw_up.log"));
				f.write(payload);
				f.close();
			} catch (final Exception e) {
				// eat it
			}
		}
		final TW_ERRORS error = doSync(a, sync);
		if (error == TW_ERRORS.NO_ERROR) {
			Log.w(TAG, "clear sync state");
			Task.resetSyncState(local_tasks);
		} else {
			setDependencies();
			return error;
		}
		// }
		setDependencies();
		return TW_ERRORS.NO_ERROR;
	}

	/**
	 * Converts a task to the json-format we need
	 * 
	 * @param task
	 * @return
	 */
	public String taskToJson(final Task task) {
		final int offset = DateTimeHelper.getTimeZoneOffset(true);
		final Map<String, String> additionals = task.getAdditionalEntries();
		String end = null;
		String status = "pending";
		final Calendar now = new GregorianCalendar();
		now.setTimeInMillis(now.getTimeInMillis() - offset);
		if (task.getSyncState() == SYNC_STATE.DELETE) {
			status = "deleted";
			end = formatCal(now);
		} else if (task.isDone()) {
			status = "completed";
			if (additionals.containsKey("end")) {
				end = additionals.get("end");
				end = end.substring(1, end.length() - 1); // Clear redundant \"
			} else {
				end = formatCal(now);
			}
		}

		String priority = null;
		switch (task.getPriority()) {
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
		default:
			Log.wtf(TAG, "unkown priority");
			break;
		}

		String json = "{";
		json += "\"uuid\":\"" + task.getUUID() + "\"";
		json += ",\"status\":\"" + status + "\"";
		final Calendar created_at = task.getCreatedAt();
		created_at.setTimeInMillis(created_at.getTimeInMillis() - offset);
		json += ",\"entry\":\"" + formatCal(created_at) + "\"";
		json += ",\"description\":\"" + escape(task.getName()) + "\"";
		if (task.getDue() != null) {
			final Calendar due = task.getDue();
			due.setTimeInMillis(due.getTimeInMillis() - offset);
			json += ",\"due\":\"" + formatCal(due) + "\"";
		}
		if (task.getList() != null && !additionals.containsKey(NO_PROJECT)) {
			json += ",\"project\":\"" + task.getList().getName() + "\"";
		}
		if (priority != null) {
			json += ",\"priority\":\"" + priority + "\"";
		}
		final Calendar updated_at = task.getUpdatedAt();
		updated_at.setTimeInMillis(updated_at.getTimeInMillis() - offset);
		json += ",\"modified\":\"" + formatCal(updated_at) + "\"";
		if (task.getReminder() != null) {
			final Calendar reminder = task.getReminder();
			reminder.setTimeInMillis(reminder.getTimeInMillis() - offset);
			json += ",\"reminder\":\"" + formatCal(reminder) + "\"";
		}
		if (end != null) {
			json += ",\"end\":\"" + end + "\"";
		}
		json += ",\"progress\":" + task.getProgress();
		// Annotations
		if (task.getContent() != null && !task.getContent().equals("")) {
			json += ",\"annotations\":[";
			/*
			 * An annotation in taskd is a line of content in Mirakel!
			 */
			final String annotations[] = escape(task.getContent()).split("\n");
			boolean first = true;
			final Calendar d = task.getUpdatedAt();
			d.setTimeInMillis(d.getTimeInMillis() - offset);
			for (final String a : annotations) {
				if (first) {
					first = false;
				} else {
					json += ",";
				}
				json += "{\"entry\":\"" + formatCal(d) + "\",";
				json += "\"description\":\"" + a.trim().replace("\n", "")
						+ "\"}";
				d.add(Calendar.SECOND, 1);
			}
			json += "]";
		}
		// Anotations end
		// TW.depends==Mirakel.subtasks!
		// Dependencies
		if (task.getSubtaskCount() > 0) {
			json += ",\"depends\":\"";
			boolean first1 = true;
			for (final Task subtask : task.getSubtasks()) {
				if (first1) {
					first1 = false;
				} else {
					json += ",";
				}
				json += subtask.getUUID();
			}
			json += "\"";
		}
		// end Dependencies
		// Additional Strings
		if (additionals != null) {
			for (final String key : additionals.keySet()) {
				if (!key.equals(NO_PROJECT)
						&& !(key.equals("status") && (status
								.equals("completed") || status
								.equals("deleted")))) {
					json += ",\"" + key + "\":" + additionals.get(key);
				}
			}
		}
		// end Additional Strings
		json += "}";
		return json;
	}
}
