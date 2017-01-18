package com.pl.sphelper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Toast;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

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

        Random random=new Random();
        long start=System.currentTimeMillis();
        for (int i=0;i<10000;i++) {
            SPHelper.save("key"+random.nextInt(200),i);
        }
        long end=System.currentTimeMillis();
        Toast.makeText(appContext,"takes "+(end-start)+"millis",Toast.LENGTH_SHORT).show();;
    }
}
