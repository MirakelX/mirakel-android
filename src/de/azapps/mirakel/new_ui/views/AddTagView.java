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


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.material_elements.utils.SoftKeyboard;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class AddTagView extends MultiAutoCompleteTextView implements  View.OnClickListener,
    View.OnKeyListener, SoftKeyboard.SoftKeyboardChanged {
    private static final String TAG = "AddTagView";
    private final Drawable background;
    @Nullable
    private SoftKeyboard keyboard;
    private List<Tag> tags = Tag.all();
    private ArrayAdapter<String> adapter;
    private List<Tag> currentTags = new ArrayList<>();
    private boolean clickEnabled = true;


    @Nullable
    private Task task;
    private boolean setText = false;
    private String postfix = "";

    private final float ITEM_HEIGHT = getItemHeight();

    public AddTagView(final Context context) {
        this(context, null);
    }

    public AddTagView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public AddTagView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final List<String> tagNames = new ArrayList<>(tags.size());
        for (final Tag t : tags) {
            tagNames.add(t.getName());
        }
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tagNames);
        setAdapter(adapter);

        background = getBackground();
        background.setColorFilter(ThemeManager.getPrimaryThemeColor(), PorterDuff.Mode.SRC_IN);
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        setMovementMethod(LinkMovementMethod.getInstance());
        setOnKeyListener(this);

        setCustomSelectionActionModeCallback(new ActionMode.Callback() {

            @Override
            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(final ActionMode mode) {
            }

            @Override
            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                return false;
            }
        });
        setOnClickListener(this);
        setEnabled(true);
        setInputType(InputType.TYPE_NULL);
        clearFocus();
    }

    public void setClickEnabled(boolean clickEnabled) {
        this.clickEnabled = clickEnabled;
    }


    private void handleTagEdit(final Tag tag) {
        final View layout = inflate(getContext(),
                                    R.layout.tag_edit_dialog, null);
        final EditText editName = (EditText) layout
                                  .findViewById(R.id.tag_edit_name);
        editName.setText(tag.getName());
        final ColorPicker picker = (ColorPicker) layout
                                   .findViewById(R.id.color_picker);
        final SVBar op = (SVBar) layout.findViewById(R.id.svbar_color_picker);
        picker.addSVBar(op);
        picker.setColor(tag.getBackgroundColor());
        picker.setOldCenterColor(tag.getBackgroundColor());
        new AlertDialog.Builder(getContext())
        .setView(layout)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tag.setBackgroundColor(picker.getColor());
                tag.setName(editName.getText().toString());
                final Optional<Tag> other = Tag.getByName(tag.getName());
                if (!other.isPresent() || (other.get().getId() == tag.getId())) {
                    tag.save();
                } else if (task != null) {
                    task.removeTag(tag);
                    task.addTag(other.get());
                }
                rebuildText();
            }
        }).show();
    }


    public void setTags(final @NonNull Task t) {
        task = t;
        setTags(t.getTags());
    }

    public void setTags(final @NonNull List<Tag> tags) {
        for (final Tag tag : currentTags) {
            adapter.add(tag.getName());
        }
        task = t;
        currentTags = t.getTags();
        for (final Tag tag : currentTags) {
            adapter.remove(tag.getName());
        }
        rebuildText();
    }

    public void setKeyboard(final @NonNull SoftKeyboard keyboard){
        this.keyboard=keyboard;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        keyboard = new SoftKeyboard((ViewGroup) getParent(),
                                    (InputMethodManager)getContext().getSystemService(Activity.INPUT_METHOD_SERVICE));
        keyboard.setSoftKeyboardCallback(this);
        final OnFocusChangeListener onFocus = getOnFocusChangeListener();
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                onFocus.onFocusChange(v, hasFocus);
                if (hasFocus) {
                    if (keyboard != null) {
                        keyboard.openSoftKeyboard();
                    }
                    setBackground(background);
                    setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
                } else {
                    if (keyboard != null) {
                        keyboard.closeSoftKeyboard();
                    }
                    setInputType(InputType.TYPE_NULL);
                    setBackground(new ColorDrawable(Color.TRANSPARENT));
                    addTag(postfix);
                    postfix = "";
                    rebuildText();
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (keyboard != null) {
            keyboard.unRegisterSoftKeyboardCallback();
        }
    }

    private void rebuildText() {
        final SpannableStringBuilder text = new SpannableStringBuilder();
        int pos = 0;
        for (final Tag tag : currentTags) {
            final String name = tag.getName();
            if (!TextUtils.isEmpty(name.trim())) {
                text.append(new SpannableString(name));
                final int textLength = name.length();
                text.setSpan(new TagSpan(tag, getContext()), pos, pos + textLength,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (clickEnabled) {
                            handleTagEdit(tag);
                        }
                    }
                }, pos, pos + textLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos += textLength;
                text.append(',');
                text.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), pos, ++pos,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        text.append(new SpannableString(postfix));
        setText = true;
        setText(text);
        setText = false;
        setSelection(getText().length());
    }


    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore,
                                 final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (setText) {
            return;
        }
        if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(text.toString().trim())) {
            final List<String> tags = Arrays.asList(text.toString().split(","));
            final List<String> currentTags=new ArrayList<>(Collections2.transform(tags, new Function<String, String>() {
                @Nullable
                @Override
                public String apply(@Nullable final String input) {
                    return (input == null) ? null : input.trim();
                }
            }));
            final List<Tag> tagsToRemove=new ArrayList<>(currentTags.size());
            for(final Tag t:this.currentTags){
                if((t != null) && !currentTags.remove(t.getName())){
                    tagsToRemove.add(t);
                }
            }
            if(lengthAfter<lengthBefore) {
                for (final Tag t : tagsToRemove) {
                    if (task != null) {
                        task.removeTag(t);
                    }
                    adapter.add(t.getName());
                }
            }
            if(lengthBefore<lengthAfter) {
                if (!text.toString().endsWith(",") && !currentTags.isEmpty()) {
                    currentTags.remove(currentTags.size() - 1);
                }
                for (final String tag : currentTags) {
                    addTag(tag);
                }
            }
            boolean hasPrefix=!text.toString().endsWith(",");
            if(hasPrefix && (lengthAfter < lengthBefore) && !tagsToRemove.isEmpty()){
                final String last=tags.get(tags.size()-1);
                hasPrefix=true;
                for(final Tag tag:tagsToRemove){
                    if(tag.getName().startsWith(last)){
                        hasPrefix=false;
                        break;
                    }
                }
            }

            if(hasPrefix){
                postfix=tags.get(tags.size()-1);
            }else{
                postfix="";
            }
            if(!tagsToRemove.isEmpty()||!currentTags.isEmpty()){
                rebuildText();
            }
        }
    }

    private void addTag(final @NonNull String name) {
        final Tag newTag;
        final Optional<Tag> existingTag = Tag.getByName(name);
        if (existingTag.isPresent()) {
            newTag = existingTag.get();
            if (currentTags.contains(newTag)) {
                return;
            }
        } else {
            newTag = Tag.newTag(name);
        }
        if (task != null) {
            task.addTag(newTag);
        }
        adapter.remove(newTag.getName());
    }


    @Override
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setBackground(@NonNull final Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.setBackground(background);
        } else {
            setBackgroundDrawable(background);
        }
    }



    @Override
    public void onClick(final View v) {
        if (clickEnabled) {
            setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
            requestFocusFromTouch();
        }
    }

    @Override
    public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
        // If the event is a key-down event on the "enter" button
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
            (keyCode == KeyEvent.KEYCODE_ENTER)) {
            final String[] tags = getText().toString().split(",");
            if (tags.length > 0) {
                addTag(tags[tags.length - 1]);
                rebuildText();
            }
            clearFocus();
            return true;
        }
        return false;

    }



    @Override
    public void onSoftKeyboardHide() {

    }

    @Override
    public void onSoftKeyboardShow() {

    }

    public float getItemHeight() {
        final TypedValue value = new TypedValue();
        final DisplayMetrics metrics = new DisplayMetrics();

        getContext().getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, value, true);
        ((WindowManager) (getContext().getSystemService(Context.WINDOW_SERVICE)))
                .getDefaultDisplay().getMetrics(metrics);

        return TypedValue.complexToDimension(value.data, metrics);
    }

    @Override
    public void onFilterComplete(final int count) {
        final int height = (int) (((count > 4) ? 4 : count) * ITEM_HEIGHT);
        setDropDownHeight(height);
        setDropDownVerticalOffset(-1*height-getHeight());

        super.onFilterComplete(count);
    }
}
