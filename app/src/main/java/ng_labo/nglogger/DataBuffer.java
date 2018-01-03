package ng_labo.nglogger;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by n-ogawa on 2017/09/16.
 */

public class DataBuffer {
    private StringBuffer buffer;
    private String cachefile;
    public DataBuffer(Context context) {
        buffer = new StringBuffer();
        cachefile = context.getFilesDir()+ File.separator + "store"; //? context.getCacheDir();
        try {
            File f = new File(cachefile);
            int len = (int) f.length();	//ファイルの長さを取得
            byte[] buf = new byte[len];
            InputStream is = new FileInputStream(f);
            is.read(buf);
            is.close();
            buffer = new StringBuffer(new String(buf));
        } catch (FileNotFoundException e) {
            // igonore
        } catch (IOException e){
        }
    }
    public void append(String s) {
        buffer.append(s);
        try {
            File f = new File(cachefile);
            OutputStream os = new FileOutputStream(f);
            os.write(buffer.toString().getBytes());
            os.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e){

        }
    }
    public void clear() {
        buffer = new StringBuffer();
        File f = new File(cachefile);
        f.delete();
    }
    public String toString() {
        return buffer.toString();
    }
    public long length() {
        return buffer.length();
    }
}
