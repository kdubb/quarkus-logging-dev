package io.quarkiverse.logging.dev.runtime;

import static java.lang.Math.max;
import static java.lang.Math.min;

class Color {
    int r;
    int g;
    int b;

    Color(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    static Color of(int r, int g, int b) {
        return new Color(r, g, b);
    }

    Color darken(float amount) {
        return multiply(1.0f - amount);
    }

    Color multiply(float amount) {
        return new Color(clamp(r * amount), clamp(g * amount), clamp(b * amount));
    }

    private static int clamp(float value) {
        return max(min((int) value, 255), 0);
    }
}
