package info.newforestcicada.audiorecorder.plugin;


import android.content.Context;
import android.util.Log;
import java.util.Arrays;

public class AudioAnalyser {

	public static final String TAG = "AudioAnalyser";
	
	// WARNING: constructor does not update this value
	public static final int SAMPLE_RATE = 44100;
	public static final int NFILTERS = 20;
	public static final int MAX_FEATURES_LENGTH = 1024;
	public static final int NUMBER_OF_FEATURES = 2;

	public static final float ratioScalingFactor = 1f;
	public static final float sensitivity = 0.5f;
	
	private Goertzel[] goertzels;

	public AudioAnalyser(Context ctx) {
		this(SAMPLE_RATE, ctx);
	}

	/**
	 * Create 20 Goertzel filters, an ArrayList of insects and add each insect
	 * you want to detect.
	 * 
	 * @param sampleRate
	 *            at which you are recording.
	 */
	public AudioAnalyser(int sampleRate, Context ctx) {

		Log.i(TAG, "baseline: " + Arrays.toString(Spectrogram.baseline));
		
		goertzels = new Goertzel[NFILTERS];
		for (int i = 0; i < NFILTERS; i++) {
			goertzels[i] = new Goertzel(128, (float) (1000 * (i + 1)),
					sampleRate);
		}

	}

	/**
	 * Update the Goertzel filters with a sample. This can be (and is) done
	 * continuously.
	 * 
	 * @param sample
	 *            from the microphone.
	 */
	public void updateWithSoundInputValue(float sample) {
		for (int i = 0; i < NFILTERS; i++) {
			goertzels[i].updateWithSample(sample);
		}
	}

	public Goertzel[] getGoertzels() {
		return goertzels;
	}


	/**
	 * Get the <b>raw frequencies</b> for further processing.
	 * 
	 * @return the list of 20 raw features
	 */
	public float[] getRawFrequencies() {
		float[] res = new float[NFILTERS];
		for (int i = 0; i < NFILTERS; i++) {
			res[i] = goertzels[i].getValue();
		}
		return res;
	}

}
