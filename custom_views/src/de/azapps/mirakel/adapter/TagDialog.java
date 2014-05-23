package de.azapps.mirakel.adapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;

public class TagDialog extends DialogFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private String searchString;
	private TagAdapter adapter;
	private Context ctx;
	private final SQLiteDatabase db;
	private Task task;
	private OnTaskChangedListner taskChanged;

	public TagDialog() {
		this.db = MirakelContentProvider.getReadableDatabase();
		this.searchString = "";
	}

	private void setContext(final Context ctx) {
		this.ctx = ctx;
	}

	private void init() {
		this.adapter = new TagAdapter(this.ctx);
		this.adapter.setOnClickListerner(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				final Tag t = (Tag) v.getTag();
				TagDialog.this.task.addTag(t);
				dismiss();
				TagDialog.this.taskChanged.onTaskChanged(TagDialog.this.task);
			}
		});
	}

	public static TagDialog newDialog(final Context ctx, final Task task,
			final OnTaskChangedListner changed) {
		final TagDialog t = new TagDialog();
		t.setContext(ctx);
		t.setTask(task);
		t.setOnTaskChanged(changed);
		t.init();

		return t;
	}

	private void setOnTaskChanged(final OnTaskChangedListner changed) {
		this.taskChanged = changed;
	}

	private void setTask(final Task task) {
		this.task = task;

	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final View view = getActivity().getLayoutInflater().inflate(
				R.layout.add_tag_dialog, null);
		final ListView tagList = (ListView) view.findViewById(R.id.tag_list);
		tagList.setAdapter(this.adapter);
		getLoaderManager().initLoader(0, null, this);
		final EditText search = (EditText) view.findViewById(R.id.search_tag);
		search.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(final CharSequence s, final int start,
					final int before, final int count) {
				TagDialog.this.searchString = search.getText().toString();
				getLoaderManager().restartLoader(0, null, TagDialog.this);

			}

			@Override
			public void beforeTextChanged(final CharSequence s,
					final int start, final int count, final int after) {
				// nothing

			}

			@Override
			public void afterTextChanged(final Editable s) {
				// nothing

			}
		});
		search.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(final TextView exampleView,
					final int actionId, final KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND
						|| actionId == EditorInfo.IME_NULL
						&& event.getAction() == KeyEvent.ACTION_DOWN) {
					createNewTag(search);
				}
				return true;
			}
		});
		final ImageButton enter = (ImageButton) view
				.findViewById(R.id.tag_enter);
		enter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				createNewTag(search);
			}

		});

		return new AlertDialog.Builder(getActivity()).setView(view).create();
	}

	protected void createNewTag(final EditText search) {
		TagDialog.this.searchString = search.getText().toString();
		getLoaderManager().restartLoader(0, null, TagDialog.this);
		final Cursor c = TagDialog.this.db.query(Tag.TABLE,
				new String[] { "count(*)" }, DatabaseHelper.NAME + " LIKE ?",
				new String[] { TagDialog.this.searchString + '%' }, null, null,
				null);
		c.moveToFirst();
		if (c.getCount() > 0 && c.getInt(0) == 0) {// tag does not
													// exists
			final InputMethodManager imm = (InputMethodManager) this.ctx
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
			final Tag t = Tag.newTag(TagDialog.this.searchString);
			TagDialog.this.task.addTag(t);
			dismiss();
			TagDialog.this.taskChanged.onTaskChanged(TagDialog.this.task);

		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int arg0, final Bundle arg1) {
		final Uri u = Uri.parse("content://"
				+ DefinitionsHelper.AUTHORITY_INTERNAL + "/" + "tags");
		final String tagsQuery = "AND " + DatabaseHelper.ID + " NOT IN ("
				+ Tag.getTagsQuery(new String[] { DatabaseHelper.ID }) + ")";
		return new CursorLoader(getActivity(), u, Tag.allColumns,
				DatabaseHelper.NAME + " LIKE ? " + tagsQuery, new String[] {
						this.searchString + "%", this.task.getId() + "" }, null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> arg0, final Cursor newCursor) {
		this.adapter.swapCursor(newCursor);

	}

	@Override
	public void onLoaderReset(final Loader<Cursor> arg0) {
		this.adapter.swapCursor(null);

	}

}
