package com.pl.sphelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.pl.sphelpersample.R;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SPHelper.init(getApplication());
        Set<String> set=new HashSet<>();
        set.add("aaa,ccc");
        set.add("bbb");
        SPHelper.save("aaa",set );

        Log.e("SPTest","aaa="+SPHelper.getStringSet("aaa",null));
    }
}
