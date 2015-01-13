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

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;

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
        final boolean forDue, final boolean dark, final boolean exact) {
        final RecurrencePickerDialog re = new RecurrencePickerDialog();
        re.initialize(r, recurring, forDue, dark, exact);
        return re;
    }

    protected OnRecurrenceSetListener mCallback;
    protected Optional<Recurring> mRecurring;
    protected boolean mForDue;
    private Spinner mRecurrenceSelection;
    protected int extraItems;
    protected CompoundButton mToggle;
    private boolean toggleIsSwitch = true;
    private Button mDoneButton;
    protected int mPosition;
    protected boolean mDark;
    protected final ToggleButton[] mWeekByDayButtons = new ToggleButton[7];
    private int numOfButtonsInRow1;

    public void initialize(final OnRecurrenceSetListener r,
                           final Optional<Recurring> recurring, final boolean forDue,
                           final boolean dark, final boolean exact) {
        this.mRecurring = recurring;
        this.mCallback = r;
        this.mForDue = forDue;
        this.mDark = dark;
        this.mInitialExact = exact;
        this.mStartDate = absent();
        this.mEndDate = absent();
    }

    private final int[] TIME_DAY_TO_CALENDAR_DAY = new int[] { Calendar.SUNDAY,
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
    protected boolean mIsCustom = false;
    protected CheckBox mUseExact;
    private boolean mInitialExact;
    private Spinner mStartSpinner;
    @NonNull
    protected Optional<Calendar> mStartDate = absent();
    protected TextView mStartDateView;
    private Context ctx;
    protected boolean mIsWeekDay;

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
            v.setBackgroundColor(this.ctx.getResources().getColor(
                                     this.mDark ? android.R.color.black : android.R.color.white));
        }
        this.mRecurrenceSelection = (Spinner) view
                                    .findViewById(R.id.freqSpinner);
        final Resources res = this.ctx.getResources();
        boolean isNotTwoRows;
        try {
            isNotTwoRows = res.getConfiguration().screenWidthDp > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;
        } catch (final NoSuchFieldError e) {
            isNotTwoRows = pxToDp(((WindowManager) this.ctx
                                   .getSystemService(Context.WINDOW_SERVICE))
                                  .getDefaultDisplay().getWidth()) > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;
        }
        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
            this.ctx, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mOptions = (LinearLayout) view.findViewById(R.id.options);
        this.mRecurrenceSelection.setAdapter(adapter);
        this.mRecurrenceSelection.setSelection(this.mPosition);
        this.mWeekGroup = (LinearLayout) view.findViewById(R.id.weekGroup);
        this.mWeekGroup2 = (LinearLayout) view.findViewById(R.id.weekGroup2);
        this.mRecurrenceSelection
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent,
                                       final View view, final int position, final long id) {
                RecurrencePickerDialog.this.mPosition = position;
                if (position < RecurrencePickerDialog.this.extraItems) {
                    switch (position) {
                    case 0:// CUSTOM
                        RecurrencePickerDialog.this.mOptions
                        .setVisibility(View.VISIBLE);
                        RecurrencePickerDialog.this.mIsCustom = true;
                        break;
                    default:
                        Log.wtf(TAG, "cannot be");
                        break;
                    }
                } else {
                    RecurrencePickerDialog.this.mIsCustom = false;
                    RecurrencePickerDialog.this.mOptions
                    .setVisibility(View.GONE);
                    RecurrencePickerDialog.this.mRecurring = Recurring.get(recurring.get(position -
                            RecurrencePickerDialog.this.extraItems).first);
                }
            }
            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                //nothing
            }
        });
        final String[] dayOfWeekString = new DateFormatSymbols()
        .getShortWeekdays();
        if (isNotTwoRows) {
            this.numOfButtonsInRow1 = 7;
            this.mWeekGroup2.setVisibility(View.GONE);
        } else {
            this.numOfButtonsInRow1 = 4;
            this.mWeekGroup2.setVisibility(View.VISIBLE);
        }
        List<Integer> weekdays = new ArrayList<Integer>();
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
            item.setTextSize(12);
            item.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            item.setTypeface(item.getTypeface(), Typeface.BOLD);
            item.setSingleLine(true);
            item.setTextOff(dayOfWeekString[this.TIME_DAY_TO_CALENDAR_DAY[day]]
                            .toUpperCase(Helpers.getLocal(this.ctx)));
            item.setTextOn(dayOfWeekString[this.TIME_DAY_TO_CALENDAR_DAY[day]]
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
        this.mToggle = (CompoundButton) view.findViewById(R.id.repeat_switch);
        if (this.mToggle == null) {
            this.mToggle = (CheckBox) view.findViewById(R.id.repeat_checkbox);
            this.toggleIsSwitch = false;
        }
        this.mToggle.setChecked(this.mRecurring.isPresent()
                                && (this.mRecurring.get().getId() != -1));
        this.mToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                                         final boolean isChecked) {
                setEnabledComponents(isChecked);
            }
        });
        this.mDoneButton = (Button) view.findViewById(R.id.done);
        this.mDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (RecurrencePickerDialog.this.mToggle.isChecked()) {
                    if (RecurrencePickerDialog.this.mIsCustom) {
                        final ArrayList<Integer> checked = new ArrayList<>();
                        boolean isOneChecked = false;
                        for (int i = 0; i < RecurrencePickerDialog.this.mWeekByDayButtons.length; i++) {
                            if (RecurrencePickerDialog.this.mWeekByDayButtons[i]
                                .isChecked()) {
                                isOneChecked = true;
                                checked.add(RecurrencePickerDialog.this.TIME_DAY_TO_CALENDAR_DAY[i]);
                            }
                        }
                        if (isOneChecked
                            && RecurrencePickerDialog.this.mIsWeekDay) {
                            RecurrencePickerDialog.this.mCallback
                            .onCustomRecurrenceSetWeekdays(
                                RecurrencePickerDialog.this.mForDue,
                                checked,
                                RecurrencePickerDialog.this.mStartDate,
                                RecurrencePickerDialog.this.mEndDate,
                                RecurrencePickerDialog.this.mUseExact
                                .isChecked());
                        } else {
                            final int type = RecurrencePickerDialog.this.mIntervalType
                                             .getSelectedItemPosition();
                            Log.d(TAG, "TYPE: " + type);
                            int intervalMonths = 0;
                            int intervalYears = 0;
                            int intervalDays = 0;
                            int intervalHours = 0;
                            int intervalMinutes = 0;
                            if (type == 0) {
                                if (RecurrencePickerDialog.this.mForDue) {
                                    intervalDays = RecurrencePickerDialog.this.mIntervalValue;
                                } else {
                                    intervalMinutes = RecurrencePickerDialog.this.mIntervalValue;
                                }
                            } else if (type == 1) {
                                if (RecurrencePickerDialog.this.mForDue) {
                                    intervalMonths = RecurrencePickerDialog.this.mIntervalValue;
                                } else {
                                    intervalHours = RecurrencePickerDialog.this.mIntervalValue;
                                }
                            } else if (type == 2) {
                                if (RecurrencePickerDialog.this.mForDue) {
                                    intervalYears = RecurrencePickerDialog.this.mIntervalValue;
                                } else {
                                    intervalDays = RecurrencePickerDialog.this.mIntervalValue;
                                }
                            } else if (type == 3) {
                                intervalMonths = RecurrencePickerDialog.this.mIntervalValue;
                            } else if (type == 4) {
                                intervalYears = RecurrencePickerDialog.this.mIntervalValue;
                            }
                            RecurrencePickerDialog.this.mCallback
                            .onCustomRecurrenceSetInterval(
                                RecurrencePickerDialog.this.mForDue,
                                intervalYears,
                                intervalMonths,
                                intervalDays,
                                intervalHours,
                                intervalMinutes,
                                RecurrencePickerDialog.this.mStartDate,
                                RecurrencePickerDialog.this.mEndDate,
                                RecurrencePickerDialog.this.mUseExact
                                .isChecked());
                        }
                    } else {
                        final Recurring r = Recurring.createTemporaryCopy(RecurrencePickerDialog.this.mRecurring.orNull());
                        r.setExact(RecurrencePickerDialog.this.mUseExact
                                   .isChecked());
                        Log.d(TAG, "exact: " + r.isExact());
                        r.save();
                        RecurrencePickerDialog.this.mCallback
                        .onRecurrenceSet(r);
                    }
                } else {
                    RecurrencePickerDialog.this.mCallback.onNoRecurrenceSet();
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
                int newValue = RecurrencePickerDialog.this.mIntervalValue;
                try {
                    newValue = Integer.parseInt(s.toString());
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                }
                if (newValue == 0) {
                    RecurrencePickerDialog.this.mIntervalCount.setText(""
                            + RecurrencePickerDialog.this.mIntervalValue);
                }
                updateIntervalType();
                RecurrencePickerDialog.this.mIntervalValue = newValue;
            }
            @Override
            public void beforeTextChanged(final CharSequence s,
                                          final int start, final int count, final int after) {
            }
            @Override
            public void afterTextChanged(final Editable s) {
            }
        });
        final int dayPosition = this.mForDue ? 0 : 2;
        this.mIntervalType
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> arg0,
                                       final View arg1, final int pos, final long arg3) {
                if (pos == dayPosition) {
                    RecurrencePickerDialog.this.mIsWeekDay = true;
                    view.findViewById(R.id.weekGroup).setVisibility(
                        View.VISIBLE);
                    view.findViewById(R.id.weekGroup2).setVisibility(
                        View.VISIBLE);
                } else {
                    RecurrencePickerDialog.this.mIsWeekDay = false;
                    view.findViewById(R.id.weekGroup).setVisibility(
                        View.GONE);
                    view.findViewById(R.id.weekGroup2).setVisibility(
                        View.GONE);
                }
            }
            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
                // TODO Auto-generated method stub
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
                            .findViewById(this.mDark ? R.id.endDate_dark
                                          : R.id.endDate_light);
        this.mEndDateView.setText(DateTimeHelper.formatDate(endDate));
        this.mEndDateView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final SupportDatePickerDialog dp = SupportDatePickerDialog.newInstance(
                new OnDateSetListener() {
                    @Override
                    public void onNoDateSet() {
                        RecurrencePickerDialog.this.mEndDate = absent();
                    }
                    @Override
                    public void onDateSet(
                        final DatePicker datePickerDialog,
                        final int year, final int month,
                        final int day) {
                        if (!RecurrencePickerDialog.this.mEndDate.isPresent()) {
                            RecurrencePickerDialog.this.mEndDate = Optional.of((Calendar) new GregorianCalendar());
                        }
                        Calendar endDate = RecurrencePickerDialog.this.mEndDate.get();
                        endDate.set(Calendar.YEAR, year);
                        endDate.set(Calendar.MONTH, month);
                        endDate.set(Calendar.DAY_OF_MONTH, day);
                        RecurrencePickerDialog.this.mEndDateView
                        .setText(DateTimeHelper
                                 .formatDate(
                                     getActivity(),
                                     RecurrencePickerDialog.this.mEndDate));
                    }
                }, endDate.get(Calendar.YEAR), endDate
                .get(Calendar.MONTH), endDate
                .get(Calendar.DAY_OF_MONTH),
                RecurrencePickerDialog.this.mDark, false);
                dp.show(getFragmentManager(), "endDate");
            }
        });
        this.mEndSpinner
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> arg0,
                                       final View arg1, final int pos, final long arg3) {
                switch (pos) {
                case 1:
                    RecurrencePickerDialog.this.mEndDateView
                    .setVisibility(View.VISIBLE);
                    if (!RecurrencePickerDialog.this.mEndDate.isPresent()) {
                        RecurrencePickerDialog.this.mEndDate = RecurrencePickerDialog.this.mStartDate;
                        if (!RecurrencePickerDialog.this.mEndDate.isPresent()) {
                            RecurrencePickerDialog.this.mEndDate = Optional.of((Calendar) new GregorianCalendar());
                        }
                        RecurrencePickerDialog.this.mEndDate.get().add(
                            Calendar.MONTH, 1);
                    }
                    break;
                default:// FOREVER
                    RecurrencePickerDialog.this.mEndDateView
                    .setVisibility(View.GONE);
                    RecurrencePickerDialog.this.mEndDate = absent();
                    break;
                }
                RecurrencePickerDialog.this.mEndDateView
                .setText(DateTimeHelper.formatDate(
                             getActivity(),
                             RecurrencePickerDialog.this.mEndDate));
            }
            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
                // nothing
            }
        });
        this.mEndSpinner.setAdapter(endSpinnerAdapter);
        final String[] start = { res.getString(R.string.recurrence_from_now),
                                 res.getString(R.string.recurrence_from) // ,res.getString(R.string.recurrence_end_count_label)
                                 // Dont
                                 // support
                                 // this
                                 // now...*/
                                 // };
                               };
        this.mStartSpinner = (Spinner) view.findViewById(R.id.startSpinner);
        this.mStartSpinner
        .setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> arg0,
                                       final View arg1, final int pos, final long arg3) {
                switch (pos) {
                case 1:
                    RecurrencePickerDialog.this.mStartDateView
                    .setVisibility(View.VISIBLE);
                    if (!RecurrencePickerDialog.this.mStartDate.isPresent()) {
                        RecurrencePickerDialog.this.mStartDate = RecurrencePickerDialog.this.mEndDate;
                        if (!RecurrencePickerDialog.this.mStartDate.isPresent()) {
                            RecurrencePickerDialog.this.mStartDate = Optional.of((Calendar) new GregorianCalendar());
                        } else {
                            RecurrencePickerDialog.this.mStartDate.get().add(
                                Calendar.MONTH, -1);
                        }
                    }
                    break;
                default:// FOREVER
                    RecurrencePickerDialog.this.mStartDateView
                    .setVisibility(View.GONE);
                    RecurrencePickerDialog.this.mStartDate = absent();
                    break;
                }
                RecurrencePickerDialog.this.mStartDateView.setText(DateTimeHelper
                        .formatDate(getActivity(),
                                    RecurrencePickerDialog.this.mStartDate));
            }
            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
                // nothing
            }
        });
        final ArrayAdapter<CharSequence> startSpinnerAdapter = new ArrayAdapter<CharSequence>(
            this.ctx, android.R.layout.simple_spinner_item, start);
        startSpinnerAdapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mStartDateView = (TextView) view
                              .findViewById(this.mDark ? R.id.startDate_dark
                                            : R.id.startDate_light);
        final Calendar startDate = new GregorianCalendar();
        this.mStartDateView.setText(DateTimeHelper.formatDate(startDate));
        this.mStartDateView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final SupportDatePickerDialog dp = SupportDatePickerDialog.newInstance(
                new OnDateSetListener() {
                    @Override
                    public void onNoDateSet() {
                        RecurrencePickerDialog.this.mStartDate = absent();
                        // cannot happen
                    }
                    @Override
                    public void onDateSet(
                        final DatePicker datePickerDialog,
                        final int year, final int month,
                        final int day) {
                        if (!RecurrencePickerDialog.this.mStartDate.isPresent()) {
                            RecurrencePickerDialog.this.mStartDate = of((Calendar) new GregorianCalendar());
                        }
                        Calendar startDate = RecurrencePickerDialog.this.mStartDate.get();
                        startDate.set(Calendar.YEAR, year);
                        startDate.set(Calendar.MONTH, month);
                        startDate.set(Calendar.DAY_OF_MONTH, day);
                        RecurrencePickerDialog.this.mStartDateView
                        .setText(DateTimeHelper
                                 .formatDate(
                                     getActivity(),
                                     RecurrencePickerDialog.this.mStartDate));
                    }
                }, startDate.get(Calendar.YEAR), startDate
                .get(Calendar.MONTH), startDate
                .get(Calendar.DAY_OF_MONTH),
                RecurrencePickerDialog.this.mDark, false);
                dp.show(getFragmentManager(), "startDate");
            }
        });
        this.mStartSpinner.setAdapter(startSpinnerAdapter);
        setEnabledComponents(this.mRecurring.isPresent()
                             && this.mRecurring.get().getId() != -1);
        if (this.mDark) {
            view.findViewById(R.id.recurrence_picker_dialog)
            .setBackgroundColor(res.getColor(R.color.dialog_gray));
            view.findViewById(R.id.recurrence_picker_head).setBackgroundColor(
                res.getColor(R.color.dialog_dark_gray));
            this.mDoneButton.setTextColor(res.getColor(R.color.White));
            if (this.toggleIsSwitch) {
                ((Switch) this.mToggle).setThumbDrawable(res
                        .getDrawable(R.drawable.switch_thumb_dark));
            }
            this.mEndDateView.setTextColor(res.getColor(R.color.White));
            this.mStartDateView.setTextColor(res.getColor(R.color.White));
            this.mUseExact
            .setButtonDrawable(R.drawable.btn_check_holo_dark_red);
        }
        if (!this.mForDue) {
            this.mUseExact.setVisibility(View.GONE);
        }
        if (this.mRecurring.isPresent() && this.mRecurring.get().isTemporary()) {
            Recurring recurringRaw = mRecurring.get();
            this.mRecurrenceSelection.setSelection(0);
            if (recurringRaw.getWeekdays().size() != 0) {
                this.mIsWeekDay = true;
                this.mIntervalType.setSelection(this.mForDue ? 0 : 2);
            } else if (recurringRaw.getMinutes() != 0) {
                this.mIntervalType.setSelection(0);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getMinutes()));
                this.mIntervalValue = recurringRaw.getMinutes();
            } else if (recurringRaw.getHours() != 0) {
                this.mIntervalType.setSelection(1);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getHours()));
                this.mIntervalValue = recurringRaw.getHours();
            } else if (recurringRaw.getDays() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 0 : 2);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getDays()));
                this.mIntervalValue = recurringRaw.getDays();
            } else if (recurringRaw.getMonths() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 1 : 3);
                this.mIntervalCount.setText(String.valueOf(recurringRaw.getMonths()));
                this.mIntervalValue = recurringRaw.getMonths();
            } else if (recurringRaw.getYears() != 0) {
                this.mIntervalType.setSelection(this.mForDue ? 2 : 4);
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

    protected void setEnabledComponents(final boolean b) {
        this.mRecurrenceSelection.setEnabled(b);
        this.mEndSpinner.setEnabled(b);
        this.mEndDateView.setEnabled(b);
        this.mStartSpinner.setEnabled(b);
        this.mStartDateView.setEnabled(b);
        for (final ToggleButton t : this.mWeekByDayButtons) {
            t.setEnabled(b);
        }
        this.mIntervalCount.setEnabled(b);
        this.mIntervalType.setEnabled(b);
        this.mUseExact.setEnabled(b);
        if (this.mDark) {
            if (b) {
                this.mStartDateView.setTextColor(getResources().getColor(
                                                     R.color.White));
                this.mEndDateView.setTextColor(getResources().getColor(
                                                   R.color.White));
            } else {
                this.mStartDateView.setTextColor(getResources().getColor(
                                                     R.color.grey));
                this.mEndDateView.setTextColor(getResources().getColor(
                                                   R.color.grey));
            }
        }
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
        final int size = this.mForDue ? 3 : 5;
        int i = 0;
        final String[] ret = new String[size];
        if (!this.mForDue) {
            ret[i++] = dialogContext.getResources().getQuantityString(
                           R.plurals.due_minute, this.mIntervalValue);
            ret[i++] = dialogContext.getResources().getQuantityString(R.plurals.due_hour,
                       this.mIntervalValue);
        }
        ret[i++] = dialogContext.getResources().getQuantityString(R.plurals.due_day,
                   this.mIntervalValue);
        ret[i++] = dialogContext.getResources().getQuantityString(R.plurals.due_month,
                   this.mIntervalValue);
        ret[i] = dialogContext.getResources().getQuantityString(R.plurals.due_year,
                 this.mIntervalValue);
        return ret;
    }

    public interface OnRecurrenceSetListener {
        void onCustomRecurrenceSetInterval(final boolean isDue,
                                           final int intervalYears, final int intervalMonths,
                                           final int intervalDays, final int intervalHours,
                                           final int intervalMinutes, @NonNull final Optional<Calendar> startDate,
                                           @NonNull final Optional<Calendar> endDate, final boolean isExact);

        void onCustomRecurrenceSetWeekdays(final boolean isDue,
                                           @NonNull final List<Integer> weekdays, @NonNull final Optional<Calendar> startDate,
                                           @NonNull final Optional<Calendar> endDate, final boolean isExact);

        void onRecurrenceSet(final Recurring r);

        void onNoRecurrenceSet();
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView,
                                 final boolean isChecked) {
        // nothing
    }

}
