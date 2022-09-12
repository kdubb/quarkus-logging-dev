package io.quarkiverse.logging.dev.runtime;

class ColorUtil {

    static String bold(CharSequence section) {
        return "\u001b[1m" + section + "\u001b[0m";
    }

    static String colorize(CharSequence section, Color fgColor) {
        return colorize(section, fgColor, null);
    }

    static String colorize(CharSequence section, Color fgColor, Color bgColor) {
        var target = new StringBuilder();
        colorize(target, section, fgColor, bgColor);
        return target.toString();
    }

    static void colorize(StringBuilder target, CharSequence section, Color fgColor) {
        colorize(target, section, fgColor, null);
    }

    static void colorize(StringBuilder target, CharSequence section, Color fgColor, Color bgColor) {
        if (fgColor != null) {
            startFgColor(target, fgColor);
        }
        if (bgColor != null) {
            startBgColor(target, bgColor);
        }
        target.append(section);
        if (bgColor != null) {
            endBgColor(target);
        }
        if (fgColor != null) {
            endFgColor(target);
        }
    }

    static void startFgColor(StringBuilder target, Color color) {
        startColor(target, 38, color);
    }

    static void startBgColor(StringBuilder target, Color color) {
        startColor(target, 48, color);
    }

    private static void startColor(StringBuilder target, int mode, Color color) {
        var r = color.r;
        var g = color.g;
        var b = color.b;
        if (trueColor) {
            target.appendCodePoint(27).append('[').append(mode).append(';').append(2).append(';').append(clip(r))
                    .append(';').append(clip(g)).append(';').append(clip(b)).append('m');
        } else {
            int ar = (5 * clip(r)) / 255;
            int ag = (5 * clip(g)) / 255;
            int ab = (5 * clip(b)) / 255;
            int col = 16 + 36 * ar + 6 * ag + ab;
            target.appendCodePoint(27).append('[').append(mode).append(';').append('5').append(';').append(col).append('m');
        }
    }

    private static int clip(int channel) {
        return Math.min(Math.max(0, channel), 255);
    }

    static void endFgColor(StringBuilder target) {
        endColor(target, 39);
    }

    static void endBgColor(StringBuilder target) {
        endColor(target, 49);
    }

    private static void endColor(StringBuilder target, int mode) {
        target.appendCodePoint(27).append('[').append(mode).append('m');
    }

    static final boolean trueColor = determineTrueColor();

    static boolean determineTrueColor() {
        final String colorterm = System.getenv("COLORTERM");
        return (colorterm != null && (colorterm.contains("truecolor") || colorterm.contains("24bit")));
    }

}
