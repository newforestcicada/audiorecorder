package info.newforestcicada.plugin.insects;

import info.newforestcicada.audiorecorder.plugin.LongTermResult;


public class NullInsect extends Insect {
	public static final int ID = 99;
	public static final String NAME = "None of these";
	public static final int frequency = 13;
	public static final float threshold = 0.5f;

	public NullInsect() {
		super(ID, NAME);
	}

	@Override
	public float getValue(float[] freqs) {
		return 0;
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

		return new LongTermResult(true, 0);

	}

}
