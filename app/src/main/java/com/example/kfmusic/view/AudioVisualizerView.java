package com.example.kfmusic.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.kfmusic.R;

public class AudioVisualizerView extends View {

    private static final int BAR_COUNT = 6;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isPlaying = false;
    private final float[] heights = new float[BAR_COUNT];
    private final float[] targets = new float[BAR_COUNT];
    private final float[] speeds = new float[BAR_COUNT];
    private long lastTime = 0;

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.accent_blue));
        for (int i = 0; i < BAR_COUNT; i++) {
            heights[i] = 0.2f; // scale factor
            targets[i] = 0.2f;
            speeds[i] = 0.05f + (float) Math.random() * 0.1f;
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (playing) {
            lastTime = System.currentTimeMillis();
            invalidate();
        } else {
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float spacing = width / (float) (BAR_COUNT * 2 - 1);
        float barWidth = spacing;

        long now = System.currentTimeMillis();
        float deltaTime = (now - lastTime) / 1000f;
        if (deltaTime > 0.1f) deltaTime = 0.1f; // cap delta
        lastTime = now;

        for (int i = 0; i < BAR_COUNT; i++) {
            if (isPlaying) {
                if (Math.abs(heights[i] - targets[i]) < 0.05f) {
                    targets[i] = 0.15f + (float) Math.random() * 0.85f;
                    speeds[i] = 0.1f + (float) Math.random() * 0.3f;
                }
                if (heights[i] < targets[i]) {
                    heights[i] += speeds[i] * deltaTime * 5f;
                    if (heights[i] > targets[i]) heights[i] = targets[i];
                } else {
                    heights[i] -= speeds[i] * deltaTime * 5f;
                    if (heights[i] < targets[i]) heights[i] = targets[i];
                }
            } else {
                // shrink to min height
                if (heights[i] > 0.15f) {
                    heights[i] -= 2f * deltaTime;
                    if (heights[i] < 0.15f) heights[i] = 0.15f;
                }
            }

            float barHeight = heights[i] * height;
            // Rounded bar top
            float left = i * spacing * 2;
            float top = height - barHeight;
            float right = left + barWidth;
            float bottom = height;
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2f, barWidth / 2f, paint);
        }

        if (isPlaying || hasAnimatingBars()) {
            postInvalidateOnAnimation();
        }
    }

    private boolean hasAnimatingBars() {
        for (float h : heights) {
            if (h > 0.16f) return true;
        }
        return false;
    }
}
