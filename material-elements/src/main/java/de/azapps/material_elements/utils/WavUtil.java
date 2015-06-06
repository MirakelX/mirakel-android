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

package de.azapps.material_elements.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.Nullable;
import android.util.Log;


public class WavUtil {
    private static int ANDROID_SAMPLE_RATES [] = {22050, 16000, 11025, 44100, 8000, 48000};

    private static int RECORDER_SAMPLERATE = 22050;
    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BITS_PER_SAMPLE = 16;
    private static int bufferSize = 0;

    @Nullable
    public static AudioRecord getRecorder() {

        // first try stereo
        RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
        final AudioRecord audioRecordStereo = tryToGetAudio();
        if (audioRecordStereo != null) {
            return audioRecordStereo;
        }

        // set it to mono
        RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        final AudioRecord audioRecordMono = tryToGetAudio();
        if (audioRecordMono != null) {
            return audioRecordMono;
        }
        return null;
    }

    private static AudioRecord tryToGetAudio() {
        for (final int sampleRate : ANDROID_SAMPLE_RATES) {
            try {
                RECORDER_SAMPLERATE = sampleRate;
                bufferSize = getBufferSize();

                final AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, RECORDER_SAMPLERATE,
                        RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING,
                        bufferSize);

                if (ar.getState() == AudioRecord.STATE_INITIALIZED) {
                    return ar;
                }
            } catch (final IllegalArgumentException ignored) {

            }
        }
        return null;
    }

    public static int getBufferSize() {
        return AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }


    /**
     * @see http://www.edumobile.org/android/android-development/audio-recording-in-wav-format-in-android-programming/
     *
     * @param totalAudioLen
     * @return
     */
    public static byte [] getWavHeader(final long totalAudioLen) {
        final long totalDataLen=totalAudioLen+36;
        final byte[] header = new byte[44];
        final long longSampleRate = (long) RECORDER_SAMPLERATE;
        final int channels = (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1 : 2;
        final long byteRate = (long) ((RECORDER_BITS_PER_SAMPLE * RECORDER_SAMPLERATE * channels) / 8);


        header[0] = (byte) 'R';  // RIFF/WAVE header
        header[1] = (byte) 'I';
        header[2] = (byte) 'F';
        header[3] = (byte) 'F';
        header[4] = (byte) (totalDataLen & 0xffL);
        header[5] = (byte) ((totalDataLen >> 8) & 0xffL);
        header[6] = (byte) ((totalDataLen >> 16) & 0xffL);
        header[7] = (byte) ((totalDataLen >> 24) & 0xffL);
        header[8] = (byte) 'W';
        header[9] = (byte) 'A';
        header[10] = (byte) 'V';
        header[11] = (byte) 'E';
        header[12] = (byte) 'f';  // 'fmt ' chunk
        header[13] = (byte) 'm';
        header[14] = (byte) 't';
        header[15] = (byte) ' ';
        header[16] = (byte) 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = (byte) 0;
        header[18] = (byte) 0;
        header[19] = (byte) 0;
        header[20] = (byte) 1;  // format = 1
        header[21] = (byte) 0;
        header[22] = (byte) channels;
        header[23] = (byte) 0;
        header[24] = (byte) (longSampleRate & 0xffL);
        header[25] = (byte) ((longSampleRate >> 8) & 0xffL);
        header[26] = (byte) ((longSampleRate >> 16) & 0xffL);
        header[27] = (byte) ((longSampleRate >> 24) & 0xffL);
        header[28] = (byte) (byteRate & 0xffL);
        header[29] = (byte) ((byteRate >> 8) & 0xffL);
        header[30] = (byte) ((byteRate >> 16) & 0xffL);
        header[31] = (byte) ((byteRate >> 24) & 0xffL);
        header[32] = (byte) ((2 * 16) / 8);  // block align
        header[33] = (byte) 0;
        header[34] = (byte) RECORDER_BITS_PER_SAMPLE;  // bits per sample
        header[35] = (byte) 0;
        header[36] = (byte) 'd';
        header[37] = (byte) 'a';
        header[38] = (byte) 't';
        header[39] = (byte) 'a';
        header[40] = (byte) (totalAudioLen & 0xffL);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xffL);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xffL);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xffL);
        return header;
    }
}

