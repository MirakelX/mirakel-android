/*
 * DragSortRecycler
 *
 * Added drag and drop functionality to your RecyclerView
 *
 *
 * Copyright 2014 Emile Belanger.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emtronics.dragsortrecycler;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class DragSortRecycler extends RecyclerView.ItemDecoration implements
    RecyclerView.OnItemTouchListener {

    final String TAG = "RecyclerDragSort";

    final boolean DEBUG = false;

    private int dragHandleWidth = 0;

    private long selectedDragItem = -1;

    private int fingerAnchorY;

    private int fingerY;

    private int fingerOffsetInViewY;

    private float autoScrollWindow = 0.1f;
    private float autoScrollSpeed = 0.5f;

    private BitmapDrawable floatingItem;
    private Rect           floatingItemStatingBounds;
    private Rect           floatingItemBounds;


    private float floatingItemAlpha = 0.5f;
    private int   floatingItemBgColor = 0;

    private int viewHandleId = -1;


    ItemMovedInterface moveInterface;

    private boolean isDragging;
    @Nullable
    OnDragStateChangedListener dragStateChangedListener;


    public interface ItemMovedInterface {
        public void moveElement(int from, int to);
    }

    public interface OnDragStateChangedListener {
        public void onDragStart();
        public void onDragStop();
    }

    private void debugLog(String log) {
        if (DEBUG) {
            Log.d(TAG, log);
        }
    }

    public RecyclerView.OnScrollListener getScrollListener() {
        return scrollListener;
    }

    /*
     * Set the item move interface
     */
    public void setItemMoveInterface(ItemMovedInterface swif) {
        moveInterface = swif;
    }

    public void setViewHandleId(int id) {
        viewHandleId = id;
    }

    public void setLeftDragArea(int w) {
        dragHandleWidth = w;
    }

    public void setFloatingAlpha(float a) {
        floatingItemAlpha = a;
    }

    public void setFloatingBgColor(int c) {
        floatingItemBgColor = c;
    }
    /*
     Set the window at top and bottom of list, must be between 0 and 0.5
     For example 0.1 uses the top and bottom 10% of the lists for scrolling
     */
    public void setAutoScrollWindow(float w) {
        autoScrollWindow = w;
    }

    /*
    Set the autoscroll speed, default is 0.5
     */
    public void setAutoScrollSpeed(float speed) {
        autoScrollSpeed = speed;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView rv, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, rv, state);

        debugLog("getItemOffsets");

        debugLog("View top = " + view.getTop());
        if (selectedDragItem != -1) {
            long itemId =  rv.getChildItemId(view);
            debugLog("itemId =" + itemId);

            //Movement of finger
            float totalMovment = fingerY - fingerAnchorY;

            if (itemId == selectedDragItem) {
                view.setVisibility(View.INVISIBLE);
            } else {
                //Make view visible uncase invisible
                view.setVisibility(View.VISIBLE);

                //Find middle of the floatingItem
                float floatMiddleY = floatingItemBounds.top + floatingItemBounds.height() / 2;

                //Moving down the list
                //These will auto-animate if the device continually send touch motion events
                // if (totalMovment>0)
                {
                    if ((itemId > selectedDragItem) && (view.getTop() < floatMiddleY)) {
                        float amountUp = ((float)floatMiddleY - view.getTop()) / (float)view.getHeight();
                        //  amountUp *= 0.5f;
                        if (amountUp > 1) {
                            amountUp = 1;
                        }

                        outRect.top = -(int)(floatingItemBounds.height() * amountUp);
                        outRect.bottom = (int)(floatingItemBounds.height() * amountUp);
                    }

                }//Moving up the list
                // else if (totalMovment < 0)
                {
                    if ((itemId < selectedDragItem) && (view.getBottom() > floatMiddleY)) {
                        float amountDown = ((float)view.getBottom() - floatMiddleY) / (float)view.getHeight();
                        //  amountDown *= 0.5f;
                        if (amountDown > 1) {
                            amountDown = 1;
                        }

                        outRect.top = (int)(floatingItemBounds.height() * amountDown);
                        outRect.bottom = -(int)(floatingItemBounds.height() * amountDown);
                    }
                }
            }
        } else {
            outRect.top = 0;
            outRect.bottom = 0;
            //Make view visible incase invisible
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Find the new position by scanning through the items on
     * screen and finding the positional relationship.
     * This *seems* to work, another method would be to use
     * getItemOffsets, but I think that could miss items?..
     */
    private long getNewPostion(RecyclerView rv) {
        int itemsOnScreen = rv.getLayoutManager().getChildCount();

        float floatMiddleY = floatingItemBounds.top + floatingItemBounds.height() / 2;

        long above = 0;
        long below = rv.getLayoutManager().getItemCount();
        for (int n = 0; n < itemsOnScreen; n++) { //Scan though items on screen, however they may not
            // be in order!
            View view = rv.getLayoutManager().getChildAt(n);
            long itemId = rv.getChildItemId(view);

            if (itemId == selectedDragItem) { //Don't check against itself!
                continue;
            }

            float viewMiddleY = view.getTop() + view.getHeight() / 2;
            if (floatMiddleY > viewMiddleY) { //Is above this item

                if (itemId > above) {
                    above = itemId;
                }
            } else if (floatMiddleY <= viewMiddleY) { //Is below this item

                if (itemId < below) {
                    below = itemId;
                }
            }
        }
        debugLog("above = " + above + " below = " + below);

        if (below < selectedDragItem) { //Need to count itself
            below++;
        }

        return below - 1;
    }


    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        Log.d(TAG, "onInterceptTouchEvent");

        //if (e.getAction() == MotionEvent.ACTION_DOWN)
        {
            View itemView = rv.findChildViewUnder(e.getX(), e.getY());

            if (itemView == null) {
                return false;
            }

            boolean dragging = false;

            if ((dragHandleWidth > 0 ) && (e.getX() < dragHandleWidth)) {
                dragging = true;
            } else if (viewHandleId != -1) {
                //Find the handle in the list item
                View handleView = itemView.findViewById(viewHandleId);

                if (handleView == null) {
                    Log.e(TAG, "The view ID " + viewHandleId + " was not found in the RecycleView item");
                    return false;
                }

                //We need to find the relative position of the handle to the parent view
                //Then we can work out if the touch is within the handle
                int[] parentItemPos = new int[2];
                itemView.getLocationInWindow(parentItemPos);

                int[] handlePos = new int[2];
                handleView.getLocationInWindow(handlePos);

                int xRel = handlePos[0] - parentItemPos[0];
                int yRel = handlePos[1] - parentItemPos[1];

                Rect touchBounds = new Rect(itemView.getLeft() + xRel, itemView.getTop() + yRel,
                                            itemView.getLeft() + xRel + handleView.getWidth(),
                                            itemView.getTop() + yRel  + handleView.getHeight()
                                           );

                if (touchBounds.contains((int)e.getX(), (int)e.getY())) {
                    dragging = true;
                }

                debugLog("parentItemPos = " + parentItemPos[0] + " " + parentItemPos[1]);
                debugLog("handlePos = " + handlePos[0] + " " + handlePos[1]);

            }

            setIsDragging(dragging);
            if (dragging) {
                debugLog("Started Drag");

                floatingItem = creatFloatingBitmap(itemView);

                fingerAnchorY = (int)e.getY();
                fingerOffsetInViewY = fingerAnchorY - itemView.getTop();
                fingerY = fingerAnchorY;

                selectedDragItem = rv.getChildItemId(itemView);
                debugLog("selectedDragItem = " + selectedDragItem);

                return true;
            }
        }
        return false;
    }

    private void setIsDragging(final boolean dragging) {
        if (dragging != isDragging) {
            isDragging = dragging;
            if (dragStateChangedListener != null) {
                if (isDragging) {
                    dragStateChangedListener.onDragStart();
                } else {
                    dragStateChangedListener.onDragStop();
                }
            }
        }
    }
    public void setDragStateChangedListener(final OnDragStateChangedListener dragStateChangedListener) {
        this.dragStateChangedListener = dragStateChangedListener;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        debugLog("onTouchEvent");

        if ((e.getAction() == MotionEvent.ACTION_UP) ||
            (e.getAction() == MotionEvent.ACTION_CANCEL)) {
            if ((e.getAction() == MotionEvent.ACTION_UP) && selectedDragItem != -1) {
                long newPos = getNewPostion(rv);
                if (moveInterface != null) {
                    moveInterface.moveElement((int) selectedDragItem, (int) newPos);
                }
            }

            selectedDragItem = -1;
            floatingItem = null;
            rv.invalidateItemDecorations();
            return;
        }
        fingerY = (int)e.getY();

        if (floatingItem != null) {
            floatingItemBounds.top = fingerY - fingerOffsetInViewY;

            if (floatingItemBounds.top < -floatingItemStatingBounds.height() /
                2) { //Allow half the view out the top
                floatingItemBounds.top = -floatingItemStatingBounds.height() / 2;
            }

            floatingItemBounds.bottom = floatingItemBounds.top + floatingItemStatingBounds.height();

            floatingItem.setBounds(floatingItemBounds);
        }

        //Do auto scrolling at end of list
        float scrollAmmount = 0;
        if (fingerY > (rv.getHeight() * (1 - autoScrollWindow))) {
            scrollAmmount = (fingerY - (rv.getHeight() * (1 - autoScrollWindow)));
        } else if (fingerY < (rv.getHeight() * autoScrollWindow)) {
            scrollAmmount = (fingerY - (rv.getHeight() * autoScrollWindow));
        }
        debugLog("Scroll: " + scrollAmmount);

        scrollAmmount *= autoScrollSpeed;
        rv.scrollBy(0, (int)scrollAmmount);

        rv.invalidateItemDecorations();// Redraw
    }

    Paint bgColor = new Paint();
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (floatingItem != null) {
            floatingItem.setAlpha((int)(255 * floatingItemAlpha));
            bgColor.setColor(floatingItemBgColor);
            c.drawRect(floatingItemBounds, bgColor);
            floatingItem.draw(c);
        }
    }

    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            // TODO Auto-generated method stub
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            debugLog("Scrolled: " + dx + " " + dy);
            fingerAnchorY -= dy;
        }
    };


    private BitmapDrawable creatFloatingBitmap(View v) {
        floatingItemStatingBounds = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        floatingItemBounds = new Rect(floatingItemStatingBounds);

        Bitmap bitmap = Bitmap.createBitmap(floatingItemStatingBounds.width(),
                                            floatingItemStatingBounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);

        BitmapDrawable retDrawable = new BitmapDrawable(v.getResources(), bitmap);
        retDrawable.setBounds(floatingItemBounds);

        return retDrawable;
    }

}
