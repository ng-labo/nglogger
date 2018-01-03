package ng_labo.nglogger;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * Created by n-ogawa on 2017/09/16.
 */

public class PostDataTask extends Thread {
    private String uri;
    private String s;
    private String edc;
    private PostDataTaskCallback cb;

    public PostDataTask(String uri, String s, String edc, PostDataTaskCallback cb){
        this.uri = uri;
        this.s = s;
        this.edc = edc;
        this.cb = cb;
    }
    public PostDataTask(String uri, String s, String edc){
        this(uri, s, edc, null);
    }
    public void run() {
        if(uri==null&&cb!=null) {
            cb.finished(false, "<<not sent>>");
            return;
        }
        HttpsURLConnection con = null;
        try {
            URL url = new URL(uri);

            con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            // Create the SSL connection
            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());
            con.setSSLSocketFactory(sc.getSocketFactory());

            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Accept-Language", "jp");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/text; charset=utf-8");
            if(edc != null) {
                con.setRequestProperty("Authorization", "Basic " + edc);
            }

            PrintStream ps = new PrintStream(con.getOutputStream());
            ps.print(s);
            ps.close();
            InputStream in = new BufferedInputStream(con.getInputStream());
            StringBuffer rt = new StringBuffer();
            while (in.available() > 0) {
                rt.append(String.valueOf(in.read()));
            }
            if(cb!=null) {
                cb.finished(true, rt.toString());
            }
        } catch(Exception e) {
            if(cb!=null) {
                cb.finished(false, e.toString());
            }
        } finally {
            if(con!=null) {
                con.disconnect();
            }
        }
    }
}

