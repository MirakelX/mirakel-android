/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.settings.semantics;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakelandroid.R;

public class SemanticsSettingsFragment extends Fragment {
	@SuppressWarnings("unused")
	private static final String TAG = "SemanticsSettingsFragment";
	private List<ListMirakel> lists;
	private Semantic semantic;
	Context ctx;
	protected AlertDialog alert;
	private boolean[] mSelectedItems;
	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = getActivity();
		view = inflater.inflate(R.layout.settings_semantic_fragment, container,
				false);
		if (semantic != null)
			update();
		return view;
	}

	public void setSemantic(Semantic semantic) {
		this.semantic = semantic;
		if (view != null)
			update();
	}

	public void update() {

	}

}
