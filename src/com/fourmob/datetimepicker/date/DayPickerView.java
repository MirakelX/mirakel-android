package com.fourmob.datetimepicker.date;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.ListView;
import de.azapps.tools.Log;

public class DayPickerView extends ListView implements
		AbsListView.OnScrollListener, DatePicker.OnDateChangedListener {
	private static final String TAG = "DayPickerView";
	public static int LIST_TOP_OFFSET = -1;
	protected SimpleMonthAdapter mAdapter;
	protected Context mContext;
	private final DatePickerController mController;
	protected int mCurrentMonthDisplayed;
	protected int mCurrentScrollState = 0;
	protected int mDaysPerWeek = 7;
	protected float mFriction = 1.0F;
	protected Handler mHandler = new Handler();
	protected int mNumWeeks = 6;
	private boolean mPerformingScroll;
	protected long mPreviousScrollPosition;
	protected int mPreviousScrollState = 0;
	protected ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();
	protected SimpleMonthAdapter.CalendarDay mSelectedDay = new SimpleMonthAdapter.CalendarDay();
	protected boolean mShowWeekNumber = false;
	protected SimpleMonthAdapter.CalendarDay mTempDay = new SimpleMonthAdapter.CalendarDay();

	public DayPickerView(final Context context,
			final DatePickerController datePickerController) {
		super(context);
		this.mController = datePickerController;
		this.mController.registerOnDateChangedListener(this);
		setLayoutParams(new AbsListView.LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT));
		setDrawSelectorOnTop(false);
		init(context);
		onDateChanged();
	}

	public int getMostVisiblePosition() {
		final int firstVisiblePosition = getFirstVisiblePosition();
		final int height = getHeight();
		int maxGap = 0;
		int mostVisiblePosition = 0;
		int childIndex = 0;
		int bottom = 0;
		View childView = null;
		while ((childView = getChildAt(childIndex)) != null) {
			if (bottom < height) {
				bottom = childView.getBottom();
				final int gap = Math.min(bottom, height)
						- Math.max(0, childView.getTop());
				if (gap > maxGap) {
					mostVisiblePosition = childIndex;
					maxGap = gap;
				}
			} else {
				return firstVisiblePosition + mostVisiblePosition;
			}
			childIndex++;
		}
		return firstVisiblePosition + mostVisiblePosition;
	}

	public boolean goTo(final SimpleMonthAdapter.CalendarDay calendarDay,
			final boolean scrollToTop, final boolean selectDay,
			final boolean displayMonth) {
		Log.w(TAG, "goto");
		if (selectDay) {
			this.mSelectedDay.set(calendarDay);
		}

		this.mTempDay.set(calendarDay);
		final int monthIndex = 12
				* (calendarDay.year - this.mController.getMinYear())
				+ calendarDay.month;
		postSetSelection(monthIndex);

		// TODO improve

		return true;
	}

	public void init(final Context paramContext) {
		this.mContext = paramContext;
		setUpListView();
		setUpAdapter();
		setAdapter(this.mAdapter);
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		if (this.mPerformingScroll) {
			this.mPerformingScroll = false;
		}
	}

	public void onChange() {
		setUpAdapter();
		setAdapter(this.mAdapter);
	}

	@Override
	public void onDateChanged() {
		goTo(this.mController.getSelectedDay(), false, true, true);
	}

	@Override
	public void onScroll(final AbsListView absListView,
			final int firstVisibleItem, final int visibleItemCount,
			final int totalItemCount) {
		final SimpleMonthView simpleMonthView = (SimpleMonthView) absListView
				.getChildAt(0);
		if (simpleMonthView == null) {
			return;
		}
		this.mPreviousScrollPosition = absListView.getFirstVisiblePosition()
				* simpleMonthView.getHeight() - simpleMonthView.getBottom();
		this.mPreviousScrollState = this.mCurrentScrollState;
	}

	@Override
	public void onScrollStateChanged(final AbsListView absListView,
			final int scroll) {
		this.mScrollStateChangedRunnable.doScrollStateChange(absListView,
				scroll);
	}

	public void postSetSelection(final int position) {
		clearFocus();
		post(new Runnable() {
			@Override
			public void run() {
				DayPickerView.this.setSelection(position);
			}
		});
		onScrollStateChanged(this, 0);
	}

	protected void setMonthDisplayed(
			final SimpleMonthAdapter.CalendarDay calendarDay) {
		this.mCurrentMonthDisplayed = calendarDay.month;
		invalidateViews();
	}

	protected void setUpAdapter() {
		if (this.mAdapter == null) {
			this.mAdapter = new SimpleMonthAdapter(getContext(),
					this.mController);
		}
		this.mAdapter.setSelectedDay(this.mSelectedDay);
		this.mAdapter.notifyDataSetChanged();
	}

	protected void setUpListView() {
		setCacheColorHint(0);
		setDivider(null);
		setItemsCanFocus(true);
		setFastScrollEnabled(false);
		setVerticalScrollBarEnabled(false);
		setOnScrollListener(this);
		setFadingEdgeLength(0);
		setFrictionIfSupported(ViewConfiguration.getScrollFriction()
				* this.mFriction);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void setFrictionIfSupported(final float friction) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setFriction(friction);
		}
	}

	protected class ScrollStateRunnable implements Runnable {

		protected ScrollStateRunnable() {
		}

		public void doScrollStateChange(final AbsListView absListView,
				final int newState) {
			DayPickerView.this.mHandler.removeCallbacks(this);
			DayPickerView.this.mHandler.postDelayed(this, 40L);
		}

		@Override
		public void run() {
			// TODO scroll to the closest month
		}
	}
}