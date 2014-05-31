/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskDeserializer;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.sync.R;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.taskwarrior.TLSClient.NoSuchCertificateException;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TaskWarriorSync {

	private static final String TW_PROTOCOL_VERSION = "v1";

	public class TaskWarriorSyncFailedExeption extends Exception {
		private static final long serialVersionUID = 3349776187699690118L;
		private final TW_ERRORS error;
		private final String message;

		TaskWarriorSyncFailedExeption(final TW_ERRORS type, final String message) {
			super();
			this.error = type;
			this.message = message;
		}

		TaskWarriorSyncFailedExeption(final TW_ERRORS type,
				final Throwable cause) {
			super(cause);
			this.error = type;
			this.message = cause.getMessage();
		}

		public TW_ERRORS getError() {
			return this.error;
		}

		@Override
		public String getMessage() {
			return this.message;
		}
	}

	public enum TW_ERRORS {
		ACCESS_DENIED, ACCOUNT_SUSPENDED, CANNOT_CREATE_SOCKET, CANNOT_PARSE_MESSAGE, CONFIG_PARSE_ERROR, MESSAGE_ERRORS, NO_ERROR, NOT_ENABLED, TRY_LATER, NO_SUCH_CERT;
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

	private Map<String, String[]> dependencies;

	private final Context mContext;

	public TaskWarriorSync(final Context ctx) {
		this.mContext = ctx;
	}

	private void doSync(final Account a, final Msg sync)
			throws TaskWarriorSyncFailedExeption {
		final AccountMirakel accountMirakel = AccountMirakel.get(this.account);
		longInfo(sync.getPayload());

		final TLSClient client = new TLSClient();
		try {
			client.init(root, user_ca, user_key);
		} catch (final ParseException e) {
			Log.e(TAG, "cannot open certificate");
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CONFIG_PARSE_ERROR, "cannot open certificate");
		} catch (final CertificateException e) {
			Log.e(TAG, "general problem with init");
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CONFIG_PARSE_ERROR, "general problem with init");
		} catch (final NoSuchCertificateException e) {
			Log.e(TAG, "NoSuchCertificateException");
			throw new TaskWarriorSyncFailedExeption(TW_ERRORS.NO_SUCH_CERT,
					"general problem with init");

		}
		try {
			client.connect(_host, _port);
		} catch (final IOException e) {
			Log.e(TAG, "cannot create socket");
			client.close();
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CANNOT_CREATE_SOCKET, "cannot create socket");
		}
		client.send(sync.serialize());

		final String response = client.recv();
		if (MirakelCommonPreferences.isEnabledDebugMenu()
				&& MirakelCommonPreferences.isDumpTw()) {
			try {
				FileUtils.writeToFile(new File(FileUtils.getLogDir(), getTime()
						+ ".tw_down.log"), response);
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
		}

		// longInfo(response);
		final Msg remotes = new Msg();
		try {
			remotes.parse(response);
		} catch (final MalformedInputException e) {
			Log.e(TAG, "cannot parse message");
			client.close();
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CANNOT_PARSE_MESSAGE, "cannot parse message");
		} catch (final NullPointerException e) {
			Log.wtf(TAG, "remotes.parse throwed NullPointer");
			client.close();
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CANNOT_PARSE_MESSAGE,
					"remotes.parse throwed NullPointer");
		}
		final int code = Integer.parseInt(remotes.get("code"));
		final TW_ERRORS error = TW_ERRORS.getError(code);
		if (error != TW_ERRORS.NO_ERROR) {
			client.close();
			throw new TaskWarriorSyncFailedExeption(error,
					"sync() throwed error");
		}

		if (remotes.get("status").equals("Client sync key not found.")) {
			Log.d(TAG, "reset sync-key");
			this.accountManager.setUserData(a, SyncAdapter.TASKWARRIOR_KEY,
					null);
			try {
				sync(a);
			} catch (final TaskWarriorSyncFailedExeption e) {
				if (e.getError() != TW_ERRORS.NOT_ENABLED) {
					client.close();
					throw new TaskWarriorSyncFailedExeption(e.getError(), e);
				}
			}
		}

		// parse tasks
		if (remotes.getPayload() == null || remotes.getPayload().equals("")) {
			Log.i(TAG, "there is no Payload");
		} else {
			final String tasksString[] = remotes.getPayload().split("\n");
			final Gson gson = new GsonBuilder().registerTypeAdapter(Task.class,
					new TaskDeserializer(true, accountMirakel, this.mContext))
					.create();
			for (final String taskString : tasksString) {
				if (taskString.charAt(0) != '{') {
					Log.d(TAG, "Key: " + taskString);
					accountMirakel.setSyncKey(taskString);
					accountMirakel.save();
					continue;
				}
				Task local_task;
				Task server_task;
				try {
					Log.i(TAG, taskString);
					server_task = gson.fromJson(taskString, Task.class);
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
						server_task.create(false, true);
						Log.d(TAG, "create " + server_task.getName());
					} catch (final NoSuchListException e) {
						Log.wtf(TAG, "List vanish");
					}
				} else {
					server_task.takeIdFrom(local_task);
					Log.d(TAG, "update " + server_task.getName());
					server_task.save(false, true);
				}
			}
		}
		final String message = remotes.get("message");
		if (message != null && !"".equals(message)) {
			Log.v(TAG, "Message from Server: " + message);
		}
		client.close();
		NotificationService.updateServices(this.mContext, true);
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
		if (c.getTimeInMillis() < 0) {
			c.setTimeInMillis(10);
		}
		return df.format(c.getTime());
	}

	/**
	 * Initialize the variables
	 * 
	 * @param aMirakel
	 * @throws TaskWarriorSyncFailedExeption
	 */
	private void init(final AccountMirakel aMirakel)
			throws TaskWarriorSyncFailedExeption {
		final String server = this.accountManager.getUserData(this.account,
				SyncAdapter.BUNDLE_SERVER_URL);
		final String srv[] = server.trim().split(":");
		if (srv.length != 2) {
			Log.wtf(TAG, "cannot determine address of server");
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CONFIG_PARSE_ERROR,
					"cannot determine address of server");
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
		if (pwds.length < 2) {
			Log.wtf(TAG, "cannot split pwds");
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CONFIG_PARSE_ERROR, "cannot split pwds");
		}
		if (pwds.length != 2) {
			// We have to remove the bad stuff and update the current password
			TaskWarriorSync.user_key = pwds[pwds.length - 2].trim();
			_key = pwds[pwds.length - 1].trim();
			this.accountManager.setPassword(this.account,
					TaskWarriorSync.user_key + "\n:" + _key);
		} else {
			TaskWarriorSync.user_key = pwds[0].trim();
			_key = pwds[1].trim();
		}
		if (_key.length() != 0 && _key.length() != 36) {
			Log.wtf(TAG, "Key is not valid");
			throw new TaskWarriorSyncFailedExeption(
					TW_ERRORS.CONFIG_PARSE_ERROR, "Key is not valid");
		}
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

	public void sync(final Account a) throws TaskWarriorSyncFailedExeption {
		this.accountManager = AccountManager.get(this.mContext);
		this.account = a;
		final AccountMirakel aMirakel = AccountMirakel.get(a);
		if (aMirakel == null || !aMirakel.isEnabled()) {
			throw new TaskWarriorSyncFailedExeption(TW_ERRORS.NOT_ENABLED,
					"TW sync is not enabled");
		}

		init(aMirakel);

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
		try {
			doSync(a, sync);
		} catch (final TaskWarriorSyncFailedExeption e) {
			setDependencies();
			throw new TaskWarriorSyncFailedExeption(e.getError(), e);
		}
		Log.w(TAG, "clear sync state");
		Task.resetSyncState(local_tasks);
		setDependencies();
	}

	/**
	 * Converts a task to the json-format we need
	 * 
	 * @param task
	 * @return
	 */
	public String taskToJson(final Task task) {
		return new GsonBuilder()
				.registerTypeAdapter(Task.class,
						new TaskWarriorTaskSerializer(this.mContext)).create()
				.toJson(task);
	}
}
