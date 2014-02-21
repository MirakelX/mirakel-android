package de.azapps.mirakel.helper;

import java.util.List;

import de.azapps.mirakel.custom_views.TaskDetailView.TYPE;

public class MirakelViewPreferences  extends MirakelCommonPreferences{
	
	public static List<Integer> getTaskFragmentLayout() {
		List<Integer> items = MirakelCommonPreferences
				.loadIntArray("task_fragment_adapter_settings");
		if (items.size() == 0) {// should not be, add all
			items.add(TYPE.HEADER);
			items.add(TYPE.DUE);
			items.add(TYPE.REMINDER);
			items.add(TYPE.CONTENT);
			items.add(TYPE.PROGRESS);
			items.add(TYPE.SUBTASK);
			items.add(TYPE.FILE);
			setTaskFragmentLayout(items);
		}
		return items;
	}

}
