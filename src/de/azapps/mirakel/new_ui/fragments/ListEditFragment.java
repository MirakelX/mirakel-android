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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.mirakel.adapter.SimpleModelListAdapter;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

/**
 * Created by az on 12/21/14.
 */
public class ListEditFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {
    private static final String ARGUMENT_LIST = "ARGUMENT_LIST";
    @NonNull
    private ListMirakel listMirakel;

    @InjectView(R.id.list_edit_name)
    EditText listEditName;
    @InjectView(R.id.list_edit_account)
    Spinner listEditAccount;
    @InjectView(R.id.list_edit_account_text)
    TextView listEditAccountText;
    /**
     * The onItemSelected is fired when creating the spinner.
     * This is absolutely not what we want to do
     */
    private boolean initSelection = false;

    public ListEditFragment() {
        // Required empty public constructor
    }


    /**
     * Currently it is only possible to edit exactly one list
     *
     * @param listMirakel
     * @return
     */
    public static ListEditFragment newInstance(@NonNull final ListMirakel listMirakel) {
        final ListEditFragment listEditFragment = new ListEditFragment();
        // Supply num input as an argument.
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_LIST, listMirakel);
        listEditFragment.setArguments(args);
        return listEditFragment;
    }


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        listMirakel = arguments.getParcelable(ARGUMENT_LIST);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_AppCompat_Light_Dialog);
        // Inflate the layout for this fragment
        final View layout = inflater.inflate(R.layout.fragment_list_edit, container, false);
        ButterKnife.inject(this, layout);

        if (getDialog() != null) {
            getDialog().setTitle(getString(R.string.list_edit_title, listMirakel));
        }

        listEditName.setText(listMirakel.getName());
        // You can only move new lists
        if (listMirakel.isStub() && AccountMirakel.countMovableTo() > 1) {
            final SimpleModelListAdapter<AccountMirakel> adapter = new SimpleModelListAdapter<>(getActivity(),
                    AccountMirakel.allMovableToCursor(), 0, AccountMirakel.class);
            listEditAccount.setAdapter(adapter);
            listEditAccount.setOnItemSelectedListener(this);
            listEditAccount.setVisibility(View.VISIBLE);
            listEditAccountText.setVisibility(View.VISIBLE);
        } else {
            listEditAccount.setVisibility(View.GONE);
            listEditAccountText.setVisibility(View.GONE);
        }
        return layout;
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
                               final long id) {
        if (!initSelection) {
            initSelection = true;
            return;
        }
        final AccountMirakel accountMirakel = (AccountMirakel) listEditAccount.getAdapter().getItem(
                position);
        listMirakel.setAccount(accountMirakel);
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {

    }

    @OnClick(R.id.list_edit_ok)
    public void saveAndDismiss() {
        final String name = listEditName.getText().toString();
        listMirakel.setName(name);
        if (listMirakel.isStub()) {
            ListMirakel.safeNewList(name, listMirakel.getAccount());
        } else {
            listMirakel.save();
        }
        getDialog().dismiss();
    }
}
