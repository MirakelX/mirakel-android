/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.mirakel.new_ui.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.AnimationHelper;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.views.FloatingActionButton;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskOverview;
import de.azapps.mirakel.new_ui.activities.MirakelActivity;
import de.azapps.mirakel.new_ui.adapter.TaskAdapter;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class TasksFragment extends Fragment implements LoaderManager.LoaderCallbacks,
    MultiSelectCursorAdapter.MultiSelectCallbacks<TaskOverview>, ActionMode.Callback,
    ActionClickListener {

    public static final String ARGUMENT_LIST = "list";
    private static final String TAG = "de.azapps.mirakel.new_ui.fragments.TasksFragment";

    private TaskAdapter mAdapter;
    @InjectView(R.id.task_listview)
    RecyclerView mListView;
    @InjectView(R.id.fabbutton)
    public FloatingActionButton floatingActionButton;
    private OnItemClickedListener<TaskOverview> mListener;
    @Nullable
    private ActionMode mActionMode;
    @InjectView(R.id.tasks_multiselect_menu)
    LinearLayout multiselectMenu;
    @InjectView(R.id.menu_move_task)
    ImageView menuMoveTask;

    private ListMirakelInterface listMirakel;

    public TasksFragment() {
        // Required empty public constructor
    }

    public static TasksFragment newInstance(final ListMirakelInterface listMirakel) {
        final TasksFragment tasksFragment = new TasksFragment();
        // Supply num input as an argument.
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_LIST, listMirakel);
        tasksFragment.setArguments(args);
        return tasksFragment;
    }

    public ListMirakelInterface getList() {
        return listMirakel;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new TaskAdapter(getActivity(), null, mListener, this);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }


    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnItemClickedListener<TaskOverview>) activity;
        } catch (final ClassCastException ignored) {
            throw new ClassCastException(activity.toString() + " OnItemClickedListener<TaskOverview>");
        }
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View layout = inflater.inflate(R.layout.fragment_tasks, container, false);
        ButterKnife.inject(this, layout);
        return layout;
    }

    @OnClick(R.id.fabbutton)
    public void addTask() {
        final ListMirakel listToAdd;
        if (listMirakel instanceof  ListMirakel) {
            listToAdd = (ListMirakel) listMirakel;
        } else {
            listToAdd = ListMirakel.getInboxList(MirakelModelPreferences.getDefaultAccount());
        }
        final Task task = Semantic.createStubTask(getString(R.string.task_new), fromNullable(listToAdd),
                          true, getActivity());
        mListener.onItemSelected(new TaskOverview(task));
    }

    public void setList(final ListMirakelInterface listMirakel) {
        this.listMirakel = listMirakel;
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_LIST, listMirakel);
        getLoaderManager().restartLoader(0, args, this);
    }

    @Override
    public Loader onCreateLoader(final int i, final Bundle arguments) {
        listMirakel = arguments.getParcelable(ARGUMENT_LIST);
        return listMirakel.getTaskOverviewSupportCursorLoader();
    }

    @Override
    public void onLoadFinished(final Loader loader, final Object o) {
        mAdapter.swapCursor((Cursor) o);
    }

    @Override
    public void onLoaderReset(final Loader loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onSelectModeChanged(boolean selectMode) {
        if (selectMode) {
            mActionMode = getActivity().startActionMode(this);
        } else {
            assert mActionMode != null;
            mActionMode.finish();
        }
    }

    @Override
    public boolean canAddItem(@NonNull TaskOverview item) {
        return true;
    }


    public void onSelectedItemCountChanged(final int itemCount) {
        assert mActionMode != null;
        if (itemCount == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(getResources().getQuantityString(R.plurals.task_multiselect_title,
                                 itemCount, itemCount));
        }
    }

    private void showOrHideMoveTasks() {
        final ArrayList<TaskOverview> tasks = mAdapter.getSelectedItems();
        if (tasks.size() == 0) {
            // Do not need to change anything
            return;
        }
        Optional<AccountMirakel> accountMirakelOptional = tasks.get(0).getAccountMirakel();
        final boolean shouldHide;
        if (accountMirakelOptional.isPresent()) {
            final long accountMirakelId = accountMirakelOptional.get().getId();
            shouldHide = Iterables.all(tasks, new Predicate<TaskOverview>() {
                @Override
                public boolean apply(@Nullable TaskOverview input) {
                    final Optional<AccountMirakel> compareAccount = input.getAccountMirakel();
                    if (compareAccount.isPresent()) {
                        return compareAccount.get().getId() == accountMirakelId;
                    } else {
                        return false;
                    }
                }
            });
        } else {
            shouldHide = true;
        }
        if (shouldHide) {
            menuMoveTask.setVisibility(View.VISIBLE);
        } else {
            menuMoveTask.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAddSelectedItem(@NonNull TaskOverview item) {
        assert mActionMode != null;
        onSelectedItemCountChanged(mAdapter.getSelectedItemCount());
        showOrHideMoveTasks();
    }

    @Override
    public void onRemoveSelectedItem(@NonNull TaskOverview item) {
        assert mActionMode != null;
        onSelectedItemCountChanged(mAdapter.getSelectedItemCount());
        showOrHideMoveTasks();
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        final MenuInflater inflater = actionMode
                                      .getMenuInflater();
        inflater.inflate(R.menu.multiselect_tasks, menu);

        AnimationHelper.slideIn(getActivity(), multiselectMenu);
        ((MirakelActivity) getActivity()).moveFABUp(multiselectMenu.getHeight());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ThemeManager.getColor(R.attr.colorCABStatus));
        }
        final int count = mAdapter.getSelectedItemCount();
        actionMode.setTitle(getResources().getQuantityString(R.plurals.task_multiselect_title, count,
                            count));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }


    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mAdapter.clearSelections();
        mActionMode = null;

        AnimationHelper.slideOut(getActivity(), multiselectMenu);
        ((MirakelActivity) getActivity()).moveFabDown(multiselectMenu.getHeight());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ThemeManager.getColor(R.attr.colorPrimaryDark));
        }
    }

    @Override
    public void onActionClicked(Snackbar snackbar) {
        // TODO implement undo
    }

    @OnClick(R.id.menu_delete)
    void onDelete() {
        final ArrayList<TaskOverview> selected = mAdapter.getSelectedItems();
        // TODO implement deleting
        final int count = selected.size();
        SnackbarManager.show(
            Snackbar.with(getActivity())
            .text(getResources().getQuantityString(R.plurals.task_multiselect_deleted, count, count))
            .actionLabel(R.string.undo)
            .actionListener(this)
            .eventListener((EventListener) getActivity())
            , getActivity());

        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @OnClick(R.id.menu_move_task)
    void onMoveTask() {
        final ArrayList<TaskOverview> tasks = mAdapter.getSelectedItems();
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(de.azapps.mirakel.main_activity.R.string.dialog_move);

        final Optional<AccountMirakel> accountMirakelOptional = tasks.get(0).getAccountMirakel();
        if (accountMirakelOptional.isPresent()) {
            final Cursor cursor = ListMirakel.allCursor(of(accountMirakelOptional.get()), false);
            builder.setCursor(cursor,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog,
                                    final int which) {
                    cursor.moveToPosition(which);
                    final ListMirakel listMirakel = new ListMirakel(cursor);
                    for (final TaskOverview taskOverview : tasks) {
                        taskOverview.withTask(new OptionalUtils.Procedure<Task>() {
                            @Override
                            public void apply(Task task) {
                                task.setList(listMirakel, true);
                                task.save();
                            }
                        });
                    }
                    cursor.close();
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                }
            }, ModelBase.NAME).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cursor.close();
                }
            }).show();
        }
    }

    @OnClick(R.id.menu_set_due)
    void onSetDue() {
        final ArrayList<TaskOverview> tasks = mAdapter.getSelectedItems();
        final Calendar dueLocal = new GregorianCalendar();
        final SupportDatePickerDialog datePickerDialog = SupportDatePickerDialog.newInstance(
        new DatePicker.OnDateSetListener() {
            @Override
            public void onDateSet(final DatePicker dp, final int year,
                                  final int month, final int day) {
                final Calendar due = new GregorianCalendar(year, month,
                        day);
                for (final TaskOverview taskOverview : tasks) {
                    taskOverview.withTask(new OptionalUtils.Procedure<Task>() {
                        @Override
                        public void apply(Task task) {
                            task.setDue(of(due));
                            task.save();
                        }
                    });
                }
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }

            @Override
            public void onNoDateSet() {
                for (final TaskOverview taskOverview : tasks) {

                    taskOverview.withTask(new OptionalUtils.Procedure<Task>() {
                        @Override
                        public void apply(Task task) {
                            task.setDue(Optional.<Calendar>absent());
                            task.save();
                        }
                    });
                }
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }
        }, dueLocal.get(Calendar.YEAR), dueLocal.get(Calendar.MONTH),
        dueLocal.get(Calendar.DAY_OF_MONTH), false,
        MirakelCommonPreferences.isDark(), true);
        datePickerDialog.show(getActivity().getSupportFragmentManager(), "datepicker");
    }

    @OnClick(R.id.menu_set_priority)
    void onSetPriority() {
        Toast.makeText(getActivity(), "Implement set priority", Toast.LENGTH_LONG);
    }

    @OnClick(R.id.menu_set_tags)
    void onSetTag() {
        Toast.makeText(getActivity(), "Implement set tags", Toast.LENGTH_LONG);
    }

    public void resetList() {
        setList(listMirakel);
    }
}
