package com.nbplus.randommusic;

/**
 * Created by basagee on 2015. 6. 18..
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.vov.vitamio.LibsChecker;

/**
 * Main activity: shows media player buttons. This activity shows the media player buttons and
 * lets the user click them. No media handling is done here -- everything is done by passing
 * Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity implements View.OnClickListener {
    /**
     * The URL we suggest as default when adding by URL. This is just so that the user doesn't
     * have to find an URL to test this sample.
     */
    final String SUGGESTED_URL = "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg";
    Button mPlayButton;
    Button mPauseButton;
    Button mSkipButton;
    Button mRewindButton;
    Button mStopButton;
    Button mEjectButton;
    /**
     * Called when the activity is first created. Here, we simply set the event listeners and
     * start the background service ({@link MusicService}) that will handle the actual media
     * playback.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        mPlayButton = (Button) findViewById(R.id.playbutton);
        mPauseButton = (Button) findViewById(R.id.pausebutton);
        mSkipButton = (Button) findViewById(R.id.skipbutton);
        mRewindButton = (Button) findViewById(R.id.rewindbutton);
        mStopButton = (Button) findViewById(R.id.stopbutton);
        mEjectButton = (Button) findViewById(R.id.ejectbutton);
        mPlayButton.setOnClickListener(this);
        mPauseButton.setOnClickListener(this);
        mSkipButton.setOnClickListener(this);
        mRewindButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mEjectButton.setOnClickListener(this);
    }
    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        Intent intent = new Intent(this, MusicService.class);

        if (target == mPlayButton) {
            intent.setAction(MusicService.ACTION_PLAY);
            startService(intent);
        } else if (target == mPauseButton) {
            intent.setAction(MusicService.ACTION_PAUSE);
            startService(intent);
        } else if (target == mSkipButton) {
            intent.setAction(MusicService.ACTION_SKIP);
            startService(intent);
        } else if (target == mRewindButton) {
            intent.setAction(MusicService.ACTION_REWIND);
            startService(intent);
        } else if (target == mStopButton) {
            intent.setAction(MusicService.ACTION_STOP);
            startService(intent);
        } else if (target == mEjectButton) {
            showUrlDialog();
        }
    }
    /**
     * Shows an alert dialog where the user can input a URL. After showing the dialog, if the user
     * confirms, sends the appropriate intent to the {@link MusicService} to cause that URL to be
     * played.
     */
    void showUrlDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Manual Input");
        alertBuilder.setMessage("Enter a URL (must be http://)");
        final EditText input = new EditText(this);
        alertBuilder.setView(input);
        input.setText(SUGGESTED_URL);
        alertBuilder.setPositiveButton("Play!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int whichButton) {
                // Send an intent with the URL of the song to play. This is expected by
                // MusicService.
                Intent i = new Intent(MusicService.ACTION_URL);
                Uri uri = Uri.parse(input.getText().toString());
                i.setData(uri);
                startService(i);
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int whichButton) {}
        });
        alertBuilder.show();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
