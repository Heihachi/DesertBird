package me.anisimov.desertbird;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class TubePair {

    private final Tube topTube;
    private final Tube bottomTube;
    private boolean scored = false;

    public TubePair(Context context, int playableHeight, float x, int gapY, int gapHeight) {
        int minRockHeight = 140;

        int topHeight = Math.max(minRockHeight, gapY);

        int bottomShift = Tube.BOTTOM_CAP_HEIGHT + 28;

        int bottomY = gapY + gapHeight + bottomShift;

        int extraIntoGround = Tube.BOTTOM_CAP_HEIGHT + 28;

        int bottomHeight = Math.max(
                minRockHeight,
                playableHeight - bottomY + extraIntoGround
        );

        topTube = new Tube(context, x, 0, Tube.TUBE_WIDTH, topHeight, true);
        bottomTube = new Tube(context, x, bottomY, Tube.TUBE_WIDTH, bottomHeight, false);
    }

    public void tick(int speed, float deltaTime) {
        topTube.tick(speed, deltaTime);
        bottomTube.tick(speed, deltaTime);
    }

    public void draw(Canvas canvas, Paint paint) {
        topTube.draw(canvas, paint);
        bottomTube.draw(canvas, paint);
    }

    public boolean isOffScreen() {
        return topTube.getX() + topTube.getWidth() < 0;
    }

    public boolean collides(Bird bird) {
        RectF b = bird.getBounds();
        return RectF.intersects(b, topTube.getBounds()) || RectF.intersects(b, bottomTube.getBounds());
    }

    public boolean checkAndScore(Bird bird) {
        if (!scored && bird.getX() > topTube.getX() + topTube.getWidth()) {
            scored = true;
            return true;
        }
        return false;
    }

    public Tube getTopTube() {
        return topTube;
    }

    public Tube getBottomTube() {
        return bottomTube;
    }
}