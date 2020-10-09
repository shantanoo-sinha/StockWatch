package com.shantanoo.stockwatch;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Shantanoo on 10/8/2020.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private Drawable mDivider;

    public DividerItemDecoration(Drawable divider) {
        this.mDivider = divider;
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin;
            final int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left+10, top, right-10, bottom);
            mDivider.draw(canvas);
        }
    }
}
