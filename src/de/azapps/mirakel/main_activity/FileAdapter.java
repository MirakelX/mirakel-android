package de.azapps.mirakel.main_activity;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakelandroid.R;

public class FileAdapter extends ArrayAdapter<FileMirakel> {
	private static final String TAG = "TaskAdapter";
	private int layoutResourceId;
	MainActivity main;
	List<FileMirakel> data = null;

	public FileAdapter(MainActivity context, int layoutResourceId,
			List<FileMirakel> data) {
		super(context, layoutResourceId, data);
		Log.d(TAG, "created");
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.main = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FileHolder holder = null;

		if (row == null) {
			// Initialize the View
			LayoutInflater inflater = ((Activity) main).getLayoutInflater();
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
		File osFile = new File(file.getPath());
		if (osFile.exists()) {
			Bitmap myBitmap = BitmapFactory
					.decodeFile(osFile.getAbsolutePath());
			if (myBitmap != null) {
				holder.fileImage.setImageBitmap(myBitmap);
				Log.e("Blubb", file.getPath());
			} else {
				LayoutParams params = (LayoutParams) holder.fileImage
						.getLayoutParams();
				params.height = 0;
				// existing height is ok as is, no need to edit it
				holder.fileImage.setLayoutParams(params);
			}

		}
		holder.fileName.setText(file.getName());
		holder.filePath.setText(file.getPath());

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
