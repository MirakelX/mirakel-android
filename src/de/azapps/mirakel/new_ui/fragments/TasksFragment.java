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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.Gravity;
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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import de.azapps.material_elements.utils.AnimationHelper;
import de.azapps.material_elements.utils.SnackBarEventListener;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.views.FloatingActionButton;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskOverview;
import de.azapps.mirakel.new_ui.activities.MirakelActivity;
import de.azapps.mirakel.new_ui.adapter.TaskAdapter;
import de.azapps.mirakel.new_ui.search.SearchListMirakel;
import de.azapps.mirakel.new_ui.search.SearchObject;
import de.azapps.mirakel.new_ui.views.SearchView;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class TasksFragment extends Fragment implements LoaderManager.LoaderCallbacks,
    MultiSelectCursorAdapter.MultiSelectCallbacks<TaskOverview>,
    TaskAdapter.TaskAdapterCallbacks {

    public static final String ARGUMENT_LIST = "list";
    private static final String TAG = "de.azapps.mirakel.new_ui.fragments.TasksFragment";
    public static final String TASKS_TO_DELETE = "tasks to delete";
    public static final String SHOULD_SHOW_DONE_TASKS = "should show done tasks";

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
    @Nullable
    private SearchObject lastSearch;

    private Optional<Snackbar> snackbar = absent();

    private boolean shouldShowDoneTasks = false;

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
        mAdapter = new TaskAdapter(getActivity(), null, mListener, this, this);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }


    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnItemClickedListener<TaskOverview>) activity;
        } catch (final ClassCastException ignored) {
            throw new ClassCastException(activity.toString() +
                                         " must implement OnItemClickedListener<TaskOverview>");
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
                          false);
        mListener.onItemSelected(new TaskOverview(task));
    }

    public void setList(final ListMirakelInterface listMirakel) {
        setList(listMirakel, false);
    }
    public void setList(final ListMirakelInterface listMirakel, final boolean shouldShowDoneTasks) {
        if (snackbar.isPresent()) {
            snackbar.get().dismiss();
        }
        this.listMirakel = listMirakel;
        final Bundle args = new Bundle();
        ((MirakelActivity) getActivity()).updateToolbar(MirakelActivity.ActionBarState.NORMAL);
        if (listMirakel instanceof SearchListMirakel) {
            lastSearch = ((SearchListMirakel) listMirakel).getSearch();
        } else {
            lastSearch = null;
        }
        args.putParcelable(ARGUMENT_LIST, listMirakel);
        args.putBoolean(SHOULD_SHOW_DONE_TASKS, shouldShowDoneTasks);
        getLoaderManager().restartLoader(0, args, this);
    }

    @Override
    public Loader onCreateLoader(final int i, final Bundle arguments) {
        MirakelQueryBuilder mirakelQueryBuilder = listMirakel.getTasksQueryBuilder();
        listMirakel = arguments.getParcelable(ARGUMENT_LIST);
        shouldShowDoneTasks = arguments.getBoolean(SHOULD_SHOW_DONE_TASKS);
        ArrayList<TaskOverview> tasksToDelete = arguments.getParcelableArrayList(TASKS_TO_DELETE);
        if (tasksToDelete != null) {

            final List<Long> ids = new ArrayList<>(Collections2.transform(tasksToDelete,
            new Function<TaskOverview, Long>() {
                @Override
                public Long apply(@Nullable final TaskOverview input) {
                    if (input != null) {
                        return input.getId();
                    } else {
                        return 0L;
                    }
                }
            }));
            mirakelQueryBuilder.and(Task.ID, MirakelQueryBuilder.Operation.NOT_IN, ids);
        }
        if (!shouldShowDoneTasks) {
            mirakelQueryBuilder.and(Task.DONE, MirakelQueryBuilder.Operation.EQ, false);
        }
        return mirakelQueryBuilder.toSupportCursorLoader(Task.URI);
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
            mActionMode = getActivity().startActionMode(new MultiSelectCallbacks());
        } else {
            if (mActionMode == null) {
                throw new IllegalArgumentException("ActionMode is null this must not happen");
            }
            mActionMode.finish();
        }
    }

    @Override
    public boolean canAddItem(@NonNull final TaskOverview item) {
        return true;
    }


    public void onSelectedItemCountChanged(final int itemCount) {
        if (mActionMode == null) {
            throw new IllegalArgumentException("ActionMode is null this must not happen");
        }
        if (itemCount == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(getResources().getQuantityString(R.plurals.task_multiselect_title,
                                 itemCount, itemCount));
        }
    }

    private void showOrHideMoveTasks() {
        final List<TaskOverview> tasks = mAdapter.getSelectedItems();
        if (tasks.isEmpty()) {
            // Do not need to change anything
            return;
        }
        final Optional<AccountMirakel> accountMirakelOptional = tasks.get(0).getAccountMirakel();
        final boolean shouldHide;
        if (accountMirakelOptional.isPresent()) {
            final long accountMirakelId = accountMirakelOptional.get().getId();
            shouldHide = Iterables.all(tasks, new Predicate<TaskOverview>() {
                @Override
                public boolean apply(@Nullable TaskOverview input) {
                    final Optional<AccountMirakel> compareAccount;
                    if (input != null) {
                        compareAccount = input.getAccountMirakel();
                    } else {
                        compareAccount = absent();
                    }
                    return compareAccount.isPresent() && (compareAccount.get().getId() == accountMirakelId);
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
    public void onAddSelectedItem(@NonNull final TaskOverview item) {
        if (mActionMode == null) {
            throw new IllegalArgumentException("ActionMode is null this must not happen");
        }
        onSelectedItemCountChanged(mAdapter.getSelectedItemCount());
        showOrHideMoveTasks();
    }

    @Override
    public void onRemoveSelectedItem(@NonNull final TaskOverview item) {
        if (mActionMode == null) {
            throw new IllegalArgumentException("ActionMode is null this must not happen");
        }

        onSelectedItemCountChanged(mAdapter.getSelectedItemCount());
        showOrHideMoveTasks();
    }

    @OnLongClick({R.id.menu_delete, R.id.menu_move_task, R.id.menu_set_due, R.id.menu_set_priority, R.id.menu_set_tags})
    public boolean onLongClick(View v) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);

        final int width = v.getWidth();
        final int height = v.getHeight();
        final int midy = screenPos[1] + height / 2;
        int referenceX = screenPos[0] + width / 2;
        if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            final int screenWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
            referenceX = screenWidth - referenceX; // mirror
        }
        Toast cheatSheet = Toast.makeText(getActivity(), v.getContentDescription(), Toast.LENGTH_SHORT);
        final int gravityY;
        if (midy < displayFrame.height()) {
            gravityY = Gravity.TOP;
        } else {
            gravityY = Gravity.BOTTOM;
        }

        cheatSheet.setGravity(gravityY | GravityCompat.END, referenceX, height);
        cheatSheet.show();
        return true;
    }


    @OnClick(R.id.menu_delete)
    void onDelete() {
        final ArrayList<TaskOverview> tasksToDelete = mAdapter.getSelectedItems();
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_LIST, listMirakel);
        arguments.putParcelableArrayList(TASKS_TO_DELETE, tasksToDelete);
        getLoaderManager().restartLoader(0, arguments, this);
        final Snackbar snackbar = Snackbar.with(getActivity())
                                  .text(getResources().getQuantityString(R.plurals.task_multiselect_deleted, tasksToDelete.size(),
                                          tasksToDelete.size()))
                                  .actionLabel(R.string.undo)
        .actionListener(new ActionClickListener() {
            @Override
            public void onActionClicked(final Snackbar snackbar) {
                tasksToDelete.clear();
            }
        })
        .eventListener(new SnackBarEventListener() {
            @Override
            public void onDismiss(final Snackbar snackbar) {
                super.onDismiss(snackbar);
                ((MirakelActivity) getActivity()).moveFabDown(snackbar.getHeight());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (final TaskOverview t : tasksToDelete) {
                            t.destroy();
                        }
                        mListView.post(new Runnable() {
                            @Override
                            public void run() {
                                setList(listMirakel);

                            }
                        });
                        if (TasksFragment.this.snackbar.isPresent()) {
                            TasksFragment.this.snackbar = absent();
                        }
                    }
                }).start();
            }

            @Override
            public void onShow(final Snackbar snackbar) {
                ((MirakelActivity) getActivity()).moveFABUp(snackbar.getHeight());
            }
        });
        TasksFragment.this.snackbar = of(snackbar);
        SnackbarManager.show(
            snackbar
            , getActivity());

        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @OnClick(R.id.menu_move_task)
    void onMoveTask() {
        final List<TaskOverview> tasks = mAdapter.getSelectedItems();
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_move);

        final Optional<AccountMirakel> accountMirakelOptional = tasks.get(0).getAccountMirakel();
        if (accountMirakelOptional.isPresent()) {
            final Cursor cursor = ListMirakel.allCursor(of(accountMirakelOptional.get()), false);
            builder.setCursor(cursor,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog,
                                    final int which) {
                    cursor.moveToPosition(which);
                    final ListMirakel list = new ListMirakel(cursor);
                    for (final TaskOverview taskOverview : tasks) {
                        taskOverview.withTask(new OptionalUtils.Procedure<Task>() {
                            @Override
                            public void apply(Task task) {
                                task.setList(list, true);
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
        final List<TaskOverview> tasks = mAdapter.getSelectedItems();
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
        dueLocal.get(Calendar.DAY_OF_MONTH), false, true);
        datePickerDialog.show(getActivity().getSupportFragmentManager(), "datepicker");
    }

    @OnClick(R.id.menu_set_priority)
    void onSetPriority() {
        Toast.makeText(getActivity(), "Implement set priority", Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.menu_set_tags)
    void onSetTag() {
        Toast.makeText(getActivity(), "Implement set tags", Toast.LENGTH_LONG).show();
    }

    public void resetList() {
        setList(listMirakel);
    }

    public void handleShowSearch() {
        getActivity().startActionMode(new SearchCallbacks());
    }


    @Override
    public void toggleShowDoneTasks() {
        setList(listMirakel, !shouldShowDoneTasks);
    }

    @Override
    public boolean shouldShowDone() {
        return shouldShowDoneTasks;
    }

    @Override
    public boolean shouldShowDoneToggle() {
        return listMirakel.shouldShowDoneToggle() &&
               (!(listMirakel instanceof ListMirakel) ||
                (new MirakelQueryBuilder(getActivity()).and(Task.LIST_ID,
                        MirakelQueryBuilder.Operation.EQ,
                        (ListMirakel) listMirakel).and(Task.DONE, MirakelQueryBuilder.Operation.EQ,
                                true).count(Task.URI) > 0L));
    }

    private class MultiSelectCallbacks implements ActionMode.Callback {
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
    }

    private class SearchCallbacks implements ActionMode.Callback, SearchView.SearchCallback {
        private ActionMode privateActionMode;
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            privateActionMode = actionMode;
            final SearchView searchView = new SearchView(getActivity());
            searchView.setSearchCallback(this);
            if (lastSearch != null) {
                searchView.setSearch(lastSearch);
            }
            actionMode.setCustomView(searchView);
            searchView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchView.showKeyboard();
                }
            }, 10L);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().getWindow().setStatusBarColor(ThemeManager.getColor(R.attr.colorCABStatus));
            }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().getWindow().setStatusBarColor(ThemeManager.getColor(R.attr.colorPrimaryDark));
            }
        }

        @Override
        public void performSearch(SearchObject searchText) {
            if (privateActionMode == null) {
                throw new IllegalArgumentException("ActionMode is null this must not happen");
            }
            privateActionMode.finish();
            final ListMirakelInterface listMirakelInterface = new SearchListMirakel(getActivity(), searchText);
            setList(listMirakelInterface);
        }
    }
}
