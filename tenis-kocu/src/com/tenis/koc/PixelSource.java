package com.tenis.koc;

/** Dik (upright) koordinatlarda piksel okuyan kaynak. rgb(): 0xRRGGBB, dışarıdaysa -1. */
public abstract class PixelSource {
    public abstract int width();
    public abstract int height();
    public abstract int rgb(int x, int y);

    /** Kamera NV21 karesi; rotation: görüntüyü dik yapmak için saat yönünde dönüş açısı. */
    public static class Nv21 extends PixelSource {
        private final byte[] d;
        private final int w, h, rot;

        public Nv21(byte[] data, int w, int h, int rotation) {
            this.d = data; this.w = w; this.h = h; this.rot = ((rotation % 360) + 360) % 360;
        }

        @Override public int width() { return rot % 180 == 0 ? w : h; }
        @Override public int height() { return rot % 180 == 0 ? h : w; }

        @Override
        public int rgb(int u, int v) {
            int x, y;
            switch (rot) {
                case 90: x = v; y = h - 1 - u; break;
                case 180: x = w - 1 - u; y = h - 1 - v; break;
                case 270: x = w - 1 - v; y = u; break;
                default: x = u; y = v;
            }
            if (x < 0 || y < 0 || x >= w || y >= h) return -1;
            int yy = (d[y * w + x] & 0xff) - 16;
            int uvi = w * h + (y >> 1) * w + (x & ~1);
            int vv = (d[uvi] & 0xff) - 128;
            int uu = (d[uvi + 1] & 0xff) - 128;
            if (yy < 0) yy = 0;
            int y1192 = 1192 * yy;
            int r = (y1192 + 1634 * vv) >> 10;
            int g = (y1192 - 833 * vv - 400 * uu) >> 10;
            int b = (y1192 + 2066 * uu) >> 10;
            r = r < 0 ? 0 : (r > 255 ? 255 : r);
            g = g < 0 ? 0 : (g > 255 ? 255 : g);
            b = b < 0 ? 0 : (b > 255 ? 255 : b);
            return (r << 16) | (g << 8) | b;
        }
    }

    /** ARGB piksel dizisi (Bitmap.getPixels). */
    public static class Pixels extends PixelSource {
        private final int[] p;
        private final int w, h;

        public Pixels(int[] pixels, int w, int h) { this.p = pixels; this.w = w; this.h = h; }

        @Override public int width() { return w; }
        @Override public int height() { return h; }

        @Override
        public int rgb(int x, int y) {
            if (x < 0 || y < 0 || x >= w || y >= h) return -1;
            return p[y * w + x] & 0xffffff;
        }
    }
}
