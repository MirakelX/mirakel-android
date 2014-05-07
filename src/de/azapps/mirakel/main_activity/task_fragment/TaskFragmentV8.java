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
package de.azapps.mirakel.main_activity.task_fragment;

import android.annotation.SuppressLint;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class TaskFragmentV8 extends TaskFragment {

	public TaskFragmentV8() {
		super();
	}

	private ActionMode mActionMode;
	@SuppressLint("NewApi")
	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the user selects a
		// contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode,
				final MenuItem item) {
			final boolean b = handleActionBarClick(item);
			if (!b) {
				mode.finish();
			}
			return b;
		}

		// Called when the action mode is
		// created; startActionMode() was
		// called
		@Override
		public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
			// Inflate a menu resource
			// providing context menu items
			final MenuInflater inflater = mode.getMenuInflater();
			final boolean b = handleCabCreateMenu(inflater, menu);
			if (b) {
				TaskFragmentV8.this.mActionMode = mode;
				TaskFragmentV8.this.mMenu = menu;
			}
			return b;
		}

		// Called when the user exits the
		// action mode
		@Override
		public void onDestroyActionMode(final ActionMode mode) {
			handleCloseCab();
			TaskFragmentV8.this.mActionMode = null;
		}

		// Called each time the action mode
		// is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if
		// the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(final ActionMode mode,
				final Menu menu) {
			return false; // Return false if
			// nothing is
			// done
		}
	};

	@Override
	protected void changeVisiblity(final boolean visible, final MenuItem item) {
		if (this.mActionMode != null && item != null) {
			item.setVisible(visible);
		}
	}

	@Override
	public void closeActionMode() {
		if (this.mActionMode != null) {
			this.mActionMode.finish();
		}
	}

	@Override
	protected void startCab() {
		if (this.mActionMode == null) {
			this.main.startSupportActionMode(this.mActionModeCallback);
		}

	}

}
