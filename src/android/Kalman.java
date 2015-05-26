package info.newforestcicada.audiorecorder.plugin;

public class Kalman
{
	float q;
	float r;
	float p;
	float x;
	float k;
	
	public Kalman(float processNoise, float measurementNoise, float estimateVariance, float estimateValue)
	{
		q = processNoise;
		r = measurementNoise;
		p = estimateVariance;
		x = estimateValue;
		k = 0;
	}
	
	public void updateWithMeasurement(float measurement)
	{
		p = p + q;
		k = p/(p+r);
		x = x + k*(measurement-x);
		p = (1-k)*p;
	}
	
	public float getEstimate()
	{
		return x;
	}
}
