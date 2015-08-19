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

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.common.base.Optional;

import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.adapter.MultiSelectModelAdapter;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class AddSubtaskFragment extends DialogFragment implements LoaderManager.LoaderCallbacks {
    public static final String ARGUMENT_PARENT_TASK = "PARENT_TASK";
    @InjectView(R.id.subtask_new_task)
    Button newTaskButton;
    @InjectView(R.id.subtask_select_old)
    Button selectTaskButton;
    @InjectView(R.id.subtask_switcher)
    ViewSwitcher switcher;
    @InjectView(R.id.subtask_add_task_edit)
    EditText taskNameEditText;
    @InjectView(R.id.subtask_searchbox)
    EditText searchBox;
    @InjectView(R.id.subtask_listview)
    ListView taskListView;

    private Task task;
    private boolean isInNewTask = true;
    private MultiSelectModelAdapter<Task> mAdapter;

    @NonNull
    private String searchString = "";


    public AddSubtaskFragment() {
    }

    public static AddSubtaskFragment newInstance(final Task task) {
        final AddSubtaskFragment fragment = new AddSubtaskFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_PARENT_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle);
        final Bundle arguments = getArguments();
        task = arguments.getParcelable(ARGUMENT_PARENT_TASK);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final View layout = getActivity().getLayoutInflater().inflate(R.layout.fragment_add_subtask, null);
        ButterKnife.inject(this, layout);
        // Adapter
        mAdapter = new MultiSelectModelAdapter<>(getActivity(), null, 0, Task.class);
        taskListView.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);

        return new AlertDialogWrapper.Builder(getActivity())
               .setTitle(R.string.add_subtask)
               .setView(layout)
               .setPositiveButton(R.string.add, onPositiveButtonClickListener)
               .setNegativeButton(android.R.string.cancel,
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismiss();
            }
        }
                                 )
        .create();
    }

    @Override
    public Loader onCreateLoader(final int i, final Bundle arguments) {
        final MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(getActivity());
        Task.addBasicFiler(mirakelQueryBuilder);
        mirakelQueryBuilder.and(Task.DONE, MirakelQueryBuilder.Operation.EQ, false);
        if (!searchString.isEmpty()) {
            mirakelQueryBuilder.and(Task.NAME, MirakelQueryBuilder.Operation.LIKE, '%' + searchString + '%');
        }
        mirakelQueryBuilder.and(ModelBase.ID, MirakelQueryBuilder.Operation.NOT_IN, task.getSubtasks());
        mirakelQueryBuilder.and(ModelBase.ID, MirakelQueryBuilder.Operation.NOT_EQ, task);
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

    // Actions

    @OnClick(R.id.subtask_new_task)
    void clickCreateTask() {
        if (!isInNewTask) {
            newTaskButton.setTextColor(ThemeManager.getColor(R.attr.colorTextBlack));
            selectTaskButton.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
            isInNewTask = true;
            switcher.showNext();
        }
    }

    @OnClick(R.id.subtask_select_old)
    void clickSelectOld() {
        if (isInNewTask) {
            newTaskButton.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
            selectTaskButton.setTextColor(ThemeManager.getColor(R.attr.colorTextBlack));
            isInNewTask = false;
            switcher.showNext();
        }
    }

    private void createSubtask(final String name) {
        if (!name.isEmpty()) {
            final ListMirakel list = MirakelModelPreferences
                                     .getListForSubtask(task);
            final Task newTask = Semantic.createTask(name, Optional.fromNullable(list),
                                 true);
            task.addSubtask(newTask);
        }
    }

    @OnEditorAction(R.id.subtask_add_task_edit)
    boolean onTaskNameEdit(final int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            createSubtask(taskNameEditText.getText().toString());
            dismiss();
            return true;
        }
        return false;
    }


    @OnTextChanged(value = R.id.subtask_searchbox, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void search() {
        searchString = searchBox.getText().toString();
        getLoaderManager().restartLoader(0, null, AddSubtaskFragment.this);
    }

    private final DialogInterface.OnClickListener onPositiveButtonClickListener = new
    DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int whichButton) {
            if (isInNewTask) {
                createSubtask(taskNameEditText.getText().toString());
            } else {
                final Set<Task> selectedItemsIds = mAdapter.getSelectedItems();
                for (final Task t : selectedItemsIds) {
                    task.addSubtask(t);
                }
            }
            dismiss();
        }
    };

}
