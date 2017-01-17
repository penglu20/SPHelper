package com.pl.sphelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pl.sphelpersample.R;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SPHelper.init(getApplication());
    }

    public void saveIntAndBool(View view){
        long start=System.currentTimeMillis();
        SPHelper.save("a",100);
        SPHelper.save("z",true);
        long end=System.currentTimeMillis();
        Toast.makeText(this,"takes "+(end-start)+"millis",Toast.LENGTH_SHORT).show();
    }
    public void saveStringAndFloat(View view){
        long start=System.currentTimeMillis();
        SPHelper.save("b","aaa");
        SPHelper.save("x",1.001f);
        long end=System.currentTimeMillis();
        Toast.makeText(this,"takes "+(end-start)+"millis",Toast.LENGTH_SHORT).show();;
    }
    public void saveStringSet(View view){
        Set<String> set=new HashSet<>();
        set.add("aaa,bbb");
        set.add("ccc");
        long start=System.currentTimeMillis();
        SPHelper.save("c",set);
        long end=System.currentTimeMillis();
        Toast.makeText(this,"takes "+(end-start)+"millis",Toast.LENGTH_SHORT).show();;
    }
    public void clean(View view){
        long start=System.currentTimeMillis();
        SPHelper.clear();
        long end=System.currentTimeMillis();
        Toast.makeText(this,"takes "+(end-start)+"millis",Toast.LENGTH_SHORT).show();;
    }
    public void getValue(View view){
        long start=System.currentTimeMillis();

        Map<String,?> map=SPHelper. getAll();

//        String result1 = "a="+SPHelper.getInt("a",0);
//        String result2 = "b="+SPHelper.getString("b","");
//
//        Set<String> set = SPHelper.getStringSet("c",null);

        long end=System.currentTimeMillis();
        Toast.makeText(this,"takes "+(end-start)+"millis",Toast.LENGTH_SHORT).show();;
        String result="";
        for (Map.Entry<String, ?> entry:map.entrySet()){
            String k = entry.getKey();
            Object v = entry.getValue();
            if (v instanceof Set){
                String result3="";
                for (String string:(Set<String>)v){
                    result3+="\""+string+"\"";
                    result3+="    ";
                }
                v=result3;
            }
            result+=k+"="+v+"\n";
        }


        TextView value= (TextView) findViewById(R.id.values);
        value.setText(result);
    }
}
