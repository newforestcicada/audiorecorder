package info.newforestcicada.audiorecorder.plugin;

/**
 * Created by dav on 08/01/16.
 */
public class LowPassFilter extends Filter {

    public LowPassFilter(double gain, double ratio) {
        super(gain, ratio);
    }

    @Override
    public void update(double sample) {

        xv0 = xv1;
        xv1 = sample / Short.MAX_VALUE / gain;
        yv0 = yv1;
        yv1 = (xv0 + xv1) + (ratio * yv0);

    }
}
