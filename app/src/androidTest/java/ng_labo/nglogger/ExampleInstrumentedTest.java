package ng_labo.nglogger;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("code.sakura.n_ogawa.mysecondisservice", appContext.getPackageName());
    }

    @Test
    public void testDataBuffer() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        DataBuffer o = new DataBuffer(appContext);
        o.clear();
        assertEquals(o.length(),0);

        o.append("ABCDEFG\n");

        assertEquals(o.length(), "ABCDEFG\n".length());
        assertEquals("ABCDEFG\n".equals(o.toString()), true);

        o.append("ABCDEFG\n");
        assertEquals("ABCDEFG\nABCDEFG\n".equals(o.toString()), true);


        DataBuffer p = new DataBuffer(appContext);
        Log.d("debug", "p="+p.toString());
        assertEquals("ABCDEFG\nABCDEFG\n".equals(p.toString()), true);

        p.clear();
        assertEquals(p.length(),0);
    }

    @Test
    public void testLogData() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        LogData o = new LogData(appContext);

        o.incStep_counter();
        o.incStep_counter();
        o.incGps_call_count();
        o.incNetwork_call_count();
        o.incNetwork_call_count();
        o.incNetwork_call_count();
        assertEquals(o.getGps_call_count(),1);
        assertEquals(o.getNetwork_call_count(),3);
        assertEquals(o.getStep_counter(),2);

        o.reset(1);
        o.setGps_call_count(5);
        o.setNetwork_call_count(2);
        o.setStep_counter(3);
        assertEquals(o.getGps_call_count(),5);
        assertEquals(o.getNetwork_call_count(),2);
        assertEquals(o.getStep_counter(),3);


        o.save();
        LogData o2 = new LogData(appContext);

        o2.load();
        assertEquals(o2.getGps_call_count(),5);
        assertEquals(o2.getNetwork_call_count(),2);
        assertEquals(o2.getStep_counter(),3);

    }
}
