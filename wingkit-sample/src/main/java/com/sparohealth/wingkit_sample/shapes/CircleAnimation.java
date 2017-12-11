package com.sparohealth.wingkit_sample.shapes;

import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by darien.sandifer on 12/5/2017.
 */

public class CircleAnimation extends Animation {
    private CircleView circle;
    private int oldRadius;
    private int newRadius;

    public CircleAnimation(CircleView newCircle,int radius){
        circle = newCircle;
        oldRadius = circle.getRadius();
        newRadius = radius;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        circle.setRadius(newRadius);
        circle.requestLayout();
    }
}
