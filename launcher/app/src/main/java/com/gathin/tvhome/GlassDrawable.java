package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * A top-bar pill or popup panel in the glass style: the blurred wallpaper behind it, an even veil, and an edge
 * that shines at the top-left and bottom-right (GlassRim), with no brighter top. A pill's glass fades
 * out as the top bar's gliding focus fill covers it, and back in as the fill moves away.
 */
final class GlassDrawable extends Drawable {

    private static final int VEIL_ALPHA = 0x14;
    private final View owner;
    private final float radius;
    private final Backdrop.Fill fill = new Backdrop.Fill();
    private final Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GlassRim rim;
    private final Glider highlight;
    /** Paints the focus shape over the glass (a menu), rather than fading the glass for it (a pill). */
    private final boolean paintsShape;
    /** How far the glass is shown, 0 to 1, for a panel fading in or out. */
    private float fade = 1f;
    private final int[] at = new int[2], highlightAt = new int[2];
    private final RectF rect = new RectF();

    GlassDrawable(View owner, float radius, float density, Glider highlight) {
        this(owner, radius, density, highlight, 1f);
    }

    /** {@code reach} as in GlassRim: larger panels keep the shine to the corners. */
    GlassDrawable(View owner, float radius, float density, Glider highlight, float reach) {
        this(owner, radius, density, highlight, reach, false);
    }

    /**
     * With {@code paintsShape}, the focus shape is painted over this glass and under what the panel
     * holds, as the dock's tray paints it: the shape glides in from outside the panel and the glass
     * never hides it.
     */
    GlassDrawable(View owner, float radius, float density, Glider highlight, float reach, boolean paintsShape) {
        this.paintsShape = paintsShape;
        this.owner = owner;
        this.highlight = highlight;
        this.radius = radius;
        veil.setColor(0xFFFFFF | VEIL_ALPHA << 24);
        rim = new GlassRim(1.5f * density, 0xD9FFFFFF, 0x1AFFFFFF, reach);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        rect.set(getBounds());
        // The glass gives way to the focus fill as it arrives: the more of this pill the gliding
        // shape covers, the fainter the glass, until a focused pill is the fill alone. As the shape
        // leaves, the glass comes back the same way.
        float covered = 0f;
        if (highlight != null && highlight.isShowing() && !paintsShape) {
            owner.getLocationInWindow(at);
            highlight.getLocationInWindow(highlightAt);
            covered = highlight.coverage(highlightAt[0] - at[0], highlightAt[1] - at[1], rect.width(), rect.height());
        }
        float glass = (1f - covered) * fade;
        if (glass > 0f) {
            fill.draw(canvas, owner, rect, radius, glass);
            veil.setAlpha(Math.round(VEIL_ALPHA * glass));
            canvas.drawRoundRect(rect, radius, radius, veil);
            if (paintsShape && highlight != null && highlight.isShowing()) {
                owner.getLocationInWindow(at);
                highlight.getLocationInWindow(highlightAt);
                canvas.save();
                canvas.clipRect(getBounds());
                highlight.drawShape(canvas, highlightAt[0] - at[0], highlightAt[1] - at[1], owner);
                canvas.restore();
            }
            float half = rim.paint.getStrokeWidth() / 2f;
            rect.inset(half, half);
            rim.fit(rect);
            rim.paint.setAlpha(Math.round(255 * glass));
            canvas.drawRoundRect(rect, radius - half, radius - half, rim.paint);
            rim.drawWindowLight(canvas, rect, radius - half, glass);
        }
        // The fill itself is the highlight view behind the pills, showing through as the glass clears;
        // painting it here as well would darken a focused pill twice over.
    }

    @Override
    public void setAlpha(int alpha) {
        fade = alpha / 255f;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
