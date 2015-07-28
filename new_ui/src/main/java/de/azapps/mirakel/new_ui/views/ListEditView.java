package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.material_elements.utils.SoftKeyboard;
import de.azapps.mirakel.adapter.SimpleModelListAdapter;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

/**
 * Created by az on 3/14/15.
 */
public class ListEditView extends LinearLayout implements AdapterView.OnItemSelectedListener {
    ListMirakel listMirakel;

    @InjectView(R.id.list_edit_name)
    EditText listEditName;
    @InjectView(R.id.list_edit_account)
    Spinner listEditAccount;
    @InjectView(R.id.list_edit_account_text)
    TextView listEditAccountText;

    SoftKeyboard keyboard;

    /**
     * The onItemSelected is fired when creating the spinner.
     * This is absolutely not what we want to do
     */
    private boolean initSelection = false;

    public ListEditView(final Context context) {
        this(context, null);
    }

    public ListEditView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListEditView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_list_edit, this);
        ButterKnife.inject(this, this);
        keyboard = new SoftKeyboard(this);
    }

    public void setListMirakel(final ListMirakel listMirakel) {
        this.listMirakel = listMirakel;
        rebuildLayout();
    }

    public void saveList() {
        final String name = listEditName.getText().toString();
        listMirakel.setName(name);
        if (listMirakel.isStub()) {
            ListMirakel.safeNewList(name, listMirakel.getAccount());
        } else {
            listMirakel.save();
        }
    }


    private void rebuildLayout() {
        listEditName.setText(listMirakel.getName());
        // You can only move new lists
        if (listMirakel.isStub() && (AccountMirakel.countMovableTo() > 1L)) {
            final SimpleModelListAdapter<AccountMirakel> adapter = new SimpleModelListAdapter<>(getContext(),
                    AccountMirakel.allMovableToCursor().getRawCursor(), 0, AccountMirakel.class);
            listEditAccount.setAdapter(adapter);
            listEditAccount.setOnItemSelectedListener(this);
            listEditAccount.setVisibility(View.VISIBLE);
            listEditAccountText.setVisibility(View.VISIBLE);

        } else {
            listEditAccount.setVisibility(View.GONE);
            listEditAccountText.setVisibility(View.GONE);
        }
    }

    public void openKeyBoard() {
        listEditName.requestFocus();
        if (listMirakel.isStub()) {
            listEditName.selectAll();
        }
    }

    public void closeKeyBoard() {
        listEditName.clearFocus();
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
}
