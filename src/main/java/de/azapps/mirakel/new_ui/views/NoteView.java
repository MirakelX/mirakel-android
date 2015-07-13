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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;

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
    private Dialog dialog;


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

    public void setNote(final String note) {
        this.note = note;
        rebuildLayout();
    }

    public void setOnNoteChangedListener(final OptionalUtils.Procedure<String> noteChangedListener) {
        this.noteChangedListener = noteChangedListener;
        rebuildLayout();
    }

    private Dialog createDialog() {
        final View view = inflate(getContext(), R.layout.view_note_edit, null);
        final EditText editText = (EditText) view.findViewById(R.id.note_edit_text);
        editText.setText(note);
        editText.setSelection(note.length());
        final Dialog d = new AlertDialogWrapper.Builder(getContext()).setTitle(getContext().getString(
                    R.string.edit_note))
        .setView(view)
        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String newNote = editText.getText().toString();
                noteText.setText(newNote);
                noteChangedListener.apply(newNote);
                NoteView.this.dialog = null;
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                NoteView.this.dialog = null;
            }
        }).create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                editText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editText.requestFocus();
                    }
                }, 10L);

            }
        });
        return d;
    }

    @OnClick({R.id.task_note_text, R.id.task_note_title})
    public void handleEditNote() {
        dialog = createDialog();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private static final String PARENT_STATE = "parent";
    private static final String DIALOG_STATE = "dialog";
    private static final String DIALOG_SHOWING = "is_showing";
    private static final String NOTE = "note";

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle out = new Bundle();
        out.putParcelable(PARENT_STATE, super.onSaveInstanceState());
        if (dialog != null) {
            out.putParcelable(DIALOG_STATE, dialog.onSaveInstanceState());
            out.putBoolean(DIALOG_SHOWING, dialog.isShowing());
        }
        out.putString(NOTE, note);
        return out;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof Bundle)) {
            return;
        }
        Bundle saved = (Bundle) state;
        super.onRestoreInstanceState(saved.getParcelable(PARENT_STATE));
        note = ((Bundle) state).getString(NOTE);
        dialog = createDialog();
        if (saved.getParcelable(DIALOG_STATE) != null) {
            dialog.onRestoreInstanceState((Bundle) saved.getParcelable(DIALOG_STATE));
        }
        if (saved.getBoolean(DIALOG_SHOWING, false) && !dialog.isShowing()) {
            NoteView.this.dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
            dialog.show();
        }
    }
}
