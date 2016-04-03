package com.listen2youtube;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.amulyakhare.textdrawable.util.ColorGenerator;

/**
 * Created by khang on 03/04/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class ProgressBar extends View {
    private static final String TAG = "ProgressBar";

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int color = ColorGenerator.MATERIAL.getRandomColor();
    private int progress = 0;

    public ProgressBar(Context context) {
        this(context, null);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setColor(int color) {
        this.color = color;
        postInvalidate();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(new RectF(0, 0, getWidth() * (progress / 100.0f), getHeight()), paint);
    }
}
