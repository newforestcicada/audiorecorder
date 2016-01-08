package info.newforestcicada.audiorecorder.plugin;

/**
 * Created by dav on 08/01/16.
 */
public abstract class Filter {

    double xv0 = 0;
    double xv1 = 0;
    double yv0 = 0;
    double yv1 = 1;

    double gain;
    double ratio;

    public Filter(double gain, double ratio) {

        this.gain = gain;
        this.ratio = ratio;

    }

    public abstract void update(double sample);

    public double getValue() {
        return yv1;
    }

}
