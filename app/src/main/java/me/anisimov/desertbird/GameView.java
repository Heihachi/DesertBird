package me.anisimov.desertbird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private boolean playing;
    private final SurfaceHolder holder;
    private final Paint paint;

    private Bird bird;
    private Bird menuBird;
    private TubeColumn tubeColumn;
    private SoundManager soundManager;

    private boolean isRunning = false;
    private boolean gameOver = false;
    private boolean deathSequenceStarted = false;

    private boolean hitSoundPlayed = false;
    private boolean deadSoundPlayed = false;
    private boolean gameOverSoundPlayed = false;

    private int highScore;

    private boolean startDelayActive = false;
    private long startDelayStartTime = 0L;
    private static final long START_DELAY_MS = 350L;

    private Bitmap background;
    private Bitmap groundTile;
    private Bitmap frameLarge;
    private Bitmap frameSmall;

    private Bitmap scaledBackground;
    private Bitmap scaledGroundTile;
    private Bitmap scaledFrameLarge;

    private int screenWidth = 0;
    private int screenHeight = 0;
    private final int groundHeight = 180;
    private float groundOffset = 0f;

    private float uiScale = 1f;
    private final Rect largeFrameRect = new Rect();
    private final Rect soundButtonRect = new Rect();

    // Splash screen
    private boolean showSplash = true;
    private long splashStartTime = 0L;
    private static final long SPLASH_DURATION_MS = 3600L;
    private Drawable splashDrawable;

    public GameView(Context context) {
        super(context);

        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        soundManager = new SoundManager(context);
        highScore = SettingsManager.loadHighScore(context);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        groundTile = BitmapFactory.decodeResource(getResources(), R.drawable.ground_tile);
        frameLarge = BitmapFactory.decodeResource(getResources(), R.drawable.frame_1);
        frameSmall = BitmapFactory.decodeResource(getResources(), R.drawable.frame_2);

        splashDrawable = ContextCompat.getDrawable(context, R.drawable.load_screen);
        splashStartTime = SystemClock.uptimeMillis();
    }

    private int getBirdSize() {
        int playableHeight = screenHeight - groundHeight;
        int size = (int) (playableHeight * 0.14f);
        if (size < 72) size = 72;
        return size;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (soundManager != null) {
            soundManager.release();
        }
    }

    private void prepareScaledAssets() {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        if (background != null && scaledBackground == null) {
            scaledBackground = Bitmap.createScaledBitmap(background, screenWidth, screenHeight, true);
        }

        if (groundTile != null && scaledGroundTile == null && groundTile.getWidth() > 0) {
            scaledGroundTile = Bitmap.createScaledBitmap(
                    groundTile,
                    groundTile.getWidth(),
                    groundHeight,
                    true
            );
        }

        if (frameLarge != null && scaledFrameLarge == null) {
            int targetW = (int) (screenWidth * 0.32f);
            int targetH = (int) (frameLarge.getHeight() * (targetW / (float) frameLarge.getWidth()));
            scaledFrameLarge = Bitmap.createScaledBitmap(frameLarge, targetW, targetH, true);
        }
    }

    private void ensureScreenSize() {
        int w = getWidth();
        int h = getHeight();

        if (w > 0 && h > 0) {
            boolean sizeChanged = (screenWidth != w || screenHeight != h);

            screenWidth = w;
            screenHeight = h;

            float scaleX = screenWidth / 1920f;
            float scaleY = screenHeight / 1080f;
            uiScale = Math.min(scaleX, scaleY);
            if (uiScale < 0.65f) {
                uiScale = 0.65f;
            }

            if (sizeChanged) {
                scaledBackground = null;
                scaledGroundTile = null;
                scaledFrameLarge = null;
            }

            prepareScaledAssets();
            updateSoundButtonRect();

            if (menuBird == null) {
                menuBird = new Bird(getContext(), screenWidth * 0.20f, screenHeight * 0.42f, getBirdSize());
            }
        }
    }

    private void updateSoundButtonRect() {
        int x = (int) (20 * uiScale);
        int y = (int) (15 * uiScale);
        int w = (int) (190 * uiScale);
        int h = (int) (68 * uiScale);

        soundButtonRect.set(x, y, x + w, y + h);
    }

    private void initGame() {
        ensureScreenSize();

        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        bird = new Bird(getContext(), screenWidth * 0.25f, screenHeight * 0.5f, getBirdSize());
        tubeColumn = new TubeColumn(getContext(), screenWidth, screenHeight, groundHeight);

        isRunning = true;
        gameOver = false;
        deathSequenceStarted = false;

        hitSoundPlayed = false;
        deadSoundPlayed = false;
        gameOverSoundPlayed = false;

        groundOffset = 0f;

        if (soundManager != null) {
            soundManager.stopGameOver();
            soundManager.ensureMainPlayingIfAllowed();
        }

        startDelayActive = true;
        startDelayStartTime = android.os.SystemClock.uptimeMillis();

        soundManager.playMainLoop();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();

        while (playing) {
            ensureScreenSize();

            if (screenWidth <= 0 || screenHeight <= 0 || !holder.getSurface().isValid()) {
                sleepBriefly();
                lastTime = System.nanoTime();
                continue;
            }

            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            if (deltaTime > 0.033f) {
                deltaTime = 0.033f;
            }

            update(deltaTime);
            drawGame();

            sleepBriefly();
        }
    }

    private void update(float deltaTime) {
        if (showSplash) {
            long elapsed = android.os.SystemClock.uptimeMillis() - splashStartTime;
            if (elapsed >= SPLASH_DURATION_MS) {
                showSplash = false;
                if (soundManager != null) {
                    soundManager.ensureMainPlayingIfAllowed();
                }
            }
            return;
        }

        if (startDelayActive) {
            long elapsed = android.os.SystemClock.uptimeMillis() - startDelayStartTime;
            if (elapsed >= START_DELAY_MS) {
                startDelayActive = false;
            } else {
                // bird holds in place before gameplay starts
                if (bird != null) {
                    bird.tickIdleOnly(deltaTime);
                }
                return;
            }
        }

        float dtScale = deltaTime * 60f;

        if (!isRunning && !gameOver && menuBird != null) {
            menuBird.tickIdleOnly(deltaTime);
            menuBird.setY((float) (screenHeight * 0.42f + Math.sin(System.nanoTime() / 180_000_000.0) * 8 * uiScale));
            return;
        }

        if (!isRunning) {
            return;
        }

        if (tubeColumn != null) {
            groundOffset -= tubeColumn.getSpeed() * dtScale;

            if (scaledGroundTile != null) {
                int tileWidth = scaledGroundTile.getWidth();
                if (tileWidth > 0) {
                    while (groundOffset <= -tileWidth) {
                        groundOffset += tileWidth;
                    }
                }
            }
        }

        if (bird != null) {
            bird.tick(deltaTime);
        }

        boolean scoredNow = false;

        if (tubeColumn != null && bird != null) {
            if (!deathSequenceStarted) {
                scoredNow = tubeColumn.tick(bird, deltaTime);

                if (tubeColumn.checkCollision(bird)) {
                    deathSequenceStarted = true;
                    bird.hitRock();
                    scoredNow = false;
                }
            } else {
                tubeColumn.tickNoScore(deltaTime);
            }
        }

        if (scoredNow) {
            soundManager.playScore();
        }

        if (!deathSequenceStarted && bird != null && bird.getY() < 0) {
            bird.setY(0);
            bird.setVelocityY(0);
        }

        if (deathSequenceStarted) {
            if (!hitSoundPlayed) {
                soundManager.playHit();
                hitSoundPlayed = true;
            } else if (!deadSoundPlayed) {
                soundManager.playDead();
                deadSoundPlayed = true;
            }
        }

        int groundCollisionInset = 85;

        int groundCollisionY = screenHeight - groundHeight + groundCollisionInset;

        if (bird != null && bird.getY() + bird.getHeight() >= groundCollisionY) {
            bird.setY(groundCollisionY - bird.getHeight());

            if (!gameOverSoundPlayed) {
                soundManager.stopMain();
                soundManager.playGameOver();
                gameOverSoundPlayed = true;
            }

            endGame();
        }
    }

    private void drawGame() {
        if (!holder.getSurface().isValid()) {
            return;
        }

        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }

        try {
            if (showSplash) {
                drawSplash(canvas);
            } else {
                drawBackground(canvas);
                drawGround(canvas);
                drawBottomCaps(canvas);

                if (tubeColumn != null) {
                    for (TubePair pair : tubeColumn.getTubePairs()) {
                        pair.draw(canvas, paint);
                    }
                }

                if (!isRunning && !gameOver && menuBird != null) {
                    menuBird.draw(canvas, paint);
                }

                if (bird != null) {
                    bird.draw(canvas, paint);
                }

                drawHUD(canvas);

                if (!isRunning) {
                    drawSoundButton(canvas);
                    drawOverlay(canvas);
                }
            }
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawSplash(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        if (splashDrawable == null || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        long elapsed = SystemClock.uptimeMillis() - splashStartTime;
        float progress = Math.min(1f, elapsed / 600f);
        int alpha = (int) (255 * progress);

        int targetW = (int) (screenWidth * 0.42f);
        int intrinsicW = splashDrawable.getIntrinsicWidth();
        int intrinsicH = splashDrawable.getIntrinsicHeight();

        int targetH;
        if (intrinsicW > 0 && intrinsicH > 0) {
            targetH = (int) (intrinsicH * (targetW / (float) intrinsicW));
        } else {
            targetH = (int) (screenHeight * 0.25f);
        }

        int left = (screenWidth - targetW) / 2;
        int top = (screenHeight - targetH) / 2;

        splashDrawable.setBounds(left, top, left + targetW, top + targetH);
        splashDrawable.setAlpha(alpha);
        splashDrawable.draw(canvas);

        //paint.setColor(Color.argb(alpha, 255, 255, 255));
        //paint.setTypeface(Typeface.DEFAULT_BOLD);
        //paint.setTextAlign(Paint.Align.CENTER);
        //paint.setTextSize(36f * uiScale);
        //canvas.drawText("Loading...", screenWidth / 2f, top + targetH + 70f * uiScale, paint);
    }

    private void drawBackground(Canvas canvas) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        if (scaledBackground != null) {
            canvas.drawBitmap(scaledBackground, 0, 0, null);
        } else {
            canvas.drawColor(Color.rgb(180, 220, 255));
        }
    }

    private void drawGround(Canvas canvas) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        int y = screenHeight - groundHeight;

        if (scaledGroundTile != null && scaledGroundTile.getWidth() > 0) {
            int tileWidth = scaledGroundTile.getWidth();

            for (float x = groundOffset - tileWidth; x < screenWidth + tileWidth; x += tileWidth) {
                canvas.drawBitmap(scaledGroundTile, x, y, null);
            }
        } else {
            paint.setColor(Color.rgb(222, 184, 135));
            canvas.drawRect(0, y, screenWidth, screenHeight, paint);
        }
    }

    private void drawBottomCaps(Canvas canvas) {
        Bitmap bottomCap = Tube.getRockBottomCap();
        if (bottomCap == null || tubeColumn == null) {
            return;
        }

        for (TubePair pair : tubeColumn.getTubePairs()) {
            Tube bottom = pair.getBottomTube();
            if (bottom == null || bottom.isTopTube()) {
                continue;
            }

            int capVisualDrop = 24;

            float drawY = bottom.getY() + bottom.getHeight() - Tube.BOTTOM_CAP_HEIGHT + capVisualDrop;

            Rect src = new Rect(0, 0, bottomCap.getWidth(), bottomCap.getHeight());
            Rect dst = new Rect(
                    (int) bottom.getX(),
                    (int) drawY,
                    (int) bottom.getX() + bottom.getWidth(),
                    (int) drawY + Tube.BOTTOM_CAP_HEIGHT
            );

            canvas.drawBitmap(bottomCap, src, dst, null);
        }
    }

    private void drawSmallFrame(Canvas canvas, int x, int y, int w, int h) {
        if (frameSmall != null) {
            canvas.drawBitmap(frameSmall, null, new Rect(x, y, x + w, y + h), null);
        } else {
            paint.setColor(Color.argb(160, 0, 0, 0));
            canvas.drawRoundRect(x, y, x + w, y + h, 25, 25, paint);
        }
    }

    private void drawLargeFrame(Canvas canvas) {
        if (scaledFrameLarge == null || screenWidth <= 0) {
            return;
        }

        int x = (screenWidth - scaledFrameLarge.getWidth()) / 2;
        int y = 0;

        largeFrameRect.set(
                x,
                y,
                x + scaledFrameLarge.getWidth(),
                y + scaledFrameLarge.getHeight()
        );

        canvas.drawBitmap(scaledFrameLarge, x, y, null);
    }

    private void drawCenteredText(Canvas canvas, String text, float centerX, float y) {
        float textWidth = paint.measureText(text);
        canvas.drawText(text, centerX - textWidth / 2f, y, paint);
    }

    private void drawSoundButton(Canvas canvas) {
        int x = soundButtonRect.left;
        int y = soundButtonRect.top;
        int w = soundButtonRect.width();
        int h = soundButtonRect.height();

        if (frameSmall != null) {
            canvas.drawBitmap(frameSmall, null, soundButtonRect, null);
        } else {
            paint.setColor(Color.argb(160, 0, 0, 0));
            canvas.drawRoundRect(x, y, x + w, y + h, 20 * uiScale, 20 * uiScale, paint);
        }

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(28f * uiScale);
        paint.setColor(Color.rgb(40, 20, 10));
        paint.setTextAlign(Paint.Align.LEFT);

        String text = soundManager.isMuted() ? "Sound: OFF" : "Sound: ON";
        float textWidth = paint.measureText(text);
        float textX = x + (w - textWidth) / 2f;

        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = y + (h / 2f) - ((fm.ascent + fm.descent) / 2f);

        canvas.drawText(text, textX, textY, paint);
    }
    private void drawHUD(Canvas canvas) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(42f * uiScale);
        paint.setColor(Color.rgb(40, 20, 10));

        int paddingX = (int) (22 * uiScale);
        int panelH = (int) (72 * uiScale);
        int topMargin = (int) (15 * uiScale);
        int textBaselineOffset = (int) (47 * uiScale);

        String highScoreText = "High Score: " + highScore;
        int highTextW = (int) paint.measureText(highScoreText);
        int highPanelW = highTextW + paddingX * 2;
        int highPanelX = screenWidth - highPanelW - (int) (20 * uiScale);
        int highPanelY = topMargin;

        drawSmallFrame(canvas, highPanelX, highPanelY, highPanelW, panelH);
        canvas.drawText(highScoreText, highPanelX + paddingX, highPanelY + textBaselineOffset, paint);

        if (isRunning && tubeColumn != null) {
            String scoreText = "Score: " + tubeColumn.getPoints();
            int scoreTextW = (int) paint.measureText(scoreText);
            int scorePanelW = scoreTextW + paddingX * 2;
            int scorePanelX = (int) (20 * uiScale);
            int scorePanelY = topMargin;

            drawSmallFrame(canvas, scorePanelX, scorePanelY, scorePanelW, panelH);
            canvas.drawText(scoreText, scorePanelX + paddingX, scorePanelY + textBaselineOffset, paint);
        }
    }

    private void drawOverlay(Canvas canvas) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        drawLargeFrame(canvas);

        float centerX = largeFrameRect.exactCenterX();
        float frameTop = largeFrameRect.top;
        float frameHeight = largeFrameRect.height();

        paint.setColor(Color.rgb(60, 30, 15));
        paint.setTextAlign(Paint.Align.LEFT);

        if (gameOver) {
            paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            paint.setTextSize(84f * uiScale);
            drawCenteredText(canvas, "Game Over", centerX, frameTop + frameHeight * 0.56f);

            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(48f * uiScale);
            int score = tubeColumn != null ? tubeColumn.getPoints() : 0;
            drawCenteredText(canvas, "Score: " + score, centerX, frameTop + frameHeight * 0.68f);
            drawCenteredText(canvas, "Tap to restart", centerX, frameTop + frameHeight * 0.81f);
            drawCenteredText(canvas, "Tap to jump", centerX, frameTop + frameHeight * 0.91f);
        } else {
            paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            paint.setTextSize(88f * uiScale);
            drawCenteredText(canvas, "Desert Bird", centerX, frameTop + frameHeight * 0.56f);

            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(48f * uiScale);
            drawCenteredText(canvas, "Tap to start", centerX, frameTop + frameHeight * 0.67f);
            drawCenteredText(canvas, "Tap during game to jump", centerX, frameTop + frameHeight * 0.80f);
        }
    }

    private void endGame() {
        isRunning = false;
        gameOver = true;
        deathSequenceStarted = false;

        if (tubeColumn != null && tubeColumn.getPoints() > highScore) {
            highScore = tubeColumn.getPoints();
            SettingsManager.saveHighScore(getContext(), highScore);
        }
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignored) {
        }
    }

    public void resume() {
        if (playing) {
            return;
        }

        playing = true;
        gameThread = new Thread(this);
        gameThread.start();

        if (soundManager != null && !soundManager.isMuted() && !gameOver && !showSplash) {
            soundManager.ensureMainPlayingIfAllowed();
        }
    }

    public void pause() {
        playing = false;

        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException ignored) {
            }
        }

        if (soundManager != null) {
            soundManager.stopMain();
            soundManager.stopGameOver();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        ensureScreenSize();

        if (screenWidth <= 0 || screenHeight <= 0) {
            return true;
        }

        float touchX = event.getX();
        float touchY = event.getY();

        if (!isRunning && soundButtonRect.contains((int) touchX, (int) touchY)) {
            if (!gameOver) {
                soundManager.toggleMute();
            } else {
                soundManager.toggleMuteWithoutResume();
            }
            return true;
        }

        if (!isRunning) {
            initGame();
            return true;
        }

        if (!gameOver && bird != null) {
            bird.jump();
            soundManager.playJump();
        }

        return true;
    }
}