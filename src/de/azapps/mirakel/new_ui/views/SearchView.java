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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.SoftKeyboard;
import de.azapps.mirakel.model.search.Autocomplete;
import de.azapps.mirakel.new_ui.adapter.AutoCompleteAdapter;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.OptionalUtils;

public class SearchView extends LinearLayout {

    @InjectView(R.id.search_text)
    AutoCompleteTextView searchText;
    private final SoftKeyboard softKeyboard;
    private SearchCallback searchCallback;

    public interface SearchCallback {
        public void performSearch(String searchText);
    }



    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_search, this);
        ButterKnife.inject(this, this);
        softKeyboard = new SoftKeyboard(this,
                                        (InputMethodManager)getContext().getSystemService(Activity.INPUT_METHOD_SERVICE));
        softKeyboard.openSoftKeyboard();
        AutoCompleteAdapter adapter = new AutoCompleteAdapter(context, Autocomplete.autocomplete(context,
                ""));
        searchText.setAdapter(adapter);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    searchButtonClick();
                    return true;
                }
                return false;
            }
        });
    }

    public void setSearchCallback(SearchCallback searchCallback) {
        this.searchCallback = searchCallback;
    }

    @OnClick(R.id.search_button)
    public void searchButtonClick() {
        if (searchCallback != null) {
            softKeyboard.closeSoftKeyboard();
            final String text = searchText.getText().toString();
            searchCallback.performSearch(text);
        }
    }


}
