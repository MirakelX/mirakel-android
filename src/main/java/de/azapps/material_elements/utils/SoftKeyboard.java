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

package de.azapps.material_elements.utils;

/*
 * Author: Felipe Herranz (felhr85@gmail.com)
 * Contributors:Francesco Verheye (verheye.francesco@gmail.com)
 * 		Israel Dominguez (dominguez.israel@gmail.com)
 */

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoftKeyboard implements View.OnFocusChangeListener
{
    private static final int CLEAR_FOCUS = 0;

    private final ViewGroup layout;
    private int layoutBottom;
    private final InputMethodManager im;
    private final int[] coords=new int[2];
    private boolean isKeyboardShow=false;
    private final SoftKeyboardChangesThread softKeyboardThread=new SoftKeyboardChangesThread();
    @NonNull
    private List<EditText> editTextList=new ArrayList<>();

    private View tempView; // reference to a focused EditText

    public SoftKeyboard(final ViewGroup layout, final InputMethodManager im)
    {
        this.layout = layout;
        keyboardHideByDefault();
        initEditTexts(layout);
        this.im = im;
        this.softKeyboardThread.start();
    }


    public void openSoftKeyboard()
    {
        if(!isKeyboardShow)
        {
            layoutBottom = getLayoutCoordinates();
            im.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);
            softKeyboardThread.keyboardOpened();
            isKeyboardShow = true;
        }
    }

    public void closeSoftKeyboard()
    {
        if(isKeyboardShow)
        {
            im.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            isKeyboardShow = false;
        }
    }

    public void setSoftKeyboardCallback(final SoftKeyboardChanged mCallback)
    {
        softKeyboardThread.setCallback(mCallback);
    }

    public void unRegisterSoftKeyboardCallback()
    {
        softKeyboardThread.stopThread();
    }

    public interface SoftKeyboardChanged
    {
        public void onSoftKeyboardHide();
        public void onSoftKeyboardShow();
    }

    private int getLayoutCoordinates()
    {
        layout.getLocationOnScreen(coords);
        return coords[1] + layout.getHeight();
    }

    private void keyboardHideByDefault()
    {
        layout.setFocusable(true);
        layout.setFocusableInTouchMode(true);
    }

    /*
     * InitEditTexts now handles EditTexts in nested views
     * Thanks to Francesco Verheye (verheye.francesco@gmail.com)
     */
    private void initEditTexts(final ViewGroup viewgroup)
    {

        final int childCount = viewgroup.getChildCount();
        for(int i=0; i< childCount;i++)
        {
            final View v = viewgroup.getChildAt(i);

            if(v instanceof ViewGroup)
            {
                initEditTexts((ViewGroup) v);
            }

            if(v instanceof EditText)
            {
                handleAddEdittext((EditText) v);
            }
        }
    }

    private void handleAddEdittext(final EditText v) {
        v.setOnFocusChangeListener(this);
        v.setCursorVisible(true);
        editTextList.add(v);
    }

    /*
     * OnFocusChange does update tempView correctly now when keyboard is still shown
     * Thanks to Israel Dominguez (dominguez.israel@gmail.com)
     */
    @Override
    public void onFocusChange(final View v, final boolean hasFocus)
    {
        if(hasFocus)
        {
            tempView = v;
            if(!isKeyboardShow)
            {
                layoutBottom = getLayoutCoordinates();
                softKeyboardThread.keyboardOpened();
                isKeyboardShow = true;
            }
        }
    }

    // This handler will clear focus of selected EditText
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message m)
        {
            switch(m.what)
            {
                case CLEAR_FOCUS:
                    if(tempView != null)
                    {
                        tempView.clearFocus();
                        tempView = null;
                    }
                    break;
            }
        }
    };

    private class SoftKeyboardChangesThread extends Thread
    {
        private static final String TAG = "SoftKeyboardChangesThread";
        private final AtomicBoolean started;
        private SoftKeyboardChanged mCallback;

        public SoftKeyboardChangesThread()
        {
            started = new AtomicBoolean(true);
        }

        public void setCallback(final SoftKeyboardChanged mCallback)
        {
            this.mCallback = mCallback;
        }

        @Override
        public void run()
        {
            while(started.get())
            {
                // Wait until keyboard is requested to open
                synchronized(this)
                {
                    try
                    {
                        wait();
                    } catch (final InterruptedException e)
                    {
                        Log.wtf(TAG,"Thread killed",e);
                    }
                }

                int currentBottomLocation = getLayoutCoordinates();

                // There is some lag between open soft-keyboard function and when it really appears.
                while((currentBottomLocation == layoutBottom) && started.get())
                {
                    currentBottomLocation = getLayoutCoordinates();
                }

                if(started.get()) {
                    mCallback.onSoftKeyboardShow();
                }

                // When keyboard is opened from EditText, initial bottom location is greater than layoutBottom
                // and at some moment equals layoutBottom.
                // That broke the previous logic, so I added this new loop to handle this.
                while((currentBottomLocation >= layoutBottom) && started.get())
                {
                    currentBottomLocation = getLayoutCoordinates();
                }

                // Now Keyboard is shown, keep checking layout dimensions until keyboard is gone
                while((currentBottomLocation != layoutBottom) && started.get())
                {
                    synchronized(this)
                    {
                        try
                        {
                            wait(500L);
                        } catch (final InterruptedException e)
                        {
                            Log.wtf(TAG, "Thread killed", e);
                        }
                    }
                    currentBottomLocation = getLayoutCoordinates();
                }

                if(started.get()) {
                    mCallback.onSoftKeyboardHide();
                }

                // if keyboard has been opened clicking and EditText.
                if(isKeyboardShow && started.get()) {
                    isKeyboardShow = false;
                }

                // if an EditText is focused, remove its focus (on UI thread)
                if(started.get()) {
                    mHandler.obtainMessage(CLEAR_FOCUS).sendToTarget();
                }
            }
        }

        public void keyboardOpened()
        {
            synchronized(this)
            {
                notify();
            }
        }

        public void stopThread()
        {
            synchronized(this)
            {
                started.set(false);
                notify();
            }
        }

    }
}