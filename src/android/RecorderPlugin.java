package info.newforestcicada.audiorecorder.plugin;

//import AudioAnalyser;
//import info.newforestcicada.audiorecorder.plugin.GetTask;
//import info.newforestcicada.audiorecorder.plugin.RecorderPlugin.LowPassFilter;
//import info.newforestcicada.audiorecorder.plugin.Hmm.HmmResult;
//import info.newforestcicada.audiorecorder.plugin.insects.Cicada;
//import info.newforestcicada.audiorecorder.plugin.insects.Insect;

import info.newforestcicada.plugin.insects.Insect;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

/**
 * This class echoes a string called from JavaScript.
 */
public class RecorderPlugin extends CordovaPlugin {

	private AudioRecord mRecorder;
	private int tBufferSize;
	private int bufferIndex;
	private int maxIndex = 0;
	//private int mAudioFormat;
	private int mSampleRate;

	private static final String TAG = "CicadaDetector";

	private boolean mIsRecording = false;
	private boolean mIsPlaying = false;
	private boolean mIsSurveying = false;

	private AudioAnalyser mAnalyser;
	private short[] mRecordBuffer = null;
	private LowPassFilter mLowPassFilter;
	private AudioTrack mAudioPlayer;
	private Timer mSurveyTimer;

	public static final SimpleDateFormat df = new SimpleDateFormat(
			"yyyyMMdd'T'HHmmss", Locale.UK);

	public static final int mChannelCount = 1;

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

		if ("initialiseAudioDetector".equals(action)) {
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
		if ("getInsects".equals(action)) {
			this.getInsects(callbackContext);
		}
		if ("getAmplitude".equals(action)) {
			callbackContext.success(String.valueOf(getAmplitude()));
			return true;
		}
		if ("getCicada".equals(action)) {
			callbackContext.success(String.valueOf(getCicada()));
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
		if ("writeRecording".equals(action)) {
			this.writeRecording(callbackContext, args.getInt(0));
		}
//		if ("startSurvey".equals(action)) {
//			this.startSurvey(callbackContext);
//		}
//		if ("stopSurvey".equals(action)) {
//			Log.e("STOP", "Trying to stop survey, length " + args.length());
//			int width;
//			int height;
//			if (args.length() <= 1) {
//				width = 300;
//				height = 200;
//			} else {
//				width = args.getInt(0);
//				height = args.getInt(1);
//			}
//			this.stopSurvey(callbackContext, width, height);
//		}
		return false; // Returning false results in a "MethodNotFound" error.
	}

	/**
	 * Initialise the audio system.
	 * 
	 * This should be called before any other call to the audio system is made,
	 * including detecting the cicada or requesting an amplitude value.
	 */
	private void initialiseAudioRecorder(CallbackContext callbackContext) {
		Log.i("initialiseAudioRecorder", "Recorder initialised");
		
		callbackContext.success();
	}
	
	/**
	 * Start detector
	 */
	private void startDetector(CallbackContext callbackContext) {

		mLowPassFilter = new LowPassFilter((float) 1.404746361e+03,
				(float) 0.9985762554);
		mRecorder = setupRecorder();
		mRecorder.startRecording();

//		mAnalyser = new AudioAnalyser(this.cordova.getActivity()
//				.getApplicationContext());
		Log.i(TAG, "Recorder started");

		mIsRecording = true;
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {

				short[] tBuffer = new short[tBufferSize];

				while (mIsRecording) {
					int numRead = mRecorder.read(tBuffer, 0, tBufferSize);

					for (int i = 0; i < numRead; i++) {
						short sample = tBuffer[i];
//						mAnalyser.updateWithSoundInputValue((float) sample);
//						mLowPassFilter.update((float) sample);
						mRecordBuffer[bufferIndex] = sample;
						bufferIndex = (bufferIndex + 1) % mRecordBuffer.length;
						maxIndex = Math.min(++maxIndex, mRecordBuffer.length);
					}
				}
			}
		});
		callbackContext.success();
	}
	

	/**
	 * Gracefully stop and destroy the audio recording system.
	 * 
	 * A call to {@link #startDetector()} is sufficient to restart the process.
	 */
	private void stopDetector(CallbackContext callbackContext) {

		mIsRecording = false;
		try {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;

			// mAnalyser = null;
			Log.i(TAG, "detector stopped");
		} catch (NullPointerException e) {
			Log.i(TAG, "detector already stopped");
		}
		callbackContext.success();
	}

//	/**
//	 * Initialise the audio system.
//	 * 
//	 * This should be called before any other call to the audio system is made,
//	 * including detecting the cicada or requesting an amplitude value.
//	 */
//	public void initialiseDetector(CallbackContext callbackContext) {
//		String phoneModel = android.os.Build.MODEL;
//		Log.i(TAG, "Model: " + phoneModel);
//		Log.i(TAG, "recorder initialised");
//		activateGPS();
//		getEmissionsUpdate();
//		callbackContext.success();
//	}
		
	private void getEmissionsUpdate(){
		
		new GetTask(this.cordova.getActivity()
				.getApplicationContext()).execute();
		
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
			float val = mLowPassFilter.getValue() / 20000;
			if (val > 1.0) {
				val = 1;
			}
			return val;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (float) 0;
		/*
		 * short[] tBuffer = new short[tBufferSize]; mRecorder.read(tBuffer, 0,
		 * tBufferSize); double sum = 0; double max = 0; for (short s : tBuffer)
		 * { sum += Math.abs(s); if (s > max) { max = s; } } double ampl = sum /
		 * tBuffer.length; sum /= 1e7; if (sum > 1.0) { sum = 1.0; } return sum;
		 */

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
			ArrayList<Float> freqs = mAnalyser.getFrequencies();
			return freqs;
		} catch (NullPointerException e) {
			// Log.e(TAG, "CD::getFrequencies NPEX"+e.toString());
			e.printStackTrace();
			//ArrayList<Float> freqs = new ArrayList<Float>(20);
			return null;

		}
	}

	@Deprecated
	/**
	 * Get the estimate of the presence of the cicada, in a float value between
	 * 0 and 1.
	 * 
	 * @return the estimated value
	 */
	private double getCicada() {
		try {

			HashMap<Insect, Float> insects = mAnalyser.getInsectsEstimates();
			for (Entry<Insect, Float> entry : insects.entrySet()) {
				if (entry.getKey().getId() == 0) {
					return entry.getValue();
				}
			}
		} catch (NullPointerException e) {
			// Log.d(TAG, "0");
			return 0;
		}
		return 0;
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
		int sampleRate = 44100;
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
				this.mSampleRate = sampleRate;

				Log.i(TAG, "Buffer size: " + this.tBufferSize);

				int REC_SECONDS = 30;
				this.mRecordBuffer = new short[sampleRate * REC_SECONDS];

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
	 * A call to {@link #startWhiteNoise()} is sufficient to restart the noise.
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

	private void getInsects(CallbackContext callbackContext) {
		try {
			HashMap<Insect, Float> insects = mAnalyser.getInsectsEstimates();
			JSONArray json_insects = new JSONArray();
			for (Entry<Insect, Float> entry : insects.entrySet()) {
				JSONObject insect = entry.getKey().toJSON();
				insect.put("value", entry.getValue());
				json_insects.put(insect);
			}
			// Log.d(TAG, ""+cicada);
			callbackContext.success(json_insects);
		} catch (NullPointerException ex1) {
			// Log.d(TAG, "0");
			ex1.printStackTrace();
			callbackContext.success(new JSONObject()); // empty
		} catch (Exception ex2) {
			ex2.printStackTrace();
			callbackContext.error("Unknown Error");
		}
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

		File waveFile = null;
		// ByteArrayOutputStream baos;
		RandomAccessFile ras;
		try {
			File waveDir = new File(Environment.getExternalStorageDirectory(),
					"CicadaHunt");
			// waveFile = new File(waveDir, t.format2445().concat(".wav"));
			waveFile = new File(waveDir, df.format(new Date()).concat(".wav"));

			boolean success = false;
			if (!waveDir.exists()) {
				success = waveDir.mkdir();

				if (!success)
					callbackContext.error(""); // empty string is agreed for
												// error message
			}
			ras = new RandomAccessFile(waveFile, "rw");

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
			int tempBufferIndex;
			int tempMaxIndex;

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
			if (tempMaxIndex == mRecordBuffer.length) {
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

			/** keep only part of the buffer */
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

		} catch (Exception e) {
			e.printStackTrace();
			callbackContext.error(""); // empty path to file
		}
		callbackContext.success(waveFile.getAbsolutePath());
	}

	private void startSurvey(CallbackContext callbackContext) {

		if (mIsSurveying) {
			callbackContext.error("Survey already started");
		} else {
			mIsSurveying = true;
			mAnalyser.initSonogram();

			Log.i("TAG", "Survey started");
			mSurveyTimer = new Timer();
			mSurveyTimer.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					// mAnalyser.updateInsectLongTimeResults();
					mAnalyser.updateLongTermResult();
					mAnalyser.updateSonogram();
				}
			}, 0, 100);

			callbackContext.success();
		}
	}

//	private void stopSurvey(CallbackContext callbackContext, int width,
//			int height) {
//
//		Log.i("STOP", "survey stopped");
//		mIsSurveying = false;
//
//		/** stop the survey thread */
//		mSurveyTimer.cancel();
//
//		/** now try to get some results */
//		try {
//
//			HmmResult[] results = mAnalyser.getHmmResult();
//			JSONArray json_insects = new JSONArray();
//
//			int message = 0;
//			boolean keepRecording = false;
//			boolean foundCicada = false;
//
//			for (int i = 0; i < results.length; i++) {
//				Insect insect = mAnalyser.getInsects().get(
//						results[i].getInsectId());
//				boolean found = results[i].isFound();
//				JSONObject json_insect = insect.toJSON();
//
//				json_insect.put("value", results[i].getValue());
//				json_insect.put("found", found);
//
//				if (found) {
//					keepRecording = true;
//					if (insect.getId() == Cicada.ID) {
//						foundCicada = true;
//					}
//				}
//				json_insects.put(json_insect);
//			}
//
//			if (keepRecording)
//				message++;
//			if (foundCicada)
//				message++;
//
//			/** now create the bitmap file */
//			String[] sonogram_created = mAnalyser.createSonogram(width, height);
//			String fullpath = sonogram_created[0];
//			//String serialised_sonogram = sonogram_created[1];
//
//			JSONObject report = new JSONObject();
//			report.put("insects", json_insects);
//			report.put("keep_recording", keepRecording);
//			report.put("message", message);
//			report.put("sonogram", fullpath);
//			//report.put("serialised_sonogram", serialised_sonogram);
//
//			callbackContext.success(report);
//
//		} catch (JSONException e) {
//			e.printStackTrace();
//			callbackContext.error("Cannot retrieve survey report.");
//		}
//
//		mSurveyTimer = null;
//
//		/*
//		 * try { report = new JSONObject("{\"insects\": " +
//		 * "[ { \"insect\": 0, \"name\": \"New Forest Cicada\", \"value\": 0.8, \"found\": true } ], "
//		 * + "\"keep_recording\": true,\"message\": 0}");
//		 * callbackContext.success(report); } catch (JSONException e) {
//		 * e.printStackTrace(); callbackContext.error("Badaboom!"); }
//		 */
//
//	}

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

