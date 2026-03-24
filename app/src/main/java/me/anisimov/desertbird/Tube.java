package me.anisimov.desertbird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class Tube {

    public static final int TUBE_WIDTH = 220;

    public static final int TOP_CAP_HEIGHT = 70;
    public static final int BOTTOM_CAP_HEIGHT = 58;
    private static final int BODY_OVERLAP = 24;

    private static Bitmap rockTopCap;
    private static Bitmap rockBodyTile;
    private static Bitmap rockBottomCap;

    private static Bitmap rockTopCapFlipped;
    private static Bitmap rockBodyTileFlipped;

    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();

    private float x;
    private float y;
    private int width;
    private int height;
    private final boolean topTube;

    public Tube(Context context, float x, float y, int width, int height, boolean topTube) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = Math.max(0, height);
        this.topTube = topTube;

        loadSprites(context);
    }

    private static void loadSprites(Context context) {
        if (rockTopCap == null) {
            rockTopCap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rock_top_cap);
        }
        if (rockBodyTile == null) {
            rockBodyTile = BitmapFactory.decodeResource(context.getResources(), R.drawable.rock_body_tile);
        }
        if (rockBottomCap == null) {
            rockBottomCap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rock_bottom_cap);
        }

        if (rockTopCap != null && rockTopCapFlipped == null) {
            rockTopCapFlipped = flipVertical(rockTopCap);
        }
        if (rockBodyTile != null && rockBodyTileFlipped == null) {
            rockBodyTileFlipped = flipVertical(rockBodyTile);
        }
    }

    private static Bitmap flipVertical(Bitmap src) {
        Matrix matrix = new Matrix();
        matrix.preScale(1f, -1f);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    public void tick(float speed, float deltaTime) {
        float dtScale = deltaTime * 60f;
        x -= speed * dtScale;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (height <= 0) {
            return;
        }

        if (topTube) {
            drawTopRock(canvas, paint);
        } else {
            drawBottomRock(canvas, paint);
        }
    }

    private void drawTopRock(Canvas canvas, Paint paint) {
        if (rockBodyTileFlipped == null || rockTopCapFlipped == null) {
            drawFallback(canvas, paint);
            return;
        }

        int capHeight = Math.min(TOP_CAP_HEIGHT, height);
        int bodyHeight = Math.max(0, height - capHeight + BODY_OVERLAP);

        drawRepeatedBody(
                canvas,
                x,
                y,
                width,
                bodyHeight,
                rockBodyTileFlipped,
                BODY_OVERLAP
        );

        float capY = y + height - capHeight;
        drawScaledBitmap(canvas, rockTopCapFlipped, x, capY, width, capHeight);
    }

    private void drawBottomRock(Canvas canvas, Paint paint) {
        if (rockBodyTile == null || rockTopCap == null) {
            drawFallback(canvas, paint);
            return;
        }

        int topCapHeight = Math.min(TOP_CAP_HEIGHT, height);

        float bodyY = y + topCapHeight - BODY_OVERLAP;
        int bodyHeight = Math.max(0, height - topCapHeight + BODY_OVERLAP);

        drawRepeatedBody(
                canvas,
                x,
                bodyY,
                width,
                bodyHeight,
                rockBodyTile,
                BODY_OVERLAP
        );

        drawScaledBitmap(canvas, rockTopCap, x, y, width, topCapHeight);
    }

    private void drawRepeatedBody(Canvas canvas,
                                  float drawX, float drawY,
                                  int drawWidth, int drawHeight,
                                  Bitmap tile, int overlap) {
        if (tile == null || drawHeight <= 0) {
            return;
        }

        int srcW = tile.getWidth();
        int srcH = tile.getHeight();

        if (srcW <= 0 || srcH <= 0) {
            return;
        }

        int step = Math.max(1, srcH - overlap);
        float currentY = drawY;
        float endY = drawY + drawHeight;

        while (currentY < endY) {
            int remaining = (int) (endY - currentY);
            int pieceHeight = Math.min(srcH, remaining);

            if (pieceHeight <= 0) {
                break;
            }

            srcRect.set(0, 0, srcW, pieceHeight);
            dstRect.set(drawX, currentY, drawX + drawWidth, currentY + pieceHeight);
            canvas.drawBitmap(tile, srcRect, dstRect, null);

            currentY += step;
        }
    }

    private void drawScaledBitmap(Canvas canvas, Bitmap bmp, float x, float y, int w, int h) {
        if (bmp == null || w <= 0 || h <= 0) {
            return;
        }

        dstRect.set(x, y, x + w, y + h);
        canvas.drawBitmap(bmp, null, dstRect, null);
    }

    private void drawFallback(Canvas canvas, Paint paint) {
        int oldColor = paint.getColor();
        Paint.Style oldStyle = paint.getStyle();
        float oldStroke = paint.getStrokeWidth();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(205, 170, 95));
        canvas.drawRect(x, y, x + width, y + height, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.rgb(140, 100, 55));
        canvas.drawRect(x, y, x + width, y + height, paint);

        paint.setStyle(oldStyle);
        paint.setStrokeWidth(oldStroke);
        paint.setColor(oldColor);
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isTopTube() {
        return topTube;
    }

    public static Bitmap getRockBottomCap() {
        return rockBottomCap;
    }
}