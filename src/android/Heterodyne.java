package info.newforestcicada.audiorecorder.plugin;

import android.util.FloatMath;

public class Heterodyne
{
	private float[] xv_H = new float[3];
	private float[] yv_H = new float[3];
	private float[] xv_L = new float[2];
	private float[] yv_L = new float[2];
	private float[] xv_DC = new float[2];
	private float[] yv_DC = new float[2];
	
	private float time;
	private float increment;
	
	private float intermediate;
	private float finalValue;

	private static final double GAIN_H = 2.967352983;
	private static final double GAIN_L = 15.01371197;
	private static final double M_2_PI = 2/Math.PI;
	
	public Heterodyne(float frequency, float samplingRate)
	{
		for ( int i=0; i<3; i++ ) {
			xv_H[i] = 0;
			yv_H[i] = 0;
		}
		
		for ( int i=0; i<2; i++ ) {
			xv_L[i] = 0;
			yv_L[i] = 0;
		}
		
		for ( int i=0; i<2; i++ ) {
			xv_DC[i] = 0;
			yv_DC[i] = 0;
		}
		setFrequency(frequency, samplingRate);
		time = 0;
	}
	
	public void setFrequency(float frequency, float samplingRate)
	{
		increment = (float) (frequency / samplingRate * M_2_PI);
	}
	
	public float updateWithSample(float sample)
	{
		
		time += increment;
		
		if ( time > M_2_PI ) {
			
			time -= M_2_PI;
			
		}
		
		// High pass filter - 2nd order Butterworth @ 10kHz
		
		xv_H[0] = xv_H[1];
		xv_H[1] = xv_H[2];
		xv_H[2] = (float) (sample / GAIN_H * 100.0);
		yv_H[0] = yv_H[1];
		yv_H[1] = yv_H[2];
		yv_H[2] = (float) (xv_H[0] + xv_H[2] - 2 * xv_H[1] + ( -0.1767613657 * yv_H[0] ) + ( 0.1712413904 * yv_H[1] ));
		
		// Mix the signal with a sine wave
		
		intermediate = (float) (yv_H[2] * FloatMath.sin(time));
		
		// Low pass filter - 1st order Butterworth @ 1000Hz
		
		xv_L[0] = xv_L[1];
		xv_L[1] = (float) (intermediate / GAIN_L * 1.0);
		yv_L[0] = yv_L[1];
		yv_L[1] = (float) (xv_L[0] + xv_L[1] + ( 0.8667884395 * yv_L[0] ));
		
		// High pass filter - 1st order Butterword @ 10Hz to remove DC component
		
		xv_DC[0] = xv_DC[1];
		xv_DC[1] = (float) (yv_L[1] / 1.0);
		yv_DC[0] = yv_DC[1];
		yv_DC[1] = (float) (xv_DC[1] - xv_DC[0] + (  0.9985762554 * yv_DC[0]));
			
		finalValue = yv_DC[1];

		return finalValue;
	}
	
	public float getOutputValue()
	{
		return finalValue;
	}
	
	
	
	
}


