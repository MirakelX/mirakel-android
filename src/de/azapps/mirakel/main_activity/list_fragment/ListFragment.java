/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.main_activity.list_fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.SharingHelper;
import de.azapps.mirakel.main_activity.DragNDropListView;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.main_activity.MirakelFragment;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;

public class ListFragment extends MirakelFragment {
	// private static final String TAG = "ListsActivity";
	private ListAdapter adapter;
	protected EditText input;
	protected boolean EditName;
	private DragNDropListView listView;
	private static final int LIST_COLOR = 0, LIST_RENAME = 1, LIST_DESTROY = 2,
			LIST_SHARE = 3;
	protected static final String TAG = "ListFragment";
	private boolean enableDrag;
	private ActionMode mActionMode = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setSingleton(this);
		main = (MainActivity) getActivity();
		EditName = false;
		enableDrag = false;
		view = inflater.inflate(R.layout.list_fragment, container, false);
		if (MirakelPreferences.isDark()) {
			view.findViewById(R.id.lists_list).setBackgroundResource(
					android.R.drawable.screen_background_dark);
		} else {
			view.findViewById(R.id.lists_list).setBackgroundColor(
					getResources().getColor(android.R.color.background_light));
		}
		// Inflate the layout for this fragment
		update();
		return view;
	}

	public void enableDrop(boolean drag) {
		enableDrag = drag;
		update();
	}

	@SuppressLint("NewApi")
	public void update() {
		if (view == null || this.getActivity() == null)
			return;
		final List<ListMirakel> values = ListMirakel.all();
		main.updateLists();

		main.showMessageFromSync();

		if (adapter != null && enableDrag == adapter.isDropEnabled()) {
			adapter.changeData(values);
			adapter.notifyDataSetChanged();
			return;
		}

		adapter = new ListAdapter(this.getActivity(), R.layout.lists_row,
				values, enableDrag);
		listView = (DragNDropListView) view.findViewById(R.id.lists_list);
		listView.setEnableDrag(enableDrag);
		listView.setItemsCanFocus(true);
		listView.setAdapter(adapter);
		listView.requestFocus();
		listView.setDragListener(new DragNDropListView.DragListener() {

			@Override
			public void onStopDrag(View itemView) {
				itemView.setVisibility(View.VISIBLE);

			}

			@Override
			public void onStartDrag(View itemView) {
				itemView.setVisibility(View.INVISIBLE);

			}

			@Override
			public void onDrag(int x, int y, ListView listView) {
				// Nothing
			}
		});
		listView.setDropListener(new DragNDropListView.DropListener() {

			@Override
			public void onDrop(int from, int to) {
				if (from != to) {
					adapter.onDrop(from, to);
					listView.requestLayout();
				}
				Log.e(TAG, "Drop from:" + from + " to:" + to);

			}
		});

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				if (EditName) {
					EditName = false;
					return;
				}

				ListMirakel list = values.get((int) id);
				main.setCurrentList(list, item);
			}
		});
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> parent,
						View item, int position, final long id) {

					AlertDialog.Builder builder = new AlertDialog.Builder(
							getActivity());
					ListMirakel list = values.get((int) id);
					builder.setTitle(list.getName());
					List<CharSequence> items = new ArrayList<CharSequence>(
							Arrays.asList(getActivity().getResources()
									.getStringArray(R.array.list_actions_items)));

					builder.setItems(
							items.toArray(new CharSequence[items.size()]),
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int item) {
									ListMirakel list = values.get((int) id);
									switch (item) {
									case LIST_COLOR:
										editColor(list);
										break;
									case LIST_RENAME:
										editList(list);
										break;
									case LIST_DESTROY:
										main.handleDestroyList(list);
										break;
									case LIST_SHARE:
										SharingHelper.share(getActivity(), list);
										break;
									}
								}
							});

					AlertDialog dialog = builder.create();
					dialog.show();

					return false;
				}
			});
		} else {
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			if (adapter != null) {
				adapter.resetSelected();
			}
			listView.setHapticFeedbackEnabled(true);
			listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					menu.findItem(R.id.edit_list).setVisible(
							adapter.getSelectedCount() <= 1);
					menu.findItem(R.id.share_list_from_lists).setVisible(
							adapter.getSelectedCount() <= 1);
					return false;
				}

				@Override
				public void onDestroyActionMode(ActionMode mode) {
					adapter.resetSelected();
				}

				@SuppressLint("NewApi")
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.context_lists, menu);
					mActionMode = mode;
					return true;
				}

				@Override
				public boolean onActionItemClicked(ActionMode mode,
						MenuItem item) {
					List<ListMirakel> lists = adapter.getSelected();
					switch (item.getItemId()) {
					case R.id.menu_delete:
						main.handleDestroyList(lists);
						break;
					case R.id.menu_color:
						editColor(lists);
						break;
					case R.id.edit_list:
						editList(lists.get(0));
						break;
					case R.id.share_list_from_lists:
						SharingHelper.share(getActivity(), lists.get(0));
						break;
					case R.id.edit_listaccount:
						editListAccount(lists);
					}
					mode.finish();
					return false;
				}

				@Override
				public void onItemCheckedStateChanged(ActionMode mode,
						int position, long id, boolean checked) {
					Log.d(TAG, "item " + position + " selected");
					int oldCount = adapter.getSelectedCount();
					adapter.setSelected(position, checked);
					int newCount = adapter.getSelectedCount();
					Log.e(TAG, "old count: " + oldCount + " | newCount: "
							+ newCount);
					mode.setTitle(main.getResources().getQuantityString(
							R.plurals.selected_lists, newCount, newCount));
					if ((oldCount < 2 && newCount >= 2)
							|| (oldCount >= 2 && newCount < 2)) {
						mode.invalidate();
					}

				}
			});
		}
	}

	protected void editListAccount(final List<ListMirakel> lists) {
		final View v = ((LayoutInflater) main
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.dialog_list_account, null);
		final Spinner s = (Spinner) v.findViewById(R.id.select_account);
		final List<AccountMirakel> accounts = AccountMirakel.getAll();
		List<String> names = new ArrayList<String>();
		for (AccountMirakel a : accounts) {
			names.add(a.getName());
		}
		ArrayAdapter<String> adp = new ArrayAdapter<String>(main,
				android.R.layout.simple_list_item_1, names);
		s.setAdapter(adp);

		new AlertDialog.Builder(main).setView(v)
				.setTitle(R.string.change_account)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String where = Task.LIST_ID + " IN (";
						boolean c = false;
						for (ListMirakel l : lists) {
							l.setAccount(accounts.get((int) s
									.getSelectedItemId()));
							l.save();
							where += (c ? "," : "") + l.getId();
							c = true;
						}
						where += ")";
						ContentValues cv = new ContentValues();
						cv.put(SyncAdapter.SYNC_STATE, SYNC_STATE.ADD.toInt());
						Mirakel.getWritableDatabase().update(
								Task.TABLE,
								cv,
								where + " AND NOT " + SyncAdapter.SYNC_STATE
										+ "=" + SYNC_STATE.DELETE, null);
String query="DELETE FROM caldav_extra where "
		+ DatabaseHelper.ID
		+ " in( select "
		+ DatabaseHelper.ID + " from "
		+ Task.TABLE + " where "
		+ where + ");";
Log.w(TAG, query);
						Mirakel.getWritableDatabase()
								.rawQuery(
										query, null);
						main.getListFragment().update();
					}
				}).setNegativeButton(android.R.string.cancel, null).show();

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void closeActionMode() {
		if (mActionMode != null
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mActionMode.finish();
	}

	public ListAdapter getAdapter() {
		return adapter;
	}

	void editColor(final ListMirakel list) {
		List<ListMirakel> l = new ArrayList<ListMirakel>();
		l.add(list);
		editColor(l);
	}

	void editColor(final List<ListMirakel> lists) {
		final View v = ((LayoutInflater) main
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.color_picker, null);
		final ColorPicker cp = ((ColorPicker) v.findViewById(R.id.color_picker));
		if (lists.size() == 1) {
			cp.setColor(lists.get(0).getColor());
			cp.setOldCenterColor(lists.get(0).getColor());
		}
		final SVBar op = ((SVBar) v.findViewById(R.id.svbar_color_picker));
		cp.addSVBar(op);
		new AlertDialog.Builder(main).setView(v)
				.setTitle(main.getString(R.string.list_change_color))
				.setPositiveButton(R.string.set_color, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (ListMirakel list : lists) {
							list.setColor(cp.getColor());
							list.save();
						}
						main.getListFragment().update();

					}
				})
				.setNegativeButton(R.string.unset_color, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (ListMirakel list : lists) {
							list.setColor(0);
							list.save();
						}
						main.getListFragment().update();
					}
				}).show();
	}

	/**
	 * Edit the name of the List
	 * 
	 * @param list
	 */
	public void editList(final ListMirakel list) {

		input = new EditText(main);
		input.setText(list == null ? "" : list.getName());
		input.setTag(main);
		input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		new AlertDialog.Builder(main)
				.setTitle(main.getString(R.string.list_change_name_title))
				.setMessage(main.getString(R.string.list_change_name_cont))
				.setView(input)
				.setPositiveButton(main.getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// List_mirakle list = values.get((int) id);
								ListMirakel l = list;
								if (list == null)
									l = ListMirakel.newList(input.getText()
											.toString());
								else
									l.setName(input.getText().toString());
								l.save(list != null);
								update();
								if (list == null) {
									listView.setSelection(listView.getAdapter()
											.getCount() - 1);
								}
							}
						})
				.setNegativeButton(main.getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
						}).show();
		input.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (getActivity() == null)
					return;
				InputMethodManager keyboard = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);

	}

	/**
	 * Pointer to the fragment itself
	 */
	private static ListFragment me = null;

	protected static void setSingleton(ListFragment me) {
		if (ListFragment.me == null)
			ListFragment.me = me;
	}

	public static ListFragment getSingleton() {
		return me;
	}

}
