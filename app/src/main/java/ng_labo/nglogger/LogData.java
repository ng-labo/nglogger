package ng_labo.nglogger;

import android.content.Context;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Created by n-ogawa on 2017/09/25.
 */

class LogData {
    class Store  {
        public int network_call_count = 0;
        public int network_fetch_count = 0;
        public int gps_call_count = 0;
        public int gps_fetch_count = 0;
        public long last_call_ts = 0;

        public int step_counter = 0;
        // time to post data last
        public long last_sent_ts;
        public long last_log_ts;
        public float last_log_distance = 0;
        public int skip_log_counter = 0;

        public float last_short_weight_distance;
        public float last_long_weight_distance;

        public long call_gps_duration;
        public long call_network_duration;
        public int break_count;

        public int reset_day;

    }
    final private String path;
    private Store S;
    private Gson gson;

    public LogData(Context c) {
        path = c.getCacheDir()+ File.separator + this.getClass().getName() + ".json";
        S = new Store();
        gson = new Gson();
        setLast_sent_ts(System.currentTimeMillis());
    }

    public void setNetwork_call_count(int i) { S.network_call_count = i;}
    public int getNetwork_call_count() { return S.network_call_count; }
    public void incNetwork_call_count() { S.network_call_count++; }
    public void setNetwork_fetch_count(int i) { S.network_fetch_count = i;}
    public int getNetwork_fetch_count() { return S.network_fetch_count; }
    public void incNetwork_fetch_count() { S.network_fetch_count++; }
    public void setGps_call_count(int i) { S.gps_call_count = i;}
    public int getGps_call_count() { return S.gps_call_count; }
    public void incGps_call_count() { S.gps_call_count++; }
    public void setGps_fetch_count(int i) { S.gps_fetch_count = i;}
    public int getGps_fetch_count() { return S.gps_fetch_count; }
    public void incGps_fetch_count() { S.gps_fetch_count++; }
    public long getCallElapsed(long ts) { return ts - S.last_call_ts; }
    public void setLast_call_ts(long ts) { S.last_call_ts = ts; }

    public void setStep_counter(int i) { S.step_counter = i;}
    public int getStep_counter() { return S.step_counter; }
    public void incStep_counter() { S.step_counter++; }

    public long getLast_sent_ts() { return S.last_sent_ts; }
    public void setLast_sent_ts(long l) { S.last_sent_ts = l; }
    public long getSentElapsed(long ts) { return ts-S.last_sent_ts;}
    public long getLogElapsed(long ts) { return ts-S.last_log_ts; }
    public void setLast_log_ts(long l) { S.last_log_ts = l; }
    public int getLast_log_day() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(S.last_log_ts);
        return (int) c.get(Calendar.DAY_OF_MONTH);
    }
    public int getLast_log_hour() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(S.last_log_ts);
        return (int) c.get(Calendar.HOUR_OF_DAY);
    }
    public void setLast_log_distance(float f) {
        S.last_log_distance = f;
    }
    public float getLast_log_distance() {return S.last_log_distance; }

    public int getSkip_log_counter() {return S.skip_log_counter; }
    public void incSkip_log_counter() { S.skip_log_counter++; }
    public void setSkip_log_counter(int i) { S.skip_log_counter=0; }

    public float getLast_short_weight_distance() { return S.last_short_weight_distance; }
    public void setLast_short_weight_distance(float f) { S.last_short_weight_distance = f; }
    public float getLast_long_weight_distance() { return S.last_long_weight_distance; }
    public void setLast_long_weight_distance(float f) { S.last_long_weight_distance = f; }

    public void addCall_gps_duration(long millisec) { S.call_gps_duration+= millisec;}
    public long getCall_gps_duration() { return S.call_gps_duration; }
    public void addCall_network_duration(long millisec) { S.call_network_duration+= millisec;}
    public long getCall_network_duration() { return S.call_network_duration; }
    public void incBreak_count() { S.break_count++; }
    public int  getBreak_count() { return S.break_count; }

    public int getReset_day() { return S.reset_day; }

    public void reset(int day) {
        setNetwork_fetch_count(0);
        setGps_fetch_count(0);
        setStep_counter(0);
        S.call_gps_duration = 0;
        S.call_network_duration = 0;
        S.break_count = 0;
        S.reset_day = day;
    }

    public void save() throws IOException {
        String json = gson.toJson(S);
        OutputStream os = new FileOutputStream(path);
        os.write(json.getBytes());
        os.close();
    }
    public void load() throws ClassNotFoundException, IOException {
        File f = new File(path);
        int len = (int) f.length();	//Length of file
        byte[] buf = new byte[len];
        InputStream is = new FileInputStream(f);
        is.read(buf);
        is.close();
        S = gson.fromJson(new String(buf), LogData.Store.class);
    }
}
