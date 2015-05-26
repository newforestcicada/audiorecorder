package info.newforestcicada.audiorecorder.plugin;


public class Goertzel
{
	private static final int MAX_FILTER_SIZE = 1024;
	private static final double M_PI = Math.PI;
	//private int k;
	private float k;
	private int N;

	private float[] hamming = new float[MAX_FILTER_SIZE];
	
	private float realW;
//	private float imagW;
	
	private float y;
	private float d1;
	private float d2;
	
	private int index;
	
	private Kalman kalman;
	
	public Goertzel(int N, float centralFrequency, float samplingRate)
	{
		this.N = N;
		float bandwidth = 4 * samplingRate / N;
		init(centralFrequency, bandwidth, samplingRate);
	}
	
	private void init(float centralFrequency, float bandpassWidth, float samplingRate)
	{
		//k = (int) Math.floor(0.5 + 4 * centralFrequency / bandpassWidth);
		k = 4 * centralFrequency / bandpassWidth;
		//Log.d("GOERTZEL", "N:"+N+" k:"+k);
		
		for ( int n=0; n<N; n++ ) {
			hamming[n] = (float) (0.54 - 0.46 * Math.cos(2*M_PI*n/N));
		}
		
		realW = (float) (2.0*Math.cos(2.0*M_PI*k/N));
//		imagW = FloatMath.sin((float) (2.0*M_PI*k/N));
		
		y = 0;
		d1 = 0;
		d2 = 0;
		
		index = 0;
		
		kalman = new Kalman(0.01f, 5.0f, 1.0f, 1.0f);

	}
	
	public void updateWithSample(float sample)
	{
		y = hamming[index]*sample + realW*d1 - d2;
		d2 = d1;
		d1 = y;
		
		if ( index++ == N ) {
			
			index = 0;
			
			kalman.updateWithMeasurement((float)Math.sqrt(d1*d1 + d2*d2 - d1*d2*realW));
			
			y = 0;
			d1 = 0;
			d2 = 0;
			
		}

	}
	
	public float getValue()
	{
		//return FloatMath.sqrt(d1*d1 + d2*d2 - d1*d2*realW);
		return kalman.getEstimate();
	}
}
