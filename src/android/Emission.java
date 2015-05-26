package info.newforestcicada.audiorecorder.plugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import info.newforestcicada.orthoptera.R;

public class Emission {


	/* THIS IS NOT COMPLETE SO FAR, AND I DON'T THINK WE WILL EVER NEED A 
	 * CONSTRUCTOR WITH 2D DOUBLE ARRAYS. USING STRINGS SHOULD ALWAYS WORK.
	 * 
	public Emission(double[][] means, double[][] vars) {
		
		means = means != null ? means : getDefaultMeans();
		vars  = vars  != null ? vars  : getDefaultVariances();
		
		
	}
	*/
	
	private static final String TAG = "EMISS";

	public static void updateEmissions(String means, String vars, Context context) {
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		
		editor.putString(context.getString(R.string.MEANS), means);
		editor.putString(context.getString(R.string.VARS), vars);
		
		editor.commit();
		
	}
	
	public static double[][] parse2DDoubleArray(String str){
		JSONArray json;
		double[][] arr = new double[Hmm.NUMBER_OF_STATES][Hmm.NUMBER_OF_FEATURES];
		try {
			json = new JSONArray(str);
			for(int i = 0; i < Hmm.NUMBER_OF_STATES; i++){
				for(int j = 0; j < Hmm.NUMBER_OF_FEATURES; j++){
					arr[i][j] = json.getJSONArray(i).getDouble(j);
				}
			}
			return arr;
		} catch (JSONException e) {
			return null;
		}
	}
	
	private static double[][] getDefaultMeans() {
		return  Baselines.hmm_means.containsKey(AudioAnalyser.phoneModel) ?
	    		Baselines.hmm_means.get(AudioAnalyser.phoneModel) :
				Baselines.hmm_means.get("default");		
	}
	
	private static double[][] getDefaultVariances() {
		return  Baselines.hmm_vars.containsKey(AudioAnalyser.phoneModel) ?
	    		Baselines.hmm_vars.get(AudioAnalyser.phoneModel) :
	    		Baselines.hmm_vars.get("default");	
	}

	/** Get the distribution means for this device model from the preferences.
	 * 
	 * If they are not available from the preferences, get them from the default
	 * values in the Baselines class. 
	 * 
	 * @param context the Activity context for the preferences
	 * @return a 2D (5x2) double array of means
	 */
	public static double[][] getMeans(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String mean_string = sharedPref.getString(context.getString(R.string.MEANS), null);
		
		double[][] means;
		
		if (mean_string == null){
			means = getDefaultMeans();
			Log.e(TAG, "means are not in preferences");
		} else {
			double[][] mean_array = parse2DDoubleArray(mean_string);
			if (mean_array == null){
				means = getDefaultMeans();
				Log.e(TAG, "means cannot be parsed from preferences");
			} else {
				means = mean_array;
				Log.d(TAG, "means are taken from preferences");
			}
		}
		
		return means;
	}

	public static double[][] getVars(Context context) {
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String vars_string = sharedPref.getString(context.getString(R.string.VARS), null);
		
		double[][] vars;
		if (vars_string == null){
			vars = getDefaultVariances();
		} else {
			double[][] vars_array = parse2DDoubleArray(vars_string);
			if (vars_array == null){
				vars = getDefaultMeans();
			} else {
				vars = vars_array;
			}
		} 
		
		return vars;
	}
	


}
