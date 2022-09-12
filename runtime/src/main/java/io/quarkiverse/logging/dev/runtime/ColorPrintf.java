package io.quarkiverse.logging.dev.runtime;

import java.util.Locale;
import java.util.Map;

import org.wildfly.common.format.GeneralFlags;
import org.wildfly.common.format.Printf;

class ColorPrintf extends Printf {

    private final Color defaultColor;
    private final Map<Class<?>, Color> typeColors;
    private final float darken;

    ColorPrintf(Color defaultColor, Map<Class<?>, Color> typeColors, float darken) {
        super(Locale.getDefault());
        this.defaultColor = defaultColor;
        this.typeColors = typeColors;
        this.darken = darken;
    }

    public StringBuilder formatDirect(StringBuilder destination, String format, Object... params) {
        ColorUtil.endFgColor(destination);
        super.formatDirect(destination, format, params);
        return destination;
    }

    protected void formatPlainString(StringBuilder target, Object item, GeneralFlags genFlags,
            int width, int precision) {
        ColorUtil.startFgColor(target, getColor(item).darken(darken));
        super.formatPlainString(target, item, genFlags, width, precision);
        ColorUtil.endFgColor(target);
    }

    protected Color getColor(Object item) {
        if (!typeColors.isEmpty()) {
            for (Map.Entry<Class<?>, Color> entry : typeColors.entrySet()) {
                if (entry.getKey().isInstance(item)) {
                    return entry.getValue();
                }
            }
        }
        return defaultColor;
    }
}
