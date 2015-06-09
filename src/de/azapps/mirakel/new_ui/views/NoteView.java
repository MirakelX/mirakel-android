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

package de.azapps.mirakel.new_ui.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.OptionalUtils;

public class NoteView extends LinearLayout {
    private String note;

    @InjectView(R.id.task_note_text)
    TextView noteText;
    private OptionalUtils.Procedure<String> noteChangedListener;


    public NoteView(final Context context) {
        this(context, null);
    }

    public NoteView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NoteView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_note, this);
        ButterKnife.inject(this, this);
    }

    private void rebuildLayout() {
        noteText.setText(note);
        Linkify.addLinks(noteText, Linkify.ALL);
        invalidate();
        requestLayout();
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
        rebuildLayout();
    }

    public void setOnNoteChangedListener(final OptionalUtils.Procedure<String> noteChangedListener) {
        this.noteChangedListener = noteChangedListener;
        rebuildLayout();
    }

    @OnClick({R.id.task_note_text, R.id.task_note_title})
    public void handleEditNote() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final EditText editText = new EditText(getContext());
        editText.setText(note);
        builder.setTitle(getContext().getString(R.string.edit_note))
        .setView(editText)
        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String newNote = editText.getText().toString();
                noteText.setText(newNote);
                noteChangedListener.apply(newNote);
            }
        })
        .setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
}
