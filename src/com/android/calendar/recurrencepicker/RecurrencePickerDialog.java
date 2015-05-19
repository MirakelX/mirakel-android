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

package com.android.calendar.recurrencepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.recurring.Recurring;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

@SuppressLint("NewApi")
public class RecurrencePickerDialog extends DialogFragment implements
    OnCheckedChangeListener {

    public RecurrencePickerDialog() {
        super();
    }

    // in dp's
    private static final int MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK = 450;
    protected static final String TAG = "RecurrencePickerDialog";

    public static RecurrencePickerDialog newInstance(
        final OnRecurrenceSetListener r, final Optional<Recurring> recurring,
        final boolean forDue, final boolean exact) {
        final RecurrencePickerDialog re = new RecurrencePickerDialog();
        re.initialize(r, recurring, forDue, exact);
        return re;
    }

    @Nullable
    protected OnRecurrenceSetListener mCallback;
    @NonNull
    protected Optional<Recurring> mRecurring;
    protected boolean mForDue;
    protected int extraItems;
    private Button mDoneButton;
    private Button mCancel;
    private Button mNoRecurring;
    protected int mPosition;
    protected final ToggleButton[] mWeekByDayButtons = new ToggleButton[7];
    private int numOfButtonsInRow1;

    public void initialize(final OnRecurrenceSetListener r,
                           final Optional<Recurring> recurring, final boolean forDue, final boolean exact) {
        this.mRecurring = recurring;
        this.mCallback = r;
        this.mForDue = forDue;
        this.mInitialExact = exact;
        this.mStartDate = absent();
        this.mEndDate = absent();
    }

    private final static int[] TIME_DAY_TO_CALENDAR_DAY = new int[] { Calendar.SUNDAY,
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY,
                                                                    };
    private LinearLayout mWeekGroup;
    private LinearLayout mWeekGroup2;
    protected LinearLayout mOptions;
    protected Spinner mIntervalType;
    protected EditText mIntervalCount;
    protected int mIntervalValue;
    private RadioGroup mRadioGroup;
    private Spinner mEndSpinner;
    @NonNull
    protected Optional<Calendar> mEndDate = absent();
    protected TextView mEndDateView;
    protected CheckBox mUseExact;
    private boolean mInitialExact;
    private Spinner mStartSpinner;
    @NonNull
    protected Optional<Calendar> mStartDate = absent();
    protected TextView mStartDateView;
    private Context ctx;

    public int pxToDp(final int px) {
        final Resources resources = this.ctx.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (px / metrics.density);
    }

    public int dpToPx(final int dp) {
        final Resources resources = this.ctx.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * metrics.density);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.ctx = getDialog().getContext();
        try {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (final RuntimeException e) {
            Log.wtf(TAG, "failed to create dialog", e);
        }
        final List<Pair<Integer, String>> recurring = Recurring
                .getForDialog(this.mForDue);
        this.extraItems = 1;
        final CharSequence[] items = new String[recurring.size()
                                                + this.extraItems];
        // there is a button...
        items[0] = this.ctx.getString(R.string.recurrence_custom);
        this.mPosition = 0;
        for (int i = this.extraItems; i < (recurring.size() + this.extraItems); i++) {
            items[i] = recurring.get(i - this.extraItems).second;
            if (this.mRecurring.isPresent()
                && items[i].equals(this.mRecurring.get().getLabel())) {
                this.mPosition = i;
            }
        }
        final View view = inflater
                          .inflate(R.layout.recurrencepicker, container);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            final View v = view.findViewById(R.id.recurrence_picker_dialog);
            v.setBackgroundColor(ThemeManager.getColor(R.attr.colorBackground));
        }
        final Resources res = this.ctx.getResources();
        boolean isNotTwoRows;
        try {
            isNotTwoRows = res.getConfiguration().screenWidthDp > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;
        } catch (final NoSuchFieldError e) {
            isNotTwoRows = pxToDp(((WindowManager) this.ctx
                                   .getSystemService(Context.WINDOW_SERVICE))
                                  .getDefaultDisplay().getWidth()) > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;
        }
        this.mOptions = (LinearLayout) view.findViewById(R.id.options);
        this.mWeekGroup = (LinearLayout) view.findViewById(R.id.weekGroup);
        this.mWeekGroup2 = (LinearLayout) view.findViewById(R.id.weekGroup2);

        final String[] dayOfWeekString = new DateFormatSymbols()
        .getShortWeekdays();
        if (isNotTwoRows) {
            this.numOfButtonsInRow1 = 7;
            this.mWeekGroup2.setVisibility(View.GONE);
        } else {
            this.numOfButtonsInRow1 = 4;
            this.mWeekGroup2.setVisibility(View.VISIBLE);
        }
        List<Integer> weekdays = new ArrayList<>();
        this.mWeekGroup.removeAllViews();
        this.mWeekGroup2.removeAllViews();
        if (this.mRecurring.isPresent()) {
            weekdays = this.mRecurring.get().getWeekdays();
        }
        final int startDay = DateTimeHelper.getFirstDayOfWeek();
        for (int i = startDay; i < (startDay + 7); i++) {
            final int day = i % 7;
            // Create Button
            final WeekButton item = new WeekButton(this.ctx);
            item.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                  LayoutParams.WRAP_CONTENT));
            item.setGravity(Gravity.CENTER);
            item.setTextSize(12.0F);
            item.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            item.setTypeface(item.getTypeface(), Typeface.BOLD);
            item.setSingleLine(true);
            item.setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[day]]
                            .toUpperCase(Helpers.getLocal(this.ctx)));
            item.setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[day]]
                           .toUpperCase(Helpers.getLocal(this.ctx)));
            item.setChecked(weekdays.contains(day + 1));
            // Add to view
            final ViewGroup root;
            if ((i - startDay) >= this.numOfButtonsInRow1) {
                root = this.mWeekGroup2;
            } else {
                root = this.mWeekGroup;
            }
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dpToPx(8), dpToPx(8), 0, 0);
            item.setLayoutParams(lp);
            root.addView(item);
            this.mWeekByDayButtons[day] = item;
        }
        if (this.mPosition != 0) {
            this.mOptions.setVisibility(View.GONE);
        }
        this.mUseExact = (CheckBox) view.findViewById(R.id.recurrence_is_exact);
        Log.w(TAG, "exact: " + this.mInitialExact);
        this.mUseExact.setChecked(this.mInitialExact);
        mUseExact.setVisibility(mForDue ? View.GONE : View.VISIBLE);

        this.mDoneButton = (Button) view.findViewById(R.id.done);
        this.mDoneButton.setOnClickListener(new onDoneCallback());

        this.mCancel = (Button) view.findViewById(R.id.cancel);
        this.mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                dismiss();
            }
        });

        this.mNoRecurring = (Button) view.findViewById(R.id.not_recurring);
        this.mNoRecurring.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mCallback != null) {
                    mCallback.onRecurrenceSet(Optional.<Recurring>absent());
                }
                dismiss();
            }
        });


        this.mIntervalType = (Spinner) view.findViewById(R.id.interval_type);
        this.mIntervalCount = (EditText) view.findViewById(R.id.interval_count);
        this.mIntervalCount.setText("1");
        this.mIntervalValue = 1;
        updateIntervalType();
        this.mIntervalCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                int newValue = mIntervalValue;
                try {
                    newValue = Integer.parseInt(s.toString());
                } catch (final NumberFormatException e) {
                    Log.wtf(TAG, e);
                    throw new IllegalArgumentException("Cannot happen", e);
                }
                if (newValue == 0) {
                    mIntervalCount.setText(String.valueOf(mIntervalValue));
                }
                updateIntervalType();
                mIntervalValue = newValue;
            }
            @Override
            public void beforeTextChanged(final CharSequence s,
                                          final int start, final int count, final int after) {
            }
            @Override
            public void afterTextChanged(final Editable s) {
            }
        });
        final int weekdayPosition = this.mForDue ? 0 : 2;
        this.mIntervalType
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent,
                                       final View v, final int position, final long id) {
                if (position == weekdayPosition) {
                    view.findViewById(R.id.weekGroup).setVisibility(
                        View.VISIBLE);
                    view.findViewById(R.id.weekGroup2).setVisibility(
                        View.VISIBLE);
                    mIntervalCount.setVisibility(View.INVISIBLE);
                } else {
                    view.findViewById(R.id.weekGroup).setVisibility(
                        View.GONE);
                    view.findViewById(R.id.weekGroup2).setVisibility(
                        View.GONE);
                    mIntervalCount.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // nothing
            }
        });
        this.mRadioGroup = (RadioGroup) view.findViewById(R.id.monthGroup);
        this.mRadioGroup.setVisibility(View.GONE);// Don't support this for
        // now...
        final String[] end = {
            res.getString(R.string.recurrence_end_continously),
            res.getString(R.string.recurrence_end_date_label) // ,res.getString(R.string.recurrence_end_count_label)
            // Dont
            // support
            // this
            // now...*/
            // };
        };
        final Calendar endDate = new GregorianCalendar();
        endDate.add(Calendar.MONTH, 1);
        this.mEndSpinner = (Spinner) view.findViewById(R.id.endSpinner);
        final ArrayAdapter<CharSequence> endSpinnerAdapter = new ArrayAdapter<CharSequence>(
            this.ctx, android.R.layout.simple_spinner_item, end);
        endSpinnerAdapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mEndDateView = (TextView) view
                            .findViewById(R.id.endDate);
        this.mEndDateView.setText(DateTimeHelper.formatDate(endDate));
        this.mEndDateView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final SupportDatePickerDialog dp = SupportDatePickerDialog.newInstance(
                new OnDateSetListener() {
                    @Override
                    public void onNoDateSet() {
                        mEndDate = absent();
                    }

                    @Override
                    public void onDateSet(
                        final DatePicker datePickerDialog,
                        final int year, final int month,
                        final int day) {
                        if (!mEndDate.isPresent()) {
                            mEndDate = of((Calendar) new GregorianCalendar());
                        }
                        final Calendar endDate = mEndDate.get();
                        endDate.set(Calendar.YEAR, year);
                        endDate.set(Calendar.MONTH, month);
                        endDate.set(Calendar.DAY_OF_MONTH, day);
                        mEndDateView
                        .setText(DateTimeHelper
                                 .formatDate(
                                     getActivity(),
                                     mEndDate));
                    }
                }, endDate.get(Calendar.YEAR), endDate
                .get(Calendar.MONTH), endDate
                .get(Calendar.DAY_OF_MONTH), false);
                dp.show(getFragmentManager(), "endDate");
            }
        });
        this.mEndSpinner
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent,
                                       final View view, final int position, final long id) {
                mEndDate = handleDate(position, mEndDateView, mEndDate, mStartDate, false);
            }
            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // nothing
            }
        });
        this.mEndSpinner.setAdapter(endSpinnerAdapter);
        final String[] start = { res.getString(R.string.recurrence_from_now), res.getString(R.string.recurrence_from) };
        this.mStartSpinner = (Spinner) view.findViewById(R.id.startSpinner);
        this.mStartSpinner
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent,
                                       final View view, final int position, final long id) {
                mStartDate = handleDate(position, mStartDateView, mStartDate, mEndDate, true);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // nothing
            }
        });
        final ArrayAdapter<CharSequence> startSpinnerAdapter = new ArrayAdapter<CharSequence>(
            this.ctx, android.R.layout.simple_spinner_item, start);
        startSpinnerAdapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mStartDateView = (TextView) view
                              .findViewById(R.id.startDate);
        final Calendar startDate = new GregorianCalendar();
        this.mStartDateView.setText(DateTimeHelper.formatDate(startDate));
        this.mStartDateView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final SupportDatePickerDialog dp = SupportDatePickerDialog.newInstance(
                new OnDateSetListener() {
                    @Override
                    public void onNoDateSet() {
                        mStartDate = absent();
                        // cannot happen
                    }
                    @Override
                    public void onDateSet(
                        final DatePicker datePickerDialog,
                        final int year, final int month,
                        final int day) {
                        if (!mStartDate.isPresent()) {
                            mStartDate = of((Calendar) new GregorianCalendar());
                        }
                        final Calendar startDate = mStartDate.get();
                        startDate.set(Calendar.YEAR, year);
                        startDate.set(Calendar.MONTH, month);
                        startDate.set(Calendar.DAY_OF_MONTH, day);
                        mStartDateView
                        .setText(DateTimeHelper.formatDate(
                                     getActivity(),
                                     mStartDate));
                    }
                }, startDate.get(Calendar.YEAR), startDate
                .get(Calendar.MONTH), startDate
                .get(Calendar.DAY_OF_MONTH), false);
                dp.show(getFragmentManager(), "startDate");
            }
        });
        this.mStartSpinner.setAdapter(startSpinnerAdapter);
        if (!this.mForDue) {
            this.mUseExact.setVisibility(View.GONE);
        }
        if (this.mRecurring.isPresent() && this.mRecurring.get().isTemporary()) {
            final Recurring recurringRaw = mRecurring.get();
            if (!recurringRaw.getWeekdays().isEmpty()) {
                this.mIntervalType.setSelection(this.mForDue ? 0 : 2);
                this.mIntervalCount.setVisibility(View.VISIBLE);
            } else if (recurringRaw.getMinutes() != 0) {
                this.mIntervalType.setSelection(0);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getMinutes()));
                this.mIntervalValue = recurringRaw.getMinutes();
            } else if (recurringRaw.getHours() != 0) {
                this.mIntervalType.setSelection(1);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getHours()));
                this.mIntervalValue = recurringRaw.getHours();
            } else if (recurringRaw.getDays() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 1 : 3);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getDays()));
                this.mIntervalValue = recurringRaw.getDays();
            } else if (recurringRaw.getMonths() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 2 : 4);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getMonths()));
                this.mIntervalValue = recurringRaw.getMonths();
            } else if (recurringRaw.getYears() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 3 : 5);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getYears()));
                this.mIntervalValue = recurringRaw.getYears();
            }
            this.mStartDate = recurringRaw.getStartDate();
            if (this.mStartDate.isPresent()) {
                this.mStartSpinner.setSelection(1);
                this.mStartDateView.setText(DateTimeHelper.formatDate(
                                                getActivity(), this.mStartDate));
            }
            this.mEndDate = recurringRaw.getEndDate();
            if (this.mEndDate.isPresent()) {
                this.mEndSpinner.setSelection(1);
                this.mEndDateView.setText(DateTimeHelper.formatDate(
                                              getActivity(), this.mEndDate));
            }
        }
        return view;
    }

    private Optional<Calendar> handleDate(final int position, final @NonNull TextView view,
                                          final @NonNull Optional<Calendar> handleDate, final @NonNull Optional<Calendar> defaultDate,
                                          boolean isStart) {
        Optional<Calendar> ret = handleDate;
        if (position == 1) {
            view.setVisibility(View.VISIBLE);
            if (!ret.isPresent()) {
                ret = defaultDate;
                if (!ret.isPresent()) {
                    ret = of((Calendar) new GregorianCalendar());
                } else {
                    ret.get().add(Calendar.MONTH, isStart ? -1 : 1);
                }
            }
        } else {
            view.setVisibility(View.GONE);
            ret = absent();
        }
        view.setText(DateTimeHelper
                     .formatDate(getActivity(),
                                 ret));
        return ret;
    }


    protected void updateIntervalType() {
        final ArrayAdapter<CharSequence> adapterInterval = new ArrayAdapter<CharSequence>(
            getDialog().getContext(), android.R.layout.simple_spinner_item,
            getDayYearValues());
        adapterInterval
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final int pos = this.mIntervalType.getSelectedItemPosition();
        this.mIntervalType.setAdapter(adapterInterval);
        this.mIntervalType.setSelection(pos);
    }

    protected String[] getDayYearValues() {
        final Context dialogContext = getDialog().getContext();
        final int size = this.mForDue ? 4 : 6;
        int i = 0;
        final String[] ret = new String[size];
        if (!this.mForDue) {
            ret[i++] = dialogContext.getResources().getQuantityString(
                           R.plurals.due_minute, this.mIntervalValue);
            ret[i++] = dialogContext.getResources().getQuantityString(R.plurals.due_hour,
                       this.mIntervalValue);
        }
        ret[i++] = dialogContext.getResources().getString(R.string.weekday);
        ret[i++] = dialogContext.getResources().getQuantityString(R.plurals.due_day,
                   this.mIntervalValue);
        ret[i++] = dialogContext.getResources().getQuantityString(R.plurals.due_month,
                   this.mIntervalValue);
        ret[i] = dialogContext.getResources().getQuantityString(R.plurals.due_year,
                 this.mIntervalValue);
        return ret;
    }

    public interface OnRecurrenceSetListener {
        void onRecurrenceSet(@NonNull final Optional<Recurring> r);

    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView,
                                 final boolean isChecked) {
        // nothing
    }

    private class onDoneCallback implements OnClickListener {
        @Override
        public void onClick(final View v) {
            final SparseBooleanArray checked = new SparseBooleanArray();
            final int type = (mForDue ? 2 : 0) + mIntervalType
                             .getSelectedItemPosition();
            for (int i = 0; i < mWeekByDayButtons.length; i++) {
                if (mWeekByDayButtons[i]
                    .isChecked()) {
                    checked.put(TIME_DAY_TO_CALENDAR_DAY[i], (type == 2) && mWeekByDayButtons[i].isChecked());
                }
            }

            Log.d(TAG, "TYPE: " + type);
            int intervalMonths = 0;
            int intervalYears = 0;
            int intervalDays = 0;
            int intervalHours = 0;
            int intervalMinutes = 0;
            switch (type) {
            case 0:
                intervalMinutes = mIntervalValue;
                break;
            case 1:
                intervalHours = mIntervalValue;
                break;
            case 2:
                //weekdays, handled above
                break;
            case 3:
                intervalDays = mIntervalValue;
                break;
            case 4:
                intervalMonths = mIntervalValue;
                break;
            case 5:
                intervalYears = mIntervalValue;
                break;
            default:
                throw new IllegalStateException("Implement another case of intervall type");
            }
            if (mCallback != null) {
                final Recurring r = Recurring.newRecurring("", intervalMinutes, intervalHours, intervalDays,
                                    intervalMonths, intervalYears, mForDue, mStartDate, mEndDate, true, mUseExact.isChecked(), checked);
                mCallback.onRecurrenceSet(of(r));

            }

            dismiss();
        }
    }
}
