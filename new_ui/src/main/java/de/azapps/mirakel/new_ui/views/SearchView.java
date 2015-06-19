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

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.azapps.material_elements.utils.SoftKeyboard;
import de.azapps.mirakel.new_ui.adapter.AutoCompleteAdapter;
import de.azapps.mirakel.new_ui.search.SearchObject;
import de.azapps.mirakelandroid.R;

public class SearchView extends LinearLayout {

    @InjectView(R.id.search_text)
    AutoCompleteTextView searchText;
    @InjectView(R.id.search_clear)
    Button searchClearButton;
    private final SoftKeyboard softKeyboard;
    private SearchCallback searchCallback;
    @Nullable
    private SearchObject lastSearch = null;
    private AutoCompleteAdapter adapter;

    public void showKeyboard() {
        searchText.clearFocus();
        searchText.requestFocus();
    }

    public interface SearchCallback {
        public void performSearch(SearchObject searchObject);
    }


    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_search, this);
        ButterKnife.inject(this, this);
        softKeyboard = new SoftKeyboard(this);
        searchText.requestFocus();
        adapter = new AutoCompleteAdapter(context,
                                          SearchObject.autocomplete(context,
                                                  ""));
        searchText.setAdapter(adapter);

        searchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final Cursor cursor = (Cursor) adapter.getItem(i);
                lastSearch = new SearchObject(cursor);
                searchButtonClick();
            }
        });
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // Clear search view when the last search was a tag
                if (lastSearch != null && lastSearch.getAutocompleteType() == SearchObject.AUTOCOMPLETE_TYPE.TAG) {
                    lastSearch = null;
                    clearText();
                    return;
                }
                lastSearch = null;
                if (charSequence.length() == 0) {
                    searchClearButton.setVisibility(GONE);
                } else {
                    searchClearButton.setVisibility(VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchButtonClick();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (adapter != null) {
            adapter.changeCursor(null);
        }
    }

    public void setSearchCallback(SearchCallback searchCallback) {
        this.searchCallback = searchCallback;
    }

    public void setSearch(final SearchObject searchObject) {
        // This is quite dirty. When you set the lastSearch to a tag it will be nullified by the TextWatcher. So let's clear it and set it afterwards
        lastSearch = null;
        searchClearButton.setVisibility(VISIBLE);
        this.searchText.setText(searchObject.getText(getContext()));
        lastSearch = searchObject;
    }

    @OnClick(R.id.search_button)
    public void searchButtonClick() {
        Cursor cursor = adapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        if (searchCallback != null) {
            if (lastSearch != null) {
                searchCallback.performSearch(lastSearch);
            } else {
                final String text = searchText.getText().toString();
                searchCallback.performSearch(new SearchObject(text));
            }
        }
    }

    @OnClick(R.id.search_clear)
    public void clearText() {
        searchClearButton.setVisibility(GONE);
        searchText.setText("");
    }


}
