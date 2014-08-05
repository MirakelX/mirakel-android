package de.azapps.mirakel.new_ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.views.NoteView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.ProgressView;

public class TaskFragment extends DialogFragment {

	View layout;
	Task task;
	ProgressDoneView progressDoneView;
	ProgressView progressView;
	TextView taskName;
	NoteView noteView;


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
		progressView = (ProgressView) layout.findViewById(R.id.task_progress);
		//noteView = (NoteView) layout.findViewById(R.id.task_note);

		progressDoneView.setProgress(task.getProgress());
		progressDoneView.setDone(task.isDone());

		taskName.setText(task.getName());

		progressView.setProgress(task.getProgress());

		//noteView.setNote(task.getContent());
		return layout;
	}

}
