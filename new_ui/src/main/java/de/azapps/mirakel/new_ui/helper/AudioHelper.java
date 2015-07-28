/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.helper;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.FileInputStream;
import java.io.IOException;

import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class AudioHelper {


    private static final String TAG = "AudioHelper";

    public static void playBack(final Activity activity, final FileMirakel file, final boolean loud) {
        final MediaPlayer mPlayer = new MediaPlayer();
        final AudioManager am = (AudioManager) activity
                                .getSystemService(Context.AUDIO_SERVICE);
        if (!loud) {
            am.setSpeakerphoneOn(false);
            am.setMode(AudioManager.MODE_IN_CALL);
            activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }
        FileInputStream stream1;
        try {
            stream1 = file.getFileStream(activity);;
            mPlayer.reset();
            if (!loud) {
                mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            }
            mPlayer.setDataSource(stream1.getFD());
            mPlayer.prepare();

            am.setMode(AudioManager.MODE_NORMAL);
        } catch (final IOException e) {
            Log.e(TAG, "prepare() failed");
            stream1 = null;
        }
        final FileInputStream stream = stream1;
        final Dialog dialog = new AlertDialogWrapper.Builder(activity)
        .setTitle(R.string.audio_playback_title)
        .setPositiveButton(R.string.audio_playback_pause, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final Button button = ((AlertDialog) dialog)
                                      .getButton(DialogInterface.BUTTON_POSITIVE);
                if (!mPlayer.isPlaying()) {
                    button.setText(R.string.audio_playback_play);
                    mPlayer.start();
                } else {
                    button.setText(R.string.audio_playback_pause);
                    mPlayer.pause();
                }
                button.invalidate();
            }
        }).setNegativeButton(R.string.audio_playback_stop, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(final DialogInterface dialog) {
                mPlayer.stop();
                mPlayer.release();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        Log.wtf(TAG, "cannot close file", e);
                    }
                }
            }
        }).show();
        mPlayer.start();
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(final MediaPlayer mp) {
                dialog.dismiss();
            }
        });
    }
}
