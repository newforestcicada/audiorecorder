package info.newforestcicada.audiorecorder.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class echoes a string called from JavaScript.
 */
public class RecorderPlugin extends CordovaPlugin {

	private static final int REC_SECONDS = 30;
	private static final int UPDATE_RATE = 100;
	/* True if you want to save to file exactly `n` seconds of recording even if
	 * the buffer contains only `m < n` */
	public static final boolean STORE_EMPTY_PART_OF_BUFFER = true; 
	private AudioRecord mRecorder;
	private int tBufferSize;
	private int bufferIndex;
	private int maxIndex = 0;
	private short[] mRecordBuffer;
	private double[] mHeterodyneBuffer;
	private int hetIndex;

	private int mSampleRate;

	private static final String TAG = RecorderPlugin.class.getSimpleName();
	public static final String SUBDIR = "AudioRecorder";

	private boolean mIsRecording = false;
	private boolean mIsPlaying = false;
	private boolean mIsSurveying = false;

	private AudioAnalyser mAnalyser;

	private LowPassFilter mLowPassFilter;
	private AudioTrack mAudioPlayer;
	private Timer mSurveyTimer;
	private Heterodyne mHeterodyne;

	public static final SimpleDateFormat df = new SimpleDateFormat(
			"yyyyMMdd'T'HHmmss", Locale.UK);

	public static final int mChannelCount = 1;
	private int sampleRate = 44100;
	private int mHeterodyneFrequency = 15000;

	private Spectrogram mSpectrogram;


	/**
	 * The only method ever to be called from the javascript interface.
	 * 
	 * The call will be in the following format:
	 * 
	 * <pre>
	 * exec(&lt;successFunction&gt;, &lt;failFunction&gt;, &lt;service&gt;, &lt;action&gt;, [&lt;args&gt;]);
	 * </pre>
	 * 
	 * where <code>&lt;service&gt;</code> will be the name of this class and
	 * <code>&lt;action&gt;</code> one the private methods below.
	 */
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {

		if ("initialiseAudioRecorder".equals(action)) {
			this.initialiseAudioRecorder(callbackContext);
			return true;
		}
		if ("startAudioRecorder".equals(action)) {
			this.startDetector(callbackContext);
			return true;
		}
		if ("stopAudioRecorder".equals(action)) {
			this.stopDetector(callbackContext);
			callbackContext.success();
			return true;
		}
		
		
		
		
		if ("getFrequencies".equals(action)) {
			callbackContext.success(new JSONArray(getFrequencies()));
			return true;
		}

		if ("getFrequencyColours".equals(action)) {
			callbackContext.success(new JSONArray(getFrequencieColours()));
			return true;
		}
		if ("getScaledFrequencies".equals(action)) {
			callbackContext.success(new JSONArray(getFrequencies()));
			return true;
		}

		if ("getScaledAmplitude".equals(action)) {
			callbackContext.success(String.valueOf(getAmplitude()));
			return true;
		}
		if ("clearBuffers".equals(action)) {
			Log.i(TAG, "clearBuffers not implemented");
			return true;
		}

		if ("startWhiteNoise".equals(action)) {
			this.startWhiteNoise(callbackContext);
			return true;
		}
		if ("stopWhiteNoise".equals(action)) {
			this.stopWhiteNoise(callbackContext);
			return true;
		}
		if ("captureRecording".equals(action)) {

			this.captureRecording(callbackContext);
			return true;
		}
		if ("writeRecording".equals(action)) {
			this.writeRecording(callbackContext, args.getInt(0));
			return true;
		}

		if ("writeSonogram".equals(action)) {
			this.writeSpectrogram(args.getInt(0), args.getInt(1), args.getInt(2), callbackContext);
			return true;
		}

		if ("setHeterodyneFrequency".equals(action)) {
			setHeterodyneFrequency(callbackContext, args.getInt(0));
			return true;
		}

		if ("startHeterodyne".equals(action)) {
			startHeterodyne(callbackContext);
			return true;
		}
		if ("stopHeterodyne".equals(action)) {
			stopHeterodyne(callbackContext);
			return true;
		}
		if ("activateGPS".equals(action)) {
			activateGPS();
			callbackContext.success();
			return true;
		}


		Log.e(TAG, "Calling unknown action " + action);
		return false; // Returning false results in a "MethodNotFound" error.
	}

	private void writeSpectrogram(int width, int height, int recLength, CallbackContext callbackContext) {
		Log.i("TAG", "Sonogram:: width: " + width + ", height: " + height + " recLength: " + recLength);

		//stopSurvey(width, height, recLength);

		/** now create the bitmap file */
		//String[] sonogram_created = mAnalyser.createSonogram(width, height, recLength);
		String[] sonogram_created = mSpectrogram.write(width, height, recLength);
		String fullpath = sonogram_created[0];
		String b64spectrogram = sonogram_created[1];

		callbackContext.success(b64spectrogram);
	}

	/**
	 * Initialise the audio system.
	 * 
	 * This should be called before any other call to the audio system is made,
	 * including detecting the cicada or requesting an amplitude value.
	 */
	private void initialiseAudioRecorder(CallbackContext callbackContext) {
		Log.i("initialiseAudioRecorder", "Recorder initialised");
		startDetector(callbackContext);
		
		//callbackContext.success();
	}

	public void keepScreenOn(){
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				Log.d(TAG, "Screen will be kept on. KeepScreenOn");
			}
		});
	}

	public void clearScreenOn() {
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				Log.d(TAG, "Screen is not allowed to sleep.");
			}
		});
	}
	
	/**
	 * Start detector
	 */
	private void startDetector(CallbackContext callbackContext) {

		keepScreenOn();

		if (mIsRecording) {
			Log.i(TAG, "Detector already initialised");
		} else {
			mLowPassFilter = new LowPassFilter((float) 1.404746361e+03,
					(float) 0.9985762554);
			mRecorder = setupRecorder();
			mRecorder.startRecording();

			mAnalyser = new AudioAnalyser(this.cordova.getActivity()
					.getApplicationContext());

			mHeterodyne = new Heterodyne(mHeterodyneFrequency, sampleRate);
			

			Log.i(TAG, "Recorder started");

			mIsRecording = true;
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {

					short[] tBuffer = new short[tBufferSize];

					while (mIsRecording) {
						int numRead = mRecorder.read(tBuffer, 0, tBufferSize);

						for (int i = 0; i < numRead; i++) {
							short sample = tBuffer[i];
							mAnalyser.updateWithSoundInputValue((float) sample);

							mLowPassFilter.update((float) sample);

							mRecordBuffer[bufferIndex] = sample;
							bufferIndex = (bufferIndex + 1) % mRecordBuffer.length;
							maxIndex = Math.min(++maxIndex, mRecordBuffer.length);

							mHeterodyneBuffer[hetIndex] = mHeterodyne.updateWithSample(sample);
							hetIndex = (hetIndex + 1) % mHeterodyneBuffer.length;
						}
					}
				}
			});


			startSurvey();

			callbackContext.success();
		}


	}
	

	/**
	 * Gracefully stop and destroy the audio recording system.
	 * 
	 * A call to {@link #startDetector(CallbackContext)} is sufficient to restart the process.
	 */
	private void stopDetector(CallbackContext callbackContext) {

		mIsRecording = false;
		try {

			stopSurvey();

			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;

			mAnalyser = null;
			Log.i(TAG, "detector stopped");
		} catch (NullPointerException e) {
			Log.i(TAG, "detector already stopped");
		}

		clearScreenOn();
		callbackContext.success();
	}

	
	/** Activate GPS */
	private void activateGPS(){

		final LocationManager manager = (LocationManager) cordova.getActivity()
				.getSystemService(Context.LOCATION_SERVICE);

		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					cordova.getActivity());
			builder.setMessage(
					"Your GPS seems to be disabled, do you want to enable it?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									cordova.getActivity()
											.startActivity(
													new Intent(
															android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			final AlertDialog alert = builder.create();
			alert.show();

		}
	}


	/**
	 * Get a value of the amplitude from the microphone.
	 * 
	 * @return a single floating point value between 0 and 1.
	 */
	private float getAmplitude() {

		try {
			float val = mLowPassFilter.getValue() / 2000;

			if (val > 1.0) {
				val = 1;
			}

			return val;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (float) 0;

	}

	/**
	 * Get array of frequency magnitudes, one per frequency bin.
	 * 
	 * The number of frequency bins will be proportional to the sampling
	 * frequency, but would normally be 20, representing frequencies between 1
	 * and 20 kHz.
	 * 
	 * @return double array
	 */
	private ArrayList<Float> getFrequencies() {
		try {
			/*
			 * short[] tBuffer = new short[tBufferSize]; mRecorder.read(tBuffer,
			 * 0, tBufferSize); for (float sample : tBuffer) {
			 * mAnalyser.updateWithSoundInputValue(sample); }
			 */
			ArrayList<Float> freqs = Spectrogram.getFrequencies(mAnalyser.getGoertzels());
			return freqs;
		} catch (NullPointerException e) {
			// Log.e(TAG, "CD::getFrequencies NPEX"+e.toString());
			e.printStackTrace();
			//ArrayList<Float> freqs = new ArrayList<Float>(20);
			return null;

		}
	}

	private ArrayList<String> getFrequencieColours() {

		return Spectrogram.getFrequencyColours(mAnalyser.getGoertzels());
	}

	/**
	 * Set up the recorder.
	 * 
	 * This function is only called internally and should be ignored for the
	 * purposes of the API.
	 * 
	 * @return an AudioRecord instance
	 */
	private AudioRecord setupRecorder() {

		int channelConfig = AudioFormat.CHANNEL_IN_MONO;
		int encoding = AudioFormat.ENCODING_PCM_16BIT;
		int minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
				channelConfig, encoding);
		if (minBufferSize != AudioRecord.ERROR_BAD_VALUE) {

			AudioRecord record = new AudioRecord(AudioSource.MIC, sampleRate,
					channelConfig, encoding, minBufferSize);
			if (record.getState() == AudioRecord.STATE_INITIALIZED) {
				this.bufferIndex = 0;
				this.tBufferSize = minBufferSize;
				this.tBufferSize = 128;
				//this.mAudioFormat = encoding;
				mSampleRate = sampleRate;

				Log.i(TAG, "Buffer size: " + this.tBufferSize);

				mRecordBuffer = new short[sampleRate * REC_SECONDS];
				mHeterodyneBuffer = new double[sampleRate * 1]; // 1 sec

				return record;
			}
		}
		Log.e(TAG, "Cannot record with sample rate = " + sampleRate);
		return null;
	}


	/**
	 * Emit white noise from the default output device;
	 */
	private void startWhiteNoise(CallbackContext callbackContext) {

		final int duration = 1; // seconds
		final int sampleRate = 44100;
		final int numSamples = duration * sampleRate;

		mAudioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				numSamples, AudioTrack.MODE_STREAM);

		mIsPlaying = true;
		cordova.getThreadPool().execute(new Runnable() {

			public void run() {
				while (mIsPlaying) {

					byte generatedSnd[] = new byte[2 * numSamples];

					// short[] noise = new short[bufferSize];
					double[] noise = new double[numSamples];

					for (int i = 0; i < numSamples; i++) {
						// noise[i] = (short) (2 * (Math.random() - 0.5));
						noise[i] = (2 * (float) Math.random() - 0.5);
					}

					// convert to 16 bit pcm sound array
					// assumes the sample buffer is normalised.
					int idx = 0;
					for (final double dVal : noise) {
						// scale to maximum amplitude
						final short val = (short) ((dVal * 32767));
						// in 16 bit wav PCM, first byte is the low order byte
						generatedSnd[idx++] = (byte) (val & 0x00ff);
						generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

					}
					mAudioPlayer.write(generatedSnd, 0, generatedSnd.length);
					Log.d(TAG, "playing");
				}
			}
		});

		try {
			mAudioPlayer.play();
			callbackContext.success();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stop emitting white noise.
	 * 
	 * A call to {@link #startWhiteNoise} is sufficient to restart the noise.
	 */
	private void stopWhiteNoise(CallbackContext callbackContext) {
		try {
			mIsPlaying = false;
			mAudioPlayer.stop();
			mAudioPlayer.release();
			mAudioPlayer = null;
		} catch (NullPointerException e) {
			;
		}
		callbackContext.success();
	}


	private void startHeterodyne(CallbackContext callbackContext) {

		final int numSamples = mHeterodyneBuffer.length;

		mAudioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				numSamples, AudioTrack.MODE_STREAM);

		mIsPlaying = true;
		cordova.getThreadPool().execute(new Runnable() {

			public void run() {

				while (mIsPlaying) {

					double[] heterodyneBuffer;
					int tempHetIndex;

					synchronized (mHeterodyneBuffer) {
						heterodyneBuffer = mHeterodyneBuffer.clone();
						tempHetIndex = hetIndex;
					}

					byte generatedSnd[] = new byte[2 * heterodyneBuffer.length];


					int idx = 0;
					double dVal;
					short val;
					int ivals;
					short[] vals = new short[heterodyneBuffer.length];
					long[] lvals = new long[heterodyneBuffer.length];

					//for (final float dVal : mHeterodyneBuffer) {
					for (int i = tempHetIndex; i < heterodyneBuffer.length; i++) {
						// scale to maximum amplitude
						//dVal = heterodyneBuffer[tempHetIndex];
						val = (short) (heterodyneBuffer[i] * Short.MAX_VALUE);
						// in 16 bit wav PCM, first byte is the low order byte
						generatedSnd[idx++] = (byte) (val & 0x00ff);
						generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
					}

					for (int i = 0; i < tempHetIndex; i++) {

						// scale to maximum amplitude
						val = (short) (heterodyneBuffer[i] * Short.MAX_VALUE);

						// in 16 bit wav PCM, first byte is the low order byte
						generatedSnd[idx++] = (byte) (val & 0x00ff);
						generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
					}

					mAudioPlayer.write(generatedSnd, 0, generatedSnd.length);


				}
			}
		});

		try {
			mAudioPlayer.play();
			callbackContext.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void stopHeterodyne(CallbackContext callbackContext) {
		try {
			mIsPlaying = false;
			mAudioPlayer.stop();
			mAudioPlayer.release();
			mAudioPlayer = null;
		} catch (NullPointerException e) {
			;
		}
		callbackContext.success();
	}


	public File getWaveFile() throws IOException {

		File waveDir = new File(Environment.getExternalStorageDirectory(), SUBDIR);
		File wavFile = new File(waveDir, df.format(new Date()).concat(".wav"));

		boolean success = false;
		if (!waveDir.exists()) {
			success = waveDir.mkdir();

			if (!success)
				throw new IOException("Could not create "+ waveDir.getName()+" directory.");
		}
		return wavFile;
	}
	/**
	 * Write the current buffer to file.
	 * 
	 * The filename is currently determined internally
	 * 
	 * @return the path to the file written.
	 */
	private void writeRecording(CallbackContext callbackContext, int recLength) {

		int bitsPerSample = 16;

		try {

			File waveFile = getWaveFile();
			RandomAccessFile ras = new RandomAccessFile(waveFile, "rw");

			/*
			 * Wave file format based on specification described here:
			 * http://www
			 * -mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html Uses
			 * basic chunk format (chunk size 16).
			 */
			// RIFF chunk
			ras.writeBytes("RIFF");
			ras.writeInt(Integer
					.reverseBytes((this.mRecordBuffer.length * 2) + 36));
			ras.writeBytes("WAVE");

			// Format chunk
			ras.writeBytes("fmt "); // Chunk id
			ras.writeInt(Integer.reverseBytes(16)); // Chunk size
			ras.writeShort(Short.reverseBytes((short) 1)); // Format code (PCM)
			ras.writeShort(Short.reverseBytes((short) mChannelCount)); // Number
			// of
			// channels
			ras.writeInt(Integer.reverseBytes(mSampleRate)); // Sampling
			// rate
			ras.writeInt(Integer.reverseBytes(mSampleRate * mChannelCount
					* bitsPerSample / 8)); // Data
			// rate,
			// SampleRate*NumberOfChannels*BitsPerSample/8
			ras.writeShort(Short.reverseBytes((short) (mChannelCount
					* bitsPerSample / 8))); // Block align,
			// NumberOfChannels*BitsPerSample/8
			ras.writeShort(Short.reverseBytes((short) bitsPerSample)); // Bits
			// per
			// sample

			// Data chunk
			ras.writeBytes("data");
			ras.writeInt(Integer.reverseBytes(this.mRecordBuffer.length * 2));

			// Write data
			byte[] outBuffer;
			short[] tempBuffer;
			int tempBufferIndex; // the point up to which the buffer has been filled
			int tempMaxIndex;    // the point up to which the buffer has been filled
			                     // or the length of the buffer, whichever is smallest

			synchronized (mRecordBuffer) {

				tempBufferIndex = this.bufferIndex;
				tempMaxIndex = this.maxIndex;

				tempBuffer = new short[this.mRecordBuffer.length];
				outBuffer = new byte[tempMaxIndex * 2];

				for (int i = 0; i < mRecordBuffer.length; i++) {
					tempBuffer[i] = mRecordBuffer[i];
				}
			}

			int g = 0;
			int endBuffer;

			Log.i(TAG, "tempBufferIndex: " + tempBufferIndex + ", "
					+ "tempMaxIndex: " + tempMaxIndex + ", "
					+ "outBuffer.length: " + outBuffer.length + ", "
					+ "mRecordBuffer.length: " + mRecordBuffer.length);
			
			if (tempMaxIndex == mRecordBuffer.length) { // we have recorded at least as much as the buffer
				for (int i = tempBufferIndex; i < tempBuffer.length; i++) {
					outBuffer[g] = (byte) (tempBuffer[i] & 0xff);
					outBuffer[g + 1] = (byte) ((tempBuffer[i] >> 8) & 0xff);
					g += 2;
				}
				endBuffer = tempBufferIndex;
			} else if (tempMaxIndex < mRecordBuffer.length) {
				endBuffer = tempMaxIndex;
			} else {
				endBuffer = 0;
				Log.e(TAG,
						"Max index cannot be greater than record buffer length.");
				System.exit(1);
			}

			for (int i = 0; i < endBuffer; i++) {
				outBuffer[g] = (byte) (tempBuffer[i] & 0xff);
				outBuffer[g + 1] = (byte) ((tempBuffer[i] >> 8) & 0xff);
				g += 2;
			}

			/** keep only part of the buffer, as specified by parameter `recLenght` */
			if (recLength < mRecordBuffer.length / mSampleRate) {
				byte[] cutBuffer = new byte[recLength * 2 * mSampleRate];
				for (int i = 0; i < cutBuffer.length; i++) {
					cutBuffer[i] = outBuffer[i];
				}

				ras.write(cutBuffer);
			} else {
				ras.write(outBuffer);
			}
			ras.close();

			Log.i(TAG, "written to "+waveFile.getAbsolutePath());

			callbackContext.success(waveFile.getAbsolutePath());

		} catch (IOException e) {

			Log.e(TAG, e.getMessage());
			callbackContext.error(""); // empty path to file

		} catch (Exception e) {

			e.printStackTrace();
			callbackContext.error(""); // empty path to file

		}
	}

	private boolean startSurvey() {

		if (mIsSurveying) {
			return false;
		} else {
			mIsSurveying = true;
			//mAnalyser.initSonogram(UPDATE_RATE, 30);
			mSpectrogram = new Spectrogram(UPDATE_RATE, 30);

			Log.i("TAG", "Survey started");
			mSurveyTimer = new Timer();
			mSurveyTimer.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {

					try {
						// mAnalyser.updateInsectLongTimeResults();
						// mAnalyser.updateLongTermResult();
						//mAnalyser.updateSonogram();

						mSpectrogram.update(mAnalyser.getGoertzels());
					} catch (NullPointerException e){
						Log.e(TAG, "Null pointer on sonogram. This should never occur.");

					}
				}
			}, 0, UPDATE_RATE);

			return true;
		}
	}

	private void stopSurvey() {

		if (mIsSurveying) {
			Log.i("STOP", "survey stopped");
			mIsSurveying = false;

			/** stop the survey thread */
			mSurveyTimer.cancel();

			mSurveyTimer = null;

		} else {

			Log.i("STOP", "not currently surveying");

		}

	}

	private void captureRecording(CallbackContext callbackContext) {
		Log.i(TAG, "captureRecording called");
		callbackContext.success(df.format(new Date())+".png");
	}

	private void setHeterodyneFrequency(CallbackContext callbackContext, int freq) {
		mHeterodyneFrequency = freq;
		callbackContext.success();
	}

	private class LowPassFilter {
		float xv0, xv1, yv0, yv1, gain, ratio = 0;

		public LowPassFilter(float gain, float ratio) {
			this.gain = gain;
			this.ratio = ratio;
		}

		public void update(float sample) {
			xv0 = xv1;
			xv1 = (float) Math.abs(sample) / this.gain;
			yv0 = yv1;
			yv1 = xv0 + xv1 + (this.ratio * yv0);
		}

		public float getValue() {
			return this.yv1;
		}
	}

}

