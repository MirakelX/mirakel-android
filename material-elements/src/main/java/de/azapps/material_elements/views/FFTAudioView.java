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

package de.azapps.material_elements.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.uol.aig.fftpack.RealDoubleFFT;
import de.azapps.material_elements.R;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.WavUtil;

public class FFTAudioView extends View {

    private static final String TAG = "FFTAudioView";
    private final String path;
    private final OnRecordFinished finishListener;
    private RealDoubleFFT transformer;

    private RecordAudio recordTask;
    private Paint paint;
    private double[] mTransformed;
    private int width;
    private int hight;
    private boolean recording;
    private File tmpFile;
    private final float padding;

    public interface OnRecordFinished {
        public void finishRecording(String path);
    }

    public static class RecordingFailedException extends Exception {
        RecordingFailedException(final Throwable e) {
            super(e);
        }
    }

    public FFTAudioView(final Context ctx, final String outputPath,
                        final OnRecordFinished finish) throws RecordingFailedException {
        super(ctx);

        transformer = new RealDoubleFFT(WavUtil.getBufferSize() / 2);
        paint = new Paint();
        paint.setColor(ThemeManager.getPrimaryThemeColor());
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(ctx.getResources().getDimension(R.dimen.fft_line_width));
        path = outputPath;
        finishListener = finish;
        padding = ctx.getResources().getDimension(R.dimen.fft_padding);
        try {
            tmpFile = File.createTempFile("prefix", "extension", ctx.getCacheDir());
        } catch (final IOException e) {
            Log.w(TAG, "Failed to create tmp file", e);
            throw new RecordingFailedException(e);
        }
    }

    public void startRecording() {
        recordTask = new RecordAudio();
        recordTask.execute();
    }

    public void stopRecording() {
        recording = false;
        finishListener.finishRecording(path);
    }

    public class RecordAudio extends AsyncTask<Void, double[], Void> {

        private void writeRawData() {
            final AudioRecord recorder = WavUtil.getRecorder();
            if (recorder == null) {
                recording = false;
                return;
            }
            recorder.startRecording();
            final byte [] buffer = new byte [WavUtil.getBufferSize()];
            final double [] toTransform = new double[buffer.length / 2];
            try  {
                final BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                while (recording) {
                    final int read = recorder.read(buffer, 0, WavUtil.getBufferSize());
                    if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                        fos.write(buffer);
                    } else {
                        throw new IllegalStateException();
                    }
                    for (int i = 1; i < buffer.length; i += 2) {
                        toTransform[i / 2] = getShort(buffer[i - 1], buffer[i]) / (Short.MAX_VALUE + 1.0);
                    }
                    transformer.ft(toTransform);
                    publishProgress(toTransform);

                }
                recorder.stop();
                recorder.release();
                fos.close();
                generateWavFile();
            } catch (IOException | IllegalStateException e) {
                Log.e(TAG, "Error writing raw data", e);
                recording = false;
            } finally {
                if (recorder.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop();
                    recorder.release();
                }
            }
        }

        private void generateWavFile() {
            final File infile = tmpFile;
            final File outfile = new File(path);
            FileInputStream fis = null;
            FileOutputStream out = null;
            try {
                fis = new FileInputStream(infile);
                out = new FileOutputStream(outfile);
                final long totalAudioLen = fis.getChannel().size();
                out.write(WavUtil.getWavHeader(totalAudioLen));

                final byte [] buffer = new byte[ WavUtil.getBufferSize() ];
                while (fis.read(buffer) != -1) {
                    out.write(buffer);
                }
            } catch (final IOException e) {
                infile.delete();
                outfile.delete();
                Log.e(TAG, "Failed to copy wavefile", e);
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                } catch (final IOException ignored) {
                    //ignore
                }
                infile.delete();
            }
        }

        @Override
        protected Void doInBackground(final Void... params) {
            recording = true;
            writeRawData();
            return null;
        }

        @Override
        protected void onProgressUpdate(final double[]... values) {
            mTransformed = values[0];
            invalidate();
        }

        /*
        *
        * Converts a byte[2] to a short, in LITTLE_ENDIAN format
        *
        */
        private short getShort(final byte argB1, final byte argB2) {
            return (short) (argB1 | (argB2 << 8));
        }


    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        hight = h;
    }


    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (mTransformed != null) {
            final float factor =  ((width - 2 * padding) / (2 * (float)mTransformed.length));
            final float upy = hight / 2;
            int counter = (int) (padding / factor);
            for (int i = mTransformed.length - 1; i >= 0; i--) {
                final float downy = (float) (upy - (mTransformed[i] * 5.0));
                canvas.drawLine(counter * factor, downy, counter * factor, upy, paint);
                counter++;
            }
            for (final double aMTransformed : mTransformed) {
                final float downy = (float) (upy - (aMTransformed * 5.0));
                canvas.drawLine(counter * factor, downy, counter * factor, upy, paint);
                counter++;
            }
        }
    }
}