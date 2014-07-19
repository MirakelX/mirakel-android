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
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.SharingHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.main_activity.MirakelFragment;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class ListFragment extends MirakelFragment {
    private static final int LIST_COLOR = 0, LIST_RENAME = 1, LIST_DESTROY = 2,
                             LIST_SHARE = 3;
    protected static final String TAG = "ListFragment";

    public ListFragment () {
        super ();
    }

    // private static final String TAG = "ListsActivity";
    protected ListAdapter adapter;
    protected boolean EditName;
    private boolean enableDrag;

    protected EditText input;

    // private DragNDropListView listView;
    protected DragSortListView listView;

    protected ActionMode mActionMode = null;

    @TargetApi (Build.VERSION_CODES.HONEYCOMB)
    public void closeNavDrawer () {
        if (this.mActionMode != null
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.mActionMode.finish ();
        }
        if (this.enableDrag) {
            this.enableDrag = false;
            update ();
        }
    }

    void editColor (final List<ListMirakel> lists) {
        final View v = ((LayoutInflater) this.main
                        .getSystemService (Context.LAYOUT_INFLATER_SERVICE)).inflate (
                           R.layout.color_picker, null);
        final ColorPicker cp = (ColorPicker) v.findViewById (R.id.color_picker);
        if (lists.size () == 1) {
            cp.setColor (lists.get (0).getColor ());
            cp.setOldCenterColor (lists.get (0).getColor ());
        }
        final SVBar op = (SVBar) v.findViewById (R.id.svbar_color_picker);
        cp.addSVBar (op);
        new AlertDialog.Builder (this.main)
        .setView (v)
        .setTitle (this.main.getString (R.string.list_change_color))
        .setPositiveButton (R.string.set_color, new OnClickListener () {
            @Override
            public void onClick (final DialogInterface dialog,
                                 final int which) {
                for (final ListMirakel list : lists) {
                    list.setColor (cp.getColor ());
                    list.save ();
                }
                ListFragment.this.main.getListFragment ().update ();
            }
        })
        .setNegativeButton (R.string.unset_color, new OnClickListener () {
            @Override
            public void onClick (final DialogInterface dialog,
                                 final int which) {
                for (final ListMirakel list : lists) {
                    list.setColor (0);
                    list.save ();
                }
                ListFragment.this.main.getListFragment ().update ();
            }
        }).show ();
    }

    void editColor (final ListMirakel list) {
        final List<ListMirakel> l = new ArrayList<ListMirakel> ();
        l.add (list);
        editColor (l);
    }

    /**
     * Edit the name of the List
     *
     * @param list
     */
    public void editList (final ListMirakel list) {
        this.input = new EditText (this.main);
        this.input.setText (list == null ? "" : list.getName ());
        this.input.setTag (this.main);
        this.input.setInputType (InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        final InputMethodManager imm = (InputMethodManager) getActivity ()
                                       .getSystemService (Context.INPUT_METHOD_SERVICE);
        new AlertDialog.Builder (this.main)
        .setTitle (this.main.getString (R.string.list_change_name_title))
        .setView (this.input)
        .setPositiveButton (this.main.getString (android.R.string.ok),
        new DialogInterface.OnClickListener () {
            @Override
            public void onClick (final DialogInterface dialog,
                                 final int whichButton) {
                ListMirakel l = list;
                if (l == null) {
                    l = ListMirakel
                        .newList (ListFragment.this.input
                                  .getText ().toString ());
                } else {
                    l.setName (ListFragment.this.input.getText ()
                               .toString ());
                }
                l.save (list != null);
                update ();
            }
        })
        .setNegativeButton (
            this.main.getString (android.R.string.cancel),
        new DialogInterface.OnClickListener () {
            @Override
            public void onClick (final DialogInterface dialog,
                                 final int whichButton) {
                imm.hideSoftInputFromWindow (getActivity ()
                                             .getCurrentFocus ().getWindowToken (),
                                             InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }).show ();
        this.input.postDelayed (new Runnable () {
            @Override
            public void run () {
                if (getActivity () == null) {
                    return;
                }
                final InputMethodManager keyboard = (InputMethodManager) getActivity ()
                                                    .getSystemService (Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput (ListFragment.this.input,
                                        InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    protected void editListAccount (final List<ListMirakel> lists) {
        final View v = ((LayoutInflater) this.main
                        .getSystemService (Context.LAYOUT_INFLATER_SERVICE)).inflate (
                           R.layout.dialog_list_account, null);
        final Spinner s = (Spinner) v.findViewById (R.id.select_account);
        final List<AccountMirakel> accounts = AccountMirakel.all ();
        final List<String> names = new ArrayList<String> ();
        for (final AccountMirakel a : accounts) {
            names.add (a.getName ());
        }
        final ArrayAdapter<String> adp = new ArrayAdapter<String> (this.main,
                android.R.layout.simple_list_item_1, names);
        s.setAdapter (adp);
        new AlertDialog.Builder (this.main).setView (v)
        .setTitle (R.string.change_account)
        .setPositiveButton (android.R.string.ok, new OnClickListener () {
            @Override
            public void onClick (final DialogInterface dialog,
                                 final int which) {
                String where = Task.LIST_ID + " IN (";
                boolean c = false;
                for (final ListMirakel l : lists) {
                    l.setAccount (accounts.get ((int) s
                                                .getSelectedItemId ()));
                    l.save ();
                    where += (c ? "," : "") + l.getId ();
                    c = true;
                }
                where += ")";
                final ContentValues cv = new ContentValues ();
                cv.put (SyncAdapter.SYNC_STATE, SYNC_STATE.ADD.toInt ());
                main.getContentResolver().update(MirakelInternalContentProvider.TASK_URI, cv,
                                                 where + " AND NOT " + SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE, null);
                getActivity().getContentResolver().delete(MirakelInternalContentProvider.CALDAV_LISTS_URI, "" +
                        "where " + ModelBase.ID + " in( select " + ModelBase.ID + " from " + Task.TABLE
                        + " where " + where + ");", null);
                ListFragment.this.main.getListFragment ().update ();
            }
        }).setNegativeButton (android.R.string.cancel, null).show ();
    }

    public void enableDrop (final boolean drag) {
        this.enableDrag = drag;
        update ();
    }

    public ListAdapter getAdapter () {
        return this.adapter;
    }

    @Override
    public View onCreateView (final LayoutInflater inflater,
                              final ViewGroup container, final Bundle savedInstanceState) {
        this.main = (MainActivity) getActivity ();
        this.EditName = false;
        this.enableDrag = false;
        this.view = inflater.inflate (R.layout.list_fragment, container, false);
        if (MirakelCommonPreferences.isDark ()) {
            this.view.findViewById (R.id.lists_list).setBackgroundResource (
                android.R.drawable.screen_background_dark);
        } else {
            this.view.findViewById (R.id.lists_list).setBackgroundColor (
                getResources ().getColor (android.R.color.background_light));
        }
        // Inflate the layout for this fragment
        update ();
        return this.view;
    }

    public void refresh () {
        this.adapter.changeData (ListMirakel.all ());
        this.adapter.notifyDataSetChanged ();
    }

    @Override
    @SuppressLint ("NewApi")
    public void update () {
        if (this.view == null || getActivity () == null) {
            return;
        }
        final List<ListMirakel> values = ListMirakel.all ();
        this.main.updateLists ();
        this.listView = (DragSortListView) this.view
                        .findViewById (R.id.lists_list);
        if (this.adapter != null
            && this.enableDrag == this.adapter.isDropEnabled ()
            && this.main != null) {
            this.adapter.changeData (values);
            this.main.runOnUiThread (new Runnable () {
                @Override
                public void run () {
                    ListFragment.this.adapter.notifyDataSetChanged ();
                }
            });
            return;
        }
        this.adapter = new ListAdapter (getActivity (), R.layout.lists_row,
                                        values, this.enableDrag);
        this.listView.setDragEnabled (this.enableDrag);
        this.listView.setParts (SpecialList.getSpecialListCount (true));
        this.listView.setAdapter (this.adapter);
        this.listView.requestFocus ();
        this.listView.setDropListener (new DropListener () {
            @Override
            public void drop (final int from, final int to) {
                if (from != to) {
                    ListFragment.this.adapter.onDrop (from, to);
                    ListFragment.this.listView.requestLayout ();
                }
                Log.v (TAG, "Drop from:" + from + " to:" + to);
            }
        });
        this.listView
        .setOnItemClickListener (new AdapterView.OnItemClickListener () {
            @Override
            public void onItemClick (final AdapterView<?> parent,
                                     final View item, final int position, final long id) {
                if (ListFragment.this.EditName) {
                    ListFragment.this.EditName = false;
                    return;
                }
                final ListMirakel list = values.get ((int) id);
                ListFragment.this.main.setCurrentList (list, item);
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            this.listView
            .setOnItemLongClickListener (new AdapterView.OnItemLongClickListener () {
                @Override
                public boolean onItemLongClick (
                    final AdapterView<?> parent, final View item,
                    final int position, final long id) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder (
                        getActivity ());
                    final ListMirakel list = values.get ((int) id);
                    builder.setTitle (list.getName ());
                    final List<CharSequence> items = new ArrayList<CharSequence> (
                        Arrays.asList (getActivity ().getResources ()
                                       .getStringArray (
                                           R.array.list_actions_items)));
                    builder.setItems (items
                                      .toArray (new CharSequence[items.size ()]),
                    new DialogInterface.OnClickListener () {
                        @Override
                        public void onClick (
                            final DialogInterface dialog,
                            final int item) {
                            final ListMirakel list = values
                                                     .get ((int) id);
                            switch (item) {
                            case LIST_COLOR:
                                editColor (list);
                                break;
                            case LIST_RENAME:
                                editList (list);
                                break;
                            case LIST_DESTROY:
                                ListFragment.this.main
                                .handleDestroyList (list);
                                break;
                            case LIST_SHARE:
                                SharingHelper.share (
                                    getActivity (), list);
                                break;
                            default:
                                break;
                            }
                        }
                    });
                    final AlertDialog dialog = builder.create ();
                    dialog.show ();
                    return false;
                }
            });
        } else {
            this.listView.setChoiceMode (AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
            if (this.adapter != null) {
                this.adapter.resetSelected ();
            }
            this.listView.setHapticFeedbackEnabled (true);
            this.listView
            .setMultiChoiceModeListener (new MultiChoiceModeListener () {
                @Override
                public boolean onActionItemClicked (
                    final ActionMode mode, final MenuItem item) {
                    final List<ListMirakel> lists = ListFragment.this.adapter
                                                    .getSelected ();
                    switch (item.getItemId ()) {
                    case R.id.menu_delete:
                        ListFragment.this.main.handleDestroyList (lists);
                        break;
                    case R.id.menu_color:
                        editColor (lists);
                        break;
                    case R.id.edit_list:
                        editList (lists.get (0));
                        break;
                    case R.id.share_list_from_lists:
                        SharingHelper.share (getActivity (), lists.get (0));
                        break;
                    case R.id.edit_listaccount:
                        editListAccount (lists);
                        break;
                    default:
                        break;
                    }
                    mode.finish ();
                    return false;
                }
                @SuppressLint ("NewApi")
                @Override
                public boolean onCreateActionMode (
                    final ActionMode mode, final Menu menu) {
                    final MenuInflater inflater = mode
                                                  .getMenuInflater ();
                    inflater.inflate (R.menu.context_lists, menu);
                    ListFragment.this.mActionMode = mode;
                    return true;
                }
                @Override
                public void onDestroyActionMode (final ActionMode mode) {
                    ListFragment.this.adapter.resetSelected ();
                }
                @Override
                public void onItemCheckedStateChanged (
                    final ActionMode mode, final int position,
                    final long id, final boolean checked) {
                    Log.d (TAG, "item " + position + " selected");
                    final int oldCount = ListFragment.this.adapter
                                         .getSelectedCount ();
                    ListFragment.this.adapter.setSelected (position,
                                                           checked);
                    final int newCount = ListFragment.this.adapter
                                         .getSelectedCount ();
                    mode.setTitle (ListFragment.this.main.getResources ()
                                   .getQuantityString (
                                       R.plurals.selected_lists, newCount,
                                       newCount));
                    if (oldCount < 2 && newCount >= 2 || oldCount >= 2
                        && newCount < 2) {
                        mode.invalidate ();
                    }
                }
                @Override
                public boolean onPrepareActionMode (
                    final ActionMode mode, final Menu menu) {
                    menu.findItem (R.id.edit_list).setVisible (
                        ListFragment.this.adapter
                        .getSelectedCount () <= 1);
                    menu.findItem (R.id.share_list_from_lists)
                    .setVisible (
                        ListFragment.this.adapter
                        .getSelectedCount () <= 1);
                    return false;
                }
            });
        }
    }

    @SuppressLint ("NewApi")
    public void hideActionMode () {
        if (this.mActionMode != null) {
            this.mActionMode.finish ();
        }
    }

}
