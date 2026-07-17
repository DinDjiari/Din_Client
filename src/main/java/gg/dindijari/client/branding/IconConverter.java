package gg.dindijari.client.branding;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts an arbitrary user-picked image into the window-icon PNG set.
 *
 * <p>Pipeline: decode via {@link NativeImage} (STB — PNG, JPEG, BMP, TGA and
 * static GIF), center-crop non-square inputs to a square, downscale with a
 * coverage-weighted box filter (high quality for downscales; small sources are
 * effectively resampled bilinearly), and write RGBA PNGs at 16&times;16,
 * 32&times;32 and 48&times;48 to the given output directory
 * ({@code icon_16.png} / {@code icon_32.png} / {@code icon_48.png}).
 *
 * <p>Anything that is not a decodable image (or absurdly large) is rejected
 * with an {@link IconConversionException} carrying a user-presentable message.
 */
public final class IconConverter {

    /** The icon edge lengths produced, matching what GLFW accepts. */
    public static final int[] SIZES = {16, 32, 48};

    /** Maximum accepted source file size (bytes). */
    private static final long MAX_FILE_BYTES = 32L * 1024 * 1024;

    /** Maximum accepted source edge length (pixels). */
    private static final int MAX_EDGE = 8192;

    private IconConverter() {
    }

    /** Thrown when the picked file cannot be converted; the message is user-presentable. */
    public static final class IconConversionException extends Exception {
        IconConversionException(String message) {
            super(message);
        }

        IconConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Converts the source image and writes the icon PNG set.
     *
     * @param source the user-picked image file
     * @param outDir the directory receiving {@code icon_<size>.png}
     * @throws IconConversionException if the file is not a usable image or
     *                                 writing fails
     */
    public static void convert(Path source, Path outDir) throws IconConversionException {
        byte[] bytes;
        try {
            if (Files.size(source) > MAX_FILE_BYTES) {
                throw new IconConversionException("Image is too large (max 32 MB).");
            }
            bytes = Files.readAllBytes(source);
        } catch (IOException e) {
            throw new IconConversionException("Could not read the file.", e);
        }

        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes))) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w <= 0 || h <= 0 || w > MAX_EDGE || h > MAX_EDGE) {
                throw new IconConversionException(
                        "Unsupported image dimensions (" + w + "x" + h + ", max 8192).");
            }
            // Center-crop non-square inputs (documented behaviour).
            int square = Math.min(w, h);
            int offX = (w - square) / 2;
            int offY = (h - square) / 2;

            Files.createDirectories(outDir);
            for (int size : SIZES) {
                try (NativeImage scaled = resample(image, offX, offY, square, size)) {
                    scaled.writeToFile(outDir.resolve("icon_" + size + ".png"));
                }
            }
        } catch (IconConversionException e) {
            throw e;
        } catch (IOException e) {
            throw new IconConversionException(
                    "Not a valid image file (PNG, JPEG, BMP, TGA or GIF).", e);
        } catch (RuntimeException e) {
            throw new IconConversionException("Could not convert the image.", e);
        }
    }

    /**
     * Coverage-weighted box resample of a square source region into a
     * {@code size}&times;{@code size} RGBA image. Every destination pixel
     * averages the exact (fractional) source rectangle it covers, which is a
     * high-quality area filter for downscaling.
     */
    private static NativeImage resample(NativeImage src, int offX, int offY, int square, int size) {
        NativeImage dst = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        double scale = (double) square / size;
        for (int dy = 0; dy < size; dy++) {
            double y0 = offY + dy * scale;
            double y1 = offY + (dy + 1) * scale;
            for (int dx = 0; dx < size; dx++) {
                double x0 = offX + dx * scale;
                double x1 = offX + (dx + 1) * scale;
                double r = 0, g = 0, b = 0, a = 0, weight = 0;
                for (int iy = (int) Math.floor(y0); iy < Math.ceil(y1); iy++) {
                    double wy = Math.min(iy + 1, y1) - Math.max(iy, y0);
                    if (wy <= 0) {
                        continue;
                    }
                    for (int ix = (int) Math.floor(x0); ix < Math.ceil(x1); ix++) {
                        double wx = Math.min(ix + 1, x1) - Math.max(ix, x0);
                        if (wx <= 0) {
                            continue;
                        }
                        int px = clamp(ix, offX, offX + square - 1);
                        int py = clamp(iy, offY, offY + square - 1);
                        // NativeImage packs ABGR: A<<24 | B<<16 | G<<8 | R.
                        int c = src.getPixelRGBA(px, py);
                        double wgt = wx * wy;
                        a += ((c >>> 24) & 0xFF) * wgt;
                        b += ((c >>> 16) & 0xFF) * wgt;
                        g += ((c >>> 8) & 0xFF) * wgt;
                        r += (c & 0xFF) * wgt;
                        weight += wgt;
                    }
                }
                int pa = channel(a, weight);
                int pb = channel(b, weight);
                int pg = channel(g, weight);
                int pr = channel(r, weight);
                dst.setPixelRGBA(dx, dy, (pa << 24) | (pb << 16) | (pg << 8) | pr);
            }
        }
        return dst;
    }

    private static int channel(double sum, double weight) {
        if (weight <= 0) {
            return 0;
        }
        int v = (int) Math.round(sum / weight);
        return Math.max(0, Math.min(255, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
