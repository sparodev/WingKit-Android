package com.sparohealth.wingkit.classes.lungfunctiontest;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 The `TestSessionRecorder` class is used to detect and record when a user blows into the Wing sensor.
 */

public class TestSessionRecorder {
    private static String TAG = "TestSessionRecorder";

    /**
     * Async Task that handles recording the audio sample from the Wing device for processing
     */
    private RecordWaveTask recordWaveTask = null;

    /**
     * Object implementing the TestRecorderDelegate interface for event callback handling
     */
    private TestRecorderDelegate delegate = null;
    /**
     * TestRecorderState Enum storing the current status of the recorder
     */
    private TestRecorderState state = null;
    /**
     * File handle for the test recording
     */
    private File file = null;

    /**
     * The maximum decibel (dB) threshold that the sensor recording strength must not exceed to be considered a valid test.
     */
    private double signalStrengthMaxThreshold = 8.0;
    /**
     * The minimum decibel (dB) threshold that the sensor recording strength must exceed to be considered a valid test.
     */
    private double signalStrengthMinThreshold = 45.0;
    /**
     * Indicates if the minimum signal strength threshold was met during the test
     */
    private boolean minimumSignalStrengthThresholdPassed = false;
    /**
     * Indicates if the maximu signal strength threshold was met during the test
     */
    private boolean maximumSignalStrengthThresholdTestFailed = false;
    /**
     * Indicates that the recording was cancelled due to an error
     */
    private boolean recordingCancelled = false;

    /**
     * Initialize the TestSessionRecorder instance with the parent context and callback object
     * @param context parent context of the calling object
     * @param delegate callback interface object
     */
    public TestSessionRecorder(Context context, TestRecorderDelegate delegate){
        try {
            file = new File(context.getExternalFilesDir(null),"audio.wav");
            this.delegate = delegate;
            setRecorderState(TestRecorderState.ready);
        }catch (Exception ex){
            Log.d("Debug", ex.getMessage());
        }
    }

    /**
     * Return the file path and name for the audio recording
     * @return recorded file name
     */
    public String getFilename() {
        return file.getAbsolutePath();
    }
    /**
     * Start the recording process
     */
    public void startRecording() {
        try {
            // clear the threshold test flags
            minimumSignalStrengthThresholdPassed = false;
            maximumSignalStrengthThresholdTestFailed = false;
            recordingCancelled = false;
            // create a new recording task
            recordWaveTask = new RecordWaveTask(this);
            recordWaveTask.execute(file);
            setRecorderState(TestRecorderState.recording);
        }catch (Exception ex){
            setRecorderState(TestRecorderState.error);
            Log.d(TAG, ex.getMessage() + "\n" + Arrays.toString(ex.getStackTrace()));
        }
    }

    /**
     * Indicate if the recording captured was valid and passed the threshold checks
     * @return valid status
     */
    public boolean isValidRecording() {
        return minimumSignalStrengthThresholdPassed && !maximumSignalStrengthThresholdTestFailed;
    }

    /**
     * Indicate if the recording was cancelled
     * @return cancelled status
     */
    public boolean isCancelled() {
        return recordingCancelled;
    }
    /**
     * Interrupt and cancel the recording process
     */
    public void stopRecording(){
        recordingCancelled = true;
        if (!recordWaveTask.isCancelled() && recordWaveTask.getStatus() == AsyncTask.Status.RUNNING) {
            recordWaveTask.cancel(true);
        }
    }

    /**
     * Set the current state of the recorder
     * @param newState new state of the recorder
     */
    void setRecorderState(TestRecorderState newState){
        state = newState;
        delegate.recorderStateChanged(state);
    }

    /**
     * Return the current status of the recorder
     * @return current recorder state
     */
    public TestRecorderState getRecorderState() {
        return state;
    }

    /**
     * Async task implementation that handles recording and checking the audio test data
     */
    private static class RecordWaveTask extends AsyncTask<File, Void, Object[]> {
        /**
         * Source for recording the audio test data
         */
        private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
        /**
         * Sample rate for the recording in Hz
         */
        private static final int SAMPLE_RATE = 44100; // Hz
        /**
         * Bit depth encoding
         */
        private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        /**
         * Number of audio channels to use when recording
         */
        private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
        /**
         * Size of the buffer to use when recording the test audio
         */
        private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
        /**
         * Duration to record for
         */
        private static final int RECORDING_DURATION = 6000;
        /**
         * Parent object
         */
        private TestSessionRecorder context;

        /**
         * Initialize the recording task
         * @param context parent object calling the task
         */
        private RecordWaveTask(TestSessionRecorder context) {
            this.context = context;
        }

        /**
         * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
         * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
         * the WAV header to include the proper final chunk sizes.
         *
         * @param files Index 0 should be the file to write to
         * @return Either an Exception (Error) or two longs, the filesize, elapsed time in ms (success)
         */
        @Override
        protected Object[] doInBackground(File... files) {
            AudioRecord audioRecord = null;
            FileOutputStream wavOut = null;
            long startTime = 0;
            long endTime = 0;

            try {
                // create and start the audio recorder
                audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
                audioRecord.startRecording();

                // open the file output stream to write audio data coming from the AudioRecord instance
                wavOut = new FileOutputStream(files[0]);

                // initialize the WAV file with the basic header information
                writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

                // allocate the recording buffer and set up loop variables
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean run = true;
                int read;
                long total = 0;

                startTime = SystemClock.elapsedRealtime();

                // While the run flag IS true AND the task has NOT been cancelled
                while (run && !isCancelled()) {
                    // read from the microphone
                    read = audioRecord.read(buffer, 0, buffer.length);

                    // if there IS data from the read above
                    if (read > 0) {
                        // write the read buffer to the output file
                        wavOut.write(buffer, 0, read);
                        double sum = 0;

                        try {
                            // allocate an array for storing the converted recording buffer for signal strength checks
                            short[] samples = new short[read / 2];
                            // convert the recording buffer and store it in the array create above
                            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

                            // loop through the converted buffer and calculate the sum of the signals
                            for (short sample : samples) {
                                sum += Math.abs(sample);
                            }

                            // average the signal strength over the size of the recording buffer to find the amplitude
                            int rawAmplitude = (int)sum / read;

                            // convert the amplitude to the decibel (dB) value
                            double amplitudeDb = (20 * Math.log10(rawAmplitude / 32767f));

                            // if the calculated value IS less than the minimum threshold...
                            if (Math.abs(amplitudeDb) < context.signalStrengthMinThreshold) {
                                context.minimumSignalStrengthThresholdPassed = true;
                            }

                            // if the calculated value IS less than the maximum threshold...
                            if (Math.abs(amplitudeDb) < context.signalStrengthMaxThreshold) {
                                context.maximumSignalStrengthThresholdTestFailed = true;
                            }

                            Log.d(TAG, "Calculated Amplitude " + String.valueOf(amplitudeDb));

                            // if the callback delegate IS set...
                            if (context.delegate != null) {
                                context.delegate.signalStrengthChanged(amplitudeDb);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    total += read;

                    // if the elapsed recording time IS greater than the maximum duration...
                    if (SystemClock.elapsedRealtime() - startTime >= RECORDING_DURATION) {
                        run = false;
                    }
                }
                Log.d(TAG, "Total bytes recorded - " + String.valueOf(total));
            } catch (IOException ex) {
                return new Object[]{ex};
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            finally {
                // if the audiorecord object IS set...
                if (audioRecord != null) {
                    try {
                        // if the audio recorder IS recording...
                        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord.stop();
                            endTime = SystemClock.elapsedRealtime();
                        }
                    } catch (IllegalStateException ex) {
                        //
                    }
                    // if the audio recorder IS initialized...
                    if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        audioRecord.release();
                    }
                }
                // if the file output stream IS set...
                if (wavOut != null) {
                    try {
                        // close the file
                        wavOut.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }

            try {
                // perform the final update to the WAV file header data
                updateWavHeader(files[0]);
            } catch (IOException ex) {
                return new Object[] { ex };
            }

            context.setRecorderState(TestRecorderState.finished);
            return new Object[] { files[0].length(), endTime - startTime };
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out         The stream to write the header to
         * @param channelMask An AudioFormat.CHANNEL_* mask
         * @param sampleRate  The sample rate in hertz
         * @param encoding    An AudioFormat.ENCODING_PCM_* value
         * @throws IOException An exception occurring during the file access
         */
        private static void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
            short channels;
            switch (channelMask) {
                case AudioFormat.CHANNEL_IN_MONO:
                    channels = 1;
                    break;
                case AudioFormat.CHANNEL_IN_STEREO:
                    channels = 2;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable channel mask");
            }

            short bitDepth;
            switch (encoding) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    bitDepth = 8;
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    bitDepth = 16;
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    bitDepth = 32;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable encoding");
            }

            writeWavHeader(out, channels, sampleRate, bitDepth);
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out        The stream to write the header to
         * @param channels   The number of channels
         * @param sampleRate The sample rate in hertz
         * @param bitDepth   The bit depth
         * @throws IOException An exception occurring during the file access
         */
        private static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
            // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
            byte[] littleBytes = ByteBuffer
                    .allocate(14)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(channels)
                    .putInt(sampleRate)
                    .putInt(sampleRate * channels * (bitDepth / 8))
                    .putShort((short) (channels * (bitDepth / 8)))
                    .putShort(bitDepth)
                    .array();

            // Not necessarily the best, but it's very easy to visualize this way
            out.write(new byte[]{
                    // RIFF header
                    'R', 'I', 'F', 'F', // ChunkID
                    0, 0, 0, 0, // ChunkSize (must be updated later)
                    'W', 'A', 'V', 'E', // Format
                    // fmt subchunk
                    'f', 'm', 't', ' ', // Subchunk1ID
                    16, 0, 0, 0, // Subchunk1Size
                    1, 0, // AudioFormat
                    littleBytes[0], littleBytes[1], // NumChannels
                    littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                    littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                    littleBytes[10], littleBytes[11], // BlockAlign
                    littleBytes[12], littleBytes[13], // BitsPerSample
                    // data subchunk
                    'd', 'a', 't', 'a', // Subchunk2ID
                    0, 0, 0, 0, // Subchunk2Size (must be updated later)
            });
        }

        /**
         * Updates the given wav file's header to include the final chunk sizes
         *
         * @param wav The wav file to update
         * @throws IOException An exception occurring during the file access
         */
        private static void updateWavHeader(File wav) throws IOException {
            byte[] sizes = ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    //Cast should be safe since if the WAV is
                    // > 4 GB we've already made a terrible mistake.
                    .putInt((int) (wav.length() - 8)) // ChunkSize
                    .putInt((int) (wav.length() - 44)) // Subchunk2Size
                    .array();

            RandomAccessFile accessWave = null;
            //noinspection CaughtExceptionImmediatelyRethrown
            try {
                accessWave = new RandomAccessFile(wav, "rw");
                // ChunkSize
                accessWave.seek(4);
                accessWave.write(sizes, 0, 4);

                // Subchunk2Size
                accessWave.seek(40);
                accessWave.write(sizes, 4, 4);
            } catch (IOException ex) {
                // Rethrow but still close accessWave in the finally block
                throw ex;
            } finally {
                if (accessWave != null) {
                    try {
                        accessWave.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }
        }

        @Override
        protected void onCancelled(Object[] results) {
            Log.d(TAG, "The Recorder task has been cancelled!");
            // Handling cancellations and successful runs in the same way
            onPostExecute(results);
        }

        @Override
        protected void onPostExecute(Object[] results) {
            if (results[0] instanceof Throwable) {
                // Error
                Throwable throwable = (Throwable) results[0];
                Log.e(RecordWaveTask.class.getSimpleName(), throwable.getMessage(), throwable);
            }

        }
    }


    public interface TestRecorderDelegate {
        void recorderStateChanged(TestRecorderState state);
        void signalStrengthChanged(Double strength);
    }

    public enum TestRecorderState {
        // Indicates the recorder could not be configured and an error occurred.
        error,

        // Indicates the recorders have been configured and are ready to start recording.
        ready,

        // Indicates that the recording is currently in progress.
        recording,

        // Indicates that the recording has concluded.
        finished
    }
}
