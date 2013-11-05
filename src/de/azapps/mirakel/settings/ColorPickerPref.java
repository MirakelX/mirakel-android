package de.azapps.mirakel.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;

import de.azapps.mirakel.helper.Log;
import de.azapps.mirakelandroid.R;

public class ColorPickerPref extends DialogPreference {
	private static final String TAG = "NumPickerPref";
	private Context ctx;
	private int COLOR;
	private int OLD_COLOR;
	private View colorBox;

	public ColorPickerPref(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		COLOR = ctx.getResources().getColor(R.color.holo_orange_dark);
		OLD_COLOR = COLOR;
	}

	@SuppressLint("NewApi")
	@Override
	public View getView(View convertView, ViewGroup parent) {
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB){
			return new View(ctx);
		}
		View v = ((Activity) ctx).getLayoutInflater().inflate(
				R.layout.color_pref, null);
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			v.setBackground(ctx.getResources().getDrawable(
					android.R.attr.selectableItemBackground));
		}*/
		colorBox = v.findViewById(R.id.color_box);
		colorBox.setBackgroundColor(COLOR);
		((TextView) v.findViewById(android.R.id.title)).setText(getTitle());
		v.findViewById(android.R.id.summary).setVisibility(View.GONE);
		return v;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		// super.onPrepareDialogBuilder(builder);
		final View v = ((LayoutInflater) ctx
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.color_picker, null);
		final ColorPicker cp = ((ColorPicker) v.findViewById(R.id.color_picker));
		final SVBar op = ((SVBar) v.findViewById(R.id.svbar_color_picker));
		cp.addSVBar(op);
		cp.setColor(COLOR);
		cp.setOldCenterColor(OLD_COLOR);
		builder.setView(v)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								COLOR = cp.getColor();
								colorBox.setBackgroundColor(cp.getColor());
								callChangeListener(COLOR);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Nothing
							}
						});
	}

	public int getColor() {
		return COLOR;
	}

	public void setColor(int newValue) {
		COLOR = newValue;
	}

	public void setOldColor(int newValue) {
		OLD_COLOR = newValue;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		Log.d(TAG, a.getString(index));
		return super.onGetDefaultValue(a, index);
	}

	@Override
	public void onBindDialogView(View view) {
		Log.d(TAG, "bar");
		super.onBindDialogView(view);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			persistBoolean(!getPersistedBoolean(true));
		}
	}

}
