package info.newforestcicada.audiorecorder.plugin;

import android.util.Log;

/**
 * Created by Davide Zilli on 08/01/16.
 */
public class Heterodyne {

    public static final double GAIN_H  = 1.863674229f;
    public static final double RATIO_H = 0.0731489272f;

    public static final double GAIN_L = 15.01371197f;
    public static final double RATIO_L = 0.8667884395f;

    public static final double GAIN_DC = 1.000712379f;
    public static final double RATIO_DC = 0.9985762554f;


    double realPart = 0;
    double imaginaryPart = 1;
    double samplingRate;

    double cosTheta;
    double sinTheta;

    HighPassFilter preMixingHighPassFilter;
    LowPassFilter postMixingLowPassFilter;
    HighPassFilter dcRemovingHighPassFilter;

    private double finalValue = 0;

    public Heterodyne(double frequency, double samplingRate) {

        this.samplingRate = samplingRate;

        setFrequency(frequency, samplingRate);

        preMixingHighPassFilter = new HighPassFilter(GAIN_H, RATIO_H);
        postMixingLowPassFilter = new LowPassFilter(GAIN_L, RATIO_L);
        dcRemovingHighPassFilter = new HighPassFilter(GAIN_DC, RATIO_DC);

    }

    public void setFrequency(double frequency, double samplingRate) {

        double theta = 2 * Math.PI * frequency / samplingRate;

        this.cosTheta = Math.cos(theta);
        this.sinTheta = Math.sin(theta);
    }

    public double updateWithSample(double sample){

        preMixingHighPassFilter.update(sample);

        double real = cosTheta * realPart - sinTheta * imaginaryPart;
        double imaginary = sinTheta * realPart + cosTheta * imaginaryPart;

        this.realPart = real;
        this.imaginaryPart = imaginary;

        double output = preMixingHighPassFilter.getValue() * realPart * 100 * Short.MAX_VALUE;

        postMixingLowPassFilter.update(output);
        output = postMixingLowPassFilter.getValue();

        dcRemovingHighPassFilter.update(output);
        output = dcRemovingHighPassFilter.getValue() * Short.MAX_VALUE;

        finalValue = output;
        return output;

    }

    public double getOutputValue()
    {
        return finalValue;
    }
}
