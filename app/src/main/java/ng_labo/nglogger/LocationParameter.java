package ng_labo.nglogger;

import android.location.Location;

/**
 * Created by n-ogawa on 2017/09/27.
 */

public class LocationParameter {
    LocationHistory locationHistory = new LocationHistory();
    Location last_location;
    public LocationParameter(){
    }

    public void add(Location l) {
        last_location = l;
        locationHistory.add(l);
    }
    public Location getLast_location() { return last_location; }
    public double getLastLat() { return last_location.getLatitude();}
    public double getLastLon() { return  last_location.getLongitude();}
    public float getLastAccuracy() { return last_location.getAccuracy();}
    public long getLastTs() {
        if(last_location==null)return 0;
        return last_location.getTime();
    }
    public long getElapsedFromLastTs(long ts) {
        if(last_location==null)return 0;
        return ts - last_location.getTime();
    }


    public float longWeightDistance(Location l){ return locationHistory.longWeightDistance(l); }
    public float shortWeightDistance(Location l){ return locationHistory.shortWeightDistance(l); }

    private String gpsStatus = LazyService.AVAILABLE;
    private String networkStatus = LazyService.AVAILABLE;
    public void setGpsStatus(String s) { gpsStatus = s;}
    public void setNetworkStatus(String s) { networkStatus = s;}
    public boolean isGpsAvailable() { return LazyService.AVAILABLE.equals(gpsStatus); }
    public boolean isNetworkAvailable() { return LazyService.AVAILABLE.equals(networkStatus); }
}


