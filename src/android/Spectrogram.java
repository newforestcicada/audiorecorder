package info.newforestcicada.audiorecorder.plugin;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by dav on 01/06/15.
 */
public class Spectrogram {

    public static final int NFILTERS = AudioAnalyser.NFILTERS;
    public static final String TAG = "Spectr";
    public static final float sonogramScalingFactor = 0.3f;
    public static final String phoneModel = android.os.Build.MODEL;
    public static final float[] baseline =
            Baselines.spectr_baselines.containsKey(phoneModel) ?
                    Baselines.spectr_baselines.get(phoneModel) :
                    Baselines.spectr_baselines.get("default");

    private int mSonogramIndex = 0;
    private int mUpdateRate;
    private int[] mSonogram;

    public int zero_color = Color.argb(255,
            (int) (0.5f + 255.0f * base(0 - 0.5f)),
            (int) (0.5f + 255.0f * base(0)),
            (int) (0.5f + 255.0f * base(0 + 0.5f)));


    public static float interpolate(float value, float x0, float x1, float y0, float y1) {

        return (value - x0) * (y1 - y0) / (x1 - x0) + y0;

    }

    public static float base(float value) {

        if (value <= -0.75) {
            return 0;
        } else if (value <= -0.25) {
            return interpolate(value, -0.75f, -0.25f, 0.0f, 1.0f);
        } else if (value <= 0.25) {
            return 1.0f;
        } else if (value <= 0.75) {
            return interpolate(value, 0.25f, 0.75f, 1.0f, 0.0f);
        } else {
            return 0.0f;
        }

    }

    public void update(Goertzel[] goertzels) {

        ArrayList<Float> freqs = getFrequencies(goertzels);

        if (mSonogramIndex * NFILTERS + NFILTERS > mSonogram.length) {
            mSonogramIndex = 0;
            Log.i("SON", "restarting circular array sonogram");
        }


        float value = 0;
        for (int i = 0; i < freqs.size(); i++) {
            value = freqs.get(i);
            int col = Color.argb(
                    255,
                    (int) (0.5f + 255.0f * base(value - 0.5f)),
                    (int) (0.5f + 255.0f * base(value)),
                    (int) (0.5f + 255.0f * base(value + 0.5f))

//					(int) (255 * freqs.get(i)),
//					(int) (255 * freqs.get(i)),
//					(int) (255 * freqs.get(i)),
//					(int) (255 * freqs.get(i))
            );
            mSonogram[mSonogramIndex*NFILTERS + i] = col;
        }
        mSonogramIndex++;
    }

    public Spectrogram(int updateRate, int recLength) {

        //mSonogram = new ArrayList<Integer>();

        mUpdateRate = 1000/updateRate;
        mSonogram = new int[NFILTERS*mUpdateRate*recLength];
        mSonogramIndex = 0;

    }

    /**
     * @param width of the sonogram image
     * @param height of the sonogram image
     * @return the path to the sonogram image
     */

    public String[] write(int width, int height, int recLength) {

        //int[] intBmp = new int[mSonogram.size()];
        int[] intBmp = new int[recLength*mUpdateRate*NFILTERS];
        Log.i(TAG, "sonogram size: " + mSonogram.length + "; recLength: " + recLength*10*NFILTERS);

        int[] orderedSonogram = new int[mSonogram.length];
        synchronized (mSonogram) {
            int idx = mSonogramIndex * NFILTERS;
            for (int i = 0; i <  mSonogram.length - idx; i++) {
                if ( 0 == mSonogram[idx + i] ) {
                    orderedSonogram[i] = zero_color;
                } else{
                    orderedSonogram[i] = mSonogram[idx + i];
                }

            }
            for (int i = 0; i < idx; i ++){
                if ( 0 == mSonogram[i] ) {
                    orderedSonogram[mSonogram.length - idx + i] = zero_color;
                } else {
                    orderedSonogram[mSonogram.length - idx + i] = mSonogram[i];
                }
            }
        }

        Bitmap bmp = Bitmap.createBitmap(orderedSonogram, NFILTERS, orderedSonogram.length	/ NFILTERS, Bitmap.Config.ARGB_8888);

        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
                matrix, true);

        bmp = Bitmap.createScaledBitmap(bmp, width, height, false);


        String filename = RecorderPlugin.df.format(new Date())+".png";
        String fullpath = "";
        String encoded = "";

        File dirpath = new File(Environment
                .getExternalStorageDirectory().getPath() + "/audiorecorder/");
        dirpath.mkdirs();
        File imagefile = new File(dirpath, filename);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();

        encoded = Base64.encodeToString(b, Base64.DEFAULT);

        return new String[]{fullpath,encoded};
    }

    public static ArrayList<String> getFrequencyColours(Goertzel[] goertzels) {
        float _min = 80f;
        float _max = 5000f;

        ArrayList<Float> freqs = getFrequencies(goertzels);
        ArrayList<String> cols = new ArrayList<String>(freqs.size());

        int counter = 0;
        short red, green, blue;

        for (float value : freqs) {
            //float scaledValue = (float) Math.min(1.0f, Math.max(0.0f, Math.log( (value + 1.0f) / _min) / Math.log(_max / _min)));

            red   = (short) (0.5f + 255.0f * base( value - 0.5f));
            green = (short) (0.5f + 255.0f * base(value));
            blue  = (short) (0.5f + 255.0f * base(value + 0.5f));

            cols.add("#"+String.format("%02X", red)+String.format("%02X", green)+String.format("%02X", blue));

            counter ++;

        }
        return cols;

    }

    /**
     * Get the frequencies <b>scaled for the sonogram</b>.
     *
     * @return the list of values
     */
    public static ArrayList<Float> getFrequencies(Goertzel[] goertzels) {

        ArrayList<Float> res = new ArrayList<Float>(NFILTERS);

        for (int i = 0; i < NFILTERS; i++) {
            res.add(Math.max(
                    0,
                    (float) (2 / (1 + Math.exp(-sonogramScalingFactor
                            * goertzels[i].getValue() / baseline[i])) - 1.0)));
        }

        return res;
    }

}
