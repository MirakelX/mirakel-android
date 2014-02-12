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
package de.azapps.mirakel.main_activity;

import android.support.v4.app.Fragment;
import android.view.View;

/**
 * This is the base Fragment for Mirakel. It defines some necessary variables
 * and functions.
 * 
 * @author az
 * 
 */
public abstract class MirakelFragment extends Fragment {
	/**
	 * Often we need to reference the main activity
	 */
	protected MainActivity main;
	/**
	 * View of the fragment
	 */
	protected View view;

	public void setActivity(MainActivity activity) {
		main = activity;
	}

	public abstract void update();
}
