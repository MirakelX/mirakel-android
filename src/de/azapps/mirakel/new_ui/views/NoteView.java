package de.azapps.mirakel.new_ui.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
        noteText.setOnClickListener(onNoteEditListener);
    }

    private void rebuildLayout() {
        noteText.setText(note);
        invalidate();
        requestLayout();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
        rebuildLayout();
    }

    public void setOnNoteChangedListener(OptionalUtils.Procedure<String> noteChangedListener) {
        this.noteChangedListener = noteChangedListener;
        rebuildLayout();
    }

    private final OnClickListener onNoteEditListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            final EditText editText = new EditText(getContext());
            editText.setText(note);
            builder.setTitle(getContext().getString(R.string.edit_note))
            .setView(editText)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newNote = editText.getText().toString();
                    noteText.setText(newNote);
                    noteChangedListener.apply(newNote);
                }
            })
            .setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    };
}
