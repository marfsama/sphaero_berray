package uk.co.petertribble.sphaero2.cutter;

public class BevelUtil {
    // This mimics Color.brighter() and Color.darker(). They multiply or
    // divide R/G/B by 0.7, and trim them to 0 or 255 if needed. I'm going
    // to use 7/10 (so it's int arithmetic), and not use Math. I don't quite
    // trust inlining yet. And I certainly don't want to make scads of Color
    // objects for each pixel. It's bad enough these are methods, and not
    // inlined in bevel().
    private static final int COLOR_NUMERATOR = 10;
    private static final int COLOR_DENOMINATOR = 7;
    private static final int COLOR_MAX_BRIGHTNESS = 255 * COLOR_DENOMINATOR / COLOR_NUMERATOR;

    /**
     * Draws bevels on data.  Check every opaque pixel's NW and SE
     * neighbors.  If NW is transparent and SE is opaque, brighten the
     * central pixel.  If it's the other way around, darken it.  If both or
     * neither are transparent, leave it alone.
     */
    public static void bevel(int[] data, int width, int height) {
        // Scan diagonal NW-SE lines.  The first and last lines can be skipped.
        // moved these out of the loop
        boolean nw; // true iff that pixel is opaque
        boolean c; // true iff that pixel is opaque
        boolean se; // true iff that pixel is opaque
        for (int i = 0; i < width + height - 3; i++) {
            nw = false;
            int x = Math.max(0, i - height + 2);
            int y = Math.max(0, height - i - 2);
            c = (((data[y * width + x] >> 24) & 0xff) > 0);
            while ((x < width) && (y < height)) {
                if ((x + 1 < width) && (y + 1 < height)) {
                    se = (((data[(y + 1) * width + (x + 1)] >> 24) & 0xff) > 0);
                } else {
                    se = false;
                }
                if (c) {
                    int datum = data[y * width + x];
                    if (nw && !se) {
                        data[y * width + x] = darker(datum);
                    } else if (!nw && se) {
                        data[y * width + x] = brighter(datum);
                    }
                }
                nw = c;
                c = se;
                x++;
                y++;
            }
        }
    }

    private static int brighter(int val) {
        int r = (val >> 16) & 0xff;
        int g = (val >> 8) & 0xff;
        int b = (val) & 0xff;

        // Black goes to #030303 gray
        if (r == 0 && g == 0 && b == 0) {
            return 0xff030303;
        }
        r = r < 3 ? 3 : r;
        g = g < 3 ? 3 : g;
        b = b < 3 ? 3 : b;

        r = r >= COLOR_MAX_BRIGHTNESS ? 255 : r * COLOR_NUMERATOR / COLOR_DENOMINATOR;
        g = g >= COLOR_MAX_BRIGHTNESS ? 255 : g * COLOR_NUMERATOR / COLOR_DENOMINATOR;
        b = b >= COLOR_MAX_BRIGHTNESS ? 255 : b * COLOR_NUMERATOR / COLOR_DENOMINATOR;
        return ((((0xff00 | r) << 8) | g) << 8) | b;
    }

    private static int darker(int val) {
        int r = (val >> 16) & 0xff;
        int g = (val >> 8) & 0xff;
        int b = (val) & 0xff;
        r = r * COLOR_DENOMINATOR / COLOR_NUMERATOR;
        g = g * COLOR_DENOMINATOR / COLOR_NUMERATOR;
        b = b * COLOR_DENOMINATOR / COLOR_NUMERATOR;
        return ((((0xff00 | r) << 8) | g) << 8) | b;
    }

    public static int[] bevel(int[] data, int width, int height, int bevelSize) {
        if (bevelSize <= 0) {
            // No bevel if size is 0 or negative
            return data;
        }

        int[] newData = new int[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);

        // First pass: find all edge pixels
        boolean[] isEdge = new boolean[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((data[index] >>> 24) == 0) continue; // Skip transparent

                // Check if adjacent to transparent pixel
                if (isAdjacentToTransparent(data, width, height, x, y)) {
                    isEdge[index] = true;
                }
            }
        }

        // Second pass: apply bevel effect based on distance from edge
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((data[index] >>> 24) == 0) continue; // Skip transparent

                // Find the closest edge in each direction
                int topDist = findEdgeDistance(isEdge, width, height, x, y, 0, -1, bevelSize);
                int rightDist = findEdgeDistance(isEdge, width, height, x, y, 1, 0, bevelSize);
                int bottomDist = findEdgeDistance(isEdge, width, height, x, y, 0, 1, bevelSize);
                int leftDist = findEdgeDistance(isEdge, width, height, x, y, -1, 0, bevelSize);

                // Calculate bevel strength based on distances
                float topRightStrength = Math.max(
                        (bevelSize - topDist) / (float) bevelSize,
                        (bevelSize - rightDist) / (float) bevelSize
                );

                float bottomLeftStrength = Math.max(
                        (bevelSize - bottomDist) / (float) bevelSize,
                        (bevelSize - leftDist) / (float) bevelSize
                );

                // Apply the effect
                int originalColor = data[index];
                if (topRightStrength > 0) {
                    float strength = Math.min(topRightStrength, 1.0f);
                    newData[index] = blend(originalColor, brighter(originalColor), strength);
                }
                if (bottomLeftStrength > 0) {
                    float strength = Math.min(bottomLeftStrength, 1.0f);
                    newData[index] = blend(newData[index], darker(originalColor), strength);
                }
            }
        }

        return newData;
    }

    private static boolean isAdjacentToTransparent(int[] data, int width, int height, int x, int y) {
        // Check all 4 directions
        return isTransparent(data, width, height, x - 1, y) ||
                isTransparent(data, width, height, x + 1, y) ||
                isTransparent(data, width, height, x, y - 1) ||
                isTransparent(data, width, height, x, y + 1);
    }

    private static int findEdgeDistance(boolean[] isEdge, int width, int height,
                                        int x, int y, int dx, int dy, int maxDist) {
        for (int i = 1; i <= maxDist; i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;

            if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                return i; // Consider out of bounds as edge
            }

            int index = ny * width + nx;
            if (isEdge[index]) {
                return i;
            }
        }
        return maxDist + 1; // No edge found within maxDist
    }

    private static int blend(int color1, int color2, float ratio) {
        if (ratio <= 0) return color1;
        if (ratio >= 1) return color2;

        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >>> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static boolean isTransparent(int[] data, int width, int height, int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return true;
        }
        int index = y * width + x;
        return (data[index] >>> 24) == 0;
    }

    public static int[] glow(int[] data, int width, int height, int glowSize, int glowColor) {
        // Expanded dimensions
        int newWidth = width + 2 * glowSize;
        int newHeight = height + 2 * glowSize;
        int[] expandedData = new int[newWidth * newHeight];

        // Step 1: Place original image in the center
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int origPos = y * width + x;
                int newPos = (y + glowSize) * newWidth + (x + glowSize);
                expandedData[newPos] = data[origPos];
            }
        }

        // Step 2: Extract alpha mask (1 where opaque, 0 where transparent)
        int[] alphaMask = new int[newWidth * newHeight];
        for (int i = 0; i < expandedData.length; i++) {
            alphaMask[i] = (expandedData[i] >>> 24) > 0 ? 0xFF000000 : 0;
        }

        // Step 3: Blur the alpha mask (Gaussian-like blur)
        int[] blurredMask = new int[newWidth * newHeight];
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int sum = 0, count = 0;
                for (int dy = -glowSize; dy <= glowSize; dy++) {
                    for (int dx = -glowSize; dx <= glowSize; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < newWidth && ny >= 0 && ny < newHeight) {
                            sum += (alphaMask[ny * newWidth + nx] >>> 24);
                            count++;
                        }
                    }
                }
                int avgAlpha = count > 0 ? (sum / count) : 0;
                blurredMask[y * newWidth + x] = avgAlpha << 24;
            }
        }

        // Step 4: Apply glow color to blurred mask
        int glowR = (glowColor >> 16) & 0xFF;
        int glowG = (glowColor >> 8) & 0xFF;
        int glowB = glowColor & 0xFF;
        for (int i = 0; i < blurredMask.length; i++) {
            int alpha = (blurredMask[i] >>> 24);
            if (alpha > 0) {
                int r = (int) (glowR * (alpha / 255f));
                int g = (int) (glowG * (alpha / 255f));
                int b = (int) (glowB * (alpha / 255f));
                blurredMask[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
            }
        }

        return blurredMask;
    }
}
