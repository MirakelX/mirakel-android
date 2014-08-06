package de.azapps.mirakel.new_ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.views.DatesView;
import de.azapps.mirakel.new_ui.views.NoteView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.ProgressView;

import static com.google.common.base.Optional.fromNullable;

public class TaskFragment extends DialogFragment {

	View layout;
	Task task;
	ProgressDoneView progressDoneView;
	ProgressView progressView;

	// TaskName
	TextView taskName;
	EditText taskNameEdit;
	ViewSwitcher taskNameViewSwitcher;


	NoteView noteView;
	DatesView datesView;


	public TaskFragment() {
	}

	public static TaskFragment newInstance(long task_id) {
		TaskFragment f = new TaskFragment();

		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putLong("task_id", task_id);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		Bundle arguments = getArguments();
		long task_id = arguments.getLong("task_id");
		task = Task.get(task_id);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		layout = inflater.inflate(R.layout.fragment_task, container, false);
		progressDoneView = (ProgressDoneView) layout.findViewById(R.id.task_progress_done);

		taskName = (TextView) layout.findViewById(R.id.task_name);
		taskNameEdit = (EditText) layout.findViewById(R.id.task_name_edit);
		taskNameViewSwitcher = (ViewSwitcher) layout.findViewById(R.id.task_name_view_switcher);

		progressView = (ProgressView) layout.findViewById(R.id.task_progress);
		noteView = (NoteView) layout.findViewById(R.id.task_note);
		datesView = (DatesView) layout.findViewById(R.id.task_dates);

		progressDoneView.setProgress(task.getProgress());
		progressDoneView.setDone(task.isDone());
		progressDoneView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				task.setDone(isChecked);
				task.save();
			}
		});

		taskName.setText(task.getName());
		taskName.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editName();
			}
		});

		progressView.setProgress(task.getProgress());

		noteView.setNote(task.getContent());

		datesView.setData(task);
		return layout;
	}

	private void editName() {
		taskNameViewSwitcher.showNext();
		taskNameEdit.setText(task.getName());
		taskNameEdit.requestFocus();
		taskNameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				switch(actionId) {
					case EditorInfo.IME_ACTION_DONE:
					case EditorInfo.IME_ACTION_SEND:
						updateName();
						return true;
				}
				return false;
			}
		});
		taskNameEdit.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
						(keyCode == KeyEvent.KEYCODE_ENTER)) {
					updateName();
					return true;
				}
				return false;
			}
		});
	}
	private void updateName() {
		task.setName(taskNameEdit.getText().toString());
		taskName.setText(task.getName());
		task.save();
		taskNameViewSwitcher.showNext();
	}

}
