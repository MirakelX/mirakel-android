/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.settings.taskfragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cocosw.undobar.UndoBarController;
import com.google.common.base.Optional;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;

import java.util.List;

import de.azapps.mirakel.custom_views.TaskDetailView;
import de.azapps.mirakel.helper.MirakelViewPreferences;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;


@SuppressLint("NewApi")
public class TaskFragmentSettingsFragment extends Fragment {
    public static final int ADD_KEY = -1;
    private final static String TAG = "de.azapps.mirakel.settings.taskfragment.TaskFragmentSettings";
    protected TaskFragmentSettingsAdapter adapter;
    protected DragSortListView listView;
    private Optional<UndoBarController.UndoBar> undoBar = absent();

    public TaskFragmentSettingsFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(
                              R.layout.activity_task_fragment_settings, null);
        setupView(view);
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(R.string.settings_task_fragment);
        }
        return view;
    }

    void setupView(final View v) {
        final List<Integer> values = MirakelViewPreferences
                                     .getTaskFragmentLayout();
        values.add(ADD_KEY);
        if (this.adapter != null) {
            this.adapter.changeData(values);
            this.adapter.notifyDataSetChanged();
            return;
        }
        this.adapter = new TaskFragmentSettingsAdapter(getActivity(),
                R.layout.row_taskfragment_settings, values);
        this.adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                if (TaskFragmentSettingsFragment.this.listView != null) {
                    TaskFragmentSettingsFragment.this.listView
                    .setParts(TaskFragmentSettingsFragment.this.adapter
                              .getCount() - 1);
                }
            }
        });
        this.listView = (DragSortListView) v
                        .findViewById(R.id.taskfragment_list);
        this.listView.setItemsCanFocus(true);
        this.listView.setAdapter(this.adapter);
        this.listView.setParts(this.adapter.getCount() - 1);
        this.listView.requestFocus();
        this.listView.setDropListener(new DropListener() {
            @Override
            public void drop(final int from, final int to) {
                if (from != to
                    && to != TaskFragmentSettingsFragment.this.listView
                    .getCount() - 1) {
                    TaskFragmentSettingsFragment.this.adapter.onDrop(from, to);
                    TaskFragmentSettingsFragment.this.listView.requestLayout();
                }
                Log.v(TAG, "Drop from:" + from + " to:" + to);
            }
        });
        this.listView.setRemoveListener(new RemoveListener() {
            @Override
            public void remove(final int which) {
                if (which != TaskFragmentSettingsFragment.this.adapter
                    .getCount() - 1) {
                    final Pair<Integer, Integer> removed = new Pair(which, adapter.getItem(which));
                    TaskFragmentSettingsFragment.this.adapter.onRemove(which);
                    if (undoBar.isPresent()) {
                        undoBar.get().clear();
                    }
                    undoBar = of(new UndoBarController.UndoBar(getActivity()));

                    try {
                        undoBar.get().listener(new UndoBarController.AdvancedUndoListener() {
                            @Override
                            public void onUndo(Parcelable parcelable) {
                                adapter.addToData(removed.first, removed.second);
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onHide(Parcelable parcelable) {

                            }

                            @Override
                            public void onClear() {
                                undoBar = absent();
                            }
                        }).message(getActivity().getString(R.string.undo_taskui_change,
                                                           TaskDetailView.TYPE.getTranslatedName(getActivity(), removed.second))).show();
                    } catch (TaskDetailView.TYPE.NoSuchItemException e) {
                        Log.wtf(TAG, "unknown item removed");
                        throw new IllegalArgumentException("This can never ever happen, because item has in list already called this and must be valid",
                                                           e);
                    }
                }
            }
        });
        this.listView.setOnItemClickListener(null);
    }


}
