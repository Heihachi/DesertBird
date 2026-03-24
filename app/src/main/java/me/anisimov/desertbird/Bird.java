package me.anisimov.desertbird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

public class Bird {

    public enum BirdState {
        IDLE, JUMP, FALL, HIT, DEAD_FALL
    }

    private static final int IDLE_BASE_FRAME_COUNT = 4;
    private static final int IDLE_ANIM_FRAME_COUNT = 6; // 0,1,2,3,2,1

    private float x, y;
    private final int width;
    private final int height;

    private float velocityY = 0f;
    private final float gravity = 0.72f;
    private final float jumpStrength = -14.5f;
    private final float fallLimit = 20f;

    private BirdState state = BirdState.IDLE;

    private Bitmap[] idleFrames;
    private Bitmap fallFrame;
    private Bitmap hitFrame;
    private Bitmap deadFrame;

    private int frameIndex = 0;
    private float animationTick = 0f;
    private float stateTimer = 0f;
    private boolean stunned = false;

    // flap animation for jump
    private boolean flapPlaying = false;
    private int flapLoopsLeft = 0;

    private final Matrix drawMatrix = new Matrix();

    public Bird(Context context, float x, float y, int size) {
        this.x = x;
        this.y = y;
        this.width = size;
        this.height = size;

        idleFrames = loadIdleSheet(context, R.drawable.eagle_idle_sheet, width, height);

        fallFrame = loadScaled(context, R.drawable.eagle_fall, width, height);
        hitFrame = loadScaled(context, R.drawable.eagle_hit, width, height);
        deadFrame = loadScaled(context, R.drawable.eagle_dead, width, height);
    }

    private Bitmap loadScaled(Context context, int resId, int targetW, int targetH) {
        Bitmap src = BitmapFactory.decodeResource(context.getResources(), resId);
        if (src == null) {
            return null;
        }
        return Bitmap.createScaledBitmap(src, targetW, targetH, true);
    }

    private Bitmap[] loadIdleSheet(Context context, int resId, int targetW, int targetH) {
        Bitmap[] frames = new Bitmap[IDLE_ANIM_FRAME_COUNT];

        Bitmap sheet = BitmapFactory.decodeResource(context.getResources(), resId);
        if (sheet == null) {
            return frames;
        }

        int frameWidth = sheet.getWidth() / IDLE_BASE_FRAME_COUNT;
        int frameHeight = sheet.getHeight();

        if (frameWidth <= 0 || frameHeight <= 0) {
            return frames;
        }

        Bitmap[] baseFrames = new Bitmap[IDLE_BASE_FRAME_COUNT];

        for (int i = 0; i < IDLE_BASE_FRAME_COUNT; i++) {
            Bitmap rawFrame = Bitmap.createBitmap(
                    sheet,
                    i * frameWidth,
                    0,
                    frameWidth,
                    frameHeight
            );

            baseFrames[i] = Bitmap.createScaledBitmap(rawFrame, targetW, targetH, true);
        }

        frames[0] = baseFrames[0];
        frames[1] = baseFrames[1];
        frames[2] = baseFrames[2];
        frames[3] = baseFrames[3];
        frames[4] = baseFrames[2];
        frames[5] = baseFrames[1];

        return frames;
    }

    public void tick(float deltaTime) {
        float dtScale = deltaTime * 60f;

        if (state == BirdState.HIT) {
            x -= 3f * dtScale;
            y -= 2f * dtScale;
            stateTimer -= dtScale;

            if (stateTimer <= 0f) {
                state = BirdState.DEAD_FALL;
                velocityY = 2f;
            }

            updateAnimation(deltaTime);
            return;
        }

        if (state == BirdState.DEAD_FALL) {
            velocityY += gravity * dtScale;
            if (velocityY > 18f) velocityY = 18f;
            y += velocityY * dtScale;
            updateAnimation(deltaTime);
            return;
        }

        velocityY += gravity * dtScale;
        if (velocityY > fallLimit) velocityY = fallLimit;

        y += velocityY * dtScale;

        if (velocityY > 1.5f) {
            state = BirdState.FALL;
        } else if (flapPlaying || velocityY < -1f) {
            state = BirdState.JUMP;
        } else {
            state = BirdState.IDLE;
        }

        updateAnimation(deltaTime);
    }

    public void tickIdleOnly(float deltaTime) {
        state = BirdState.IDLE;
        velocityY = 0f;
        flapPlaying = false;
        flapLoopsLeft = 0;
        updateAnimation(deltaTime);
    }

    private void updateAnimation(float deltaTime) {
        float dtScale = deltaTime * 60f;

        if (state == BirdState.IDLE) {
            animationTick += dtScale;
            if (animationTick >= 6f) {
                animationTick = 0f;
                frameIndex = (frameIndex + 1) % idleFrames.length;
            }
            return;
        }

        if (state == BirdState.JUMP) {
            // flap animation plays faster than idle
            animationTick += dtScale * 1.7f;

            if (animationTick >= 4f) {
                animationTick = 0f;
                frameIndex++;

                if (frameIndex >= idleFrames.length) {
                    frameIndex = 0;

                    if (flapPlaying && flapLoopsLeft > 0) {
                        flapLoopsLeft--;
                    }

                    if (flapLoopsLeft <= 0) {
                        flapPlaying = false;
                    }
                }
            }
            return;
        }

        // for FALL / HIT / DEAD_FALL no cyclic idle animation needed
        animationTick += dtScale;
    }

    public void jump() {
        if (state == BirdState.HIT || state == BirdState.DEAD_FALL) return;

        velocityY = jumpStrength;
        state = BirdState.JUMP;

        // start or refresh flap animation without visual glitch
        if (!flapPlaying) {
            flapPlaying = true;
            flapLoopsLeft = 1;
            frameIndex = 0;
            animationTick = 0f;
        } else {
            // fast taps: keep animation alive, but don't brutally reset every time
            flapLoopsLeft = 1;
            if (frameIndex >= idleFrames.length - 2) {
                frameIndex = 0;
                animationTick = 0f;
            }
        }
    }

    public void hitRock() {
        if (stunned) return;
        stunned = true;
        state = BirdState.HIT;
        stateTimer = 14f;
        velocityY = -1.5f;
        flapPlaying = false;
        flapLoopsLeft = 0;
    }

    public void draw(Canvas canvas, Paint paint) {
        Bitmap current = getCurrentFrame();

        float angle;
        if (state == BirdState.HIT) {
            angle = -20f;
        } else if (state == BirdState.DEAD_FALL) {
            angle = 75f;
        } else if (state == BirdState.IDLE) {
            angle = -5f;
        } else {
            angle = Math.max(-30f, Math.min(70f, velocityY * 5f));
        }

        if (current != null) {
            drawMatrix.reset();
            drawMatrix.postTranslate(x, y);
            drawMatrix.postRotate(angle, x + width / 2f, y + height / 2f);
            canvas.drawBitmap(current, drawMatrix, paint);
        } else {
            drawFallbackBird(canvas, paint);
        }
    }

    private Bitmap getCurrentFrame() {
        switch (state) {
            case JUMP:
                return getSafeIdleFrame(frameIndex);
            case FALL:
                return fallFrame != null ? fallFrame : getSafeIdleFrame(2);
            case HIT:
                return hitFrame != null ? hitFrame : getSafeIdleFrame(2);
            case DEAD_FALL:
                return deadFrame != null ? deadFrame : getSafeIdleFrame(2);
            case IDLE:
            default:
                return getSafeIdleFrame(frameIndex);
        }
    }

    private Bitmap getSafeIdleFrame(int index) {
        if (idleFrames == null || idleFrames.length == 0) return null;
        if (index < 0 || index >= idleFrames.length) return idleFrames[0];
        return idleFrames[index] != null ? idleFrames[index] : idleFrames[0];
    }

    private void drawFallbackBird(Canvas canvas, Paint paint) {
        int oldColor = paint.getColor();
        Paint.Style oldStyle = paint.getStyle();
        float oldStroke = paint.getStrokeWidth();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFD54F);
        canvas.drawOval(x + 15, y + 25, x + width - 15, y + height - 25, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(0xFF000000);
        canvas.drawOval(x + 15, y + 25, x + width - 15, y + height - 25, paint);

        paint.setStyle(oldStyle);
        paint.setStrokeWidth(oldStroke);
        paint.setColor(oldColor);
    }

    public RectF getBounds() {
        return new RectF(x + width * 0.17f, y + height * 0.17f, x + width * 0.83f, y + height * 0.83f);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setY(float y) { this.y = y; }
    public void setVelocityY(float velocityY) { this.velocityY = velocityY; }
}