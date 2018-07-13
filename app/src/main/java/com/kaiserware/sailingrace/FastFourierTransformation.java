package com.kaiserware.sailingrace;
import android.util.Log;

import java.util.LinkedList;

/**
 * Created by Volker Petersen on 3/11/2017.
 *
 * Fast Fourier Transformation
 *
 * see Evernote under TAG FastFourierTransformation
 * Stackoverflow: http://stackoverflow.com/questions/9272232/fft-library-in-android-sdk
 *
 * used to compute the primary frequency / period of the wind oscillation
 *
 * call FastFourierTransformation.fetch_data() to convert the LinkedList to x[] and y[] array
 * call FastFourierTransformation.FastFourierTransformation() to compute the FFT
 * call FastFourierTransformation.frequency() to compute the primary frequency
 */
public class FastFourierTransformation {
    int m, nfft;
    static final String LOG_TAG = FastFourierTransformation.class.getSimpleName();

    // Lookup tables. Only need to recompute when size of FFT changes.
    double[] cos;
    double[] sin;

    /**
     * Initialize the Fast Fourier Transformation and adjust the size of the data array
     * to the nearest power of 2
     *
     * @param n size of the data array.
     */
    public FastFourierTransformation(int n) {
        // compute the length of the FastFourierTransformation to the nearest power of 2 to
        // the length of the input param n (desired length)
        double constant;

        m = nearest_power_of_two(n);
        nfft = (int)Math.pow(2.0d, (double)m);

        //Log.d(LOG_TAG, "fft() - nfft="+nfft+"  m="+m+"  (1 << m)="+(1<<m));

        // pre-compute the sine and cosine tables
        cos = new double[nfft / 2];
        sin = new double[nfft / 2];

        constant = -2.0 * Math.PI / nfft;
        for (int i = 0; i < nfft / 2; i++) {
            cos[i] = Math.cos(constant * i);
            sin[i] = Math.sin(constant * i);
        }
    }

    /**
     * Function to compute the nearest power of 2 such that n >= 2^return value
     *
     * @param x value to be evaluated
     * @return nearest power of 2 such that n >= 2^return value
     */
    private int nearest_power_of_two(int x) {
        double power = 0.0d;
        while (Math.pow(2.0d, power) < x) {
            power += 1.0d;
        }
        return (int)(power);
    }

    /**
     * Function computes the Fast Fourier Transformation and returns the final values
     * in the input arrays x and y
     *
     * @param x array[nfft] with Real-component
     * @param y array[nfft] with Imaginary-component
     *
     */
    public void fft(double[] x, double[] y) {
        int i, j, k, n1, n2, a;
        double c, s, t1, t2;

        // Bit-reverse
        j = 0;
        n2 = nfft / 2;
        for (i = 1; i < nfft - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < nfft; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }

    /**
     *
     * @param x array[nfft] with the power values from the fft() function
     * @param sampling_rate of the input dataset
     * @return frequency derived from the fft power values
     *         NaN for invalid or zero result
     */
    public double frequency(double[] x, double sampling_rate) {
        // find maximum value and index of absolute values in x (real part of the FastFourierTransformation)
        double freq;
        double max = -1.0d;
        int idx = 0;
        int i;

		// original code was
        //      for (i=0; i<nfft; i++) {
        // since idx=0 yields frequency 0, we search foor the largest power value for
        // an index "idx" greater than zero.  We only search thru the first half of the
        // power array "x" since this array is symmetrical.
        for (i=1; i<nfft/2; i++) {
            if (Math.abs(x[i]) > max) {
                max = Math.abs(x[i]);
                idx = i;
            }
        }

        //Log.d(LOG_TAG, "fft() idx="+idx+"  max="+max+"  sample_rate="+sampling_rate);

        // the frequency vector is an array f containing the frequency bin centers in cycles per unit
        // of the sample spacing (with zero at the start). For instance, if the sample spacing is in
        // seconds, then the frequency unit is cycles/second.
        // Given a window length n and a sample spacing d (in sec) = 1/sampling_rate then
        // f = [0, 1, ...,     n/2-1,     n/2] / (d*n)   if n is even
        //
        // Since we only need the value at index idx we can compute that as:
        // f = idx * 1/2 * (d*n)
        freq = (double)idx * sampling_rate / (double)nfft;

        if (Math.abs(freq) < NavigationTools.PRECISION) {
            freq = Double.NaN;
        }
        
        return freq;
    }

    /**
     * transfer the content of a LinkList<Double> into an 2D array with the Real components
     * stored in array[0][] and the Imaginary component in array[1][].
     * We assume that the LinkedList contains only real numbers.  Thus Real component go into
     * array[0][] and the Imaginary part is assumed to be 0 (array[1][] = 0.0d).
     * The array is padded with zero values when nfft > list.size()
     *
     * @param list LinkedList holding the dataset points for which we compute the FFT
     * @param mean value of the data in the LinkedList
     * @return array[2][nfft]
     */
    public double[][] fetch_data(LinkedList<Double> list, double mean) {
        double[][] array = new double[2][this.nfft];
        int size = list.size();

        for (int i = 0; i < nfft; i++) {
            if (i < size) {
                array[0][i] = NavigationTools.HeadingDelta(list.get(i), mean);
                array[1][i] = 0.0d;
            } else {
                // pad array with zeros
                array[0][i] = 0.0d;
                array[1][i] = 0.0d;
            }
        }
        return array;
    }

    /**
     * Create a test dataset with a sin wave plus some random noise
     *
     * @param sampling_rate
     * @param freq
     * @param n size of the dataset
     * @return none
     */
    public void create_dataset(double sampling_rate, double freq, int n) {
        double amplitude = 10.0d;
        double constant = freq/sampling_rate*2.0d*Math.PI;
        double sumTWD=0.0d;

        for (int i = 0; i < n; i++) {
            NavigationTools.TWD.add((Math.sin((double)i*constant)+Math.random()*0.2d)*amplitude);
            sumTWD += NavigationTools.TWD.getLast();
        }

        NavigationTools.TWD_longAVG=sumTWD/(double)n;
    }
}
