package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * The edge of a glass shape in ReactDrops Soap Bubble / Liquid Glass style:
 * Captures light with thin-film prismatic iridescence (diamond white glint,
 * iridescent cyan, soap violet, and golden pearl shimmer) along the top-left,
 * transitioning into clean translucent waist and soft ambient twilight bounce on the bottom-right.
 * Also renders an overhead "Window Light" specular reflection glint along the top curve.
 */
final class GlassRim {

    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint windowLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final Matrix windowLightMatrix = new Matrix();

    GlassRim(float strokePx, int shine, int faint) {
        this(strokePx, shine, faint, 1f);
    }

    /**
     * @param reach how far each shine runs towards the faint corners: 1 fades it out right at
     *              them, 0.5 only halfway. Long shapes such as the dock tray use the shorter reach,
     *              or the shine runs most of the length of their long edges.
     */
    GlassRim(float strokePx, int shine, int faint, float reach) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokePx);

        float alphaFactor = ((shine >>> 24) & 0xFF) / 255f;

        // Thin-film iridescent interference spectrum inspired by ReactDrops soap-bubble:
        // Pure brilliant specular white -> electric cyan -> soap violet -> gold pearl glint -> clear glass -> ambient twilight bounce -> specular return
        int cWhiteGlint = modulateAlpha(0xFFFFFFFF, alphaFactor);
        int cCyanShimmer = modulateAlpha(0xCC67E8F9, alphaFactor);
        int cVioletSoap = modulateAlpha(0xB3C084FC, alphaFactor);
        int cGoldSheen = modulateAlpha(0x80FDE047, alphaFactor);
        int cClearWaist = modulateAlpha(faint != 0 ? faint : 0x22FFFFFF, alphaFactor);
        int cTwilightLilac = modulateAlpha(0x59818CF8, alphaFactor);
        int cSkyCyan = modulateAlpha(0x8038BDF8, alphaFactor);
        int cWhiteRim = modulateAlpha(0xD9FFFFFF, alphaFactor);

        int[] colors = new int[]{
                cWhiteGlint,
                cCyanShimmer,
                cVioletSoap,
                cGoldSheen,
                cClearWaist,
                cClearWaist,
                cTwilightLilac,
                cSkyCyan,
                cWhiteRim
        };

        float[] stops = new float[]{
                0.00f,
                0.08f * reach,
                0.18f * reach,
                0.30f * reach,
                0.46f,
                0.54f,
                1.0f - (0.30f * reach),
                1.0f - (0.15f * reach),
                1.00f
        };

        paint.setShader(new LinearGradient(0f, 0f, 1f, 1f, colors, stops, Shader.TileMode.CLAMP));

        // Secondary overhead "Window Light" specular highlight running along the top bevel
        windowLightPaint.setStyle(Paint.Style.STROKE);
        windowLightPaint.setStrokeWidth(Math.max(1f, strokePx * 0.9f));
        int[] wlColors = new int[]{
                0x00FFFFFF,
                0x00FFFFFF,
                0xD9FFFFFF,
                0x8067E8F9,
                0x00FFFFFF,
                0x00FFFFFF
        };
        float[] wlStops = new float[]{
                0.00f,
                0.15f,
                0.32f,
                0.46f,
                0.68f,
                1.00f
        };
        windowLightPaint.setShader(new LinearGradient(0f, 0f, 1f, 0f, wlColors, wlStops, Shader.TileMode.CLAMP));
    }

    private static int modulateAlpha(int color, float factor) {
        int a = Math.round(((color >>> 24) & 0xFF) * factor);
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, a)) << 24);
    }

    /** Lines the shine up with {@code rect}: brightest at its top-left and bottom-right. */
    void fit(RectF rect) {
        matrix.setScale(rect.width(), rect.height());
        matrix.postTranslate(rect.left, rect.top);
        paint.getShader().setLocalMatrix(matrix);
    }

    /** Draws the overhead window light glint along the top curve of the soap bubble. */
    void drawWindowLight(Canvas canvas, RectF rect, float cornerRadius, float alpha) {
        if (alpha <= 0.05f) return;
        windowLightMatrix.setScale(rect.width(), rect.height());
        windowLightMatrix.postTranslate(rect.left, rect.top);
        windowLightPaint.getShader().setLocalMatrix(windowLightMatrix);
        windowLightPaint.setAlpha(Math.round(200 * alpha));
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, windowLightPaint);
    }
}
