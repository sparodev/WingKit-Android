package com.sparohealth.wingkit_sample.shapes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by darien.sandifer on 11/1/2017.
 */

public class CircleView extends View {
    private Paint paint;
    private Canvas canvas;
    private int radius = 600;
    public CircleView(Context context) {
        super(context);

        paint = new Paint();
        paint.setColor(Color.parseColor("#FF33b5e5"));
    }

    @Override
    protected void onDraw(Canvas newCanvas) {
        super.onDraw(canvas);
        canvas = newCanvas;
        float xPoint = getWidth()/2;
        float yPoint = getHeight()+200;

        canvas.drawCircle(xPoint,yPoint,radius,paint);

    }

    public int getRadius(){
        return radius;
    }

    public void setRadius(int newRad){
        radius = newRad;
    }
}
