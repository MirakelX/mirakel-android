package de.azapps.mirakel.helper;

import java.util.List;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;

public class MirakelModelPreferences extends MirakelPreferences {
	
	public static void setDefaultAccount(AccountMirakel a) {
		settings.edit().putInt("defaultAccountID", a.getId()).commit();
	}
	
	public static AccountMirakel getDefaultAccount() {
		int id = settings.getInt("defaultAccountID", AccountMirakel.getLocal()
				.getId());
		AccountMirakel a = AccountMirakel.get(id);
		if (a != null) return a;
		return AccountMirakel.getLocal();
	}
	
	public static ListMirakel getImportDefaultList(boolean safe) {
		if (settings.getBoolean("importDefaultList", false)) {
			int listId = settings.getInt("defaultImportList", 0);
			if (listId == 0) return null;
			return ListMirakel.getList(listId);
		}
		if (!safe) return null;
		return ListMirakel.safeFirst(context);
	}
	
	public static ListMirakel getListForSubtask(Task parent) {
		if (settings.contains("subtaskAddToSameList")) {
			if (MirakelCommonPreferences.addSubtaskToSameList()) return parent.getList();
			return subtaskAddToList();
		}
		// Create a new list and set this list as the default list for future subtasks
		return ListMirakel.newList(context
				.getString(R.string.subtask_list_name));
	}

	private static ListMirakel getListFromIdString(int preference) {
		ListMirakel list;
		try {
			list = ListMirakel.getList(preference);
		} catch (NumberFormatException e) {
			list = SpecialList.firstSpecial();
		}
		if (list == null) {
			list = ListMirakel.safeFirst(context);
		}
		return list;
	}
	
	public static ListMirakel getNotificationsList() {
		return getListFromIdString(MirakelCommonPreferences.getNotificationsListId());
	}
	
	public static ListMirakel getNotificationsListOpen() {
		return ListMirakel.getList(MirakelCommonPreferences.getNotificationsListOpenId());
	}
	
	public static ListMirakel getStartupList() {
		try {
			return ListMirakel.safeGetList(Integer.parseInt(settings.getString(
					"startupList", "-1")));
		} catch (NumberFormatException E) {
			return ListMirakel.safeFirst(context);
		}
	}

	public static int getSyncFrequency(AccountMirakel account) {
		try {
			return Integer.parseInt(settings.getString("syncFrequency"
					+ account.getName(), "-1"));
		} catch (NumberFormatException E) {
			return -1;
		}
	}
	
	public static ListMirakel subtaskAddToList() {
		try {
			return ListMirakel.getList(Integer.parseInt(settings.getString(
					"subtaskAddToList", "-1")));
		} catch (NumberFormatException E) {
			return null;
		}
	}
	
	public static boolean useSync() {
		List<AccountMirakel>all=AccountMirakel.getAll();
		for (AccountMirakel a : all) {
			if (a.getType() != ACCOUNT_TYPES.LOCAL && a.isEnabeld()) return true;
		}
		return false;
	}

}
