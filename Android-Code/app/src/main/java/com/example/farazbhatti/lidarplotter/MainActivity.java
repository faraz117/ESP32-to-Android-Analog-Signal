package com.example.farazbhatti.lidarplotter;

import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;
//import com.xxmassdeveloper.mpchartexample.custom.RadarMarkerView;
//import com.xxmassdeveloper.mpchartexample.notimportant.DemoBase;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

//TODO: Add Name of Customer
//TODO: Add Distance Label to the Graph
//TODO: Add Delay Text View ?
//TODO: Change Color
//TODO: Change Toast Message
//TODO: Change Color to Green
//TODO: Add Previous Sweep to the Graph too

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private LineChart chart;
    EvictingQueue<Integer> time;
    EvictingQueue<Integer> bpm;
    int refresh_counter= 0;
    int flush_counter = 0;
    WebSocket ws = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chart = (LineChart) findViewById(R.id.chart);
        Random r = new Random();
        time = new EvictingQueue<Integer>(100);
        bpm = new EvictingQueue<Integer>(100);
        setChart();
        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);
        // Create a WebSocket. The timeout value set above is used.
        try {
            ws = factory.createSocket("ws://192.168.0.72:81");
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    Log.d("TAG", "onTextMessage: " + message + " " );
                    if(message.equals("!")) {
                        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                        toastMessage("Patient in Danger or Device Removed");
                    }
                    refresh_counter = refresh_counter + 1;
                    flush_counter = flush_counter + 1;
                    try {
                    bpm.add(Integer.parseInt(message));
                    time.add(flush_counter);
                    if(refresh_counter > 2){
                        try {
                            setData(time, bpm);
                            chart.invalidate(); // refresh
                        } catch (Exception e) {
                            // toastMessage(e.toString());
                        }
                        refresh_counter = 0;
                    }}catch(Exception e){

                    }
                }
            });

            ws.connectAsynchronously();
        } catch (IOException e) {
            toastMessage("Unable to Register Listener");
            e.printStackTrace();
        }
        chart = findViewById(R.id.chart);
        callAsynchronousTask();

    }
    private void setChart(){
        // no description text
        chart.getDescription().setEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);

        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.WHITE);
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);
        //set manual x bounds
        chart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date(millis));
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(170f);
        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }
    private void setData(EvictingQueue<Integer> time, EvictingQueue<Integer> bpm) {


        ArrayList<Entry> entries1 = new ArrayList<Entry>();
        // count = hours
        // increment by 1 hour
        for(int i= 0; i < time.size() ; ++i){
            entries1.add(new Entry(time.get(i),bpm.get(i)));
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(entries1, "DataSet 1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1.5f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);

        // create a data object with the data sets
        LineData data = new LineData(set1);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        // set data
        chart.setData(data);
    }
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        final Timer timer = new Timer();
        final TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            // Ask for new data
                            sendMessage();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Toast.makeText(MainActivity.this,e.toString(),
                                    Toast.LENGTH_LONG).show();
                            //Toast.makeText(MainActivity.this,"Please Connect to the LIDAR and restart the app",
                            //        Toast.LENGTH_SHORT).show();

                            timer.cancel();

                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 100); //execute in every 50000 ms
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
    }

    public void sendMessage() {
        if (ws.isOpen()) {
            ws.sendText("Go");
        }
    }

}

