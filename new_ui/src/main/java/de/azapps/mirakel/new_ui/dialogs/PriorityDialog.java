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

package de.azapps.mirakel.new_ui.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.google.common.base.Optional;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.azapps.mirakelandroid.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class PriorityDialog extends DialogFragment implements View.OnClickListener {

    @Nullable
    private OnPrioritySetListener onSetListener;
    private Optional<Integer> initialPriority = absent();

    public static PriorityDialog newInstance(final @Nullable OnPrioritySetListener listener) {
        PriorityDialog dialog = new PriorityDialog();
        dialog.onSetListener = listener;
        dialog.initialPriority = absent();
        return dialog;
    }

    public static PriorityDialog newInstance(final @IntRange(from = -1, to = 2) int initialValue,
            final @Nullable OnPrioritySetListener listener) {
        PriorityDialog dialog = new PriorityDialog();
        dialog.onSetListener = listener;
        dialog.initialPriority = of(initialValue);
        return dialog;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.menu_set_priority);
        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) inflater.inflate(R.layout.priority_dialog, null);
        ButterKnife.inject(v);
        if (initialPriority.isPresent()) {
            final @IdRes int checkedID;
            switch (initialPriority.get()) {
            case -1:
            case -2:
                checkedID = R.id.wrapper_priority_low;
                break;
            case 1:
                checkedID = R.id.wrapper_priority_high;
                break;
            case 2:
                checkedID = R.id.wrapper_priority_veryhigh;
                break;
            case 0:
            default:
                checkedID = R.id.wrapper_priority_normal;
                break;
            }
            ((RadioButton) v.findViewById(checkedID).findViewById(R.id.radio_priority)).setChecked(true);
        }
        for (int i = 0; i < v.getChildCount(); i++) {
            v.getChildAt(i).setOnClickListener(this);
        }
        return v;
    }

    @Override
    @OnClick({R.id.radio_priority, R.id.wrapper_priority_high, R.id.wrapper_priority_low, R.id.wrapper_priority_normal, R.id.wrapper_priority_veryhigh})
    public void onClick(final View v) {
        if (v.getId() == R.id.radio_priority && v.getParent() instanceof View) {
            onClick((View) v.getParent());
        }
        if (onSetListener != null) {
            switch (v.getId()) {
            case R.id.wrapper_priority_veryhigh:
                onSetListener.setPriority(2);
                break;
            case R.id.wrapper_priority_high:
                onSetListener.setPriority(1);
                break;
            case R.id.wrapper_priority_low:
                onSetListener.setPriority(-1);
                break;
            case R.id.wrapper_priority_normal:
                onSetListener.setPriority(0);
                break;
            default:
                throw new IllegalArgumentException("Unknown priority set");
            }
        }
        if (v.getParent() instanceof LinearLayout) {
            for (int i = 0; i < ((LinearLayout) v.getParent()).getChildCount(); i++) {
                ((RadioButton)((LinearLayout) v.getParent()).getChildAt(i).findViewById(
                     R.id.radio_priority)).setChecked(false);
            }
            ((RadioButton)v.findViewById(R.id.radio_priority)).setChecked(true);
        }
        dismiss();
    }



    public interface OnPrioritySetListener {
        void setPriority(final @IntRange(from = -1, to = 2) int newPriority);
    }
}
