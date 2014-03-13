package com.mobeta.android.dslv;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

/**
 * A subclass of {@link android.widget.CursorAdapter} that provides reordering
 * of the elements in the Cursor based on completed drag-sort operations. The
 * reordering is a simple mapping of list positions into Cursor positions (the
 * Cursor is unchanged). To persist changes made by drag-sorts, one can retrieve
 * the mapping with the {@link #getCursorPositions()} method, which returns the
 * reordered list of Cursor positions.
 * 
 * An instance of this class is passed to
 * {@link DragSortListView#setAdapter(ListAdapter)} and, since this class
 * implements the {@link DragSortListView.DragSortListener} interface, it is
 * automatically set as the DragSortListener for the DragSortListView instance.
 */
public abstract class DragSortCursorAdapter extends CursorAdapter implements
		DragSortListView.DragSortListener {

	public static final int REMOVED = -1;

	/**
	 * Key is ListView position, value is Cursor position
	 */
	private final SparseIntArray mListMapping = new SparseIntArray();

	private final ArrayList<Integer> mRemovedCursorPositions = new ArrayList<Integer>();

	public DragSortCursorAdapter(final Context context, final Cursor c) {
		super(context, c);
	}

	public DragSortCursorAdapter(final Context context, final Cursor c,
			final boolean autoRequery) {
		super(context, c, autoRequery);
	}

	public DragSortCursorAdapter(final Context context, final Cursor c,
			final int flags) {
		super(context, c, flags);
	}

	/**
	 * Swaps Cursor and clears list-Cursor mapping.
	 * 
	 * @see android.widget.CursorAdapter#swapCursor(android.database.Cursor)
	 */
	@Override
	public Cursor swapCursor(final Cursor newCursor) {
		final Cursor old = super.swapCursor(newCursor);
		resetMappings();
		return old;
	}

	/**
	 * Changes Cursor and clears list-Cursor mapping.
	 * 
	 * @see android.widget.CursorAdapter#changeCursor(android.database.Cursor)
	 */
	@Override
	public void changeCursor(final Cursor cursor) {
		super.changeCursor(cursor);
		resetMappings();
	}

	/**
	 * Resets list-cursor mapping.
	 */
	public void reset() {
		resetMappings();
		notifyDataSetChanged();
	}

	private void resetMappings() {
		this.mListMapping.clear();
		this.mRemovedCursorPositions.clear();
	}

	@Override
	public Object getItem(final int position) {
		return super.getItem(this.mListMapping.get(position, position));
	}

	@Override
	public long getItemId(final int position) {
		return super.getItemId(this.mListMapping.get(position, position));
	}

	@Override
	public View getDropDownView(final int position, final View convertView,
			final ViewGroup parent) {
		return super.getDropDownView(this.mListMapping.get(position, position),
				convertView, parent);
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		return super.getView(this.mListMapping.get(position, position),
				convertView, parent);
	}

	/**
	 * On drop, this updates the mapping between Cursor positions and ListView
	 * positions. The Cursor is unchanged. Retrieve the current mapping with
	 * {@link getCursorPositions()}.
	 * 
	 * @see DragSortListView.DropListener#drop(int, int)
	 */
	@Override
	public void drop(final int from, final int to) {
		if (from != to) {
			final int cursorFrom = this.mListMapping.get(from, from);

			if (from > to) {
				for (int i = from; i > to; --i) {
					this.mListMapping.put(i,
							this.mListMapping.get(i - 1, i - 1));
				}
			} else {
				for (int i = from; i < to; ++i) {
					this.mListMapping.put(i,
							this.mListMapping.get(i + 1, i + 1));
				}
			}
			this.mListMapping.put(to, cursorFrom);

			cleanMapping();
			notifyDataSetChanged();
		}
	}

	/**
	 * On remove, this updates the mapping between Cursor positions and ListView
	 * positions. The Cursor is unchanged. Retrieve the current mapping with
	 * {@link getCursorPositions()}.
	 * 
	 * @see DragSortListView.RemoveListener#remove(int)
	 */
	@Override
	public void remove(final int which) {
		final int cursorPos = this.mListMapping.get(which, which);
		if (!this.mRemovedCursorPositions.contains(cursorPos)) {
			this.mRemovedCursorPositions.add(cursorPos);
		}

		final int newCount = getCount();
		for (int i = which; i < newCount; ++i) {
			this.mListMapping.put(i, this.mListMapping.get(i + 1, i + 1));
		}

		this.mListMapping.delete(newCount);

		cleanMapping();
		notifyDataSetChanged();
	}

	/**
	 * Does nothing. Just completes DragSortListener interface.
	 */
	@Override
	public void drag(final int from, final int to) {
		// do nothing
	}

	/**
	 * Remove unnecessary mappings from sparse array.
	 */
	private void cleanMapping() {
		final ArrayList<Integer> toRemove = new ArrayList<Integer>();

		int size = this.mListMapping.size();
		for (int i = 0; i < size; ++i) {
			if (this.mListMapping.keyAt(i) == this.mListMapping.valueAt(i)) {
				toRemove.add(this.mListMapping.keyAt(i));
			}
		}

		size = toRemove.size();
		for (int i = 0; i < size; ++i) {
			this.mListMapping.delete(toRemove.get(i));
		}
	}

	@Override
	public int getCount() {
		return super.getCount() - this.mRemovedCursorPositions.size();
	}

	/**
	 * Get the Cursor position mapped to by the provided list position (given
	 * all previously handled drag-sort operations).
	 * 
	 * @param position
	 *            List position
	 * 
	 * @return The mapped-to Cursor position
	 */
	public int getCursorPosition(final int position) {
		return this.mListMapping.get(position, position);
	}

	/**
	 * Get the current order of Cursor positions presented by the list.
	 */
	public ArrayList<Integer> getCursorPositions() {
		final ArrayList<Integer> result = new ArrayList<Integer>();

		for (int i = 0; i < getCount(); ++i) {
			result.add(this.mListMapping.get(i, i));
		}

		return result;
	}

	/**
	 * Get the list position mapped to by the provided Cursor position. If the
	 * provided Cursor position has been removed by a drag-sort, this returns
	 * {@link #REMOVED}.
	 * 
	 * @param cursorPosition
	 *            A Cursor position
	 * @return The mapped-to list position or REMOVED
	 */
	public int getListPosition(final int cursorPosition) {
		if (this.mRemovedCursorPositions.contains(cursorPosition)) {
			return REMOVED;
		}

		final int index = this.mListMapping.indexOfValue(cursorPosition);
		if (index < 0) {
			return cursorPosition;
		} else {
			return this.mListMapping.keyAt(index);
		}
	}

}
