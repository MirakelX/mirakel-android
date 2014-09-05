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

package de.azapps.mirakel.settings.model_settings.special_list.dialogfragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.common.base.Optional;

import static com.google.common.base.Optional.of;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList.CONJUNCTION;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsBooleanProperty;
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
import de.azapps.mirakel.settings.model_settings.special_list.helper.SpecialListsConditionAdapter;
import de.azapps.tools.Log;

public class EditDialogFragment extends DialogFragment implements Spinner.OnItemSelectedListener,
    View.OnClickListener {

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
    public static final int DUE = 2;
    public static final int FILE = 3;
    public static final int LISTS = 4;
    public static final int NAME = 5;
    public static final int PRIORITY = 6;
    public static final int PROGRESS = 7;
    public static final int REMINDER = 8;
    public static final int TAGS = 9;
    public static final int SUBTASKS = 10;
    public static final int SUBCONDITION = 11;

    private SpecialList mList;
    private SpecialListsBaseProperty property;
    private SpecialListsConjunctionList rootProperty;
    private ArrayList<Integer> backStack;


    public static EditDialogFragment newInstance(@NonNull final SpecialList list,
            @NonNull final SpecialListsBaseProperty property, @NonNull final ArrayList<Integer> backStack,
            @NonNull final OnPropertyEditListener listener,
            @NonNull final SpecialListsConjunctionList rootProperty) {
        EditDialogFragment fragment = new EditDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(LIST_KEY, list);
        args.putParcelable(PROPERTY_KEY, property);
        args.putParcelable(ROOT_PROPERTY_KEY, rootProperty);
        args.putIntegerArrayList(BACK_STACK_KEY, backStack);
        fragment.setArguments(args);
        fragment.setOnPropertyEditListener(listener);

        return fragment;
    }

    private void setOnPropertyEditListener(@NonNull OnPropertyEditListener listener) {
        onEditListener = listener;
    }

    public EditDialogFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mList = getArguments().getParcelable(LIST_KEY);
        property = getArguments().getParcelable(PROPERTY_KEY);
        rootProperty = getArguments().getParcelable(ROOT_PROPERTY_KEY);
        backStack = getArguments().getIntegerArrayList(BACK_STACK_KEY);
    }

    /**
     * @throws java.lang.IllegalArgumentException if unkown property type
     */
    private static int propertyToType(SpecialListsBaseProperty property) {
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
            }
        }
        throw new IllegalArgumentException("Could not get type of " + ((Object)
                                           property).getClass().getCanonicalName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.special_lists_edit_dialog_fragment, null);
        ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.special_lists_filter_type, android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner)rootView.findViewById(R.id.property_spinner);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(propertyToType(property), false);
        handleNewFragment(propertyToType(property));
        Button ok = (Button)rootView.findViewById(R.id.saveButton);
        ok.setOnClickListener(this);
        ((LinearLayout)rootView).setShowDividers(LinearLayout.SHOW_DIVIDER_END);
        return rootView;
    }



    private void handleNewFragment(int id) {
        BasePropertyFragement fragment;
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
                    rootProperty.getConjunction() == CONJUNCTION.AND ? CONJUNCTION.OR : CONJUNCTION.AND);
            fragment = ConjunctionFragment.newInstance((SpecialListsConjunctionList) property, mList,
                       backStack);
            break;
        default:
            Log.wtf(TAG, "unknown type");
            return;
        }
        if (getView() != null && getView().findViewById(R.id.property_dialog_container) != null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.property_dialog_container, fragment, "dialog");
            transaction.commit();
        }
        if (getDialog() != null) {
            getDialog().setTitle(property.getTitle(getActivity()));
        }
    }

    @Override
    public void onClick(View v) {
        final SpecialListsBaseProperty property = ((BasePropertyFragement)
                getChildFragmentManager().findFragmentById(R.id.property_dialog_container)).getProperty();
        mList = execOnTree(mList, backStack, new WorkOnTree() {
            @Override
            public void onTreeExists(int position, @NonNull SpecialListsConjunctionList currentProperty) {
                if (backStack.get(position) != SpecialListsConditionAdapter.NEW_PROPERTY) {
                    currentProperty.getChilds().set(backStack.get(position), property);
                } else {
                    currentProperty.getChilds().add(property);
                }
            }

            @Override
            public Optional<SpecialListsBaseProperty> onTreeNotExists() {
                return of(property);
            }
        }, 1);
        mList.save();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onEditListener.onEditFinish(mList);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        handleNewFragment(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public interface WorkOnTree {
        public abstract void onTreeExists(final int position,
                                          @NonNull final SpecialListsConjunctionList currentProperty);
        @NonNull
        public abstract Optional<SpecialListsBaseProperty> onTreeNotExists();
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
                        List<SpecialListsBaseProperty> childs = new ArrayList<>();
                        childs.add(newCurrentProperty);
                        newCurrentProperty = new SpecialListsConjunctionList(i % 2 == 0 ?
                                SpecialListsConjunctionList.CONJUNCTION.AND
                                : SpecialListsConjunctionList.CONJUNCTION.OR, childs);
                        ((SpecialListsConjunctionList) currentProperty).getChilds().set(backStack.get(i),
                                newCurrentProperty);
                        currentProperty = newCurrentProperty;
                        ++i;
                        break;
                    }
                    currentProperty = newCurrentProperty;

                }
            } else {
                List<SpecialListsBaseProperty> childs = new ArrayList<>();
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
