package de.azapps.mirakel.helper;

import de.azapps.mirakel.model.list.ListMirakel;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.View;

public class ModellHelper {

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static void setListColorBackground(ListMirakel list, View row, int w) {

		int color;
		if (list == null) {
			color = 0;
		} else {
			color = list.getColor();
		}
		if (color != 0) {
			if (MirakelCommonPreferences.isDark()) {
				color ^= 0x66000000;
			} else {
				color ^= 0xCC000000;
			}
		}
		ShapeDrawable mDrawable = new ShapeDrawable(new RectShape());
		mDrawable.getPaint().setShader(
				new LinearGradient(0, 0, w / 4, 0, color, Color
						.parseColor("#00FFFFFF"), Shader.TileMode.CLAMP));
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			row.setBackground(mDrawable);
		} else {
			row.setBackgroundDrawable(mDrawable);
		}
	}

}
