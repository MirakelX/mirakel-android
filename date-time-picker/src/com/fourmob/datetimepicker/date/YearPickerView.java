package com.fourmob.datetimepicker.date;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.tools.Log;

@SuppressLint("ViewConstructor")
public class YearPickerView extends ListView implements
		AdapterView.OnItemClickListener, DatePicker.OnDateChangedListener {
	private class YearAdapter extends ArrayAdapter<String> {

		public YearAdapter(final Context context, final int resourceId,
				final List<String> years) {
			super(context, resourceId, years);
		}

		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			final TextViewWithCircularIndicator textViewWithCircularIndicator = (TextViewWithCircularIndicator) super
					.getView(position, convertView, parent);
			textViewWithCircularIndicator.requestLayout();
			textViewWithCircularIndicator
					.setTextColor(getResources()
							.getColorStateList(
									YearPickerView.this.mDark ? R.color.date_picker_year_selector_dark
											: R.color.date_picker_year_selector));

			final int year = getYearFromTextView(textViewWithCircularIndicator);
			textViewWithCircularIndicator
					.drawIndicator(YearPickerView.this.mController
							.getSelectedDay().year == year);
			textViewWithCircularIndicator.setBackgroundColor(getResources()
					.getColor(android.R.color.transparent));
			if (year == new GregorianCalendar().get(Calendar.YEAR)) {
				Log.wtf("foo", "current year " + year);
				textViewWithCircularIndicator
						.setTextColor(YearPickerView.this.mCurrentYear);
			}

			return textViewWithCircularIndicator;
		}
	}

	protected static int getYearFromTextView(final TextView textView) {
		return Integer.valueOf(textView.getText().toString()).intValue();
	}

	private YearAdapter mAdapter;
	private final int mChildSize;
	private final DatePickerController mController;
	protected final int mCurrentYear;
	public boolean mDark;

	private TextViewWithCircularIndicator mSelectedView;

	private final int mViewSize;

	public YearPickerView(final Context context,
			final DatePickerController datePickerController) {
		super(context);
		this.mController = datePickerController;
		this.mController.registerOnDateChangedListener(this);
		setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
		final Resources resources = context.getResources();
		this.mDark = MirakelCommonPreferences.isDark();
		this.mViewSize = resources
				.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height);
		this.mChildSize = resources
				.getDimensionPixelOffset(R.dimen.year_label_height);
		this.mCurrentYear = getResources().getColor(
				this.mDark ? R.color.Red : R.color.clock_blue);
		setVerticalFadingEdgeEnabled(true);
		setFadingEdgeLength(this.mChildSize / 3);
		init(context);
		setOnItemClickListener(this);
		setSelector(new StateListDrawable());
		setDividerHeight(0);
		onDateChanged();
	}

	public int getFirstPositionOffset() {
		final View view = getChildAt(0);
		if (view == null) {
			return 0;
		}
		return view.getTop();
	}

	private void init(final Context context) {
		final ArrayList<String> years = new ArrayList<String>();
		for (int year = this.mController.getMinYear(); year <= this.mController
				.getMaxYear(); year++) {
			years.add(String.format("%d", year));
		}
		this.mAdapter = new YearAdapter(context, R.layout.year_label_text_view,
				years);
		setAdapter(this.mAdapter);
	}

	@Override
	public void onDateChanged() {
		Log.d("foo", "data changed");
		this.mAdapter.notifyDataSetChanged();
		postSetSelectionCentered(this.mController.getSelectedDay().year
				- this.mController.getMinYear());
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		this.mController.tryVibrate();
		final TextViewWithCircularIndicator textViewWithCircularIndicator = (TextViewWithCircularIndicator) view;
		if (textViewWithCircularIndicator != null) {
			if (textViewWithCircularIndicator != this.mSelectedView) {
				if (this.mSelectedView != null) {
					this.mSelectedView.drawIndicator(false);
					this.mSelectedView.requestLayout();
				}
				textViewWithCircularIndicator.drawIndicator(true);
				textViewWithCircularIndicator.requestLayout();
				this.mSelectedView = textViewWithCircularIndicator;
			}
			this.mController
					.onYearSelected(getYearFromTextView(textViewWithCircularIndicator));
			this.mAdapter.notifyDataSetChanged();
		}
	}

	public void postSetSelectionCentered(final int position) {
		postSetSelectionFromTop(position, this.mViewSize / 2 - this.mChildSize
				/ 2);
	}

	public void postSetSelectionFromTop(final int position, final int y) {
		post(new Runnable() {
			@Override
			public void run() {
				YearPickerView.this.setSelectionFromTop(position, y);
				YearPickerView.this.requestLayout();
			}
		});
	}
}