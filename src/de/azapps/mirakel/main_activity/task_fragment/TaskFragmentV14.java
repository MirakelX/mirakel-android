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
import android.annotation.TargetApi;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TaskFragmentV14 extends TaskFragment {

	private ActionMode					mActionMode;
	@SuppressLint("NewApi")
	private final ActionMode.Callback	mActionModeCallback	= new ActionMode.Callback() {

		// Called when the user selects a
		// contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			boolean b = handleActionBarClick(item);
			if (!b) {
				mode.finish();
			}
			return b;
		}

		// Called when the action mode is
		// created; startActionMode() was
		// called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource
			// providing context menu items
			MenuInflater inflater = mode
					.getMenuInflater();
			boolean b = handleCabCreateMenu(
					inflater,
					menu);
			if (b) {
				TaskFragmentV14.this.mActionMode = mode;
				TaskFragmentV14.this.mMenu = menu;
			}
			return b;
		}

		// Called when the user exits the
		// action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			handleCloseCab();
		}

		// Called each time the action mode
		// is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if
		// the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if
			// nothing is
			// done
		}
	};

	@Override
	protected void changeVisiblity(boolean visible, MenuItem item) {
		if(this.mActionMode!=null&&item!=null){
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
			this.main.startActionMode(this.mActionModeCallback);
		}

	}

}
