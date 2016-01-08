package info.newforestcicada.audiorecorder.plugin;

/**
 * Created by dav on 08/01/16.
 */
public class HighPassFilter extends Filter {

    public HighPassFilter(double gain, double ratio) {
        super(gain, ratio);
    }

    public void update(double sample) {

        xv0 = xv1;
        xv1 = sample / Short.MAX_VALUE / gain;
        yv0 = yv1;
        yv1 = (xv1 - xv0) + (ratio * yv0);

    }

}
