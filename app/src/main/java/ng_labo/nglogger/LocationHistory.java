package ng_labo.nglogger;

import android.location.Location;

/**
 * Created by n-ogawa on 2017/10/08.
 */

public class LocationHistory {
    private final int RING_SZ = 20;
    private final int RING_SHORT_SZ = 5;
    private final double ring_lat[] = new double[RING_SZ];
    private final double ring_lon[] = new double[RING_SZ];
    private final long ring_ts[] = new long[RING_SZ];
    private int ring_counter = 0;
    private double avg(final double[] a,final int counter) {
        if(counter==0) return -999.9;
        double sum = 0.0;
        for(int i=0;i<a.length;i++){
            sum += a[i];
        }
        if(counter<a.length){
            return sum / counter;
        }
        return sum / a.length;
    }
    public void add(Location l) {
        ring_lat[ring_counter%RING_SZ] = l.getLatitude();
        ring_lon[ring_counter%RING_SZ] = l.getLongitude();
        ring_ts[ring_counter%RING_SZ] = l.getTime();

        ring_counter++;
    }
    public double latShortAvg() {
        if(ring_counter==0)return -999.0;
        double[] f = new double[RING_SHORT_SZ];
        int counter = ring_counter-1;
        if(counter<=RING_SHORT_SZ){
            for(int i=0;i<counter;i++) f[i]=ring_lat[i];
            return avg(f, counter);
        }
        for(int i=0;i<RING_SHORT_SZ;i++) f[(counter-RING_SHORT_SZ+i)%RING_SHORT_SZ]=ring_lat[i];
        return avg(f, RING_SHORT_SZ);
    }
    public double lonShortAvg() {
        if(ring_counter==0)return -999.0;
        double[] f = new double[RING_SHORT_SZ];
        int counter = ring_counter-1;
        if(counter<=RING_SHORT_SZ){
            for(int i=0;i<counter;i++) f[i]=ring_lon[i];
            return avg(f, counter);
        }
        for(int i=0;i<RING_SHORT_SZ;i++) f[(counter-RING_SHORT_SZ+i)%RING_SHORT_SZ]=ring_lon[i];
        return avg(f, RING_SHORT_SZ);
    }
    public double latLongAvg() {
        return avg(ring_lat, ring_counter);
    }
    public double lonLongAvg() {
        return avg(ring_lon, ring_counter);
    }

    /*public long tsshortdiff() {
        if(ring_counter==0) return 0;
        return ring_short_ts[(ring_counter-1)%RING_SHORT_SZ] - ring_short_ts[ring_counter%RING_SHORT_SZ];
    }

    public long tslongdiff() {
        if(ring_counter<2) return 0;
        return ring_ts[(ring_counter-1)%RING_SZ] - ring_ts[(ring_counter-2)%RING_SZ];
    }*/

    public float longWeightDistance(Location l){
        float[] r = new float[3];
        double lat = latLongAvg();
        if(lat<-360.0){
            return 0.0f;
        }
        Location.distanceBetween(lat, lonLongAvg(),
                l.getLatitude(), l.getLongitude(), r);
        return r[0];
    }

    public float shortWeightDistance(Location l){
        float[] r = new float[3];
        double lat = latShortAvg();
        if(lat<-360.0){
            return 0.0f;
        }
        Location.distanceBetween(lat, lonShortAvg(),
                l.getLatitude(), l.getLongitude(), r);
        return r[0];
    }
}
