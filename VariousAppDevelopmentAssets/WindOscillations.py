#!/usr/bin/env python
# -*- coding: utf-8 -*-

__date__ = "Sun Jan 04 12:51:48 2015"
__author__ = "Volker Petersen"
__version__ = "WindOscillator.py ver 1.0"
__copyright__ = "Copyright (c) 2014 Volker Petersen"
__license__ = "Python 2.7 | GPL http://www.gnu.org/licenses/gpl.txt"
__doc__ = """
---------------------------------------------------------------------------
Program Description
---------------------------------------------------------------------------
"""
try:
    # import python system modules
    import sys
    import numpy as np
    from matplotlib import pyplot as plt
    import pandas as pd
    import math
    from FastFourierTransformation import FastFourierTransformation
    from mysql_tools import MYSQL 
except ImportError as e:
    print "Import error:", e, "\nAborting the program", __version__
    sys.exit()
    
avg_names = ['2% mov. avg', '20% mov. avg']    
fig = 1

def get_data(sample_rate, source='CSV', filename='TWD_raw.csv'):    
    if 'set' in source:
        data = [294.0, 288.5, 293.8, 295.8, 299.4, 298.5, 300.0, 301.8, 303.0, 
            294.7, 296.9, 289.7, 293.3, 284.7, 288.3, 283.1, 284.3, 285.0, 
            281.2, 283.0, 277.5, 278.6, 279.2, 285.2, 285.5, 292.0, 296.5, 
            291.8, 297.8, 295.4, 302.5, 297.0, 296.8, 298.0, 300.7, 298.9, 
            290.7, 287.3, 285.7, 288.3, 288.1, 280.3, 277.0, 280.2, 280.0, 
            276.5, 280.6, 283.2, 284.2, 287.5, 288.0, 296.5, 293.8, 296.8, 
            301.4, 298.5, 298.0, 298.8, 302.0, 297.7, 292.9, 296.7, 290.3, 
            285.7, 284.3, 284.1, 280.3, 277.0, 284.2, 282.0, 278.5, 282.6, 
            286.2, 287.2, 283.5, 289.0, 296.5, 293.8, 293.8, 302.4, 295.5, 
            298.0, 295.8, 301.0, 295.7, 291.9, 289.7, 289.3, 284.7, 283.3, 
            285.1, 283.3, 279.0, 281.2, 280.0, 284.5, 281.6, 282.2, 288.2, 
            283.5, 286.0, 289.5, 297.8, 296.8, 301.4, 296.5, 304.0, 301.8, 
            302.0, 293.7, 298.9, 289.7, 287.3, 288.7, 282.3, 285.1, 282.3, 
            280.0, 283.2, 283.0, 280.5, 282.6, 287.2, 287.2, 291.5, 288.0, 
            294.5, 295.8, 296.8, 299.4, 297.5, 298.0, 303.8, 303.0, 293.7, 
            292.9, 290.7, 295.3, 292.7, 290.3, 281.1, 279.3, 280.0, 279.2, 
            281.0, 283.5, 278.6, 281.2, 288.2, 286.5, 291.0, 292.5, 291.8, 
            294.8, 301.4, 298.5, 298.0, 303.8, 298.0, 298.7, 294.9, 292.7, 
            288.3, 284.7, 285.3, 285.1, 286.3, 281.0, 278.2, 282.0, 276.5, 
            281.6, 281.2, 283.2, 290.5, 294.0, 296.5, 292.8, 300.8, 295.4, 
            300.5, 297.0, 297.8, 295.0, 301.7, 292.9, 294.7, 289.3, 287.7, 
            288.3, 283.1, 284.3, 285.0, 282.2, 280.0, 282.5, 278.6, 285.2, 
            281.2, 290.5, 293.0]
        df = pd.DataFrame(np.array(data))
        df = df.rename(columns={0:'TWD'})
    elif 'csv' in source:
        df=pd.read_csv(filename,               
                index_col="Date",
                parse_dates=True,
                names=['Date', 'TWD', 'TWS'],
                na_values=['nan'],
                sep=',',
                header=1)
                
        amplitude = (np.max(df['TWD'])-np.min(df['TWD'])) / 2.0 * 0.8
    elif 'sine' in source:
        f = 5.0   # 5Hz frequency
        amplitude = 5.0;
        data_range = np.array(np.arange(0, 5.0, 1.0/sample_rate))
        df = pd.DataFrame(index=data_range)
        df['TWD'] = np.sin(2.0*math.pi*data_range*f) * amplitude
        df['TWD'] = df['TWD'] + np.random.rand(len(data_range)) * 0.2*amplitude
        
        #print df.tail()
    elif 'square':
        f = 7.0    # 7Hz frequency
        amplitude = 4.0;
        data_range = np.array(np.arange(0, 2.0, 1.0/sample_rate))
        df = pd.DataFrame(index=data_range)
        arr = np.sin(2.0*math.pi*data_range*f) * amplitude
        for i in range(3,12,2):        
            arr = arr + np.sin(2.0*math.pi*data_range*f*i) * amplitude/i
        df['TWD'] = arr
    else:
        print "'%s' is an undefined data source.  Please use either 'CSV', 'set' or 'sine'!"
        return None

    win = int(0.02*len(df))    
    df = get_rolling_mean(df, 'TWD', window=win, name=avg_names[0])
    df = get_rolling_mean(df, 'TWD', window=win*10, name=avg_names[1])
    #print "df:"    
    #print df.head()
    return (df, amplitude)


def get_rolling_mean(df, col, window=2, name='moving avg'):
    """Return rolling mean of values in df[col], using specified window size."""
    #temp = pd.rolling_mean(df, window=window, min_periods=1)  # old version
    temp = df.rolling(min_periods=1, window=window, center=False).mean()
    temp = temp.rename(columns={col:name})
    df = df.join(temp[name])
    return df

def get_rolling_std(df, col, window=2, name='std dev'):
    """Return rolling standard deviation of given values, using specified window size."""
    #temp = pd.rolling_std(df, window=window, min_periods=1)  # old version
    temp = df.rolling(min_periods=1, window=window, center=False).std()
    temp = temp.rename(columns={col:name})
    df = df.join(temp[name])
    return df

def frequency_from_fft(data, sample_rate, plot=False):
    """ Compute the frequency of the d=sample data using the FFT. """
    global fig

    # center data around the mean
    data = data - np.mean(data)
    
    # get the length of the FFT set at nearest power of 2
    x = len(data)
    size = x
    nfft = pow(2,int(math.log(x)/math.log(2))+1)  # length of the FFT
    
    # compute the FFT on the column data    
    x = np.fft.fft(data, nfft)
    
    # FFT is symmetric, hence dump second half of the FTT data
    x = x[0:nfft/2]

    # compute magintude of FFT of x and get the index of the max
    mx = np.abs(x)
    idx = np.argmax(mx)
    val = np.max(mx)
    
    # build frequency vector
    N = len(x)
    if sample_rate < 1.0:
        f = np.fft.fftfreq(N)/sample_rate/2.0
    else:
        f = np.fft.fftfreq(N)*sample_rate/2.0

    freq = abs(f[idx])
    if freq > 0.0000001:  
        period = 1.0/freq
    else:
        period = 0
    units = 'sec'
    if period > 120.0:
        period = period / 60.0
        units = 'min'
        
    print 'idx = %d  yielding freq = %.5fHz' %(idx, (idx)*sample_rate/(1.0*nfft))
    print 'Max value of frequency spectrum is %.1f at frequency %.5fHz and Period %.2f%s' %(val, freq, period, units)
    print 'Max value at index %d of %d values' %(idx, nfft/2)
    print 'nfft = %d   N = %d   dataset length = %d' %(nfft, N, len(data))    
    print 'sample rate = %.2f' %sample_rate

    fftx = np.fft.fft(data, nfft) # the frequency transformed part
    # now discard anything  that we do not need..
    fftx = fftx[range(int(len(fftx)/2))]
    
    return freq
    
    if (plot):
        plt.figure(fig)
        fig = fig + 1
        plt.plot(f, mx)
        plt.title('Power Spectrum of a noisy Sine Wave')
        plt.xlabel('Frequency (Hz)')
        plt.ylabel('Power')
        plt.grid()
        plt.show()

def test():
    x = np.random.rand(100) # create 100 random numbers of which we want the fourier transform
    x = x - np.mean(x) # make sure the average is zero, so we don't get a huge DC offset.
    dt = 0.1 #[s] 1/the sampling rate
    fftx = np.fft.fft(x) # the frequency transformed part
    
    # now discard anything  that we do not need..
    fftx = fftx[range(int(len(fftx)/2))]
    
    # now create the frequency axis: it runs from 0 to the sampling rate /2
    freq_fftx = np.linspace(0,2/dt,len(fftx))
        
    # and plot a power spectrum
    #plt.plot(freq_fftx,abs(fftx)**2)
    #plt.show()    
    idx = np.argmax(np.abs(fftx))
    max_val = np.max(np.abs(fftx))
    print ("max=%.2f  at index=%d" %(max_val, idx))
    print ("freq=%.2f" %freq_fftx[idx])

def plot_data(df):
    global fig    
    fig = fig + 1
    plt.figure(fig)
    ind = df.index.values
    plt.plot(ind, df['TWD'].values, 'b', lw=1.0, label='TWD')
    plt.plot(ind, df[avg_names[0]].values, 'r', lw=3, label=avg_names[0])
    plt.plot(ind, df[avg_names[1]].values, 'g', lw=3, label=avg_names[1])
    plt.plot(ind, df['FFT'].values, 'k', lw=2.0, label='FTT')
    plt.title('TWD Oscillation Analysis')
    plt.grid()
    plt.xlabel('Time (sec)')
    plt.ylabel('TWD')
    plt.legend()
    plt.show()
    
def run(source, col, filename='TWD_raw.csv', plot=False):
    global fig
    
            
    if 'sine' in source:
        sample_rate = 100   # 150 samples every second
    elif 'square' in source:
        sample_rate = 120    # 60 samples every second
    elif 'set' in source:
        sample_rate = 2     # two samples every 1 seconds
    else:
        # used for the 'csv' data source
        sample_rate = 0.25 # one sample every 4 seconds

    print "\nProcessing data from source '%s - %s with sample rate %d'" %(source, col, sample_rate)

    (data, amplitude) = get_data(sample_rate, source, filename)
    freq = frequency_from_fft(data[col].values, sample_rate, plot)

    length = len(data) #/ sample_rate # seconds of datapoint in dataset
    cycles = length / sample_rate * freq
    print "# of cycles in range = ",cycles
    data_range = np.array(np.arange(0, cycles-cycles/len(data)/2.0, cycles/len(data)))
    
    data['FFT'] = np.mean(data[col]) + (np.sin(data_range*2.0*math.pi) * amplitude)            
    plot_data(data)
    
    return data

def nearest_power_of_two(x):
    power = 0
    while (2**power < x):
        power += 1
    
    return power
    

def plot_test():
    global fig    

    size = 1024
    current_period = 100
    sample_rate = 1
    cycles = 3.4
    frequency = cycles / size * sample_rate
    cycles = frequency * size / sample_rate
    above_mean = False
    if (above_mean):
        sine_start = (1.0 - (cycles-int(cycles)) ) * size/cycles + current_period
    else:
        sine_start = (0.5 - (cycles-int(cycles)) ) * size/cycles + current_period
    print "Frequency=%.5f  Cycles=%.2f  curr=%d above_mean=%s   sine_start=%.3f / %.2f%%  points/cycle=%.1f" %(frequency, cycles, current_period, above_mean, sine_start, sine_start/(size/cycles)*100, 1.0*size/cycles) 
    print "current %% = %.2f%%" %(100.0*current_period/size*cycles)
    x = []
    y = []
    for i in range(1, size+1):
        x.append(i+current_period-size)
        y.append(math.sin(2.0*math.pi*frequency/sample_rate*(sine_start+i)) * 5.0) 

    print "at %5d:  x=%5d  y=%.3f" %(0, x[0], y[0])
    print "at %5d:  x=%5d  y=%.3f" %((size-current_period-1), x[size-current_period-1], y[size-current_period-1])
    print "at %5d:  x=%5d  y=%.3f" %((size-1), x[size-1], y[size-1])
    print len(x)

    fig = fig + 1
    plt.figure(fig)
    plt.plot(x, y, 'g', lw=1.0, label='Wind Period')
    plt.grid()     
    plt.show()

def test_fft_vp():
    sample_rate = 200
    (data, amplitude) = get_data(sample_rate, "sine")
    size = len(data)
    fft = FastFourierTransformation(size)
    x = data['TWD'].values
    
    if len(x) < fft.nfft:
        x=np.append(x, np.zeros(fft.nfft-len(x)))

    y = np.zeros(fft.nfft)

    print "length of x=%d and y=%d and nfft=%d" %(len(x), len(y), fft.nfft)

    fft.fft(x, y)
    freq = fft.frequency(x, sample_rate)
    print freq


def wind_analysis(raceID=1491627600, plot=True):
    global fig
    
    sample_rate = 1.0/4.0  # one wind reading every 4 seconds
    if raceID < 0:
        twd = 'TWD'
        f = 1.0/900        # frequency with 900 sec (15min) period
        amplitude = 5.0;
        data_range = np.array(np.arange(0, 3.0*900, 1.0/sample_rate))
        wind = pd.DataFrame(index=data_range)
        wind[twd] = np.sin(2.0*math.pi*data_range*f) * amplitude
        wind[twd] = wind[twd] + np.random.rand(len(data_range)) * 0.6 * amplitude
    else:
        mysql = MYSQL()
        twd = mysql.wind[3]    
        query = "SELECT %s, %s FROM wind WHERE %s='%s'" %(mysql.wind[0], twd, mysql.wind[10], raceID)
        print query
        wind = mysql.fetch_query(query, mysql.wind[0])
    
    # compute short, long rolling average and Std Dev bands    
    short_avg = pd.rolling_mean(wind[twd].values, window=15, min_periods=0)
    long_avg = pd.rolling_mean(wind[twd].values, window=450, min_periods=0)
    wind_std = pd.rolling_std(wind[twd].values, window=450, min_periods=0)
    upper = long_avg + wind_std
    lower = long_avg - wind_std 

    # compute FFT
    size = len(wind)
    print "TWD records: %d" %size
    if size == 0:
        return
        
    fft = FastFourierTransformation(size)
    x = wind[twd].values - long_avg[size-1]
    
    if len(x) < fft.nfft:
        x=np.append(x, np.zeros(fft.nfft-len(x)))

    y = np.zeros(fft.nfft)

    
    print "length of x=%d and y=%d and nfft=%d" %(len(x), len(y), fft.nfft)
    fft.fft(x, y)
    freq = fft.frequency(x, sample_rate)
    period = 1.0 / freq    
    print "Frequency = %.3e HZ  period = %.0f sec or %.1f min" %(freq, period, period/60.0)

    if plot:    
        fig += 1
        x = x[range(int(len(x)/2))]    
        ind = np.linspace(0,len(x/2),len(x/2))
        plt.figure(fig)
        plt.plot(ind,abs(x))
        plt.show()

    
    # try the numpy fft    
    x = wind[twd].values - long_avg
    #if len(x) < fft.nfft:
    #    x=np.append(x, np.zeros(fft.nfft-len(x)))
    
    #print np.argwhere(np.isnan(x))
    fftx = np.fft.fft(x) # the frequency transformed part
    
    # now discard anything  that we do not need..
    fftx = fftx[range(int(len(fftx)/2))]
    
    # now create the frequency axis: it runs from 0 to the sampling rate /2
    freq_fftx = np.linspace(0.0,2.0/sample_rate,len(fftx))
        
    """
    fig += 1    
    ind = np.linspace(0,len(x),len(x))
    plt.plot(freq_fftx,abs(fftx))
    plt.show()
    """


    idx = np.argmax(np.abs(fftx))
    max_val = np.max(np.abs(fftx))
    print "\nlength of x=%d and nfft=%d   idx=%d   power=%.1f" %(len(x), len(fftx), idx, max_val)
    if (idx == 0):
        freq = 0.5 * sample_rate / len(fftx) / 2.0
    else:
        freq = idx * sample_rate / len(fftx) / 2.0
    period = 1.0 / freq    
    print "Frequency = %.3e HZ  period = %.0f sec or %.1f min" %(freq, period, period/60.0)

    if plot:    
        fig += 1    
        ind = wind.index.values
        plt.figure(fig)
        plt.plot(ind, wind[twd].values, 'b', lw=1.0, label='TWD')
        plt.plot(ind, short_avg, 'r', lw=3, label='1-min mean')
        plt.plot(ind, long_avg, 'g', lw=3, label='30-min min')
        plt.plot(ind, upper, 'c-', lw=2.0, label='+1 Std Dev')
        plt.plot(ind, lower, 'c-', lw=2.0, label='-1 Std Dev')
        plt.title('TWD Oscillation Analysis')
        plt.grid()
        plt.xlabel('Time (sec)')
        plt.ylabel('TWD')
        plt.legend()
        plt.show()
    

    
"""
|------------------------------------------------------------------------------------------
|------------------------------------------------------------------------------------------
"""
if __name__ == "__main__":
    print "\n", __version__
    
    #data = run('sine', 'TWD')
    #data = run('square', 'TWD')
    #data = run('set', 'TWD')
    
    #data = run('csv', 'TWD', filename='TWD_raw.csv')
    #data = run('csv', 'TWD', filename='TWD_raw3.csv')
    #test()
    
    #plot_test()

    #for i in range(1,257):
    #    x = nearest_power_of_two(i)
    #    print "%d = %d  -  %d" %(i, x, 2**x)

    #test_fft_vp()
    wind_analysis(raceID=1491627600, plot=True)
    #wind_analysis(raceID=-1, plot=False)

    print "\nDone!"    