package de.azapps.mirakel.model.list;

import java.util.List;

import android.content.Context;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class SearchList extends ListMirakel {
	private List<Task> tasks;

	public SearchList(Context ctx, String query) {
		super(0, ctx.getString(R.string.search_result_title, query));
		this.tasks = Task.searchName(query);
	}

	@Override
	public List<Task> tasks() {
		return tasks(false);
	}

	@Override
	public List<Task> tasks(boolean showDone) {
		return this.tasks;
	}
}
