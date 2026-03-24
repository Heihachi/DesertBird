package me.anisimov.desertbird;

import android.content.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TubeColumn {

    private final Context context;
    private final List<TubePair> tubePairs = new ArrayList<>();
    private final Random random = new Random();

    private int points = 0;

    private int speed = 12;
    private final int MAX_SPEED = 18;

    private float spawnTimer = 0f;
    private int spawnDelay = 72;
    private final int MIN_SPAWN_DELAY = 48;
    private int gapHeight;
    private int minGapHeight;

    private final int screenWidth;
    private final int screenHeight;
    private final int groundHeight;
    private final int playableHeight;

    public TubeColumn(Context context, int screenWidth, int screenHeight, int groundHeight) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.groundHeight = groundHeight;
        this.playableHeight = screenHeight - groundHeight;

        gapHeight = (int) (playableHeight * 0.22f);

        minGapHeight = (int) (playableHeight * 0.16f);

        addTubePair();
    }

    private void addTubePair() {
        int minRockHeight = 140;

        int minGapY = minRockHeight;
        int maxGapY = playableHeight - gapHeight - minRockHeight;

        if (maxGapY < minGapY) {
            maxGapY = minGapY;
        }

        int gapY = minGapY + random.nextInt(maxGapY - minGapY + 1);

        tubePairs.add(new TubePair(
                context,
                playableHeight,
                screenWidth + 150,
                gapY,
                gapHeight
        ));
    }

    public void tickNoScore(float deltaTime) {
        Iterator<TubePair> iterator = tubePairs.iterator();

        while (iterator.hasNext()) {
            TubePair pair = iterator.next();
            pair.tick(speed, deltaTime);

            if (pair.isOffScreen()) {
                iterator.remove();
            }
        }
    }

    public boolean tick(Bird bird, float deltaTime) {
        boolean scoredNow = false;

        float dtScale = deltaTime * 60f;
        spawnTimer += dtScale;

        Iterator<TubePair> iterator = tubePairs.iterator();

        while (iterator.hasNext()) {
            TubePair pair = iterator.next();

            pair.tick(speed, deltaTime);

            if (pair.checkAndScore(bird)) {
                points++;
                scoredNow = true;
                increaseDifficulty();
            }

            if (pair.isOffScreen()) {
                iterator.remove();
            }
        }

        if (spawnTimer >= spawnDelay) {
            spawnTimer = 0f;
            addTubePair();
        }

        return scoredNow;
    }

    private void increaseDifficulty() {
        if (points % 5 == 0 && speed < MAX_SPEED) {
            speed++;
        }

        if (points % 4 == 0 && gapHeight > minGapHeight) {
            gapHeight -= (int) (playableHeight * 0.01f);
        }

        if (points % 6 == 0 && spawnDelay > MIN_SPAWN_DELAY) {
            spawnDelay -= 2;
        }
    }

    public boolean checkCollision(Bird bird) {
        for (TubePair pair : tubePairs) {
            if (pair.collides(bird)) {
                return true;
            }
        }
        return false;
    }

    public List<TubePair> getTubePairs() {
        return tubePairs;
    }

    public int getPoints() {
        return points;
    }

    public int getSpeed() {
        return speed;
    }
    public void setGapHeight(int gapHeight) {
        this.gapHeight = Math.max(minGapHeight, gapHeight);
    }

    public int getGapHeight() {
        return gapHeight;
    }
}