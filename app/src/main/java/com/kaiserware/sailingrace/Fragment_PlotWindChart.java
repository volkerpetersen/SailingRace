package com.kaiserware.sailingrace;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

/**
 * Helper Class to XY-scatter plot the current wind data stored in the SQLIte DB.
 * This fragment gets initiated on left swipe in the Fragment_RaceInfo
 *
 * Created by Volker Petersen on 10/20/2016.
 */

public class Fragment_PlotWindChart extends Fragment {
    private Context appContext;
    private View view;
    private static final String LOG_TAG = Fragment_PlotWindChart.class.getSimpleName();
    private int size;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_plot_wind_chart, container, false);
        view.setBackgroundColor(Color.BLACK);
        appContext = getActivity().getApplicationContext();
        plotChart();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void plotChart() {
        int line_width_major = 8;
        int line_width_minor = 4;
        int j;
        double ymax = 0.0d;
        double ymin = 0.0d;
        double avg, x, y;
        double sample_rate;
        XYSeries TWD = new XYSeries("TWD");
        XYSeries long_AVG_TWD = new XYSeries("long Avg");
        XYSeries short_AVG_TWD = new XYSeries("60sec Avg");
        XYSeries UPPER = new XYSeries("+1 StdDev");
        XYSeries LOWER = new XYSeries("-1 StdDev");
        XYSeries SIN = new XYSeries("sin()");

        size = NavigationTools.TWD.size();
        sample_rate = (double) Fragment_RaceInfo.screenUpdates;

        // only add a wind oscillation sine wave if the period is at least 3 min long ( <= 1.0/(3.0*60) )
        boolean add_sine_curve = (!Double.isNaN(NavigationTools.wind_frequency) && NavigationTools.wind_frequency <= 1.0d/180.0d);

        for(int i=1; i<size+1; i++) {
            j = i-1;

            /*
            // plot the actual values
            TWD.add((double)(i-size), NavigationTools.TWD.get(j));
            long_AVG_TWD.add((double)(i-size), NavigationTools.TWD_long_AVG.get(j));
            short_AVG_TWD.add((double)(i-size), NavigationTools.TWD_short_AVG.get(j));
            UPPER.add((double)(i-size), NavigationTools.TWD_long_AVG.get(j)+NavigationTools.TWD_Std_Dev.get(j));
            LOWER.add((double)(i-size), NavigationTools.TWD_long_AVG.get(j)-NavigationTools.TWD_Std_Dev.get(j));
            */

            // plot the heading differences around the long-term average TWD
            avg = NavigationTools.TWD_longAVG;
            x = (double)(i+ Fragment_RaceInfo.current_duration-size);  // minus seconds in the past plus duration of current Oscillation
            TWD.add(x, NavigationTools.HeadingDelta(avg, NavigationTools.TWD.get(j)));
            long_AVG_TWD.add(x, NavigationTools.HeadingDelta(avg, NavigationTools.TWD_long_AVG.get(j)));
            short_AVG_TWD.add(x, NavigationTools.HeadingDelta(avg, NavigationTools.TWD_short_AVG.get(j)));
            UPPER.add(x, NavigationTools.HeadingDelta(avg, NavigationTools.TWD_long_AVG.get(j)+NavigationTools.TWD_Std_Dev.get(j)));
            LOWER.add(x, NavigationTools.HeadingDelta(avg, NavigationTools.TWD_long_AVG.get(j)-NavigationTools.TWD_Std_Dev.get(j)));
            
            if ( add_sine_curve ) {
                y = Math.sin(6.28318530717959*NavigationTools.wind_frequency/sample_rate*(NavigationTools.wind_sine_start+i)) * NavigationTools.TWD_Std_Dev.get(size-1);
                SIN.add(x, y);
            }

            if (NavigationTools.TWD_Std_Dev.get(j) > ymax) {
                ymax = NavigationTools.TWD_Std_Dev.get(j);
            }
        }

        // Creating a dataset to hold each series
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        // Adding the different data-series to the dataset
        dataset.addSeries(TWD);
        dataset.addSeries(long_AVG_TWD);
        dataset.addSeries(short_AVG_TWD);
        dataset.addSeries(UPPER);
        dataset.addSeries(LOWER);

        // Creating XYSeriesRenderer to customize each Series
        XYSeriesRenderer twd_Renderer = new XYSeriesRenderer();
        twd_Renderer.setColor(Color.YELLOW);
        //twd_Renderer.setPointStyle(PointStyle.CIRCLE);
        //twd_Renderer.setFillPoints(true);
        twd_Renderer.setStroke(BasicStroke.DOTTED);
        twd_Renderer.setLineWidth(line_width_minor);
        twd_Renderer.setDisplayChartValues(false);
        //XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
        //fill.setColor(Color.YELLOW);
        //twd_Renderer.addFillOutsideLine(fill);

        XYSeriesRenderer long_avg_Renderer = new XYSeriesRenderer();
        long_avg_Renderer.setColor(Color.LTGRAY);
        long_avg_Renderer.setLineWidth(line_width_major);
        long_avg_Renderer.setDisplayChartValues(false);

        XYSeriesRenderer short_avg_Renderer = new XYSeriesRenderer();
        short_avg_Renderer.setColor(Color.RED);
        short_avg_Renderer.setLineWidth(line_width_major);
        short_avg_Renderer.setDisplayChartValues(false);

        XYSeriesRenderer upper_Renderer = new XYSeriesRenderer();
        upper_Renderer.setColor(Color.GREEN);
        //upper_Renderer.setFillPoints(true);
        upper_Renderer.setLineWidth(line_width_major);
        upper_Renderer.setStroke(BasicStroke.DOTTED);
        upper_Renderer.setDisplayChartValues(false);

        XYSeriesRenderer lower_Renderer = new XYSeriesRenderer();
        lower_Renderer.setColor(Color.GREEN);
        lower_Renderer.setLineWidth(line_width_major);
        lower_Renderer.setStroke(BasicStroke.DOTTED);
        lower_Renderer.setDisplayChartValues(false);

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        multiRenderer.setMargins(new int[] {150, 50, 190, 50}); //first value is space between top edge and end of Y-Axe
                                                                //second value is space between left edge and labels of Y-Axe
                                                                //third value is space between bottom edge and labels of X-Axe         //forth value is space between right edge and end of X-Axe
        multiRenderer.setXTitle("Elapsed Time (seconds)");
        multiRenderer.setChartTitle("TWD vs. Long-Avg");
        multiRenderer.setChartTitleTextSize(80);
        multiRenderer.setLegendTextSize(50);
        multiRenderer.setLabelsTextSize(50);
        multiRenderer.setAxisTitleTextSize(70);
        multiRenderer.setZoomButtonsVisible(false);
        multiRenderer.setShowGrid(true);
        multiRenderer.setGridColor(Color.WHITE);

        // Adding the various Series Renderer to the multipleRenderer
        // Note: The order of adding dataseries to dataset and
        // renderers to multipleRenderer should be same
        multiRenderer.addSeriesRenderer(twd_Renderer);
        multiRenderer.addSeriesRenderer(long_avg_Renderer);
        multiRenderer.addSeriesRenderer(short_avg_Renderer);
        multiRenderer.addSeriesRenderer(upper_Renderer);
        multiRenderer.addSeriesRenderer(lower_Renderer);

        if (add_sine_curve) {
            dataset.addSeries(SIN);
            XYSeriesRenderer sin_Renderer = new XYSeriesRenderer();
            sin_Renderer.setColor(Color.CYAN);
            sin_Renderer.setStroke(BasicStroke.DOTTED);
            sin_Renderer.setLineWidth(line_width_major);
            sin_Renderer.setDisplayChartValues(false);
            multiRenderer.addSeriesRenderer(sin_Renderer);
        }

        ymin = NavigationTools.HeadingDelta(0.0d, -1.5*ymax);
        ymax = NavigationTools.HeadingDelta(0.0d, 1.5*ymax);
        //Log.d(LOG_TAG, "ymin="+ymin+"  ymax="+ymax);
        multiRenderer.setYAxisMax(ymax);
        multiRenderer.setYAxisMin(ymin);

        GraphicalView chartView = ChartFactory.getLineChartView(appContext, dataset, multiRenderer);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.chart_container);
        layout.addView(chartView);

        //chartView.repaint();

    }
}
