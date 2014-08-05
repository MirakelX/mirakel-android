package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import de.azapps.mirakel.new_ui.R;
import de.azapps.tools.OptionalUtils;

public class NoteView extends LinearLayout {
	private String note;

	private TextView noteText;
	private EditText noteEdit;
	private ViewSwitcher viewSwitcher;
	private OptionalUtils.Procedure<String> noteChangedListener;


	public NoteView(Context context) {
		this(context, null);
	}

	public NoteView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NoteView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		inflate(context, R.layout.view_note, this);

		noteText = (TextView) findViewById(R.id.task_note_text);
		noteEdit = (EditText) findViewById(R.id.task_note_edit);
		viewSwitcher = (ViewSwitcher) findViewById(R.id.task_note_view_switcher);
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO
				noteEdit.setText(note);
				viewSwitcher.getNextView();
				String newNote=noteEdit.getText().toString();

				// On Success
//				noteChangedListener.apply(newNote);
//				setNote(newNote);
//				viewSwitcher.getNextView();
			}
		});
	}


	@Override
	public void dispatchDraw(Canvas canvas) {
		noteText.setText(note);
		super.dispatchDraw(canvas);
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public void setOnNoteChangedListener(OptionalUtils.Procedure<String> noteChangedListener) {
		this.noteChangedListener = noteChangedListener;
	}
}
