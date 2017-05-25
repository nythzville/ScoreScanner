package com.maven.scorescanner.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by nathan on 5/25/2017.
 */


public class PlayMedia extends AsyncTask<Void, Void, Void> {

    private static final String LOG_TAG = PlayMedia.class.getSimpleName();

    Context context;
    private MediaPlayer mediaPlayer;
    int[] soundIDs;
    int idx =1;

    public PlayMedia(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }
    public PlayMedia(final Context context, final int[] soundIDs) {
        this.context = context;
        this.soundIDs=soundIDs;
        mediaPlayer = MediaPlayer.create(context,soundIDs[0]);
        setNextMediaForMediaPlayer(mediaPlayer);
    }

    public void setNextMediaForMediaPlayer(MediaPlayer player){
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if(soundIDs.length>idx){
                    mp.release();
                    mp = MediaPlayer.create(context,soundIDs[idx]);
                    setNextMediaForMediaPlayer(mp);
                    mp.start();
                    idx+=1;
                }
            }
        });
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "", e);
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "", e);
        }

        return null;
    }
}