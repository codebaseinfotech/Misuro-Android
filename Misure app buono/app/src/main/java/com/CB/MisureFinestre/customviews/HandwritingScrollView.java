package com.CB.MisureFinestre.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import com.bugfender.sdk.Bugfender;

/**
 * Custom ScrollView that doesn't intercept touch events meant for EditTexts,
 * specifically fixing stylus handwriting input issues.
 */
public class HandwritingScrollView extends ScrollView {

    public HandwritingScrollView(Context context) {
        super(context);
    }

    public HandwritingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HandwritingScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Check if touch is from stylus/pen
        boolean isStylus = (ev.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                           ev.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER);

        if (isStylus) {
            // Find which view is being touched
            View touchedView = findViewAt((int) ev.getRawX(), (int) ev.getRawY(), this);

            if (touchedView instanceof EditText) {
                // Don't intercept - let EditText handle stylus input for handwriting
                Bugfender.d("SCROLLVIEW", "Stylus touch on EditText - NOT intercepting");
                return false;
            }
        }

        // For non-stylus touches or non-EditText touches, use default behavior
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Recursively find the view at the given coordinates
     */
    private View findViewAt(int x, int y, View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = viewGroup.getChildCount() - 1; i >= 0; i--) {
                View child = viewGroup.getChildAt(i);

                // Get child's position on screen
                int[] location = new int[2];
                child.getLocationOnScreen(location);

                int left = location[0];
                int top = location[1];
                int right = left + child.getWidth();
                int bottom = top + child.getHeight();

                // Check if touch is within child bounds
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    // Recursively check this child's children
                    View found = findViewAt(x, y, child);
                    if (found != null) {
                        return found;
                    }
                    return child;
                }
            }
        }
        return view;
    }
}
