package me.anisimov.desertbird;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class SoundManager {

    private final Context context;

    private MediaPlayer mainPlayer;
    private MediaPlayer gameOverPlayer;

    private SoundPool soundPool;

    private int jumpSound;
    private int scoreSound;
    private int hitSound;
    private int deadSound;

    private boolean muted = false;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();

        muted = SettingsManager.loadMuted(this.context);

        initSoundPool();
        loadSounds();
        loadMusic();
    }

    private void initSoundPool() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();
    }

    private void loadSounds() {
        jumpSound = soundPool.load(context, R.raw.jump, 1);
        scoreSound = soundPool.load(context, R.raw.score, 1);
        hitSound = soundPool.load(context, R.raw.hit, 1);
        deadSound = soundPool.load(context, R.raw.dead, 1);
    }

    private void loadMusic() {
        releaseMusicOnly();

        mainPlayer = MediaPlayer.create(context, R.raw.main);
        if (mainPlayer != null) {
            mainPlayer.setLooping(true);
        }

        gameOverPlayer = MediaPlayer.create(context, R.raw.game_over);
    }

    public void playMainLoop() {
        if (muted || mainPlayer == null) return;

        try {
            if (!mainPlayer.isPlaying()) {
                mainPlayer.seekTo(0);
                mainPlayer.start();
            }
        } catch (IllegalStateException e) {
            loadMusic();
            if (!muted && mainPlayer != null) {
                try {
                    mainPlayer.start();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMain() {
        if (mainPlayer == null) return;

        try {
            if (mainPlayer.isPlaying()) {
                mainPlayer.pause();
            }
            mainPlayer.seekTo(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playGameOver() {
        if (muted || gameOverPlayer == null) return;

        try {
            if (gameOverPlayer.isPlaying()) {
                gameOverPlayer.seekTo(0);
            } else {
                gameOverPlayer.start();
            }
        } catch (IllegalStateException e) {
            loadMusic();
            if (!muted && gameOverPlayer != null) {
                try {
                    gameOverPlayer.start();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopGameOver() {
        if (gameOverPlayer == null) return;

        try {
            if (gameOverPlayer.isPlaying()) {
                gameOverPlayer.pause();
            }
            gameOverPlayer.seekTo(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ensureMainPlayingIfAllowed() {
        if (!muted) {
            playMainLoop();
        }
    }

    private void play(int soundId) {
        if (muted || soundPool == null || soundId == 0) return;
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    public void playJump() {
        play(jumpSound);
    }

    public void playScore() {
        play(scoreSound);
    }

    public void playHit() {
        play(hitSound);
    }

    public void playDead() {
        play(deadSound);
    }

    public void toggleMute() {
        muted = !muted;
        SettingsManager.saveMuted(context, muted);

        if (muted) {
            stopMain();
            stopGameOver();
        } else {
            playMainLoop();
        }
    }

    public void toggleMuteWithoutResume() {
        muted = !muted;
        SettingsManager.saveMuted(context, muted);

        if (muted) {
            stopMain();
            stopGameOver();
        }
    }

    public boolean isMuted() {
        return muted;
    }

    private void releaseMusicOnly() {
        if (mainPlayer != null) {
            try {
                mainPlayer.release();
            } catch (Exception ignored) {
            }
            mainPlayer = null;
        }

        if (gameOverPlayer != null) {
            try {
                gameOverPlayer.release();
            } catch (Exception ignored) {
            }
            gameOverPlayer = null;
        }
    }

    public void release() {
        releaseMusicOnly();

        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {
            }
            soundPool = null;
        }
    }
}