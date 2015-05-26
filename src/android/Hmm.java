package info.newforestcicada.audiorecorder.plugin;

import info.newforestcicada.plugin.insects.Cicada;
import info.newforestcicada.plugin.insects.DarkBushCricket;
import info.newforestcicada.plugin.insects.NullInsect;
import info.newforestcicada.plugin.insects.RoeselsBushCricket;
import android.content.Context;
import android.util.Log;

public class Hmm {
	
	public static final int MAX_FEATURE_LENGTH = AudioAnalyser.MAX_FEATURES_LENGTH;
	public static final int NUMBER_OF_FEATURES = AudioAnalyser.NUMBER_OF_FEATURES;
	public static final int NUMBER_OF_STATES = 5;
	
    public double[][] DISTRIBUTION_MEAN;
    public double[][] DISTRIBUTION_VARIANCE;

    		
	public double[][] logDistributionMean;
	public double[][] logDistributionVariance;
    		
//    public static final double[][] TRANSITION_MATRIX = {
//            { 0.97,   0.01,   0.00,   0.01,   0.01 },
//            { 0.01,   0.89,   0.10,   0.00,   0.00 },
//            { 0.00,   0.50,   0.50,   0.00,   0.00 },
//            { 0.01,   0.00,   0.00,   0.99,   0.01 },
//            { 0.01,   0.00,   0.00,   0.00,   0.99 }};

	
    String[][] DISTRIBUTIONS = new String [][]
    	    {{"lognormal", "normal"},
             {"lognormal", "normal"},
             {"lognormal", "normal"},
             {"lognormal", "normal"},
             {"lognormal", "normal"}};
    
    public static final double[][] TRANSITION_MATRIX = 
    {{  0.9997, 0.0000, 0.0001, 0.0001, 0.0001 },
        {  0.0000, 0.50, 0.50, 0.00, 0.00 },
        {  0.0001, 0.30, 0.6999, 0.00, 0.00 },
        {  0.0001, 0.00, 0.00, 0.9999, 0.00 },
        {  0.0001, 0.00, 0.00, 0.00, 0.9999 }   };
    

        
    public static final double[] INITIAL_PROBABILITIES = { 0.25, 0.20, 0.05, 0.25, 0.25 };
	private static final String TAG = "HMM";
	
	public Hmm(Context context) {
		
		DISTRIBUTION_MEAN = Emission.getMeans(context);
		DISTRIBUTION_VARIANCE = Emission.getVars(context);
		
		/** WARNING: ONLY ln the emissions that will use a log-normal 
		 * distribution, in this case the first feature (14/8)
		 */
		
		for (int i=0; i<NUMBER_OF_STATES; i++){ 
			for (int j=0; j<NUMBER_OF_FEATURES; j++){
				
				if (DISTRIBUTIONS[i][j] == "lognormal") {
					
					double mean = DISTRIBUTION_MEAN[i][j];
					double variance = DISTRIBUTION_VARIANCE[i][j];
					Log.i(TAG, String.format("Logging state %s feature %s", i, j));  
					DISTRIBUTION_MEAN[i][j] = Math.log( Math.pow(mean ,2)/
							Math.sqrt(variance+Math.pow(mean,2)));
					DISTRIBUTION_VARIANCE[i][j] = Math.sqrt(Math.log(1+
							variance/Math.pow(mean,2)));
				}
			}
		}
		
	}

	public double normalpdf(double x, double mean, double variance) {
		return Math.sqrt( 0.5 / Math.PI / variance ) * Math.exp( - (x - mean) * (x - mean) / 2.0 / variance);
	}
	
	public double lognormalpdf(double x, double mean, double variance) {
		return ( 1. / ( x*Math.sqrt(2*Math.PI*variance) ) * 
				Math.exp( - Math.pow((Math.log(x) - mean), 2) / (2.* variance) ) );
	}
	
	public HmmResult[] classifyWithFeatures(float[][] features, int length){
		
		Log.i("FEAT", ""+features.length);
		Log.i("FEAT", ""+features[0].length);
		
		double[][] T1 = new double[MAX_FEATURE_LENGTH][NUMBER_OF_STATES];
		int   [][] T2 = new int   [MAX_FEATURE_LENGTH][NUMBER_OF_STATES];
		int   []   Z  = new int   [MAX_FEATURE_LENGTH];
		
		float silentValue             = 0;
		float cicadaValue             = 0;
		float darkBushCricketValue    = 0;
		float roeselsBushCricketValue = 0;
		
		try {
			for (int i=0; i<length; i++) {
				
				// calculate the emission probabilities
				double[] emissions = INITIAL_PROBABILITIES;
				
				for (int s=0; s<NUMBER_OF_STATES; s++) {
					for (int f=0; f<NUMBER_OF_FEATURES; f++){
						
						if (DISTRIBUTIONS[s][f] == "lognormal"){
							
							emissions[s] = emissions[s] * lognormalpdf( 
									(double) features[i][f], 
									DISTRIBUTION_MEAN[s][f], 
									DISTRIBUTION_VARIANCE[s][f]);
						} else {

							emissions[s] = emissions[s] * normalpdf( 
									(double) features[i][f], 
									DISTRIBUTION_MEAN[s][f], 
									DISTRIBUTION_VARIANCE[s][f]);
						}
						if (i==0){
							Log.d(TAG, String.format("[%9s] S%d F%d; mean: %.3f, var: %.3f, ",
									DISTRIBUTIONS[s][f], s+1, f+1, 
									DISTRIBUTION_MEAN[s][f], 
									DISTRIBUTION_VARIANCE[s][f]));
						}
					}
				}
				
				// normalise them so that none is greater than 100 times another
				double max_emission = -1.0;
				
				for (int s=0; s<NUMBER_OF_STATES; s++) {
					max_emission = Math.max(max_emission, emissions[s]);
				}
				
				max_emission = max_emission/100.;
				
				for (int s=0; s<NUMBER_OF_STATES; s++) {
					emissions[s] = Math.max(max_emission, emissions[s]);
				}
				
				
				if (i==0) {
					
					// initial step
					for (int j=0; j<NUMBER_OF_STATES; j++) {
						T1[0][j] = Math.log(INITIAL_PROBABILITIES[j]) + Math.log(emissions[j]);
						T2[0][j] = 0;
					}
				} else {
					
					// subsequent steps
					for (int s=0; s<NUMBER_OF_STATES; s++) {
						
						T1[i][s] = -Double.MAX_VALUE;
						for (int k=0; k<NUMBER_OF_STATES; k++){
							
							double value = T1[i-1][k] + Math.log(TRANSITION_MATRIX[k][s]) + Math.log(emissions[s]);
							
							if (value > T1[i][s]) {
								T1[i][s] = value;
								T2[i][s] = k;
							}
						}
					}
				}

			}
			
			// Backward pass
			double value = -Double.MAX_VALUE;
			for (int s=0; s<NUMBER_OF_STATES; s++){
				if (T1[length-1][s] > value) {
					value = T1[length-1][s];
					Z[length-1] = s;
				}
			}
			
			for (int i=length-1; i>0; i--){
				Z[i-1] = T2[i][Z[i]];
			}
			
			// Count up the states
			
			for (int i=0; i<length; i++){
				
				switch (Z[i]){
				
				case 0:
					silentValue++;
					break;
				case 1: 
					darkBushCricketValue++;
					break;
				case 2:
					darkBushCricketValue++;
					break;
				case 3:
					cicadaValue++;
					break;
				case 4:
					roeselsBushCricketValue++;
					break;
				}
			}
			
			silentValue /= (float) length;
			cicadaValue /= (float) length;
			darkBushCricketValue /= (float) length;
			roeselsBushCricketValue /= (float) length;
			
		} catch (Exception e){
			e.printStackTrace();
			Log.e(TAG, "Exception thrown in HMM code. Defaulting to safe sounds interesting case.");

			silentValue = 0.5f;
			cicadaValue = 0.5f;
			darkBushCricketValue = 0.0f;
			roeselsBushCricketValue = 0.0f;
		}
		
		Log.i(TAG, "HMM Output - Silent                : "+silentValue);
	    Log.i(TAG, "HMM Output - New Forest Cicada     : "+cicadaValue);
	    Log.i(TAG, "HMM Output - Dark Bush Cricket     : "+darkBushCricketValue);
	    Log.i(TAG, "HMM Output - Roesel's Bush Cricket : "+roeselsBushCricketValue);
	    
	    //cicadaValue = 0.9f;
	    // Calculate the return array
	    HmmResult[] results = new HmmResult[4];
	    results[0] = new HmmResult(Cicada.ID, cicadaValue,					cicadaValue			 	> 0.2 ? true : false);
	    results[1] = new HmmResult(DarkBushCricket.ID, darkBushCricketValue,			darkBushCricketValue 	> 0.2 ? true : false);
	    results[2] = new HmmResult(RoeselsBushCricket.ID, roeselsBushCricketValue,		roeselsBushCricketValue > 0.2 ? true : false);
	    results[3] = new HmmResult(NullInsect.ID, 0,				false);
	    //result.put(CommonFieldGrasshopper.ID, 	false);
	    //result.put(WoodCricket.ID, 				false);
	    
	    return results;
	}
	
	public class HmmResult {
		private int insectId;
		private float value;
		private boolean found;
		
		public HmmResult(int insectId, float value, boolean found){
			this.insectId = insectId;
			this.value = value;
			this.found = found;
		}

		public int getInsectId() {
			return insectId;
		}

		public float getValue() {
			return value;
		}

		public boolean isFound() {
			return found;
		}
	}

}
