package info.newforestcicada.plugin.insects;


import info.newforestcicada.audiorecorder.plugin.AudioAnalyser;
import info.newforestcicada.audiorecorder.plugin.LongTermResult;

import java.util.ArrayList;
import java.util.Collections;

public class Cicada extends Insect {
	public static final int ID = 0;
	public static final String NAME = "New Forest Cicada";
	public static final int frequency = 13;
	public static final float threshold = 0.5f;

	public Cicada() {
		super(ID, NAME);
	}

	@Override
	public float getValue(float[] freqs) {
		float value = (freqs[frequency - 1] / freqs[8]);
		return Math.max(0, (2 / (1 + (float) Math
				.exp(-AudioAnalyser.ratioScalingFactor * value)) - 1));

	}

	/**
	 * Smooth out the results to take care of any chirping song. 
	 * 
	 * If the smooth value ever exceeds the threshold, the cicada is detected. 
	 * The confidence in the result is 0.5 + however many times it exceeded the
	 * threshold (if found) and one minus the max smooth value if not found.
	 */
	@Override
	public LongTermResult getLongTermResult() {

		ArrayList<Float> smoothValues = new ArrayList<Float>();
		smoothValues.add(0f);

		int moreThanThreshold = 0; // how many time it exceeded the threshold

		for (int i = 1; i < longTermValues.size(); i++) {
			float smoothValue = 0.9f * smoothValues.get(i - 1) + 0.1f
					* longTermValues.get(i);
			smoothValues.add(smoothValue);
			if (smoothValue > threshold) moreThanThreshold++;
			//Log.i("CicadaClass", "value: "+longTermValues.get(i) + " smooth value: " + smoothValues.get(i));
		}

		LongTermResult result;

		if (Collections.max(smoothValues) > threshold) {
			float howMuchTimeCicada = moreThanThreshold / longTermValues.size();
			result = new LongTermResult(true, Math.min(1, threshold + (float)(-Math.exp(-15*howMuchTimeCicada/2)/2+1)));
		} else {
			result = new LongTermResult(false, 1 - Collections.max(smoothValues));
		}
		return result;

	}

}
