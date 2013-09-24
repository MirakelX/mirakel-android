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
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakelandroid.R;

public class FileAdapter extends MirakelArrayAdapter<FileMirakel> {
	private static final String TAG = "TaskAdapter";
	
	public FileAdapter(Context c){
		//do not call this, only for error-fixing there
		super(c,0,(List<FileMirakel>)new ArrayList<FileMirakel>(),false);	
	}

	public FileAdapter(MainActivity context, int layoutResourceId,
			List<FileMirakel> data) {
		super(context, layoutResourceId, data,PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("DarkTheme", false));
		Log.d(TAG, "created");
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FileHolder holder = null;

		if (row == null) {
			// Initialize the View
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new FileHolder();
			holder.fileImage = (ImageView) row.findViewById(R.id.file_image);
			holder.fileName = (TextView) row.findViewById(R.id.file_name);
			holder.filePath = (TextView) row.findViewById(R.id.file_path);

			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}
		FileMirakel file = data.get(position);
		Bitmap preview = file.getPreview();
		if (preview != null) {
			holder.fileImage.setImageBitmap(preview);
			Log.e("Blubb", file.getPath());
		} else {
			LayoutParams params = (LayoutParams) holder.fileImage
					.getLayoutParams();
			params.height = 0;
			holder.fileImage.setLayoutParams(params);
		}
		holder.fileName.setText(file.getName());
		holder.filePath.setText(file.getPath());
//		if (selected.get(position)) {
//			row.setBackgroundColor(context.getResources().getColor(
//					darkTheme ? R.color.highlighted_text_holo_dark
//							: R.color.highlighted_text_holo_light));
//		}else{
//			row.setBackgroundColor(context.getResources().getColor(
//					android.R.color.transparent));
//		}

		return row;
	}

	/**
	 * The class, holding the Views of the Row
	 * 
	 * @author az
	 * 
	 */
	static class FileHolder {
		ImageView fileImage;
		TextView fileName;
		TextView filePath;
	}
}
