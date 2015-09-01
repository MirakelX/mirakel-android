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

package com.sleepbot.datetimepicker.time;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fourmob.datetimepicker.Utils;
import com.google.common.base.Optional;

import org.joda.time.LocalTime;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;

import static com.google.common.base.Optional.of;

public class TimePicker extends LinearLayout implements
    RadialPickerLayout.OnValueSelectedListener {


    public class KeyboardListener implements OnKeyListener {
        public KeyboardListener(final Dialog d) {
            TimePicker.this.mDialog = d;
        }

        @Override
        public boolean onKey(final View v, final int keyCode,
                             final KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                return processKeyUp(keyCode);
            }
            return false;
        }
    }

    /**
     * Simple node class to be used for traversal to check for legal times.
     * mLegalKeys represents the keys that can be typed to get to the node.
     * mChildren are the children that can be reached from this node.
     */
    public class Node {
        private final ArrayList<Node> mChildren;
        private final int[] mLegalKeys;

        public Node(final int... legalKeys) {
            this.mLegalKeys = legalKeys;
            this.mChildren = new ArrayList<Node>();
        }

        public void addChild(final Node child) {
            this.mChildren.add(child);
        }

        public Node canReach(final int key) {
            if (this.mChildren == null) {
                return null;
            }
            for (final Node child : this.mChildren) {
                if (child.containsKey(key)) {
                    return child;
                }
            }
            return null;
        }

        public boolean containsKey(final int key) {
            for (final int mLegalKey : this.mLegalKeys) {
                if (mLegalKey == key) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class OnTimeSetListener implements Parcelable {

        public void onTimeSet(final RadialPickerLayout view, final @NonNull Optional<LocalTime> newTime) {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }

        public static final Parcelable.Creator<OnTimeSetListener> CREATOR = new
        Parcelable.Creator<OnTimeSetListener>() {
            @Override
            public OnTimeSetListener createFromParcel(final Parcel source) {
                return new OnTimeSetListener();
            }

            @Override
            public OnTimeSetListener[] newArray(final int size) {
                return new OnTimeSetListener[size];
            }
        };
    }

    public static final int AM = 0;
    // NOT a real index for the purpose of what's showing.
    public static final int AMPM_INDEX = 2;
    // Also NOT a real index, just used for keyboard mode.
    public static final int ENABLE_PICKER_INDEX = 3;
    public static final int HOUR_INDEX = 0;
    private static final String KEY_CURRENT_ITEM_SHOWING = "current_item_showing";

    private static final String KEY_TIME = "time";
    private static final String KEY_IN_KB_MODE = "in_kb_mode";
    private static final String KEY_IS_24_HOUR_VIEW = "is_24_hour_view";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_TYPED_TIMES = "typed_times";
    private static final String PARENT_KEY = "parent";
    private static final String CALLBACK_KEY = "callback";
    public static final int MINUTE_INDEX = 1;

    public static final int PM = 1;
    // Delay before starting the pulse animation, in ms.
    private static final int PULSE_ANIMATOR_DELAY = 300;

    private static final String TAG = "TimePicker";

    private static int getValFromKeyCode(final int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_0:
            return 0;
        case KeyEvent.KEYCODE_1:
            return 1;
        case KeyEvent.KEYCODE_2:
            return 2;
        case KeyEvent.KEYCODE_3:
            return 3;
        case KeyEvent.KEYCODE_4:
            return 4;
        case KeyEvent.KEYCODE_5:
            return 5;
        case KeyEvent.KEYCODE_6:
            return 6;
        case KeyEvent.KEYCODE_7:
            return 7;
        case KeyEvent.KEYCODE_8:
            return 8;
        case KeyEvent.KEYCODE_9:
            return 9;
        default:
            return -1;
        }
    }

    private final Context ctx;
    private final View layout;
    private boolean mAllowAutoAdvance;
    private int mAmKeyCode;
    private View mAmPmHitspace;
    private TextView mAmPmTextView;
    private String mAmText;

    private OnTimeSetListener mCallback;
    private String mDeletedKeyFormat;
    public Dialog mDialog;

    private TextView mDoneButton;
    private String mDoublePlaceholderText;
    // Accessibility strings.
    private String mHourPickerDescription;
    private TextView mHourSpaceView;

    private TextView mHourView;
    private LocalTime mTime;
    private boolean mInKbMode;
    private boolean mIs24HourMode;
    private Node mLegalTimesTree;
    private String mMinutePickerDescription;
    private TextView mMinuteSpaceView;

    private TextView mMinuteView;
    private Button mNoDateButton;
    // For hardware IME input.
    private char mPlaceholderText;
    private int mPmKeyCode;

    private String mPmText;

    private int mSelectedColor;
    private String mSelectHours;

    private String mSelectMinutes;

    private RadialPickerLayout mTimePicker;

    private ArrayList<Integer> mTypedTimes;

    private int mUnselectedColor;


    public TimePicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                             R.styleable.DatePicker, 0, 0);
        mTime = new LocalTime();
        try {
            mTime = mTime.withHourOfDay(a.getInt(
                                            R.styleable.TimePicker_initialHour, 0));
            mTime = mTime.withMinuteOfHour(a.getInt(
                                               R.styleable.TimePicker_initialMinute, 0));
        } finally {
            a.recycle();
        }
        this.mIs24HourMode = DateTimeHelper.is24HourLocale(Helpers.getLocale(context));
        this.layout = inflate(context, R.layout.time_picker_view, this);
        initLayout();
    }

    public LocalTime getTime() {
        if (mTimePicker != null) {
            return mTimePicker.getTime();
        } else {
            return mTime;
        }
    }

    private boolean addKeyIfLegal(final int keyCode) {
        // If we're in 24hour mode, we'll need to check if the input is full. If
        // in AM/PM mode,
        // we'll need to see if AM/PM have been typed.
        if ((this.mIs24HourMode && (this.mTypedTimes.size() == 4))
            || (!this.mIs24HourMode && isTypedTimeFullyLegal())) {
            return false;
        }
        this.mTypedTimes.add(keyCode);
        if (!isTypedTimeLegalSoFar()) {
            deleteLastTypedKey();
            return false;
        }
        final int val = getValFromKeyCode(keyCode);
        Utils.tryAccessibilityAnnounce(this.mTimePicker,
                                       String.format("%d", val));
        // Automatically fill in 0's if AM or PM was legally entered.
        if (isTypedTimeFullyLegal()) {
            if (!this.mIs24HourMode && (this.mTypedTimes.size() <= 3)) {
                this.mTypedTimes.add(this.mTypedTimes.size() - 1,
                                     KeyEvent.KEYCODE_0);
                this.mTypedTimes.add(this.mTypedTimes.size() - 1,
                                     KeyEvent.KEYCODE_0);
            }
            this.mDoneButton.setEnabled(true);
        }
        return true;
    }

    private int deleteLastTypedKey() {
        final int deleted = this.mTypedTimes
                            .remove(this.mTypedTimes.size() - 1);
        if (!isTypedTimeFullyLegal()) {
            this.mDoneButton.setEnabled(false);
        }
        return deleted;
    }

    /**
     * Get out of keyboard mode. If there is nothing in typedTimes, revert to
     * TimePicker's time.
     *
     * @param updateDisplays
     *            If true, update the displays with the relevant time.
     */
    protected void finishKbMode(final boolean updateDisplays) {
        this.mInKbMode = false;
        if (!this.mTypedTimes.isEmpty()) {
            final int values[] = getEnteredTime(null);
            this.mTimePicker.setTime(new LocalTime(values[0], values[1], 0));
            if (!this.mIs24HourMode) {
                this.mTimePicker.setAmOrPm(values[2]);
            }
            this.mTypedTimes.clear();
        }
        if (updateDisplays) {
            updateDisplay(false);
            this.mTimePicker.trySettingInputEnabled(true);
        }
    }

    /**
     * Create a tree for deciding what keys can legally be typed.
     */
    private void generateLegalTimesTree() {

        // The root of the tree doesn't contain any numbers.
        this.mLegalTimesTree = new Node();
        // Create a quick cache of numbers to their keycodes.
        final int k0 = KeyEvent.KEYCODE_0;
        final int k1 = KeyEvent.KEYCODE_1;
        final int k2 = KeyEvent.KEYCODE_2;
        final int k3 = KeyEvent.KEYCODE_3;
        final int k4 = KeyEvent.KEYCODE_4;
        final int k5 = KeyEvent.KEYCODE_5;
        final int k6 = KeyEvent.KEYCODE_6;
        final int k7 = KeyEvent.KEYCODE_7;
        final int k8 = KeyEvent.KEYCODE_8;
        final int k9 = KeyEvent.KEYCODE_9;
        if (this.mIs24HourMode) {
            // We'll be re-using these nodes, so we'll save them.
            final Node minuteFirstDigit = new Node(k0, k1, k2, k3, k4, k5);
            final Node minuteSecondDigit = new Node(k0, k1, k2, k3, k4, k5, k6,
                                                    k7, k8, k9);
            // The first digit must be followed by the second digit.
            minuteFirstDigit.addChild(minuteSecondDigit);
            // The first digit may be 0-1.
            Node firstDigit = new Node(k0, k1);
            this.mLegalTimesTree.addChild(firstDigit);
            // When the first digit is 0-1, the second digit may be 0-5.
            Node secondDigit = new Node(k0, k1, k2, k3, k4, k5);
            firstDigit.addChild(secondDigit);
            // We may now be followed by the first minute digit. E.g. 00:09,
            // 15:58.
            secondDigit.addChild(minuteFirstDigit);
            // When the first digit is 0-1, and the second digit is 0-5, the
            // third digit may be 6-9.
            final Node thirdDigit = new Node(k6, k7, k8, k9);
            // The time must now be finished. E.g. 0:55, 1:08.
            secondDigit.addChild(thirdDigit);
            // When the first digit is 0-1, the second digit may be 6-9.
            secondDigit = new Node(k6, k7, k8, k9);
            firstDigit.addChild(secondDigit);
            // We must now be followed by the first minute digit. E.g. 06:50,
            // 18:20.
            secondDigit.addChild(minuteFirstDigit);
            // The first digit may be 2.
            firstDigit = new Node(k2);
            this.mLegalTimesTree.addChild(firstDigit);
            // When the first digit is 2, the second digit may be 0-3.
            secondDigit = new Node(k0, k1, k2, k3);
            firstDigit.addChild(secondDigit);
            // We must now be followed by the first minute digit. E.g. 20:50,
            // 23:09.
            secondDigit.addChild(minuteFirstDigit);
            // When the first digit is 2, the second digit may be 4-5.
            secondDigit = new Node(k4, k5);
            firstDigit.addChild(secondDigit);
            // We must now be followd by the last minute digit. E.g. 2:40, 2:53.
            secondDigit.addChild(minuteSecondDigit);
            // The first digit may be 3-9.
            firstDigit = new Node(k3, k4, k5, k6, k7, k8, k9);
            this.mLegalTimesTree.addChild(firstDigit);
            // We must now be followed by the first minute digit. E.g. 3:57,
            // 8:12.
            firstDigit.addChild(minuteFirstDigit);
        } else {
            // We'll need to use the AM/PM node a lot.
            // Set up AM and PM to respond to "a" and "p".
            final Node ampm = new Node(getAmOrPmKeyCode(AM),
                                       getAmOrPmKeyCode(PM));
            // The first hour digit may be 1.
            Node firstDigit = new Node(k1);
            this.mLegalTimesTree.addChild(firstDigit);
            // We'll allow quick input of on-the-hour times. E.g. 1pm.
            firstDigit.addChild(ampm);
            // When the first digit is 1, the second digit may be 0-2.
            Node secondDigit = new Node(k0, k1, k2);
            firstDigit.addChild(secondDigit);
            // Also for quick input of on-the-hour times. E.g. 10pm, 12am.
            secondDigit.addChild(ampm);
            // When the first digit is 1, and the second digit is 0-2, the third
            // digit may be 0-5.
            Node thirdDigit = new Node(k0, k1, k2, k3, k4, k5);
            secondDigit.addChild(thirdDigit);
            // The time may be finished now. E.g. 1:02pm, 1:25am.
            thirdDigit.addChild(ampm);
            // When the first digit is 1, the second digit is 0-2, and the third
            // digit is 0-5,
            // the fourth digit may be 0-9.
            final Node fourthDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7,
                                              k8, k9);
            thirdDigit.addChild(fourthDigit);
            // The time must be finished now. E.g. 10:49am, 12:40pm.
            fourthDigit.addChild(ampm);
            // When the first digit is 1, and the second digit is 0-2, the third
            // digit may be 6-9.
            thirdDigit = new Node(k6, k7, k8, k9);
            secondDigit.addChild(thirdDigit);
            // The time must be finished now. E.g. 1:08am, 1:26pm.
            thirdDigit.addChild(ampm);
            // When the first digit is 1, the second digit may be 3-5.
            secondDigit = new Node(k3, k4, k5);
            firstDigit.addChild(secondDigit);
            // When the first digit is 1, and the second digit is 3-5, the third
            // digit may be 0-9.
            thirdDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
            secondDigit.addChild(thirdDigit);
            // The time must be finished now. E.g. 1:39am, 1:50pm.
            thirdDigit.addChild(ampm);
            // The hour digit may be 2-9.
            firstDigit = new Node(k2, k3, k4, k5, k6, k7, k8, k9);
            this.mLegalTimesTree.addChild(firstDigit);
            // We'll allow quick input of on-the-hour-times. E.g. 2am, 5pm.
            firstDigit.addChild(ampm);
            // When the first digit is 2-9, the second digit may be 0-5.
            secondDigit = new Node(k0, k1, k2, k3, k4, k5);
            firstDigit.addChild(secondDigit);
            // When the first digit is 2-9, and the second digit is 0-5, the
            // third digit may be 0-9.
            thirdDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
            secondDigit.addChild(thirdDigit);
            // The time must be finished now. E.g. 2:57am, 9:30pm.
            thirdDigit.addChild(ampm);
        }
    }

    /**
     * Get the keycode value for AM and PM in the current language.
     */
    @SuppressLint("InlinedApi")
    private int getAmOrPmKeyCode(final int amOrPm) {
        // Cache the codes.
        if ((this.mAmKeyCode == -1) || (this.mPmKeyCode == -1)) {
            // Find the first character in the AM/PM text that is unique.
            final KeyCharacterMap kcm = KeyCharacterMap
                                        .load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            for (int i = 0; i < Math.max(this.mAmText.length(),
                                         this.mPmText.length()); i++) {
                char amChar = this.mAmText.toLowerCase(Locale.getDefault())
                              .charAt(i);
                char pmChar = this.mPmText.toLowerCase(Locale.getDefault())
                              .charAt(i);
                if (amChar != pmChar) {
                    final KeyEvent[] events = kcm.getEvents(new char[] {
                                                                amChar, pmChar
                                                            });
                    // There should be 4 events: a down and up for both AM and
                    // PM.
                    if (events.length == 4) {
                        this.mAmKeyCode = events[0].getKeyCode();
                        this.mPmKeyCode = events[2].getKeyCode();
                    } else {
                        Log.e(TAG, "Unable to find keycodes for AM and PM.");
                    }
                    break;
                }
            }
        }
        if (amOrPm == AM) {
            return this.mAmKeyCode;
        } else if (amOrPm == PM) {
            return this.mPmKeyCode;
        }
        return -1;
    }

    /**
     * Get the currently-entered time, as integer values of the hours and
     * minutes typed.
     *
     * @param enteredZeros
     *            A size-2 boolean array, which the caller should initialize,
     *            and which may then be used for the caller to know whether
     *            zeros had been explicitly entered as either hours of minutes.
     *            This is helpful for deciding whether to show the dashes, or
     *            actual 0's.
     * @return A size-3 int array. The first value will be the hours, the second
     *         value will be the minutes, and the third will be either
     *         TimePickerDialog.AM or TimePickerDialog.PM.
     */
    private int[] getEnteredTime(final Boolean[] enteredZeros) {
        int amOrPm = -1;
        int startIndex = 1;
        if (!this.mIs24HourMode && isTypedTimeFullyLegal()) {
            final int keyCode = this.mTypedTimes
                                .get(this.mTypedTimes.size() - 1);
            if (keyCode == getAmOrPmKeyCode(AM)) {
                amOrPm = AM;
            } else if (keyCode == getAmOrPmKeyCode(PM)) {
                amOrPm = PM;
            }
            startIndex = 2;
        }
        int minute = -1;
        int hour = -1;
        for (int i = startIndex; i <= this.mTypedTimes.size(); i++) {
            final int val = getValFromKeyCode(this.mTypedTimes
                                              .get(this.mTypedTimes.size() - i));
            if (i == startIndex) {
                minute = val;
            } else if (i == (startIndex + 1)) {
                minute += 10 * val;
                if ((enteredZeros != null) && (val == 0)) {
                    enteredZeros[1] = true;
                }
            } else if (i == (startIndex + 2)) {
                hour = val;
            } else if (i == (startIndex + 3)) {
                hour += 10 * val;
                if ((enteredZeros != null) && (val == 0)) {
                    enteredZeros[0] = true;
                }
            }
        }
        return new int[] { hour, minute, amOrPm };
    }


    public KeyboardListener getNewKeyboardListner(final Dialog dialog) {
        return new KeyboardListener(dialog);
    }

    private void initLayout() {
        final View dialog = this.layout.findViewById(R.id.time_picker_dialog);
        final KeyboardListener keyboardListener = new KeyboardListener(null);
        this.layout.findViewById(R.id.time_picker_dialog).setOnKeyListener(
            keyboardListener);
        final Resources res = getResources();
        this.mHourPickerDescription = res
                                      .getString(R.string.hour_picker_description);
        this.mSelectHours = res.getString(R.string.select_hours);
        this.mMinutePickerDescription = res
                                        .getString(R.string.minute_picker_description);
        this.mSelectMinutes = res.getString(R.string.select_minutes);
        this.mSelectedColor = ThemeManager.getColor(R.attr.colorTextWhite);
        this.mUnselectedColor = ThemeManager.getColor(R.attr.colorControlNormal);
        this.mHourView = (TextView) this.layout.findViewById(R.id.hours);
        this.mHourView.setOnKeyListener(keyboardListener);
        this.mHourSpaceView = (TextView) this.layout
                              .findViewById(R.id.hour_space);
        this.mMinuteSpaceView = (TextView) this.layout
                                .findViewById(R.id.minutes_space);
        this.mMinuteView = (TextView) this.layout.findViewById(R.id.minutes);
        this.mMinuteView.setOnKeyListener(keyboardListener);
        this.mAmPmTextView = (TextView) this.layout
                             .findViewById(R.id.ampm_label);
        this.mAmPmTextView.setOnKeyListener(keyboardListener);
        final String[] amPmTexts = new DateFormatSymbols().getAmPmStrings();
        this.mAmText = amPmTexts[0];
        this.mPmText = amPmTexts[1];
        this.mTimePicker = (RadialPickerLayout) this.layout
                           .findViewById(R.id.time_picker_radial);
        this.mTimePicker.setOnValueSelectedListener(this);
        this.mTimePicker.setOnKeyListener(keyboardListener);
        this.mTimePicker.initialize(this.ctx, mTime, this.mIs24HourMode);
        final int currentItemShowing = HOUR_INDEX;
        setCurrentItemShowing(currentItemShowing, false, true, true);
        this.mTimePicker.invalidate();
        this.mHourView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                setCurrentItemShowing(HOUR_INDEX, true, false, true);
            }
        });
        this.mMinuteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                setCurrentItemShowing(MINUTE_INDEX, true, false, true);
            }
        });
        this.mDoneButton = (TextView) this.layout.findViewById(R.id.done);
        this.mDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (TimePicker.this.mInKbMode && isTypedTimeFullyLegal()) {
                    finishKbMode(false);
                }
                if (TimePicker.this.mCallback != null) {
                    TimePicker.this.mCallback.onTimeSet(
                        TimePicker.this.mTimePicker,
                        of(TimePicker.this.mTimePicker.getTime()));
                }
            }
        });
        this.mDoneButton.setOnKeyListener(keyboardListener);
        this.mNoDateButton = (Button) this.layout.findViewById(R.id.dismiss);
        this.mNoDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (TimePicker.this.mCallback != null) {
                    TimePicker.this.mCallback.onTimeSet(TimePicker.this.mTimePicker, Optional.<LocalTime>absent());
                }
            }
        });
        final View header = this.layout.findViewById(R.id.time_dialog_head);
        header.setBackgroundColor(ThemeManager.getPrimaryThemeColor());
        dialog.setBackgroundColor(ThemeManager.getColor(R.attr.colorBackground));
        final View header_background = this.layout
                                       .findViewById(R.id.header_background_timepicker);
        header_background.setBackgroundColor(ThemeManager.getPrimaryThemeColor());
        final View hairline = this.layout
                              .findViewById(R.id.hairline_timepicker);
        if (hairline != null) {
            hairline.setBackgroundColor(ThemeManager.getColor(R.attr.colorTextGrey));
        }
        this.mDoneButton.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
        this.mNoDateButton.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
        // Enable or disable the AM/PM view.
        this.mAmPmHitspace = this.layout.findViewById(R.id.ampm_hitspace);
        if (this.mIs24HourMode) {
            this.mAmPmTextView.setVisibility(View.GONE);
            final RelativeLayout.LayoutParams paramsSeparator = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            paramsSeparator.addRule(RelativeLayout.CENTER_IN_PARENT);
            final TextView separatorView = (TextView) this.layout
                                           .findViewById(R.id.separator);
            separatorView.setLayoutParams(paramsSeparator);
        } else {
            this.mAmPmTextView.setVisibility(View.VISIBLE);
            updateAmPmDisplay(((this.mTime.getHourOfDay() / 12) == 1) ? PM : AM);
            this.mAmPmHitspace.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    int amOrPm = TimePicker.this.mTimePicker
                                 .getIsCurrentlyAmOrPm();
                    if (amOrPm == AM) {
                        amOrPm = PM;
                    } else if (amOrPm == PM) {
                        amOrPm = AM;
                    }
                    updateAmPmDisplay(amOrPm);
                    TimePicker.this.mTimePicker.setAmOrPm(amOrPm);
                }
            });
        }
        this.mAllowAutoAdvance = true;

        setHour(this.mTime.getHourOfDay(), true);
        setMinute(this.mTime.getMinuteOfHour());
        // Set up for keyboard mode.
        this.mDoublePlaceholderText = res.getString(R.string.time_placeholder);
        this.mDeletedKeyFormat = res.getString(R.string.deleted_key);
        this.mPlaceholderText = this.mDoublePlaceholderText.charAt(0);
        this.mAmKeyCode = this.mPmKeyCode = -1;
        generateLegalTimesTree();
        if (this.mInKbMode) {
            // mTypedTimes = savedInstanceState
            // .getIntegerArrayList(KEY_TYPED_TIMES);
            tryStartingKbMode(-1);
            this.mHourView.invalidate();
        } else if (this.mTypedTimes == null) {
            this.mTypedTimes = new ArrayList<Integer>();
        }
    }

    /**
     * Check if the time that has been typed so far is completely legal, as is.
     */
    private boolean isTypedTimeFullyLegal() {
        if (this.mIs24HourMode) {
            // For 24-hour mode, the time is legal if the hours and minutes are
            // each legal. Note:
            // getEnteredTime() will ONLY call isTypedTimeFullyLegal() when NOT
            // in 24hour mode.
            final int[] values = getEnteredTime(null);
            return (values[0] >= 0) && (values[1] >= 0) && (values[1] < 60);
        }
        // For AM/PM mode, the time is legal if it contains an AM or PM, as
        // those can only be
        // legally added at specific times based on the tree's algorithm.
        return this.mTypedTimes.contains(getAmOrPmKeyCode(AM))
               || this.mTypedTimes.contains(getAmOrPmKeyCode(PM));
    }

    /**
     * Traverse the tree to see if the keys that have been typed so far are
     * legal as is, or may become legal as more keys are typed (excluding
     * backspace).
     */
    private boolean isTypedTimeLegalSoFar() {
        Node node = this.mLegalTimesTree;
        for (final int keyCode : this.mTypedTimes) {
            node = node.canReach(keyCode);
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        final Bundle saved = (Bundle) state;
        super.onRestoreInstanceState(saved.getParcelable(PARENT_KEY));
        mTime = (LocalTime) saved.getSerializable(KEY_TIME);
        if (mTimePicker != null) {
            mTimePicker.setTime(mTime);
            mTimePicker.setCurrentItemShowing(saved.getInt(KEY_CURRENT_ITEM_SHOWING), false);
        };
        setHour(mTime.getHourOfDay(), false);
        setMinute(mTime.getMinuteOfHour());
        mCallback = saved.getParcelable(CALLBACK_KEY);
    }

    @Override
    public Parcelable onSaveInstanceState() {

        final Bundle outState = new Bundle();
        outState.putParcelable(PARENT_KEY, super.onSaveInstanceState());
        outState.putParcelable(CALLBACK_KEY, mCallback);
        if (this.mTimePicker != null) {
            outState.putSerializable(KEY_TIME, this.mTimePicker.getTime());
            outState.putBoolean(KEY_IS_24_HOUR_VIEW, this.mIs24HourMode);
            outState.putInt(KEY_CURRENT_ITEM_SHOWING,
                            this.mTimePicker.getCurrentItemShowing());
            outState.putBoolean(KEY_IN_KB_MODE, this.mInKbMode);
            if (this.mInKbMode) {
                outState.putIntegerArrayList(KEY_TYPED_TIMES, this.mTypedTimes);
            }
        }
        return outState;
    }

    /**
     * Called by the picker for updating the header display.
     */
    @Override
    public void onValueSelected(final int pickerIndex, final int newValue,
                                final boolean autoAdvance) {
        if (pickerIndex == HOUR_INDEX) {
            setHour(newValue, false);
            String announcement = String.format(Helpers.getLocale(getContext()),
                                                "%d", newValue);
            if (this.mAllowAutoAdvance && autoAdvance) {
                setCurrentItemShowing(MINUTE_INDEX, true, true, false);
                announcement += ". " + this.mSelectMinutes;
            }
            Utils.tryAccessibilityAnnounce(this.mTimePicker, announcement);
        } else if (pickerIndex == MINUTE_INDEX) {
            setMinute(newValue);
        } else if (pickerIndex == AMPM_INDEX) {
            updateAmPmDisplay(newValue);
        } else if (pickerIndex == ENABLE_PICKER_INDEX) {
            if (!isTypedTimeFullyLegal()) {
                this.mTypedTimes.clear();
            }
            finishKbMode(true);
        }
    }

    /**
     * For keyboard mode, processes key events.
     *
     * @param keyCode
     *            the pressed key.
     * @return true if the key was successfully processed, false otherwise.
     */
    protected boolean processKeyUp(final int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE
            || keyCode == KeyEvent.KEYCODE_BACK) {
            if (this.mDialog != null) {
                this.mDialog.dismiss();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_TAB) {
            if (this.mInKbMode) {
                if (isTypedTimeFullyLegal()) {
                    finishKbMode(true);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (this.mInKbMode) {
                if (!isTypedTimeFullyLegal()) {
                    return true;
                }
                finishKbMode(false);
            }
            if (this.mCallback != null) {
                this.mCallback.onTimeSet(this.mTimePicker, of(mTimePicker.getTime()));
            }
            if (this.mDialog != null) {
                this.mDialog.dismiss();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (this.mInKbMode) {
                if (!this.mTypedTimes.isEmpty()) {
                    final int deleted = deleteLastTypedKey();
                    final String deletedKeyStr;
                    if (deleted == getAmOrPmKeyCode(AM)) {
                        deletedKeyStr = this.mAmText;
                    } else if (deleted == getAmOrPmKeyCode(PM)) {
                        deletedKeyStr = this.mPmText;
                    } else {
                        deletedKeyStr = String.format(
                                            Helpers.getLocale(getContext()), "%d",
                                            getValFromKeyCode(deleted));
                    }
                    Utils.tryAccessibilityAnnounce(this.mTimePicker, String
                                                   .format(this.mDeletedKeyFormat, deletedKeyStr));
                    updateDisplay(true);
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_0
                   || keyCode == KeyEvent.KEYCODE_1
                   || keyCode == KeyEvent.KEYCODE_2
                   || keyCode == KeyEvent.KEYCODE_3
                   || keyCode == KeyEvent.KEYCODE_4
                   || keyCode == KeyEvent.KEYCODE_5
                   || keyCode == KeyEvent.KEYCODE_6
                   || keyCode == KeyEvent.KEYCODE_7
                   || keyCode == KeyEvent.KEYCODE_8
                   || keyCode == KeyEvent.KEYCODE_9
                   || !this.mIs24HourMode
                   && (keyCode == getAmOrPmKeyCode(AM) || keyCode == getAmOrPmKeyCode(PM))) {
            if (!this.mInKbMode) {
                if (this.mTimePicker == null) {
                    // Something's wrong, because time picker should definitely
                    // not be null.
                    Log.e(TAG,
                          "Unable to initiate keyboard mode, TimePicker was null.");
                    return true;
                }
                this.mTypedTimes.clear();
                tryStartingKbMode(keyCode);
                return true;
            }
            // We're already in keyboard mode.
            if (addKeyIfLegal(keyCode)) {
                updateDisplay(false);
            }
            return true;
        }
        return false;
    }

    public void set24HourMode(final boolean mode) {
        this.mIs24HourMode = mode;
        if (this.mTimePicker != null) {
            this.mTimePicker.initialize(this.ctx, mTime, this.mIs24HourMode);
            this.mTimePicker.invalidate();
        }
        updateDisplay(true);
    }

    // Show either Hours or Minutes.
    protected void setCurrentItemShowing(final int index,
                                         final boolean animateCircle, final boolean delayLabelAnimate,
                                         final boolean announce) {
        this.mTimePicker.setCurrentItemShowing(index, animateCircle);
        final TextView labelToAnimate;
        if (index == HOUR_INDEX) {
            int hours = this.mTimePicker.getTime().getHourOfDay();
            if (!this.mIs24HourMode) {
                hours = hours % 12;
            }
            this.mTimePicker.setContentDescription(this.mHourPickerDescription
                                                   + ": " + hours);
            if (announce) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker,
                                               this.mSelectHours);
            }
            labelToAnimate = this.mHourView;
        } else {
            final int minutes = this.mTimePicker.getTime().getMinuteOfHour();
            this.mTimePicker
            .setContentDescription(this.mMinutePickerDescription + ": "
                                   + minutes);
            if (announce) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker,
                                               this.mSelectMinutes);
            }
            labelToAnimate = this.mMinuteView;
        }
        final int hourColor = (index == HOUR_INDEX) ? this.mSelectedColor : this.mUnselectedColor;
        final int minuteColor = (index == MINUTE_INDEX) ? this.mSelectedColor : this.mUnselectedColor;
        this.mHourView.setTextColor(hourColor);
        this.mMinuteView.setTextColor(minuteColor);
        final com.nineoldandroids.animation.ObjectAnimator pulseAnimator = Utils
                .getPulseAnimator(labelToAnimate, 0.85f, 1.1f);
        if (delayLabelAnimate) {
            pulseAnimator.setStartDelay(PULSE_ANIMATOR_DELAY);
        }
        pulseAnimator.start();
    }

    public void setHour(int value, final boolean announce) {
        final String format;
        if (this.mIs24HourMode) {
            format = "%02d";
        } else {
            updateAmPmDisplay(((value / 12) == 1) ? PM : AM);
            format = "%d";
            value = value % 12;
            if (value == 0) {
                value = 12;
            }

        }
        final CharSequence text = String.format(format, value);
        if (this.mHourView != null) {
            this.mHourView.setText(text);
        }
        if (this.mHourSpaceView != null) {
            this.mHourSpaceView.setText(text);
        }
        if (announce) {
            Utils.tryAccessibilityAnnounce(this.mTimePicker, text);
        }
    }

    public void setMinute(int value) {
        if (value == 60) {
            value = 0;
        }
        final CharSequence text = String.format(Locale.getDefault(), "%02d",
                                                value);
        Utils.tryAccessibilityAnnounce(this.mTimePicker, text);
        if (this.mMinuteView != null) {
            this.mMinuteView.setText(text);
        }
        if (this.mMinuteSpaceView != null) {
            this.mMinuteSpaceView.setText(text);
        }
    }

    public void setOnKeyListener(final KeyboardListener keyboardListener) {
        if (this.mDoneButton != null) {
            this.mDoneButton.setOnKeyListener(keyboardListener);
        }
        if (this.mNoDateButton != null) {
            this.mNoDateButton.setOnKeyListener(keyboardListener);
        }
        if (this.mMinuteView != null) {
            this.mMinuteView.setOnKeyListener(keyboardListener);
        }
        if (this.mHourView != null) {
            this.mHourView.setOnKeyListener(keyboardListener);
        }
        if (this.mAmPmTextView != null) {
            this.mAmPmTextView.setOnKeyListener(keyboardListener);
        }
        if (this.mTimePicker != null) {
            this.mTimePicker.setOnKeyListener(keyboardListener);
        }
    }

    public void setOnTimeSetListener(final OnTimeSetListener callback) {
        this.mCallback = callback;
    }

    public void setTime(final @NonNull LocalTime time) {
        setHour(time.getHourOfDay(), false);
        setMinute(time.getMinuteOfHour());
        mTime = time;
        this.mTimePicker.setTime(time);
        setCurrentItemShowing(HOUR_INDEX, true, false, true);
    }

    /**
     * Try to start keyboard mode with the specified key, as long as the
     * timepicker is not in the middle of a touch-event.
     *
     * @param keyCode
     *            The key to use as the first press. Keyboard mode will not be
     *            started if the key is not legal to start with. Or, pass in -1
     *            to get into keyboard mode without a starting key.
     */
    private void tryStartingKbMode(final int keyCode) {
        if (this.mTimePicker.trySettingInputEnabled(false)
            && (keyCode == -1 || addKeyIfLegal(keyCode))) {
            this.mInKbMode = true;
            this.mDoneButton.setEnabled(false);
            updateDisplay(false);
        }
    }

    private void updateAmPmDisplay(final int amOrPm) {
        if (amOrPm == AM) {
            this.mAmPmTextView.setText(this.mAmText);
            Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mAmText);
            this.mAmPmHitspace.setContentDescription(this.mAmText);
        } else if (amOrPm == PM) {
            this.mAmPmTextView.setText(this.mPmText);
            Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mPmText);
            this.mAmPmHitspace.setContentDescription(this.mPmText);
        } else {
            this.mAmPmTextView.setText(this.mDoublePlaceholderText);
        }
    }

    /**
     * Update the hours, minutes, and AM/PM displays with the typed times. If
     * the typedTimes is empty, either show an empty display (filled with the
     * placeholder text), or update from the timepicker's values.
     *
     * @param allowEmptyDisplay
     *            if true, then if the typedTimes is empty, use the placeholder
     *            text. Otherwise, revert to the timepicker's values.
     */
    private void updateDisplay(final boolean allowEmptyDisplay) {
        if (!allowEmptyDisplay && this.mTypedTimes.isEmpty()) {
            final int hour = this.mTimePicker.getTime().getHourOfDay();
            final int minute = this.mTimePicker.getTime().getMinuteOfHour();
            setHour(hour, true);
            setMinute(minute);
            if (!this.mIs24HourMode) {
                updateAmPmDisplay((hour < 12) ? AM : PM);
            }
            setCurrentItemShowing(this.mTimePicker.getCurrentItemShowing(),
                                  true, true, true);
            this.mDoneButton.setEnabled(true);
        } else {
            final Boolean[] enteredZeros = { false, false };
            final int[] values = getEnteredTime(enteredZeros);
            final String hourFormat = enteredZeros[0] ? "%02d" : "%2d";
            final String minuteFormat = enteredZeros[1] ? "%02d" : "%2d";
            final String hourStr = (values[0] == -1) ? this.mDoublePlaceholderText : String.format(hourFormat,
                                   values[0]).replace(' ',
                                           this.mPlaceholderText);
            final String minuteStr = (values[1] == -1) ? this.mDoublePlaceholderText : String.format(
                                         minuteFormat, values[1]).replace(' ',
                                                 this.mPlaceholderText);
            this.mHourView.setText(hourStr);
            this.mHourSpaceView.setText(hourStr);
            this.mHourView.setTextColor(this.mUnselectedColor);
            this.mMinuteView.setText(minuteStr);
            this.mMinuteSpaceView.setText(minuteStr);
            this.mMinuteView.setTextColor(this.mUnselectedColor);
            if (!this.mIs24HourMode) {
                updateAmPmDisplay(values[2]);
            }
        }
    }

}
