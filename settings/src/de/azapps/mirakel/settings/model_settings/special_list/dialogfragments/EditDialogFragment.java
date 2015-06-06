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

package de.azapps.mirakel.settings.model_settings.special_list.dialogfragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsBooleanProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList.CONJUNCTION;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueExistsProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSubtaskProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsTagProperty;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.BasePropertyFragement;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.ConjunctionFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.DuePropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.ListPropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.NegatedPropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.PriorityPropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.ProgressPropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.StringPropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.SubtaskPropertyFragment;
import de.azapps.mirakel.settings.model_settings.special_list.dialogfragments.editfragments.TagPropertyFragment;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.of;

public class EditDialogFragment extends DialogFragment implements Spinner.OnItemSelectedListener,
    View.OnClickListener {

    public static final String DIALOG = "dialog";
    private OnPropertyEditListener onEditListener;

    public interface OnPropertyEditListener {
        public abstract void onEditFinish(@NonNull SpecialList list);
    }

    private static final String TAG = "EditDialogFragment";

    public static final String PROPERTY_KEY = "PROPERTY";
    public static final String LIST_KEY = "LIST";
    public static final String BACK_STACK_KEY = "BACK";
    public static final String ROOT_PROPERTY_KEY = "ROOT_PROPERTY";

    public static final int DONE = 0;
    public static final int CONTENT = 1;
    public static final int DUE_EXIST = 2;
    public static final int DUE = 3;
    public static final int FILE = 4;
    public static final int LISTS = 5;
    public static final int LIST_NAME = 6;
    public static final int NAME = 7;
    public static final int PRIORITY = 8;
    public static final int PROGRESS = 9;
    public static final int REMINDER = 10;
    public static final int TAGS = 11;
    public static final int SUBTASKS = 12;
    public static final int SUBCONDITION = 13;


    @NonNull
    private SpecialList mList;
    @NonNull
    private SpecialListsBaseProperty property;
    @NonNull
    private ArrayList<Integer> backStack = new ArrayList<>();


    public static EditDialogFragment newInstance(@NonNull final SpecialList list,
            @NonNull final SpecialListsBaseProperty property, @NonNull final ArrayList<Integer> backStack,
            @NonNull final OnPropertyEditListener listener,
            @NonNull final SpecialListsConjunctionList rootProperty) {
        final EditDialogFragment fragment = new EditDialogFragment();
        final Bundle args = new Bundle();
        args.putParcelable(LIST_KEY, list);
        args.putParcelable(PROPERTY_KEY, property);
        args.putParcelable(ROOT_PROPERTY_KEY, rootProperty);
        args.putIntegerArrayList(BACK_STACK_KEY, backStack);
        fragment.setArguments(args);
        fragment.onEditListener = listener;

        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mList = getArguments().getParcelable(LIST_KEY);
        property = getArguments().getParcelable(PROPERTY_KEY);
        backStack = getArguments().getIntegerArrayList(BACK_STACK_KEY);
    }

    @Override
    public int getTheme() {
        return ThemeManager.getDialogTheme();
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                                          WindowManager.LayoutParams.WRAP_CONTENT);
    }

    /**
     * @throws java.lang.IllegalArgumentException if unknown property type
     */
    private static int propertyToType(final SpecialListsBaseProperty property) {
        if (property != null) {
            if (property instanceof SpecialListsDoneProperty) {
                return DONE;
            } else if (property instanceof SpecialListsContentProperty) {
                return CONTENT;
            } else if (property instanceof SpecialListsDueProperty) {
                return DUE;
            } else if (property instanceof SpecialListsFileProperty) {
                return FILE;
            } else if (property instanceof SpecialListsListProperty) {
                return LISTS;
            } else if (property instanceof SpecialListsNameProperty) {
                return NAME;
            } else if (property instanceof SpecialListsPriorityProperty) {
                return PRIORITY;
            } else if (property instanceof SpecialListsProgressProperty) {
                return PROGRESS;
            } else if (property instanceof SpecialListsReminderProperty) {
                return REMINDER;
            } else if (property instanceof SpecialListsTagProperty) {
                return TAGS;
            } else if (property instanceof SpecialListsSubtaskProperty) {
                return SUBTASKS;
            } else if (property instanceof SpecialListsConjunctionList) {
                return SUBCONDITION;
            } else if (property instanceof SpecialListsListNameProperty) {
                return LIST_NAME;
            } else if (property instanceof SpecialListsDueExistsProperty) {
                return DUE_EXIST;
            }

        }
        throw new IllegalArgumentException("Could not get type of " + ((Object)
                                           property).getClass().getCanonicalName());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.special_lists_edit_dialog_fragment, null);
        final ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(
                    getActivity(),
                    R.array.special_lists_filter_type, android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = (Spinner)rootView.findViewById(R.id.property_spinner);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(propertyToType(property), false);
        handleNewFragment(propertyToType(property));
        final Button ok = (Button)rootView.findViewById(R.id.saveButton);
        ok.setOnClickListener(this);
        ((LinearLayout)rootView).setShowDividers(LinearLayout.SHOW_DIVIDER_END);
        return rootView;
    }



    private void handleNewFragment(final int id) {
        final BasePropertyFragement fragment;
        switch (id) {
        case DONE:
            property = new SpecialListsDoneProperty(property);
            fragment = NegatedPropertyFragment.newInstance((SpecialListsBooleanProperty) property);
            break;
        case FILE:
            property = new SpecialListsFileProperty(property);
            fragment = NegatedPropertyFragment.newInstance((SpecialListsBooleanProperty)property);
            break;
        case CONTENT:
            property = new SpecialListsContentProperty(property);
            fragment = StringPropertyFragment.newInstance((SpecialListsStringProperty)property);
            break;
        case DUE:
            property = new SpecialListsDueProperty(property);
            fragment = DuePropertyFragment.newInstance((SpecialListsDueProperty) property);
            break;
        case LISTS:
            property = new SpecialListsListProperty(property);
            fragment = ListPropertyFragment.newInstance((SpecialListsListProperty) property);
            break;
        case NAME:
            property = new SpecialListsNameProperty(property);
            fragment = StringPropertyFragment.newInstance((SpecialListsStringProperty)property);
            break;
        case PRIORITY:
            property = new SpecialListsPriorityProperty(property);
            fragment = PriorityPropertyFragment.newInstance((SpecialListsPriorityProperty) property);
            break;
        case PROGRESS:
            property = new SpecialListsProgressProperty(property);
            fragment = ProgressPropertyFragment.newInstance((SpecialListsProgressProperty) property);
            break;
        case REMINDER:
            property = new SpecialListsReminderProperty(property);
            fragment = NegatedPropertyFragment.newInstance((SpecialListsReminderProperty)property);
            break;
        case TAGS:
            property = new SpecialListsTagProperty(property);
            fragment = TagPropertyFragment.newInstance((SpecialListsTagProperty) property);
            break;
        case SUBTASKS:
            property = new SpecialListsSubtaskProperty(property);
            fragment = SubtaskPropertyFragment.newInstance((SpecialListsSubtaskProperty) property);
            break;
        case SUBCONDITION:
            property = new SpecialListsConjunctionList(property,
                    ((backStack.size() % 2) == 0) ? CONJUNCTION.AND : CONJUNCTION.OR);
            fragment = ConjunctionFragment.newInstance((SpecialListsConjunctionList) property, mList,
                       backStack);
            break;
        case LIST_NAME:
            property = new SpecialListsListNameProperty(property);
            fragment = StringPropertyFragment.newInstance((SpecialListsStringProperty)property);
            break;
        case DUE_EXIST:
            property = new SpecialListsDueExistsProperty(property);
            fragment = NegatedPropertyFragment.newInstance((SpecialListsDueExistsProperty) property);
            break;
        default:
            Log.wtf(TAG, "unknown type");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                transaction.replace(R.id.property_dialog_container, fragment, DIALOG);
                transaction.commit();
            }
        });
        if (getDialog() != null) {
            getDialog().setTitle(property.getTitle(getActivity()));
        }
    }

    @Override
    public void onClick(final View v) {
        final SpecialListsBaseProperty property = ((BasePropertyFragement)
                getChildFragmentManager().findFragmentByTag(DIALOG)).getProperty();
        mList = execOnTree(mList, backStack, new WorkOnTree() {
            @Override
            public void onTreeExists(final int position,
                                     @NonNull final SpecialListsConjunctionList currentProperty) {
                if (position >= backStack.size()) {
                    return;
                }
                if (backStack.get(position) != ConjunctionFragment.NEW_PROPERTY) {
                    if (backStack.get(position) < currentProperty.getChilds().size()) {
                        currentProperty.getChilds().set(backStack.get(position), property);
                    } else {
                        currentProperty.addChild(property);
                    }
                } else {
                    currentProperty.getChilds().add(property);
                }
            }

            @NonNull
            @Override
            public Optional<SpecialListsBaseProperty> onTreeNotExists() {
                return of(property);
            }
        }, 1);
        mList.save();
        dismiss();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        onEditListener.onEditFinish(mList);
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
                               final long id) {
        handleNewFragment(position);
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {

    }

    public interface WorkOnTree {
        void onTreeExists(final int position,
                          @NonNull final SpecialListsConjunctionList currentProperty);
        @NonNull
        Optional<SpecialListsBaseProperty> onTreeNotExists();
    }

    @NonNull
    public static SpecialList execOnTree(@NonNull final SpecialList list,
                                         @NonNull final List<Integer> backStack, @NonNull final WorkOnTree payload, final int level) {
        if (list.getWhere().isPresent()) {
            SpecialListsBaseProperty rootProperty = list.getWhere().get();
            SpecialListsBaseProperty currentProperty;
            int i = 0;
            if (rootProperty instanceof SpecialListsConjunctionList) {
                currentProperty = rootProperty;
                for (; i < backStack.size() - level; i++) {
                    SpecialListsBaseProperty newCurrentProperty = ((SpecialListsConjunctionList)
                            currentProperty).getChilds().get(backStack.get(i));
                    if (!(newCurrentProperty instanceof SpecialListsConjunctionList)) {
                        final List<SpecialListsBaseProperty> childs = new ArrayList<>(1);
                        childs.add(newCurrentProperty);
                        newCurrentProperty = new SpecialListsConjunctionList(((i % 2) == 1) ? CONJUNCTION.AND :
                                CONJUNCTION.OR, childs);
                        ((SpecialListsConjunctionList) currentProperty).getChilds().set(backStack.get(i),
                                newCurrentProperty);
                        currentProperty = newCurrentProperty;
                        ++i;
                        break;
                    }
                    currentProperty = newCurrentProperty;

                }
            } else {
                List<SpecialListsBaseProperty> childs = new ArrayList<>(0);
                childs.add(rootProperty);
                currentProperty = new SpecialListsConjunctionList(SpecialListsConjunctionList.CONJUNCTION.AND,
                        childs);
                rootProperty = currentProperty;
            }
            payload.onTreeExists(i, (SpecialListsConjunctionList) currentProperty);
            list.setWhere(of(rootProperty));
        } else {
            list.setWhere(payload.onTreeNotExists());
        }
        return list;
    }
}
