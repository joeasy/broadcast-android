package com.nbplus.media;

/**
 * Created by basagee on 2015. 6. 18..
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Receives broadcasted intents. In particular, we are interested in the
 * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON intents, which is
 * broadcast, for example, when the user disconnects the headphones. This class works because we are
 * declaring it in a &lt;receiver&gt; tag in AndroidManifest.xml.
 */
public class MusicIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            Toast.makeText(context, "Headphones disconnected.", Toast.LENGTH_SHORT).show();

            // send an intent to our MusicService to telling it to pause the audio
            Intent musicServiceIntent = new Intent(context, MusicService.class);
            musicServiceIntent.setAction(MusicService.ACTION_PAUSE);
            context.startService(musicServiceIntent);
        } else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;

            Intent musicServiceIntent = new Intent(context, MusicService.class);
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    musicServiceIntent.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
                    context.startService(musicServiceIntent);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    musicServiceIntent.setAction(MusicService.ACTION_PLAY);
                    context.startService(musicServiceIntent);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    musicServiceIntent.setAction(MusicService.ACTION_PAUSE);
                    context.startService(musicServiceIntent);
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    musicServiceIntent.setAction(MusicService.ACTION_STOP);
                    context.startService(musicServiceIntent);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    musicServiceIntent.setAction(MusicService.ACTION_SKIP);
                    context.startService(musicServiceIntent);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    // previous song
                    musicServiceIntent.setAction(MusicService.ACTION_REWIND);
                    context.startService(musicServiceIntent);
                    break;
            }
        }
    }
}
