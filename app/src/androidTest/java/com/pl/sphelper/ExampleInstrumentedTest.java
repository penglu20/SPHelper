package com.pl.sphelper;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
//        for (int j=0;j<5;j++) {
            // Context of the app under test.
            Context appContext = InstrumentationRegistry.getTargetContext();
            SPHelper.init((Application) appContext.getApplicationContext());
            Random random = new Random();
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                SPHelper.save("key" + random.nextInt(200), i);
            }
            long end = System.currentTimeMillis();
            Log.e("ExampleInstrumentedTest", "SPHelper takes " + (end - start) + "millis");

            start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                SharedPreferences sp = appContext.getSharedPreferences("text", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("key" + random.nextInt(200), i);
                editor.commit();
            }
            end = System.currentTimeMillis();
            Log.e("ExampleInstrumentedTest", "SharedPreferences takes " + (end - start) + "millis");
//        }
    }
}
