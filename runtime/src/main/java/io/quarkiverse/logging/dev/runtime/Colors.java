package io.quarkiverse.logging.dev.runtime;

import static java.lang.Math.*;
import static org.jboss.logmanager.Level.ERROR;
import static org.jboss.logmanager.Level.TRACE;

import java.util.logging.Level;

class Colors {

    static Color CTX_PRIMARY_COLOR = Color.of(0x00, 0xaa, 0xff);
    static Color CTX_SECONDARY_COLOR = CTX_PRIMARY_COLOR.darken(0.15f);
    static Color CTX_TERTIARY_COLOR = CTX_SECONDARY_COLOR.darken(0.15f);

    static Color HTTP_CTX_PRIMARY_COLOR = Color.of(0xff, 0xff, 0x44);
    static Color HTTP_CTX_IMPORTANT_COLOR = Color.of(0xff, 0x99, 0x22);

    static Color EXC_PRIMARY_COLOR = Color.of(0xff, 0x42, 0x42);
    static Color EXC_SECONDARY_COLOR = Color.of(0xff, 0x62, 0x62);
    static Color EXC_TERTIARY_COLOR = Color.of(0xff, 0x82, 0x82);
    static Color EXC_QUATERNARY_COLOR = Color.of(0xff, 0xa2, 0xa2);
    static Color EXC_MESSAGE_COLOR = Color.of(0xaa, 0xa0, 0xa0);

    static Color DELIM_COLOR = Color.of(0xaa, 0xaa, 0xaa);
    static Color HI_TEXT_COLOR = Color.of(0xdd, 0xdd, 0xdd);
    static Color LO_TEXT_COLOR = Color.of(0x88, 0x88, 0x88);

    static Color levelColor(Level level) {
        int largestLevel = ERROR.intValue();
        int smallestLevel = TRACE.intValue();
        int levelVal = max(min(level.intValue(), largestLevel), smallestLevel) - smallestLevel;
        int r = (levelVal < 300 ? 0 : (levelVal - 300) * 189 / 300) + 66;
        int g = (((300 - abs(levelVal - 300)) * 189) / 300) + 66;
        int b = (levelVal > 300 ? 0 : levelVal * 189 / 300) + 66;
        return Color.of(r, g, b);
    }
}
