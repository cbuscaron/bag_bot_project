package com.flomio.smartcartlib.activity.pairing;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;

public class SignalStrengthColor {
    public static final int RSSI_TAP_THRESHOLD = -30;
    private long max = RSSI_TAP_THRESHOLD;
    private long min = -60;

    public float factor;

    public SignalStrengthColor(View background) {
        this.background = background;
    }

    private int currentColor;
    private View background;
    private ValueAnimator colorAnimation;

    public void finish() {
        this.currentColor = 0;
        this.background.setBackgroundColor(0);
        if (this.colorAnimation != null) {
            this.colorAnimation.cancel();
            this.colorAnimation = null;
        }
    }

    public void onRssi(int rssi) {
        long abs = (Math.abs(max - rssi));
        abs -= abs % 5;
        factor = 1 - (1f / Math.abs(max - min)) * abs;
        factor = Math.max(0, factor);
        factor = Math.min(1, factor);

        if (colorAnimation != null) {
            colorAnimation.cancel();
        }
        int factored = RedToGreen.getFactored(Math.min
                (factor, 1f));
        colorAnimation = ValueAnimator.ofObject(new
                ArgbEvaluator(), currentColor, factored);
        currentColor = factored;
        colorAnimation.setDuration(400);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator s) {
                currentColor = (Integer) s.getAnimatedValue();
                background.setBackgroundColor(
                        currentColor);
            }
        });
        colorAnimation.start();
    }

    public static class RedToGreen {
        private static ArgbEvaluator evaluator = new ArgbEvaluator();
        public static int getFactored(float factor) {
            return (Integer) evaluator.evaluate(factor, Color.RED, Color.GREEN);
        }
    }
}
