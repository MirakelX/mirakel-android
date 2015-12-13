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
package de.azapps.mirakel.new_ui.fragments;


import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.emtronics.dragsortrecycler.DragSortRecycler;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.material_elements.utils.SnackBarEventListener;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.new_ui.activities.LockableDrawer;
import de.azapps.mirakel.new_ui.activities.MirakelActivity;
import de.azapps.mirakel.new_ui.adapter.ListAdapter;
import de.azapps.mirakel.new_ui.views.ListEditView;
import de.azapps.mirakelandroid.R;

public class ListsFragment extends Fragment implements LoaderManager.LoaderCallbacks,
    MultiSelectCursorAdapter.MultiSelectCallbacks<ListMirakel>, ActionMode.Callback,
    DragSortRecycler.OnItemMovedListener {

    private static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String LISTS_TO_DELETE = "LISTS_TO_DELETE";
    private ListAdapter mAdapter;
    private OnItemClickedListener<ListMirakel> mListener;
    @NonNull
    private Optional<AccountMirakel> accountMirakelOptional = Optional.absent();
    @InjectView(R.id.list_lists)
    RecyclerView mListView;
    @Nullable
    private ActionMode mActionMode;
    private int numberOfSelectedEditables = 0;
    private int numberOfSelectedDeletables = 0;


    public ListsFragment() {
        // Required empty public constructor
    }


    public Optional<AccountMirakel> getAccount() {
        return accountMirakelOptional;
    }


    public void setAccount(final Optional<AccountMirakel> accountMirakelOptional) {
        this.accountMirakelOptional = accountMirakelOptional;
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_ACCOUNT, accountMirakelOptional.orNull());
        getLoaderManager().restartLoader(0, args, this);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ListAdapter(getActivity(), null, mListener, this);
        getLoaderManager().initLoader(0, null, this);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.setItemAnimator(null);

        final DragSortRecycler dragSortRecycler = new DragSortRecycler();
        dragSortRecycler.setViewHandleId(R.id.row_list_drag);
        dragSortRecycler.setOnDragStateChangedListener(new DragSortRecycler.OnDragStateChangedListener() {
            @Override
            public void onDragStart() {
                ((LockableDrawer) getActivity()).lockDrawer();
            }

            @Override
            public void onDragStop() {
                ((LockableDrawer) getActivity()).unlockDrawer();

            }
        });

        dragSortRecycler.setOnItemMovedListener(this);

        mListView.addItemDecoration(dragSortRecycler);
        mListView.addOnItemTouchListener(dragSortRecycler);
        mListView.setOnScrollListener(dragSortRecycler.getScrollListener());
    }


    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnItemClickedListener<ListMirakel>) activity;
            if (!((activity instanceof EventListener) || (activity instanceof LockableDrawer))) {
                throw new ClassCastException();
            }
        } catch (final ClassCastException ignored) {
            throw new ClassCastException(activity.toString() +
                                         " must implement OnListSelectedListener, EventListener and LockableDrawer");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View layout = inflater.inflate(R.layout.fragment_lists, container, false);
        ButterKnife.inject(this, layout);
        return layout;
    }

    @Override
    public Loader onCreateLoader(final int id, final Bundle args) {
        if (args != null) {
            accountMirakelOptional = Optional.fromNullable((AccountMirakel) args.getParcelable(
                                         ARGUMENT_ACCOUNT));
            if (accountMirakelOptional.isPresent() &&
                (accountMirakelOptional.get().getType() == AccountMirakel.ACCOUNT_TYPES.ALL)) {
                accountMirakelOptional = Optional.absent(); // Remove the hack as soon as possible
            }
            final ArrayList<ListMirakel> listsToDelete = args.getParcelableArrayList(LISTS_TO_DELETE);
            if (listsToDelete != null) {
                final List<Long> ids = new ArrayList<>(Collections2.transform(listsToDelete,
                new Function<ListMirakel, Long>() {
                    @Override
                    public Long apply(@Nullable final ListMirakel input) {
                        if (input != null) {
                            return input.getId();
                        } else {
                            return 0L;
                        }
                    }
                }));
                return ListMirakel.allWithSpecialMQB(accountMirakelOptional).and(ListMirakel.ID,
                        MirakelQueryBuilder.Operation.NOT_IN,
                        ids).toSupportCursorLoader(MirakelInternalContentProvider.LIST_WITH_COUNT_URI);
            }

        }
        return ListMirakel.allWithSpecialSupportCursorLoader(accountMirakelOptional);
    }

    @Override
    public void onLoadFinished(final Loader loader, final Object data) {
        mAdapter.swapCursor((Cursor) data);
    }

    @Override
    public void onLoaderReset(final Loader loader) {
        mAdapter.swapCursor(null);
    }

    public void onCloseNavDrawer() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onSelectModeChanged(final boolean selectMode) {
        if (selectMode) {
            mActionMode = getActivity().startActionMode(this);
        } else if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public boolean canAddItem(@NonNull final ListMirakel listMirakel) {
        if (!listMirakel.isDeletable() && !listMirakel.isEditable()) {
            SnackbarManager.show(Snackbar.with(getActivity())
                                 .text(R.string.can_not_edit_list)
                                 .eventListener(new SnackBarEventListener())
                                 , getActivity());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onAddSelectedItem(@NonNull final ListMirakel listMirakel) {
        onSelectedItemCountChanged(mAdapter.getSelectedItemCount());
        if ((mAdapter.getSelectedItemCount() > 0) && (mActionMode == null)) {
            onSelectModeChanged(true);
        }
        if (listMirakel.isEditable()) {
            numberOfSelectedEditables++;
            if (numberOfSelectedEditables == 1) {
                mActionMode.getMenu().findItem(R.id.menu_edit).setVisible(true);
            } else {
                mActionMode.getMenu().findItem(R.id.menu_edit).setVisible(false);

            }
        }
        if (listMirakel.isDeletable()) {
            numberOfSelectedDeletables++;
            // Show remove icon
            if (numberOfSelectedDeletables == 1) {
                mActionMode.getMenu().findItem(R.id.menu_delete).setVisible(true);
            }
        }
    }

    @Override
    public void onRemoveSelectedItem(@NonNull final ListMirakel listMirakel) {
        final int count = mAdapter.getSelectedItemCount();
        if ((count > 0) && (mActionMode == null)) {
            onSelectModeChanged(true);
        }
        onSelectedItemCountChanged(count);
        if (count > 0) {
            if ((count == 1) && listMirakel.isEditable()) {
                mActionMode.getMenu().findItem(R.id.menu_edit).setVisible(true);
            }
            if (listMirakel.isDeletable()) {
                numberOfSelectedDeletables--;
                if (numberOfSelectedDeletables == 0) {
                    mActionMode.getMenu().findItem(R.id.menu_delete).setVisible(false);
                }
            }
        }
    }

    public void onSelectedItemCountChanged(final int itemCount) {
        if (mActionMode == null) {
            onSelectModeChanged(true);
        }
        if (itemCount == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(getResources().getQuantityString(R.plurals.list_multiselect_title,
                                 itemCount, itemCount));
        }
    }

    @Override
    public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
        final int count = mAdapter.getSelectedItemCount();
        final MenuInflater inflater = mode
                                      .getMenuInflater();
        inflater.inflate(R.menu.multiselect_lists, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ThemeManager.getColor(R.attr.colorCABStatus));
        }
        mode.setTitle(getResources().getQuantityString(R.plurals.list_multiselect_title, count, count));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
        final ArrayList<ListMirakel> selected = new ArrayList<>(Collections2.transform(
        mAdapter.getSelectedItems(), new Function<ListMirakel, ListMirakel>() {
            @Override
            public ListMirakel apply(@Nullable ListMirakel input) {
                if ((input != null) && input.isSpecial()) {
                    return SpecialList.get(input.getId()).get();
                }
                return input;
            }
        }));
        switch (item.getItemId()) {
        case R.id.menu_delete:
            handleDelete(selected);
            break;
        case R.id.menu_edit:
            editList(selected.get(0));
            break;
        default:
            return false;
        }
        mode.finish();
        return true;
    }

    private void handleDelete(final ArrayList<ListMirakel> listsToDelete) {
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_ACCOUNT, accountMirakelOptional.orNull());
        args.putParcelableArrayList(LISTS_TO_DELETE, listsToDelete);
        getLoaderManager().restartLoader(0, args, this);
        final int count = listsToDelete.size();

        SnackbarManager.show(
            Snackbar.with(getActivity())
            .text(getResources().getQuantityString(R.plurals.list_multiselect_deleted, count, count))
            .actionLabel(R.string.undo)
        .actionListener(new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                listsToDelete.clear();
            }
        })
        .eventListener(new SnackBarEventListener() {
            @Override
            public void onDismiss(final Snackbar snackbar) {
                super.onDismiss(snackbar);
                ((MirakelActivity) getActivity()).moveFabDown(snackbar.getHeight());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (final ListMirakel listMirakel : listsToDelete) {
                            listMirakel.destroy();
                        }
                        mListView.post(new Runnable() {
                            @Override
                            public void run() {
                                setAccount(accountMirakelOptional);
                            }
                        });
                    }
                }).start();
            }

            @Override
            public void onShow(final Snackbar snackbar) {
                ((MirakelActivity) getActivity()).moveFABUp(snackbar.getHeight());
            }
        })
        , getActivity());
    }

    public void editList(final ListMirakel listMirakel) {
        Context ctx = new ContextThemeWrapper(getActivity(), R.style.MirakelBaseTheme);
        final ListEditView listEditView = new ListEditView(ctx);
        listEditView.setListMirakel(listMirakel);
        final Dialog dialog = new AlertDialogWrapper.Builder(ctx).setView(listEditView)
        .setTitle(R.string.list_edit_title)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                listEditView.saveList();
                listEditView.closeKeyBoard();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listEditView.closeKeyBoard();
            }
        }).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                listEditView.openKeyBoard();
            }
        });
        dialog.show();
    }

    @Override
    public void onDestroyActionMode(final ActionMode mode) {
        mAdapter.clearSelections();
        mActionMode = null;
        numberOfSelectedDeletables = 0;
        numberOfSelectedEditables = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ThemeManager.getColor(R.attr.colorPrimaryDark));
        }
    }

    /**
     * We want to move a list from position A to position B.
     *
     * We have a column lft. The lists are ordered by this column. When we are moving lists down we have to do the following:
     *
     * lft  | list
     * 1    | A
     * 3    | B
     * 5    | C
     * 7    | D
     * 9    | E
     *
     * ## Moving Down
     * We want to move list B below D. We do the following:
     * 1. Decrement the lft-values by 2 of the lists with lft > lft(B) && lft <= lft(D)
     * 2. Set the lft of B to the old lft(D)
     *
     * ## Moving Up
     * We want to move list D above list B. We do the following:
     * 1. Increment the lft-values by 2 of the lists with lft>=lft(B) && lft < lft(D)
     * 2. Set the lft of D to the old lft(B)
     *
     * @param tmpFrom List to move
     * @param tmpTo Target list to move the `from` list to. If the user is moving from down, than it should be moved below `to` otherwise above
     */
    @Override
    public void onItemMoved(final int tmpFrom, final int tmpTo) {
        mAdapter.onItemMoved(tmpFrom, tmpTo);
        // update divider position
        final int dividerPosition = MirakelModelPreferences.getDividerPosition();
        final int from;
        final int to;
        final boolean changedPosition;
        if ((tmpFrom < dividerPosition) && (tmpTo >= dividerPosition)) {
            MirakelModelPreferences.setDividerPosition(dividerPosition - 1);
            from = tmpFrom;
            to = tmpTo - 1;
            changedPosition = true;
        } else if ((tmpFrom > dividerPosition) && (tmpTo <= dividerPosition)) {
            MirakelModelPreferences.setDividerPosition(dividerPosition + 1);
            from = tmpFrom - 1;
            to = tmpTo;
            changedPosition = true;
        } else if ((tmpFrom > dividerPosition) && (tmpTo > dividerPosition)) {
            from = tmpFrom - 1;
            to = tmpTo - 1;
            changedPosition = false;
        } else {
            from = tmpFrom;
            to = tmpTo;
            changedPosition = false;
        }
        final Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(from);
        final ListMirakel fromList = new ListMirakel(CursorGetter.unsafeGetter(cursor));
        cursor.moveToPosition(to);
        final ListMirakel toList = new ListMirakel(CursorGetter.unsafeGetter(cursor));
        MirakelInternalContentProvider.withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                for (final String table : new String[] {SpecialList.TABLE, ListMirakel.TABLE}) {
                    final ContentValues cv = new ContentValues();
                    cv.put("TABLE", table);
                    if (to < from) { // move list up
                        getActivity().getContentResolver().update(MirakelInternalContentProvider.UPDATE_LIST_MOVE_UP_URI,
                                cv,
                                ListMirakel.LFT + " >= "
                                + toList.getLft() + " and " + ListMirakel.LFT + " < "
                                + fromList.getLft(), null);
                    } else if (to > from) { // move list down
                        getActivity().getContentResolver().update(MirakelInternalContentProvider.UPDATE_LIST_MOVE_DOWN_URI,
                                cv,
                                ListMirakel.LFT + " > "
                                + fromList.getLft() + " and " + ListMirakel.LFT + " <= "
                                + toList.getLft(), null);
                    } else if (changedPosition) {
                        setAccount(accountMirakelOptional);
                        return;
                    } else {
                        return;
                    }
                    getActivity().getContentResolver().update(MirakelInternalContentProvider.UPDATE_LIST_FIX_RGT_URI,
                            cv,
                            null, null);
                }
                fromList.setLft(toList.getLft());
                fromList.setRgt(toList.getLft() + 2);
                fromList.save();
            }
        });
    }
}
