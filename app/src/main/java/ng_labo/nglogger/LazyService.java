package ng_labo.nglogger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;

import java.io.IOException;
import java.util.Calendar;

/**
 * Created by n-ogawa on 2017/02/19.
 */

public class LazyService extends Service implements LocationListener, SensorEventListener, PostDataTaskCallback {

    private static final String LOGTAG = "nglabo_LS";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LazyService getService() {
            Log.d(LOGTAG, "LocalBinder::getService");
            // Return this instance of LocalService so clients can call public methods
            return LazyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        String dbg = "#onBind";
        mylog(dbg);
        return mBinder;
    }

    private MainTaskThread mainTaskThread;

    private SensorManager sensorManager;
    private StepDetector simpleStepDetector;
    private Sensor accel;

    // buffer for posting data
    private DataBuffer buffer;
    //
    private LogData logData;

    private LocationProcessor locationProcessor;
    private LocationParameter locationParameter = new LocationParameter();


    public void mylog(String s){
        Log.d(LOGTAG, s);
        if(dbglog==-1)return;
        boolean dolog=false;
        if(dbglog==0){
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            dolog = wifi.isWifiEnabled();
            Log.d(LOGTAG, "wifi.isWifiEnabled() =" + dolog);
        }
        dolog |= (dbglog==1);

        if(dolog) {
            Calendar c = Calendar.getInstance();
            s = String.format("#%d:%d:%d:",
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)) + s;
            new PostDataTask(mkUrl(), s, mkAuth()).start();
        }
    }
    NotificationCompat.Builder notifyBuilder;
    NotificationManager notificationManager;
    final int notifyID = 8686;
    @Override
    public void onCreate() {
        String dbg = "#onCreate";
        mylog(dbg);
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, AppCompatActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        notifyBuilder = new NotificationCompat.Builder(this);
        notifyBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notifyBuilder.setContentTitle("nglogger");
        notifyBuilder.setContentText("On Create ...");
        notifyBuilder.setContentIntent(pendingIntent);
        startForeground(notifyID, notifyBuilder.build());

        // refer preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String _pref_device_name = sharedPref.getString("pref_device_name", "");
        Log.d(LOGTAG, "_pref_device_name=" + _pref_device_name);
        if (_pref_device_name.length() > 0) {
            pref_device_name = _pref_device_name;
        }

        String _pref_server_url = sharedPref.getString("pref_server_url", "");
        Log.d(LOGTAG, "_pref_server_url=" + _pref_server_url);
        if (_pref_server_url.length() > 0) {
            pref_uri_template = _pref_server_url + "?%s";
        }

        try {
            long n = Long.parseLong(sharedPref.getString("pref_postSizeMax", "2000"));
            if (n > 0 && n < 65535) {
                postSizeMax = n;
            }
        } catch (Exception e) {
        }
        Log.d(LOGTAG, "postSizeMax=" + postSizeMax);

        try {
            pref_thresold_step = Integer.parseInt(sharedPref.getString("pref_thresold_step", "10"));
            if (pref_thresold_step < 1) pref_thresold_step = 10;
        } catch (Exception e) {
        }
        Log.d(LOGTAG, "pref_thresold_step=" + pref_thresold_step);

        try {
            pref_thresold_sensing_motion = Integer.parseInt(sharedPref.getString("pref_thresold_sensing_motion", "20"));
            if (pref_thresold_sensing_motion < 1) pref_thresold_sensing_motion = 20;
        } catch (Exception e) {
        }
        Log.d(LOGTAG, "pref_thresold_sensing_motion=" + pref_thresold_sensing_motion);

        try {
            dbglog = Integer.parseInt(sharedPref.getString("pref_dbglog", "-1"));
            if (dbglog < -1 || dbglog > 1) dbglog = -1;
        } catch (Exception e) {
        }
        Log.d(LOGTAG, "pref_dbglog=" + dbglog);

        // initialize pedometer
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        int accel_ring_sz = 50;
        int vel_ring_sz = 10;
        float step_th = (float) pref_thresold_step;
        int step_delay_ns = 250 * 1000 * 1000;

        simpleStepDetector = new StepDetector(accel_ring_sz, vel_ring_sz, step_th, step_delay_ns);
        simpleStepDetector.registerListener(this);

        buffer = new DataBuffer(this);
        logData = new LogData(this);
        try {
            logData.load();
        } catch (Exception e) {
            logData.setLast_sent_ts(System.currentTimeMillis());
            Log.e(LOGTAG, "Failed to restore LogData", e);
        }
        Log.d(LOGTAG, "last_sent_ts=" + logData.getLast_sent_ts());
        if(logData.getReset_day()==0){
            logData.reset(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        }

        //
        locationProcessor = new LocationProcessor(this, this);
        locationProcessor.locationStart();

        Location lastlocation = locationProcessor.getLastKnowLocation();
        if (lastlocation != null) {
            locationParameter.add(lastlocation);
        }

        //
        initializeMeasuringBattery();

        //
        if (mainTaskThread != null) {
            mainTaskThread.terminate();
        }
        mainTaskThread = new MainTaskThread();
        mainTaskThread.start();

    }

    private class MainTaskThread extends Thread {
        private boolean alive = true;
        private MainTaskThread(){
            super();
        }
        private void terminate(){
            alive = false;
        }
        private void firstTask() {
            final long cts = System.currentTimeMillis();

            location_fetch_strategy = 0;
            final long init_mintTime = getNext_minTime();
            final float init_minDistance = getNext_minDistance();

            next_ts = cts + getNext_ts_delta();

            locationProcessor.requestLocationNetwork(cts, init_mintTime, init_minDistance);
            on_call_network = true;
            mylog(String.format("requestLocationNetwork(%d,%f)",init_mintTime, init_minDistance));
            locationProcessor.requestLocationGPS(cts, init_mintTime, init_minDistance);
            on_call_gps = true;
            mylog(String.format("requestLocationGPS(%d,%f)",init_mintTime, init_minDistance));
            logData.setLast_call_ts(cts);

        }
        public void run() {
            firstTask();
            while(alive) {
                try {
                    final long cts = System.currentTimeMillis();
                    stepnum[(stepnum_idx++)%stepnum_sz] = step_counter;
                    step_counter=0;

                    periodicProcessingData(cts, stepscore());

                }catch (Exception e){
                    Log.e(LOGTAG, "In MainTaskThread", e);
                }

                try {
                    Thread.sleep(interval);
                } catch (Exception e) {
                    Log.e(LOGTAG, "In MainTaskThread", e);
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOGTAG,"MainTaskThread terminate");
        }
        private int step_counter=0;
        private int stepnum_idx=0;
        private final int stepnum_sz = 6;
        private final int stepnum[] = new int[stepnum_sz];
        // call in Sensor callback function
        public void step() {
            step_counter++;
        }

        private int stepscore() {
            int r = 0;
            for(int i=0;i<stepnum_sz;i++) r += stepnum[i];
            return r;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String dbg = "#onStartCommand:flags=" + flags + "startId=" + startId + "intent=" + intent;
        try {
            mylog(dbg);
            //
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);

            // Gets data from the incoming Intent
            if (intent != null) {
                Log.d(LOGTAG, String.format("Service::startService(),dataString=%s", intent.getDataString()));
            }
                {
                StringBuffer sb = new StringBuffer();
                sb.append("#");
                sb.append("device=" + pref_device_name);
                sb.append(",postIntervalMax=" + postIntervalMax);
                sb.append(",logIntervalMax=" + logIntervalMax);
                sb.append(",postSizeMax=" + postSizeMax);
                sb.append("\n");
                new PostDataTask(mkUrl(), sb.toString(), mkAuth()).start();
            }
        } catch(Exception ex) {
            Log.e(LOGTAG, dbg , ex);
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        String dbg = "#onDestroy";
        mylog(dbg);
        //
        sensorManager.unregisterListener(this);
        //
        mainTaskThread.terminate();
        try {
            mainTaskThread.join();
        } catch (Exception e) {
            Log.d(LOGTAG, "mainTaskThread.join()",e);
        }
        // push out buffer data
        processBufferedData(logData.getLast_sent_ts() + postIntervalMax + 1);
        //
        long[] r = locationProcessor.breakLocation(System.currentTimeMillis());
        if(r!=null){
            logData.addCall_gps_duration(r[0]);
            logData.addCall_network_duration(r[1]);
            logData.incBreak_count();
        }
        try {
            logData.save();
        }catch(IOException e){
            Log.d(LOGTAG, "logData.save()",e);
        }
    }

    private long interval = 10000; // millisecond
    private long next_ts = 0; // next timestamp to update

    private long postIntervalMax = 10800 * 1000; // millisecond
    private long logIntervalMax = 1800 * 1000; // millisecond

    private long postSizeMax = 2000; // byte

    private int staying_distance = 150;  // meter
    private int moving_distance = 1000; // meter

    //
    private static String pref_device_name = null;
    // url to post data
    private static String pref_uri_template = null;

    private String mkUrl() {
        if(pref_device_name==null){
            return null;
        }
        return String.format(pref_uri_template, pref_device_name);
    }
    private String mkAuth() {
        return android.util.Base64.encodeToString("user:password".getBytes(),0);
    }

    private int dbglog = 0; //1;always,0:when wifi available,-1:never

    private int pref_thresold_step = 10; // velocity?
    private int pref_thresold_sensing_motion = 20; // count number in interval

    public static final String AVAILABLE = "AVAILABLE";
    public static final String OUT_OF_SERVICE = "OUT_OF_SERVICE";
    public static final String TEMPORARILY_UNAVAILABLE = "TEMPORARILY_UNAVAILABLE";

    public static final String GPS = "gps"; // provider
    public static final String NETWORK = "network"; // provider

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        String s = "";
        switch (status) {
            case LocationProvider.AVAILABLE:
                s = AVAILABLE;
                break;
            case LocationProvider.OUT_OF_SERVICE:
                s = OUT_OF_SERVICE;
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                s = TEMPORARILY_UNAVAILABLE;
                break;
        }
        Log.d(LOGTAG, "onStatusChanged:provider=" + provider + ",status=" + s );
        if(GPS.equals(provider)){
            locationParameter.setGpsStatus(s);
        }
        if(NETWORK.equals(provider)){
            locationParameter.setNetworkStatus(s);
        }
    }

    //private Location last_location = null;
    private int location_fetch_strategy = 0;
    private boolean on_call_gps = false;
    private boolean on_call_network = false;

    private long getNext_ts_delta() {
        if(location_fetch_strategy ==0) return 120000;
        if(location_fetch_strategy ==1) return 600000;
        if(location_fetch_strategy ==2) return 120000;
        if(location_fetch_strategy ==3) return 60000;
        return 600000;
    }
    private long getNext_minTime() {
        if(location_fetch_strategy ==0) return 115000;
        if(location_fetch_strategy ==1) return 595000;
        if(location_fetch_strategy ==2) return 115000;
        if(location_fetch_strategy ==3) return 55000;
        return 600000;

    }
    private float getNext_minDistance() {
        if(location_fetch_strategy ==0) return 100;
        if(location_fetch_strategy ==1) return 200;
        if(location_fetch_strategy ==2) return 500;
        if(location_fetch_strategy ==3) return 800;
        return 1000;

    }
    @Override
    public void onLocationChanged(Location location) {
        long ts = location.getTime();
        Log.d(LOGTAG, "onLocationChanged:" + location.toString());

        // coming means future time strangely
        if(ts>(System.currentTimeMillis()+10000)) {
            mylog("# invalid location.getTime():"+ts);
            return;
        }

        // coming is older than previous fetched
        if(locationParameter.getLast_location()!=null &&
           locationParameter.getLast_location().getTime()>ts) {
            long d = (locationParameter.getLast_location().getTime()-ts);
            mylog("# older location.getTime():"+location.getProvider()+":"+d+"msec");
            return;
        }

        boolean pass_this_location = false;
        float shortWeightDis=-1, longWeightDis=-1;

        if(GPS.equals(location.getProvider())) {
            if(locationParameter.getElapsedFromLastTs(ts)<120000) {
                if (location.getAccuracy() > 50) {
                    pass_this_location = true;
                }
            }else if(locationParameter.getElapsedFromLastTs(ts)<300000){
                if(location.getAccuracy()>100){
                    pass_this_location = true;
                }
            }else if(locationParameter.getElapsedFromLastTs(ts)<600000){
                if(location.getAccuracy()>200){
                    pass_this_location = true;
                }
            }
            if(!pass_this_location) {
                logData.incGps_fetch_count();
                shortWeightDis = locationParameter.shortWeightDistance(location);
                longWeightDis = locationParameter.longWeightDistance(location);
            }
        }
        if(NETWORK.equals(location.getProvider())) {
            if(locationParameter.getElapsedFromLastTs(ts)<120000) {
                if (location.getAccuracy() > 200) {
                    pass_this_location = true;
                }
            }else if(locationParameter.getElapsedFromLastTs(ts)<300000){
                if(location.getAccuracy()>500){
                    pass_this_location = true;
                }
            }else if(locationParameter.getElapsedFromLastTs(ts)<600000){
                if(location.getAccuracy()>1000){
                    pass_this_location = true;
                }
            }
            if(!pass_this_location) {
                logData.incNetwork_fetch_count();
                shortWeightDis = locationParameter.shortWeightDistance(location);
                longWeightDis = locationParameter.longWeightDistance(location);
            }
        }
        if(locationParameter.getLast_location()==null){
            pass_this_location = false;
        }

        String s = String.format("%d,%2.6f,%3.6f,%d,%d,%d,%.3f,%f,%s,%d,%d,%d\n",
                ts/1000 ,location.getLatitude(), location.getLongitude(),
                level, logData.getSkip_log_counter(), logData.getStep_counter(),
                location.getSpeed(), location.getAccuracy(), location.getProvider(),
                (pass_this_location) ? 1 : 0,
                logData.getGps_fetch_count(), logData.getNetwork_fetch_count());

        // store locaiton at first time
        if(locationParameter.getLast_location()==null){
            location_fetch_strategy = 0;
            buffer.append(s);
            next_ts = ts + getNext_ts_delta();
            locationParameter.add(location);
            return;
        }

        float r[] = new float[3];
        Location.distanceBetween(locationParameter.getLastLat(), locationParameter.getLastLon() ,
                location.getLatitude(), location.getLongitude(), r);
        float dis = r[0];

        String dbgmsg = String.format("%s dis=%.2f w_dis(%.2f,%.2f) %d\n",
                location.getProvider(), dis, shortWeightDis, longWeightDis,
                pass_this_location ? 1 : 0);
        if(((!pass_this_location)&&(dis>staying_distance))||
            (dis>location.getAccuracy()&&dis>staying_distance)||
             logData.getLogElapsed(ts)>logIntervalMax){
            logData.setLast_log_ts(ts);
            logData.setLast_log_distance(dis);
            logData.setLast_short_weight_distance(shortWeightDis);
            logData.setLast_long_weight_distance(longWeightDis);
            logData.setSkip_log_counter(0);
            logData.setGps_fetch_count(0);
            logData.setNetwork_fetch_count(0);
            //mylog(dbgmsg);
            //buffer.append(s);
            locationParameter.add(location);
        }else {
            logData.incSkip_log_counter();
        }
        // redundancy output
        mylog(dbgmsg);
        buffer.append(s); // to put only logging

        if(dis<staying_distance){
            location_fetch_strategy = 1; // stable
        }else if(dis<moving_distance){
            location_fetch_strategy = 2; // moving slowly
        }else{
            location_fetch_strategy = 3; // moving fast
        }

        next_ts = getNext_ts_delta() + ts;

        try {
            logData.save();
        }catch(Exception e){
            Log.e(LOGTAG, "Failed to save LogData", e);
        }

    }

    /*

    periodic location sensing and upload history data

    - Probing location data
    - Buffered data are send to server

    */

    private void periodicProcessingData(long ts, int stepscore) {
        probingLocation(ts, stepscore);
        processBufferedData(ts);
    }

    // call request location if depend on condition
    private void probingLocation(final long ts, final int stepscore) {
        Log.d(LOGTAG, "gps=(" + on_call_gps + "," + shouldTryGPS(ts, stepscore) +
                ") network=(" + on_call_network + "," + shouldTryNetwork(ts, stepscore) +
                "),(next_ts - ts)=" + (next_ts - ts)/1000);
        if (next_ts < ts) {
            next_ts += getNext_ts_delta();
            if(on_call_gps == true || on_call_network == true) {
                boolean reason1 = (logData.getSkip_log_counter()>((int)(getNext_ts_delta()*10.0/600000.0))); // 20 when ntext_ts_delta=600000
                boolean reason2 = (logData.getLogElapsed(ts)> 600000); // getNext_ts_delta() * 2
                if(reason2){
                    Location l = locationProcessor.getLastKnowLocation();
                    if(l!=null){
                        float[] r = new float[3];
                        Location.distanceBetween(l.getLatitude(),l.getLongitude(),
                                locationParameter.getLastLat(),locationParameter.getLastLon(),r);
                        if(r[0]>1000.0){ // XXX TODO
                            reason2=false;
                        }
                    }
                }
                boolean reason3 = !(locationParameter.isGpsAvailable()||locationParameter.isNetworkAvailable());
                if (reason1 || reason2 || reason3) {
                    long[] r = locationProcessor.breakLocation(ts);
                    if (r != null) {
                        logData.addCall_gps_duration(r[0]);
                        logData.addCall_network_duration(r[1]);
                        logData.incBreak_count();
                    }
                    mylog(String.format("breakLocation() : %d,%d,%d",
                            reason1 ? 1 : 0, reason2 ? 1 : 0, reason3 ? 1 : 0));
                    on_call_network = false;
                    on_call_gps = false;
                }
                return;
            }

            if (on_call_gps == false) {
                if (shouldTryGPS(ts, stepscore) &&
                        (locationParameter.getLastTs() + getNext_ts_delta()) < ts) {
                    locationProcessor.requestLocationGPS(ts, getNext_minTime(), getNext_minDistance());
                    logData.setLast_call_ts(ts);
                    logData.incGps_call_count();
                    on_call_gps = true;
                    mylog(String.format("requestLocationGPS1(%d,%f)",getNext_minTime(), getNext_minDistance()));
                }
            } else {
                if (shouldTryGPS(ts, stepscore) &&
                        (locationParameter.getLastTs() + getNext_ts_delta() * 6) < ts) {
                    locationProcessor.requestLocationGPS(ts, getNext_minTime(), getNext_minDistance());
                    logData.setLast_call_ts(ts);
                    logData.incGps_call_count();
                    on_call_gps = true;
                    mylog(String.format("requestLocationGPS2(%d,%f)",getNext_minTime(), getNext_minDistance()));
                }
            }
            if (on_call_network == false) {
                if (shouldTryNetwork(ts, stepscore) &&
                        (locationParameter.getLastTs() + getNext_ts_delta()) < ts
                        ) {
                    locationProcessor.requestLocationNetwork(ts, getNext_minTime(), getNext_minDistance());
                    logData.setLast_call_ts(ts);
                    logData.incNetwork_call_count();
                    on_call_network = true;
                    mylog(String.format("requestLocationNetwork1(%d,%f)",getNext_minTime(), getNext_minDistance()));
                }
            } else {
                if (shouldTryNetwork(ts, stepscore) &&
                        (locationParameter.getLastTs() + getNext_ts_delta() * 6) < ts) {
                    locationProcessor.requestLocationNetwork(ts, getNext_minTime(), getNext_minDistance());
                    logData.setLast_call_ts(ts);
                    logData.incNetwork_call_count();
                    on_call_network = true;
                    mylog(String.format("requestLocationNetwork2(%d,%f)",getNext_minTime(), getNext_minDistance()));
                }
            }

        }
        Calendar c = Calendar.getInstance();
        String t1 = String.format("[%d][%02d:%02d]",logData.getStep_counter(),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        String t2 = String.format("[%s,%s][%d][%.2f,%.2f,%.2f][%d,%d]",
                /*locationParameter.isGpsAvailable() ?*/ (on_call_gps ? "+" : "-") /*: ""*/,
                locationParameter.isNetworkAvailable() ? (on_call_network ? "+" : "-") : "",
                location_fetch_strategy,logData.getLast_log_distance(),logData.getLast_short_weight_distance(),logData.getLast_long_weight_distance(),
                logData.getLogElapsed(ts)/1000,logData.getSentElapsed(ts)/1000);
        notificationManager.notify(notifyID, notifyBuilder.setContentTitle(t1).setContentText(t2).build());

    }

    private boolean shouldTryGPS(long ts, int stepscore) {
        if(this.level<15){
            return false;
        }
        /*if(!locationParameter.isGpsAvailable()){
            return false;
        }*/
        if(pref_thresold_sensing_motion<stepscore){
            return true;
        }
        if(logData.getLast_log_distance()>500){
            return true;
        }
        if((ts- locationParameter.getLastTs())>logIntervalMax){
            return true;
        }
        return false;
    }

    private boolean shouldTryNetwork(long ts, int stepscore) {
        if(this.level<15){
            return false;
        }
        if(!locationParameter.isNetworkAvailable()){
            return false;
        }
        if(pref_thresold_sensing_motion<stepscore){
            return true;
        }
        if(logData.getLast_log_distance()>500){
            return true;
        }
        if((ts-locationParameter.getLastTs())>logIntervalMax){
            return true;
        }
        return false;
    }

    // periodic process for buffered history data
    private void processBufferedData(long ts) {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        int reason = 0;
        if(buffer.length()>postSizeMax) reason = 1;
        if(logData.getSentElapsed(ts)>postIntervalMax) reason |= 2;
        if( hour==23) reason |= 4;

        Log.d(LOGTAG, "processData:" + reason + ":buflen=" + buffer.length() + ":dt=" + logData.getSentElapsed(ts));
        if(reason>0 && buffer.length()>0) {
            new PostDataTask(mkUrl(), buffer.toString(), mkAuth(), this).start();
        }

        if(logData.getReset_day() != day) {
            String s = String.format("#reseting-LogData:duration(gps,network)=(%d sec,%d sec) count(gps,network,break)=(%d,%d,%d)\n",
                    logData.getCall_gps_duration()/1000, logData.getCall_network_duration()/1000,
                    logData.getGps_call_count(), logData.getNetwork_call_count(), logData.getBreak_count());
            new PostDataTask(mkUrl(), s, mkAuth(), this).start();
            logData.reset(day);
        }
        if(logData.getLast_log_hour() != hour) {
            try {
                logData.save();
            } catch (Exception e) {
                Log.e(LOGTAG, "Failed to save LogData");
            }
        }
    }


    // interface PostDataTaskCalback
    public void finished(boolean rc, String msg){
        // debug print
        String ss = buffer.toString();
        if(buffer.length()>80){
            ss = ss.substring(0,77) + "...";
        }
        Log.d(LOGTAG, "PostDataTask finished:"+rc);
        Log.d(LOGTAG, ">>>" + ss);
        Log.d(LOGTAG, "<<<" + msg);

        if(rc){
            buffer.clear();
            logData.setLast_sent_ts(System.currentTimeMillis());
        }else{
            if (buffer.length() > 65535) {
                // TODO how to recover
                buffer.clear();
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /*
     to get battery level
      */
    boolean mBroadcastReceiverResisted = false;
    private static int level=0;

    private void initializeMeasuringBattery() {
        Log.d(LOGTAG, "initializeMeasuringBattery()");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);
        mBroadcastReceiverResisted = true;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                level = intent.getIntExtra("level", 0);
                // available items
                // status = intent.getIntExtra("status", 0);
                //health = intent.getIntExtra("health", 0);
                //boolean present = intent.getBooleanExtra("present", false);
                // scale = intent.getIntExtra("scale", 0);
                //int icon_small = intent.getIntExtra("icon-small", 0);
                //int plugged = intent.getIntExtra("plugged", 0);
                //int voltage = intent.getIntExtra("voltage", 0);
                //int temperature = intent.getIntExtra("temperature", 0);
                //String technology = intent.getStringExtra("technology");
            }
        }
    };

    /*
     acceler sensor processing
      */
    /** timestamp of last data */
    private long last_step_ts=0;

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(LOGTAG,"onAccuracyChanged");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.v(LOGTAG,"onSensorChanged");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.d(String.format("#x,%d,%f,%f,%f\n",event.timestamp,event.values[0], event.values[1], event.values[2]));
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    public void step(long timeNs) {
        logData.incStep_counter();
        last_step_ts = System.currentTimeMillis();
        mainTaskThread.step();
    }
}
