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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.recurring.Recurring;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

@SuppressLint("NewApi")
public class RecurrencePickerDialog extends DialogFragment {


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
    protected Spinner mIntervalType;
    protected EditText mIntervalCount;
    protected int mIntervalValue;
    private RadioGroup mRadioGroup;
    private TextView mEndButton;
    @NonNull
    protected Optional<DateTime> mEndDate = absent();
    protected CheckBox mUseExact;
    private boolean mInitialExact;
    private TextView mStartButton;
    @NonNull
    protected Optional<DateTime> mStartDate = absent();
    private Context ctx;

    public int pxToDp(final int px) {
        final Resources resources = this.ctx.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (px / metrics.density);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        onViewStateRestored(savedInstanceState);
        final Context ctx = new ContextThemeWrapper(getActivity(), R.style.MirakelBaseTheme);
        return new AlertDialogWrapper.Builder(ctx)
               .setView(createView(LayoutInflater.from(ctx)))
               .setPositiveButton(android.R.string.ok, new onDoneCallback())
        .setNegativeButton(R.string.remove_reminder, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mCallback != null) {
                    mCallback.onRecurrenceSet(Optional.<Recurring>absent());
                }
            }
        }).create();
    }

    public View createView(final LayoutInflater inflater) {
        this.ctx = new ContextThemeWrapper(getActivity(), R.style.MirakelBaseTheme);
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
                          .inflate(R.layout.recurrencepicker, null);
        boolean isNotTwoRows = view.getMeasuredWidth() > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;

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
            item.setTextAppearance(this.ctx, R.style.TextAppearance_Bold);
            item.setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[day]]
                            .toUpperCase(Helpers.getLocale(this.ctx)));
            item.setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[day]]
                           .toUpperCase(Helpers.getLocale(this.ctx)));
            item.setChecked(weekdays.contains(day + 1));
            item.setTextColor(new ColorStateList(new int[][] {new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[] {ThemeManager.getPrimaryThemeColor(), ThemeManager.getColor(R.attr.colorTextGrey)}));
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
            lp.setMargins(ViewHelper.dpToPx(ctx, 5), ViewHelper.dpToPx(ctx, 5), ViewHelper.dpToPx(ctx, 5),
                          ViewHelper.dpToPx(ctx, 5));
            item.setLayoutParams(lp);
            root.addView(item);
            this.mWeekByDayButtons[day] = item;
        }
        this.mUseExact = (CheckBox) view.findViewById(R.id.recurrence_is_exact);
        this.mUseExact.setChecked(this.mInitialExact);
        mUseExact.setVisibility(mForDue ? View.GONE : View.VISIBLE);



        this.mIntervalType = (Spinner) view.findViewById(R.id.interval_type);
        this.mIntervalCount = (EditText) view.findViewById(R.id.interval_count);
        this.mIntervalCount.setText("1");
        this.mIntervalValue = 1;
        updateIntervalType();
        this.mIntervalCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                int newValue;
                try {
                    newValue = Integer.parseInt(s.toString());
                } catch (final NumberFormatException e) {
                    Log.wtf(TAG, e);
                    newValue = 0;
                }
                if (newValue == 0) {
                    mIntervalCount.setText(String.valueOf(mIntervalValue));
                }
                mIntervalValue = newValue;
                updateIntervalType();

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
                    mIntervalCount.setVisibility(View.GONE);
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
        this.mEndButton = (TextView) view.findViewById(R.id.endButton);
        view.findViewById(R.id.endGroup).setOnClickListener(onEndClicked);
        this.mStartButton = (TextView) view.findViewById(R.id.startButton);
        view.findViewById(R.id.startGroup).setOnClickListener(onStartClicked);

        if (!this.mForDue) {
            this.mUseExact.setVisibility(View.GONE);
        }
        if (this.mRecurring.isPresent() && this.mRecurring.get().isTemporary()) {
            final Recurring recurringRaw = mRecurring.get();
            if (!recurringRaw.getWeekdays().isEmpty()) {
                this.mIntervalType.setSelection(this.mForDue ? 0 : 2);
                this.mIntervalCount.setVisibility(View.VISIBLE);
            } else if (recurringRaw.getInterval().getMinutes() != 0) {
                this.mIntervalType.setSelection(0);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getInterval().getMinutes()));
                this.mIntervalValue = recurringRaw.getInterval().getMinutes();
            } else if (recurringRaw.getInterval().getHours() != 0) {
                this.mIntervalType.setSelection(1);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getInterval().getHours()));
                this.mIntervalValue = recurringRaw.getInterval().getHours();
            } else if (recurringRaw.getInterval().getDays() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 1 : 3);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getInterval().getDays()));
                this.mIntervalValue = recurringRaw.getInterval().getDays();
            } else if (recurringRaw.getInterval().getMonths() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 2 : 4);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getInterval().getMonths()));
                this.mIntervalValue = recurringRaw.getInterval().getMonths();
            } else if (recurringRaw.getInterval().getYears() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 3 : 5);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getInterval().getYears()));
                this.mIntervalValue = recurringRaw.getInterval().getYears();
            }
            this.mStartDate = recurringRaw.getStartDate();
            this.mEndDate = recurringRaw.getEndDate();
        }
        mStartButton.setText(getDateText(mStartDate));
        mEndButton.setText(getDateText(mEndDate));
        return view;
    }

    private CharSequence getDateText(final @Nullable Optional<DateTime> c) {
        if (c.isPresent()) {
            return DateTimeHelper.formatDate(ctx, c);
        }
        return ctx.getText(R.string.never);
    }


    protected void updateIntervalType() {
        final ArrayAdapter<CharSequence> adapterInterval = new ArrayAdapter<CharSequence>(
            getActivity(), R.layout.simple_list_item_1,
            getDayYearValues());
        adapterInterval
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final int pos = this.mIntervalType.getSelectedItemPosition();
        this.mIntervalType.setAdapter(adapterInterval);
        this.mIntervalType.setSelection(pos);
    }

    protected String[] getDayYearValues() {
        final Context dialogContext = getActivity();
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

    private final OnClickListener onStartClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final SupportDatePickerDialog dp = SupportDatePickerDialog.newInstance(
            new OnDateSetListener() {
                @Override
                public void onDateSet(
                    final DatePicker datePickerDialog, final @NonNull Optional<LocalDate> newDate) {
                    if (newDate.isPresent()) {
                        mStartDate = of(newDate.get().toDateTimeAtStartOfDay());
                        if (mEndDate.isPresent() && mEndDate.get().isBefore(mStartDate.get())) {
                            mEndDate = absent();
                            mEndButton.setText(getDateText(mEndDate));
                        }
                    } else {
                        mStartDate = absent();
                    }
                    mStartButton.setText(getDateText(mStartDate));
                }
            }, mStartDate);
            dp.show(getFragmentManager(), "startDate");
        }
    };

    private final static String RECURRING = "recurring";
    private final static String IS_WEEKDAYS = "weekdays";
    private final static String IS_DUE = "due";

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RECURRING, getRecurring());
        outState.putBoolean(IS_DUE, mForDue);
        outState.putBoolean(IS_WEEKDAYS, ((mForDue ? 2 : 0) + mIntervalType
                                          .getSelectedItemPosition()) == 2);

    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mRecurring = fromNullable(savedInstanceState.<Recurring>getParcelable(RECURRING));
            mForDue = savedInstanceState.getBoolean(IS_DUE);
            if (savedInstanceState.getBoolean(IS_WEEKDAYS) && (mIntervalType != null)) {
                mIntervalType.setSelection((mForDue ? 0 : 2));
            }
        }
    }

    private final OnClickListener onEndClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final SupportDatePickerDialog dp = SupportDatePickerDialog.newInstance(
            new OnDateSetListener() {

                @Override
                public void onDateSet(
                    final DatePicker datePickerDialog, final @NonNull Optional<LocalDate> newDate) {
                    if (mEndDate.isPresent()) {
                        mEndDate = of(newDate.get().toDateTimeAtCurrentTime());
                        if (!mStartDate.or(new DateTime()).isBefore(mEndDate.get())) {
                            mEndDate = mStartDate;
                        }
                    } else {
                        mEndDate = absent();
                    }
                    mEndButton.setText(getDateText(mEndDate));
                }
            }, mEndDate);
            dp.show(getFragmentManager(), "startDate");
        }
    };



    private class onDoneCallback implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mCallback != null) {
                mCallback.onRecurrenceSet(of(getRecurring()));

            }
        }
    }

    @NonNull
    private Recurring getRecurring() {
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
        return Recurring.newRecurring("", new Period(intervalYears, intervalMonths, 0, intervalDays,
                                      intervalHours, intervalMinutes, 0, 0)
                                      , mForDue, mStartDate, mEndDate, true, mUseExact.isChecked(), checked);
    }
}
