# -*- coding: utf-8 -*-
__date__ = "March 17 12:51:48 2017"
__author__ = "Volker Petersen"
__version__ = "FastFourierTransformation.py ver 1.0"
__copyright__ = "Copyright (c) 2014 Volker Petersen"
__license__ = "Python 2.7 | GPL http://www.gnu.org/licenses/gpl.txt"
__doc__ = """
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
"""
import sys, os, math
import numpy as np

class FastFourierTransformation():
    def __init__(self, n):
        """
        ------------------------------------------------------------------------------------------
         * Initialize the Fast Fourier Transformation and adjust the size of the data array
         * to the nearest power of 2
         *
         * @param n size of the data array.
        ------------------------------------------------------------------------------------------
        """
        self.PRECISION = 0.00000001
        self.m = self.nearest_power_of_two(n)
 

        self.nfft = (int)(math.pow(2.0, self.m))
        self.n = self.nfft
        #n = self.nfft

        ### precompute tables
        self.cos = []
        self.sin = []
        print "n=%d,  n/2=%d" %(self.n, self.n/2)

        stop = self.n/2
        for i in range(0, stop):
            self.cos.append(math.cos(-2.0 * math.pi * i / self.n))
            self.sin.append(math.sin(-2.0 * math.pi * i / self.n))
        
        
    def nearest_power_of_two(self, x):
        """
        ------------------------------------------------------------------------------------------
         * Function to compute the nearest power of 2 such that n >= 2^return value
         *
         * @param n
         * @return nearest power of 2 such that n >= 2^return value
        ------------------------------------------------------------------------------------------
        """
        power = 0
        while (2**power < x):
            power += 1
        
        return power

    def fft(self, x, y):
        """
        ------------------------------------------------------------------------------------------
         * Function computes the Fast Fourier Transformation and returns the final values
         * in the input arrays x and y
         *
         * @param x array[nfft] with Real-component
         * @param y array[nfft] with Imaginary-component
        ------------------------------------------------------------------------------------------
        """
        ### Bit-reverse
        j = 0
        n2 = self.n / 2
        for i  in range(1, self.n-1):
            n1 = n2;
            while (j >= n1):
                j = j - n1
                n1 = n1 / 2
            j = j + n1

            if (i < j):
                t1 = x[i]
                x[i] = x[j]
                x[j] = t1
                t1 = y[i]
                y[i] = y[j]
                y[j] = t1

        ### FFT
        n1 = 0
        n2 = 1

        for i in range(0, self.m):
            n1 = n2
            n2 = n2 + n2
            a = 0

            for j in range(0, n1):
                c = self.cos[a]
                s = self.sin[a]
                a += 1 << (self.m - i - 1)

                for k in xrange(j, self.n, n2):
                    t1 = c * x[k + n1] - s * y[k + n1]
                    t2 = s * x[k + n1] + c * y[k + n1]
                    x[k + n1] = x[k] - t1
                    y[k + n1] = y[k] - t2
                    x[k] = x[k] + t1
                    y[k] = y[k] + t2

    def frequency(self, x, sampling_rate):
        """
        ------------------------------------------------------------------------------------------
         * @param x array[nfft] with the power values from the fft() function
         * @param sampling_rate of the input dataset
         * @return frequency derived from the fft power values
         *         NaN for invalid or zero result
        ------------------------------------------------------------------------------------------
        """
        ### find maximum value and index of absolute values in x (real part of the FastFourierTransformation)
        max_power = -1.0
        idx = 0

        # original code for i in range(0, self.n):        
        for i in range(1, self.n/2):
            if (abs(x[i]) > max_power):
                max_power = abs(x[i])
                idx = i

        print "fft() idx=%d  max=%.2f  sample_rate=%.2f" %(idx, max_power, sampling_rate)

        # the frequency vector is an array f containing the frequency bin centers in cycles per unit
        # of the sample spacing (with zero at the start). For instance, if the sample spacing is in
        # seconds, then the frequency unit is cycles/second.
        # Given a window length n and a sample spacing d (in sec) = 1/sampling_rate then
        # f = [0,  1,  2,  ...,    n/2-1,    n/2] / (d*n)   if n is even
        #
        # Since we only need the value at index idx we can compute that as:
        # f = idx * 1/2 * (d*n)
        if (idx == 0):
            freq = 0.5 * sampling_rate / self.nfft
        else:
            freq = idx * sampling_rate / self.nfft
            
        if (abs(freq) < self.PRECISION):
            freq = np.isnan
        
        return freq
"""
|------------------------------------------------------------------------------------------
|------------------------------------------------------------------------------------------
"""
if __name__ == "__main__":
    print "no main() function implememted for ", __version__
    fft = FastFourierTransformation(750)