package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * The selection: a light rim around the focused tile, in the style of Apple's Liquid Glass, with
 * no fill, no brighter top and no outer glow, so the app underneath keeps its own brightness. One instance floats over the grid and is moved
 * with spring animations between tiles.
 */
public class GlassView extends View {

    private final GlassRim rim;
    private final RectF rect = new RectF();
    private final float density;
    private float radius;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 20f * density;
        // Subtle cyan electric focus rim matching Section 4 requirements
        rim = new GlassRim(2.0f * density, 0xFF38BDF8, 0x4D38BDF8);
        // Cache the slab as a texture. The spring only changes translation, so the GPU can
        // move it without replaying clipPath and the glow on every frame. That path is
        // software-rendered on the MT5895 and is what makes the highlight crawl.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    void setRadius(float px) {
        radius = px;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The slab keeps its old size: this inset once made room for an outer glow, and the tile
        // positions and the spring are measured around it.
        float inset = 6f * density;
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);
        // A rim only: the slab sits on top of the tile, so any fill would wash the app out.
        rim.fit(rect);
        canvas.drawRoundRect(rect, radius, radius, rim.paint);
        rim.drawWindowLight(canvas, rect, radius, 1f);
    }
}
