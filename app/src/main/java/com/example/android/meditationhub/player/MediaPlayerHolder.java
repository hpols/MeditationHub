/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.meditationhub.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.example.android.meditationhub.ui.PlayerActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link PlayerActivity} can control music playback.
 */
public final class MediaPlayerHolder implements PlayerAdapter {

    private static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;

    private final Context ctxt;
    private MediaPlayer mediaPlayer;
    private Uri audioUri;
    private PlaybackInfoListener playbackInfoListener;
    private ScheduledExecutorService executorService;
    private Runnable runnableSeekbarUpdate;

    private int currentPosition;

    public MediaPlayerHolder(Context ctxt) {
        this.ctxt = ctxt.getApplicationContext();
    }

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link PlayerActivity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link PlayerActivity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopUpdatingCallbackWithPosition();
                    if (playbackInfoListener != null) {
                        playbackInfoListener.onStateChanged(PlaybackInfoListener.State.COMPLETED);
                        playbackInfoListener.onPlaybackCompleted();
                    }
                }
            });
        }
    }

    public void setPlaybackInfoListener(PlaybackInfoListener listener) {
        playbackInfoListener = listener;
    }

    // Implements PlaybackControl.
    @Override
    public void loadMedia(Uri audioUri) {
        this.audioUri = audioUri;
        String audioSource = audioUri.toString();
        String[] separated = audioSource.split("//");
        String audioAsFile = "File://"+ separated[1];
        Uri audioFile = Uri.parse(audioAsFile);
        initializeMediaPlayer();

        try {
            mediaPlayer.setDataSource(ctxt, audioUri);
        } catch (Exception e) {
            Timber.e(e.toString());
        }

        try {
            mediaPlayer.prepare();
        } catch (Exception e) {
            Timber.e(e.toString());
        }

        initializeProgressCallback();
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(currentPosition);
            mediaPlayer.start();
            if (playbackInfoListener != null) {
                playbackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);

                if(currentPosition != 0) {
                    mediaPlayer.seekTo(currentPosition);
                }
            }
            startUpdatingCallbackWithPosition();
        }
    }

    @Override
    public void reset() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            setPosition(0);
            loadMedia(audioUri);
            if (playbackInfoListener != null) {
                playbackInfoListener.onStateChanged(PlaybackInfoListener.State.RESET);
                currentPosition = 0;
            }
            stopUpdatingCallbackWithPosition();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (playbackInfoListener != null) {
                playbackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
                currentPosition = mediaPlayer.getCurrentPosition();
            }
        }
    }

    @Override
    public void seekTo(int position) {
        this.currentPosition = position;
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    /**
     * Syncs the mediaPlayer currentPosition with mPlaybackProgressCallback via recurring task.
     */
    private void startUpdatingCallbackWithPosition() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }
        if (runnableSeekbarUpdate == null) {
            runnableSeekbarUpdate = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                }
            };
        }
        executorService.scheduleAtFixedRate(
                runnableSeekbarUpdate,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    // Reports media playback currentPosition to mPlaybackProgressCallback.
    private void stopUpdatingCallbackWithPosition() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
            runnableSeekbarUpdate = null;
            if (true && playbackInfoListener != null) {
                playbackInfoListener.onPositionChanged(0);
            }
        }
    }

    private void updateProgressCallbackTask() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentPosition = mediaPlayer.getCurrentPosition();
            if (playbackInfoListener != null) {
                playbackInfoListener.onPositionChanged(currentPosition);
            }
        }
    }

    @Override
    public void initializeProgressCallback() {
        final int duration = mediaPlayer.getDuration();
        if (playbackInfoListener != null) {
            playbackInfoListener.onDurationChanged(duration);
            playbackInfoListener.onPositionChanged(0);
        }
    }

    public int getPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void setPosition(int position) {
        this.currentPosition = position;
    }
}
