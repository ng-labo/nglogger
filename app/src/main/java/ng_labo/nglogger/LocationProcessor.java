package ng_labo.nglogger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created on 2017/09/21.
 */

public class LocationProcessor {
    private static String LOGTAG = "nglabo_LP";

    //
    // location process
    //
    private LocationManager locationManager;

    private LocationListener listener;
    private Context context;

    private long last_gps_call = 0;
    private long last_network_call = 0;

    public LocationProcessor(LocationListener listener, Context context) {
        this.listener = listener;
        this.context = context;
    }

    public void locationStart() {
        Log.d(LOGTAG, "locationStart()");

        // generate instance of LocationManager
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(LOGTAG, "gpsEnabled="+gpsEnabled);
        Log.d(LOGTAG, "networkEnabled="+networkEnabled);

        //requestLocation(minTime, minDistance);
    }

    private boolean noPermisison() {
        return (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
    }
    public void requestLocationNetwork(final long ts, final long minTime,final float minDistance){
        Log.d(LOGTAG, "requestLocationUpdatesNetwork("+minTime+","+minDistance+")");
        if(noPermisison()){
            Log.d(LOGTAG, "requestLocationUpdatesNetwork no permission");
            return;
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, minTime, minDistance, listener, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(LOGTAG, "requestLocationNetwork", e);
            return;
        }
        if(last_network_call==0) {
            last_network_call = ts;
        }
    }

    public void requestLocationGPS(final long ts, final long minTime,final float minDistance) {
        Log.d(LOGTAG, "requestLocationUpdatesGPS("+minTime+","+minDistance+")");
        if(noPermisison()){
            Log.d(LOGTAG, "requestLocationUpdatesGPS no permission");
            return;
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, minTime, minDistance, listener, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(LOGTAG, "requestLocationUpdatesGPS", e);
            return;
        }
        if(last_gps_call==0) {
            last_gps_call = ts;
        }
    }

    public long[] breakLocation(final long ts){
        if(noPermisison()){
            return null;
        }
        try {
            locationManager.removeUpdates(listener);
        } catch (SecurityException e) {
            Log.e(LOGTAG, "breakLocation", e);
            return null;
        }
        long[] ret = new long[2];
        if(last_gps_call>0){
            ret[0] = ts - last_gps_call;
            last_gps_call = 0;
        }
        if(last_network_call>0){
            ret[1] = ts - last_network_call;
            last_network_call = 0;
        }
        return ret;
    }

    public Location getLastKnowLocation() {
        if(noPermisison()){
            Log.d(LOGTAG, "getLastKnowLocation no permission");
            return null;
        }
        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.e(LOGTAG, "getLastKnownLocation", e);
        }
        if(location!=null) return location;
        try {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {
            Log.e(LOGTAG, "getLastKnownLocation", e);
        }

        return location;
    }

}
