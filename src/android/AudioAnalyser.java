package info.newforestcicada.audiorecorder.plugin;


import info.newforestcicada.audiorecorder.plugin.Hmm.HmmResult;
import info.newforestcicada.plugin.insects.Cicada;
import info.newforestcicada.plugin.insects.CommonFieldGrasshopper;
import info.newforestcicada.plugin.insects.DarkBushCricket;
import info.newforestcicada.plugin.insects.Insect;
import info.newforestcicada.plugin.insects.NullInsect;
import info.newforestcicada.plugin.insects.RoeselsBushCricket;
import info.newforestcicada.plugin.insects.WoodCricket;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

public class AudioAnalyser {

	public static final String TAG = "AudioAnalyser";
	
	// WARNING: constructor does not update this value
	public static final int SAMPLE_RATE = 44100;
	public static final int NFILTERS = 20;
	public static final int MAX_FEATURES_LENGTH = 1024;
	public static final int NUMBER_OF_FEATURES = 2;
	
	public static final int ROESELS_RATIO_HIGH_FREQUENCY = 18;
	public static final int CICADA_RATIO_HIGH_FREQUENCY  = 13;
	public static final int CICADA_RATIO_LOW_FREQUENCY   = 7;

	public static final String phoneModel = android.os.Build.MODEL;
	
    public static final float[] baseline = 
    		Baselines.spectr_baselines.containsKey(phoneModel) ?
    		Baselines.spectr_baselines.get(phoneModel) :
			Baselines.spectr_baselines.get("default");

	public static final float sonogramScalingFactor = 0.3f;
	public static final float ratioScalingFactor = 1f;
	public static final float sensitivity = 0.5f;
	
	private Goertzel[] goertzels;
	private SparseArray<Insect> insects;
	private ArrayList<Integer> mSonogram;
	
	private Context ctx;
	
	private float[][] features;
	private int featuresIndex = 0;

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

		Log.i(TAG, "baseline: " + Arrays.toString(baseline));
		this.ctx = ctx;
		
		goertzels = new Goertzel[NFILTERS];
		for (int i = 0; i < NFILTERS; i++) {
			goertzels[i] = new Goertzel(128, (float) (1000 * (i + 1)),
					sampleRate);
		}
		
		features = new float[MAX_FEATURES_LENGTH][NUMBER_OF_FEATURES];

		insects = new SparseArray<Insect>();

		insects.put(Cicada.ID, new Cicada());
		insects.put(CommonFieldGrasshopper.ID, new CommonFieldGrasshopper());
		insects.put(DarkBushCricket.ID, new DarkBushCricket());
		insects.put(RoeselsBushCricket.ID, new RoeselsBushCricket());
		insects.put(WoodCricket.ID, new WoodCricket());
		insects.put(NullInsect.ID, new NullInsect());
		
		//df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.UK);
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

	/**
	 * Pass to the various insects the values for the survey. All insects have
	 * the same behaviour here, in that they get entire range of raw goertzel
	 * filters.
	 */
	public void updateInsectLongTimeResults() {
		float[] freqs = getRawFrequencies();
		for (int i=0; i<insects.size(); i++) {
			insects.valueAt(i).updateLongTermValue(freqs);
		}
	}
	
	/**
	 * Same as above, but for the HMM method instead of the per-insect method
	 */
	public void updateLongTermResult(){
		
		float[] freqs = getRawFrequencies();
		
//		float cHigh = goertzels[CICADA_RATIO_HIGH_FREQUENCY].getValue();
//		float cLow = goertzels[CICADA_RATIO_LOW_FREQUENCY].getValue();
//		float rHigh = goertzels[ROESELS_RATIO_HIGH_FREQUENCY].getValue();
		
		float cHigh = freqs[CICADA_RATIO_HIGH_FREQUENCY];
		float cLow  = freqs[CICADA_RATIO_LOW_FREQUENCY];
		float rHigh = freqs[ROESELS_RATIO_HIGH_FREQUENCY];
		
//		features[featuresIndex][0] = goertzels[CICADA_RATIO_HIGH_FREQUENCY].getValue()/goertzels[CICADA_RATIO_LOW_FREQUENCY].getValue();
//		features[featuresIndex][1] = goertzels[ROESELS_RATIO_HIGH_FREQUENCY].getValue()/goertzels[CICADA_RATIO_HIGH_FREQUENCY].getValue();
		features[featuresIndex][0] = cHigh/cLow;
		features[featuresIndex][1] = rHigh/cHigh;
		
//		Log.i("BBB", features[featuresIndex][0] + " :: " + features[featuresIndex][1]);
		
//		Log.i("FEATX", features[featuresIndex][0]+"\t"+features[featuresIndex][1]);
//		
//		String log = "";
//		for (float freq : freqs) {
//			log += freq + "\t";
//		}
//		Log.i("GOERZ", log);
////				  cHigh + "\t"
////				+ cLow + "\t"
////				+ rHigh);
		
		featuresIndex++;
	}
	

	/**
	 * Get the frequencies <b>scaled for the sonogram</b>.
	 * 
	 * @return the list of 20 values
	 */
	public ArrayList<Float> getFrequencies() {
		// Log.i("ANLYSR",goertzel14kHz.getValue()+"/"+goertzel10kHz.getValue());
		ArrayList<Float> res = new ArrayList<Float>(NFILTERS);
		for (int i = 0; i < NFILTERS; i++) {
			res.add(Math.max(
					0,
					(float) (2 / (1 + Math.exp(-sonogramScalingFactor
							* goertzels[i].getValue() / baseline[i])) - 1.0)));
			// System.out.print(goertzels[i].getValue() + " ");
		}
		// System.out.println();
		return res;
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

	/*
	public float getCicadaValue() {
		return insects.get(0).getValue(getRawFrequencies());
	}
	*/

	/**
	 * Get <b>instantaneous</b> values for all the insects.
	 * 
	 * @return a HashMap that has the insect as key and the estimate as value.
	 */
	public HashMap<Insect, Float> getInsectsEstimates() {
		HashMap<Insect, Float> estim = new HashMap<Insect, Float>();
		float[] freqs = getRawFrequencies();
		for (int i=0; i<insects.size(); i++) {
			estim.put(insects.valueAt(i), insects.valueAt(i).getValue(freqs));
		}
		return estim;

	}

	/**
	 * Same as <code>getInsectsEstimates()</code>, but return a list.
	 * 
	 * @return a list where the index is the insect id and the value is the
	 *         estimate.
	 */
	public ArrayList<Float> getInsectsEstimatesAsList() {
		ArrayList<Float> estim = new ArrayList<Float>();
		float[] freqs = getRawFrequencies();
		for (int i=0; i<insects.size(); i++) {
			estim.add(insects.keyAt(i), insects.valueAt(i).getValue(freqs));
		}
		return estim;
	}

	/**
	 * 
	 * @return the size of the analyser, i.e. the number of insects detectable.
	 */
	public int getSize() {
		return insects.size();
	}

	/**
	 * Return the survey results.
	 * 
	 * @return a list of LongTermResults.
	 */
	public ArrayList<LongTermResult> getInsectsLongTimeResults() {
		ArrayList<LongTermResult> longTermResults = new ArrayList<LongTermResult>(
				this.getSize());
		for (int i=0; i<insects.size(); i++) {
			longTermResults.add(insects.valueAt(i).getLongTermResult());
		}
		return longTermResults;
	}
	
	public HmmResult[] getHmmResult() {
		return new Hmm(this.ctx).classifyWithFeatures(features, featuresIndex);
	}
	/*
		HashMap<Insect, LongTermResult> longTermResults = new HashMap<Insect, LongTermResult>(result.size());

		try {
			for (int i=0; i<insects.size(); i++) {
				Insect insect = insects.valueAt(i);
				if (i == 3){
					longTermResults.put(insects.get(NullInsect.ID), new LongTermResult(false, 0.9f));
					break;
				}

				boolean res = result.get(insect.getId());
				if (res) {
					longTermResults.put(insect, new LongTermResult(res, 0.9f));
				} else {
					Log.w(TAG, "Insect "+insect.getName()+" was not in the HMM");
					longTermResults.put(insect, new LongTermResult(false, 0.6f));
				}
			}
			return longTermResults;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static
	<T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
	  List<T> list = new ArrayList<T>(c);
	  java.util.Collections.sort(list);
	  return list;
	}
	 */
	public void updateSonogram() {
		ArrayList<Float> freqs = getFrequencies();
		for (int i = 0; i < freqs.size(); i++) {
			int col = Color.argb(
					(int) (255 * freqs.get(i)),
					(int) (255 * freqs.get(i)),
					(int) (255 * freqs.get(i)),
					(int) (255 * freqs.get(i))
//					255, 
//					(int) (67 + 178 * freqs.get(i)),
//					(int) (86 + 159 * freqs.get(i)),
//					(int) (78 + 167 * freqs.get(i))
					);
			mSonogram.add(col);
		}
	}

	public void initSonogram() {
		mSonogram = new ArrayList<Integer>();
	}

	/**
	 * @param width of the sonogram image
	 * @param height of the sonogram image
	 * @return the path to the sonogram image
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public String[] createSonogram(int width, int height) {

		int[] intBmp = new int[mSonogram.size()];
		Iterator<Integer> iterator = mSonogram.iterator();
		for (int i = 0; i < intBmp.length; i++) {
			intBmp[i] = iterator.next().intValue();
		}

		Bitmap bmp = Bitmap.createBitmap(intBmp, NFILTERS, mSonogram.size()
				/ NFILTERS, Bitmap.Config.ARGB_8888);

		Matrix matrix = new Matrix();
		matrix.postRotate(270);
		bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
				matrix, true);
		
		WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		int screen_height = 0;
		
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			display.getSize(size);
			screen_height = size.y;
		} else {
			screen_height = display.getHeight();  // deprecated
		}
		
		
		if (screen_height <= 320) height /= 2;

		bmp = Bitmap.createScaledBitmap(bmp, width, height, false);

		
		String filename = RecorderPlugin.df.format(new Date())+".png";
		String fullpath = "";
		String encoded = "";
		FileOutputStream stream = null;
		try {
			File dirpath = new File(Environment
					.getExternalStorageDirectory().getPath() + "/CicadaHunt/");
			dirpath.mkdirs();
			File imagefile = new File(dirpath, filename);

			stream = new FileOutputStream(imagefile);
			bmp.compress(CompressFormat.PNG, 100, stream);
			fullpath = imagefile.getAbsolutePath();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			bmp.compress(CompressFormat.PNG, 100, baos); //bm is the bitmap object   
			byte[] b = baos.toByteArray();

			encoded = Base64.encodeToString(b, Base64.DEFAULT);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fullpath = "";
		} catch (Exception e1) {
			e1.printStackTrace();
			fullpath = "";
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new String[]{fullpath,encoded};
	}

	public SparseArray<Insect> getInsects() {
		return insects;
	}
}
