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
package de.azapps.material_elements.utils;



import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class SoftKeyboard extends ResultReceiver {
    private static final String TAG = "SoftKeyboard";

    private final ViewGroup layout;
    private boolean isKeyboardShown = false;
    @NonNull
    private List<EditText> editTextList = new ArrayList<>();
    @Nullable
    private EditText currentEditText;
    @Nullable
    private SoftKeyboardChanged callback;


    public SoftKeyboard(final ViewGroup layout) {
        super(null);
        this.layout = layout;
        keyboardHideByDefault();
        initEditTexts(layout);
    }

    @Override
    protected void onReceiveResult(final int resultCode, final Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        if ((resultCode != 0) && (currentEditText != null)) {
            currentEditText.requestFocus();
        }
    }

    private void openSoftKeyboard(final @NonNull EditText v) {
        if (!isKeyboardShown) {
            final InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(
                                               Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, InputMethodManager.SHOW_FORCED, this);
            currentEditText = v;
            isKeyboardShown = true;
            if (callback != null) {
                callback.onSoftKeyboardShow();
            }
        }
    }

    private void closeSoftKeyboard() {
        if (isKeyboardShown) {
            if (currentEditText != null) {
                final InputMethodManager imm = (InputMethodManager) currentEditText.getContext().getSystemService(
                                                   Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentEditText.getWindowToken(), 0);
            }
            currentEditText = null;
            isKeyboardShown = false;
            if (callback != null) {
                callback.onSoftKeyboardHide();
            }
        }
    }

    public void setSoftKeyboardCallback(final SoftKeyboardChanged mCallback) {
        callback = mCallback;
    }


    public boolean isShown() {
        return isKeyboardShown;
    }

    public interface SoftKeyboardChanged {
        public void onSoftKeyboardHide();
        public void onSoftKeyboardShow();
    }


    private void keyboardHideByDefault() {
        layout.setFocusable(true);
        layout.setFocusableInTouchMode(true);
    }

    private void initEditTexts(final ViewGroup viewgroup) {

        final int childCount = viewgroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View v = viewgroup.getChildAt(i);

            if (v instanceof ViewGroup) {
                initEditTexts((ViewGroup) v);
            }

            if (v instanceof EditText) {
                handleAddEdittext((EditText) v);
            }
        }
    }

    private void handleAddEdittext(final EditText v) {
        final View.OnFocusChangeListener onFocusChange = v.getOnFocusChangeListener();
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                onFocusChangeImpl((EditText) v, hasFocus);
                if (onFocusChange != null) {
                    onFocusChange.onFocusChange(v, hasFocus);
                }
            }
        });
        v.setCursorVisible(true);
        editTextList.add(v);
    }

    public void onFocusChangeImpl(final EditText v, final boolean hasFocus) {
        if (hasFocus) {
            if (!v.equals(currentEditText)) {
                if (currentEditText != null) {
                    currentEditText.clearFocus();
                }
                v.requestFocus();
                v.setSelection(v.getText().length());
                currentEditText = v;
            }
            if (!isKeyboardShown) {
                openSoftKeyboard(v);
            }
        } else {
            if ((v.equals(currentEditText) && isKeyboardShown)) {
                closeSoftKeyboard();
            } else if (!v.equals(currentEditText) && isKeyboardShown) {
                v.setFocusable(true);
                v.requestFocus();
            } else {
                v.setFocusable(true);
                v.requestFocus();
                openSoftKeyboard(v);
            }
        }
    }


}