package com.zynelabs.streamcheck;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

// Shared ZyneLabs palette — "Calm Night" (same as the IPTV app default).
public class Theme {
    public static final int PAPER = 0xFF0C0906;
    public static final int CARD = 0xFF17100A;
    public static final int CARD2 = 0xFF20150C;
    public static final int ACCENT = 0xFFFFAA33;
    public static final int ACCENT_DARK = 0xFF7A4A1E;
    public static final int ACCENT_END = 0xFFC97A2E;
    public static final int CYAN = 0xFF00E5FF;
    public static final int RED = 0xFFFF5A4E;
    public static final int INK = 0xFFF5EDE0;
    public static final int MUTED = 0xFFA89A86;
    public static final int GREEN = 0xFF66BB6A;

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }

    // Primary rounded button (amber gradient, like the IPTV app).
    public static GradientDrawable primaryBtn(Context c) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ACCENT_DARK, ACCENT_END});
        d.setCornerRadius(dp(c, 18));
        return d;
    }

    // Secondary rounded button (dark card with amber outline).
    public static GradientDrawable outlineBtn(Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD2);
        d.setStroke(dp(c, 2), ACCENT);
        d.setCornerRadius(dp(c, 18));
        return d;
    }

    // Disabled button look.
    public static GradientDrawable disabledBtn(Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0xFF2A2018);
        d.setCornerRadius(dp(c, 18));
        return d;
    }

    public static GradientDrawable cardBg(Context c) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{CARD2, CARD});
        d.setCornerRadius(dp(c, 14));
        return d;
    }

    public static GradientDrawable inputBg(Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setStroke(dp(c, 1), 0xFF3A2C1C);
        d.setCornerRadius(dp(c, 14));
        return d;
    }

    // ---- 3D glossy style ("Gemini 3D" look) ----
    public static final int COPPER_TOP = 0xFFF2B183;
    public static final int COPPER_BOT = 0xFF9A5A28;
    public static final int SILVER_TOP = 0xFFF4F4F4;
    public static final int SILVER_BOT = 0xFF8E8E8E;
    public static final int PILL_RED_TOP = 0xFFEC8B76;
    public static final int PILL_RED_BOT = 0xFFA83A2C;
    public static final int PILL_GREEN_TOP = 0xFF93D1A0;
    public static final int PILL_GREEN_BOT = 0xFF3F7D4F;
    public static final int PILL_GRAY_TOP = 0xFFBDBDBD;
    public static final int PILL_GRAY_BOT = 0xFF616161;
    public static final int PILL_OFF_TOP = 0xFF5C6068;
    public static final int PILL_OFF_BOT = 0xFF33363C;
    public static final int BG_TOP = 0xFF272B34;
    public static final int BG_BOT = 0xFF13151A;

    public static int shade(int color, float f) {
        int a = android.graphics.Color.alpha(color);
        int r = Math.min(255, (int) (android.graphics.Color.red(color) * f));
        int g = Math.min(255, (int) (android.graphics.Color.green(color) * f));
        int b = Math.min(255, (int) (android.graphics.Color.blue(color) * f));
        return android.graphics.Color.argb(a, r, g, b);
    }

    /** Raised glossy 3D button: drop shadow + 4-stop vertical gradient + top sheen. */
    public static android.graphics.drawable.LayerDrawable glossy3d(
            Context c, int top, int bottom, int radiusDp) {
        int r = dp(c, radiusDp);
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(0xAA000000);
        shadow.setCornerRadius(r);

        GradientDrawable main = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{shade(top, 1.12f), top, bottom, shade(bottom, 0.70f)});
        main.setCornerRadius(r);
        main.setStroke(dp(c, 1), shade(bottom, 0.55f));

        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66FFFFFF, 0x14FFFFFF, 0x00000000});
        gloss.setCornerRadius(r);

        android.graphics.drawable.LayerDrawable ld =
                new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{shadow, main, gloss});
        int s = dp(c, 3);
        ld.setLayerInset(0, 0, s, 0, 0);          // shadow peeks below
        ld.setLayerInset(1, 0, 0, 0, s);          // face sits above shadow
        ld.setLayerInset(2, dp(c, 4), dp(c, 3), dp(c, 4), s + dp(c, 3)); // sheen
        return ld;
    }

    public static GradientDrawable bgGradient() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG_TOP, BG_BOT});
    }
}
