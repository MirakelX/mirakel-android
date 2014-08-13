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

package de.azapps.mirakel.new_ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;

import java.util.Set;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.adapter.MultiSelectModelAdapter;

public class AddSubtaskFragment extends DialogFragment implements LoaderManager.LoaderCallbacks {
    public static final String ARGUMENT_PARENT_TASK = "PARENT_TASK";
    private Button newTaskButton;
    private Button selectTaskButton;
    private ViewSwitcher switcher;
    private EditText taskNameEditText;
    private EditText searchBox;
    private ListView taskListView;

    private Task task;
    private View layout;
    private boolean isInNewTask = true;
    private MultiSelectModelAdapter<Task> mAdapter;

    @NonNull
    private String searchString = "";


    public AddSubtaskFragment() {
    }

    public static AddSubtaskFragment newInstance(Task task) {
        AddSubtaskFragment f = new AddSubtaskFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_PARENT_TASK, task);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle);
        Bundle arguments = getArguments();
        task = arguments.getParcelable(ARGUMENT_PARENT_TASK);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = initView();
        return new AlertDialog.Builder(getActivity())
               .setTitle(R.string.add_subtask)
               .setView(view)
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

    public View initView() {
        // Inflate
        layout = getActivity().getLayoutInflater().inflate(R.layout.fragment_add_subtask, null);
        newTaskButton = (Button) layout.findViewById(R.id.subtask_new_task);
        selectTaskButton = (Button) layout.findViewById(R.id.subtask_select_old);
        switcher = (ViewSwitcher) layout.findViewById(R.id.subtask_switcher);
        taskNameEditText = (EditText) layout.findViewById(R.id.subtask_add_task_edit);
        searchBox = (EditText) layout.findViewById(R.id.subtask_searchbox);
        taskListView = (ListView) layout.findViewById(R.id.subtask_listview);
        // Set Actions
        newTaskButton.setOnClickListener(newTaskButtonClick);
        selectTaskButton.setOnClickListener(selectTaskButtonClick);
        taskNameEditText.setOnEditorActionListener(taskNameEditTextActionListener);
        searchBox.addTextChangedListener(searchBoxListener);
        // Adapter
        mAdapter = new MultiSelectModelAdapter<Task>(getActivity(), null, 0, Task.class);
        taskListView.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
        return layout;
    }

    @Override
    public Loader onCreateLoader(int i, Bundle arguments) {
        MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(getActivity());
        Task.addBasicFiler(mirakelQueryBuilder);
        mirakelQueryBuilder.and(Task.DONE, MirakelQueryBuilder.Operation.EQ, false);
        if (searchString.length() > 0) {
            mirakelQueryBuilder.and(Task.NAME, MirakelQueryBuilder.Operation.LIKE, "%" + searchString + "%");
        }
        mirakelQueryBuilder.and(ModelBase.ID, MirakelQueryBuilder.Operation.NOT_IN, task.getSubtasks());
        mirakelQueryBuilder.and(ModelBase.ID, MirakelQueryBuilder.Operation.NOT_EQ, task);
        return mirakelQueryBuilder.toCursorLoader(Task.URI);
    }

    @Override
    public void onLoadFinished(Loader loader, Object o) {
        mAdapter.swapCursor((Cursor) o);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

    // Actions

    private View.OnClickListener newTaskButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isInNewTask) {
                return;
            } else {
                newTaskButton.setTextColor(getResources().getColor(R.color.Black));
                selectTaskButton.setTextColor(getResources().getColor(R.color.Grey));
                isInNewTask = true;
                switcher.showNext();
            }
        }
    };
    private View.OnClickListener selectTaskButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!isInNewTask) {
                return;
            } else {
                newTaskButton.setTextColor(getResources().getColor(R.color.Grey));
                selectTaskButton.setTextColor(getResources().getColor(R.color.Black));
                isInNewTask = false;
                switcher.showNext();
            }
        }
    };

    private void createSubtask(String name) {
        if (name.length() > 0) {
            final ListMirakel list = MirakelModelPreferences
                                     .getListForSubtask(task);
            Task newTask = Semantic.createTask(name, Optional.fromNullable(list),
                                               MirakelCommonPreferences.useSemanticNewTask(), getActivity());
            task.addSubtask(newTask);
        }
    }

    private TextView.OnEditorActionListener taskNameEditTextActionListener = new
    TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                createSubtask(textView.getText().toString());
                dismiss();
                return true;
            }
            return false;
        }
    };


    private TextWatcher searchBoxListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // Do nothing
        }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // Do nothing
        }
        @Override
        public void afterTextChanged(Editable editable) {
            searchString = searchBox.getText().toString();
            getLoaderManager().restartLoader(0, null, AddSubtaskFragment.this);
        }
    };

    private final DialogInterface.OnClickListener onPositiveButtonClickListener = new
    DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
            if (isInNewTask) {
                createSubtask(taskNameEditText.getText().toString());
            } else {
                Set<Task> selectedItemsIds = mAdapter.getSelectedItems();
                for (Task t : selectedItemsIds) {
                    task.addSubtask(t);
                }
            }
            dismiss();
        }
    };

}
