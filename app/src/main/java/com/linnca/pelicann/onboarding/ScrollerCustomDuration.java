package com.linnca.pelicann.onboarding;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

//used for a slower view pager
class ScrollerCustomDuration extends Scroller {
    private double mScrollFactor = 1;

    ScrollerCustomDuration(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    /**
     * Set the factor by which the duration will change
     */
    void setScrollDurationFactor(double scrollFactor) {
        mScrollFactor = scrollFactor;
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, (int) (duration * mScrollFactor));
    }

}