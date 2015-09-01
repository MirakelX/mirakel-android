/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.fourmob.datetimepicker.date;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;

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
            final TextViewWithCircularIndicator textViewWithCircularIndicator = (TextViewWithCircularIndicator)
                    super
                    .getView(position, convertView, parent);
            textViewWithCircularIndicator.requestLayout();
            textViewWithCircularIndicator
            .setTextColor(DatePicker.getSelectorColorStates());
            final int year = getYearFromTextView(textViewWithCircularIndicator);
            textViewWithCircularIndicator
            .drawIndicator(YearPickerView.this.mController
                           .getSelectedDay().year == year);
            textViewWithCircularIndicator.setBackgroundColor(getResources()
                    .getColor(android.R.color.transparent));
            if (year == new GregorianCalendar().get(Calendar.YEAR)) {
                textViewWithCircularIndicator
                .setTextColor(YearPickerView.this.mCurrentYear);
            } else if (mSelectedView != null &&
                       String.valueOf(mController.getSelectedDay().year).equals(mSelectedView.getText())) {
                textViewWithCircularIndicator.setTextColor(ThemeManager.getAccentThemeColor());
            } else {
                textViewWithCircularIndicator.setTextColor(ThemeManager.getColor(R.attr.colorTextBlack));
            }
            return textViewWithCircularIndicator;
        }
    }

    protected static int getYearFromTextView(final TextView textView) {
        return Integer.valueOf(textView.getText().toString());
    }

    private YearAdapter mAdapter;
    private final int mChildSize;
    private final DatePickerController mController;
    protected final int mCurrentYear;

    private TextViewWithCircularIndicator mSelectedView;

    private final int mViewSize;

    public YearPickerView(final Context context,
                          final DatePickerController datePickerController) {
        super(context);
        this.mController = datePickerController;
        this.mController.registerOnDateChangedListener(this);
        setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        final Resources resources = context.getResources();
        this.mViewSize = resources
                         .getDimensionPixelOffset(R.dimen.date_picker_view_animator_height);
        this.mChildSize = resources
                          .getDimensionPixelOffset(R.dimen.year_label_height);
        this.mCurrentYear = ThemeManager.getPrimaryDarkThemeColor();
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
        final ArrayList<String> years = new ArrayList<>(mController.getMaxYear() -
                mController.getMinYear());
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
        this.mAdapter.notifyDataSetChanged();
        postSetSelectionCentered(this.mController.getSelectedDay().year
                                 - this.mController.getMinYear());
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view,
                            final int position, final long id) {
        final TextViewWithCircularIndicator textViewWithCircularIndicator = (TextViewWithCircularIndicator)
                view;
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