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
package de.azapps.mirakel.helper;

import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class Helpers {
	private static String TAG = "Helpers";

	// Contact
	public static void contact(Context context) {
		String mirakelVersion = "unknown";
		try {
			mirakelVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "could not get version name from manifest!");
			e.printStackTrace();
		}
		contact(context, context.getString(R.string.contact_subject),
				context.getString(R.string.contact_text, mirakelVersion,
						android.os.Build.VERSION.SDK_INT,
						android.os.Build.DEVICE));
	}

	public static void contact(Context context, String subject, String content) {

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL,
				new String[] { context.getString(R.string.contact_email) });
		i.putExtra(Intent.EXTRA_SUBJECT, subject);
		i.putExtra(Intent.EXTRA_TEXT, content);
		try {
			Intent ci = Intent.createChooser(i,
					context.getString(R.string.contact_chooser));
			ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(ci);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(context,
					context.getString(R.string.contact_no_client),
					Toast.LENGTH_SHORT).show();
		}
	}

	// Help
	public static void openHelp(Context ctx) {
		openHelp(ctx, null);
	}

	public static void openHelp(Context ctx, String title) {
		String url = "http://mirakel.azapps.de/help_en.html";
		if (title != null)
			url += "#" + title;
		openURL(ctx, url);
	}

	public static void openURL(Context ctx, String url) {
		Intent i2 = new Intent(Intent.ACTION_VIEW);
		i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i2.setData(Uri.parse(url));
		ctx.startActivity(i2);
	}

	// MISC

	/**
	 * Wrapper-Class
	 * 
	 * @author az
	 * 
	 */
	public interface ExecInterface {
		public void exec();
	}
	public interface ExecInterfaceWithTask {
		public void exec(Task task);
	}

	public static void showFileChooser(int code, String title, Activity activity) {

		Intent fileDialogIntent = new Intent(Intent.ACTION_GET_CONTENT);
		fileDialogIntent.setType("*/*");
		fileDialogIntent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			activity.startActivityForResult(
					Intent.createChooser(fileDialogIntent, title), code);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(activity, R.string.no_filemanager,
					Toast.LENGTH_SHORT).show();
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static void setListColorBackground(ListMirakel list, View row,
			boolean darkTheme, int w) {

		int color;
		if (list == null)
			color = 0;
		else
			color = list.getColor();
		if (color != 0) {
			if (darkTheme) {
				color ^= 0x66000000;
			} else {
				color ^= 0xCC000000;
			}
		}
		ShapeDrawable mDrawable = new ShapeDrawable(new RectShape());
		mDrawable.getPaint().setShader(
				new LinearGradient(0, 0, w / 4, 0, color, Color
						.parseColor("#00FFFFFF"), Shader.TileMode.CLAMP));
		if (android.os.Build.VERSION.SDK_INT >= 16)
			row.setBackground(mDrawable);
		else
			row.setBackgroundDrawable(mDrawable);
	}

	/*
	 * Scaling down the image
	 * "Source: http://www.androiddevelopersolution.com/2012/09/bitmap-how-to-scale-down-image-for.html"
	 */
	public static Bitmap getScaleImage(Bitmap bitmap, float boundBoxInDp,
			int rotate) {

		// Get current dimensions
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		// Determine how much to scale: the dimension requiring
		// less scaling is.
		// closer to the its side. This way the image always
		// stays inside your.
		// bounding box AND either x/y axis touches it.
		float xScale = ((float) boundBoxInDp) / width;
		float yScale = ((float) boundBoxInDp) / height;
		float scale = (xScale <= yScale) ? xScale : yScale;

		// Create a matrix for the scaling and add the scaling data
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		// matrix.postRotate(rotate);

		// Create a new bitmap and convert it to a format understood

		// by the
		// ImageView
		Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, false);

		// Apply the scaled bitmap
		return scaledBitmap;

	}

	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static Locale getLocal(Context ctx) {
		String current = MirakelPreferences.getLanguage();
		return current.equals("-1") ? Locale.getDefault() : new Locale(current);

	}

}
