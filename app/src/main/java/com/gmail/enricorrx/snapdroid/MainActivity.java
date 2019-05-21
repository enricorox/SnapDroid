package com.gmail.enricorrx.snapdroid;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button startB = findViewById(R.id.start);
        final Switch clientS = findViewById(R.id.switchClient);
        final Switch serverS = findViewById(R.id.switchServer);
        final EditText et = findViewById(R.id.editText);
        // Capture button clicks
        startB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if(!et.getText().toString().matches("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}")) {
                    Toast.makeText(MainActivity.this,"Valid IP required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!isNetworkAvailable()){
                    Toast.makeText(MainActivity.this,"Check your WiFi!", Toast.LENGTH_SHORT).show();

                    return;
                }

                // Start NewActivity.class
                Intent myIntent = new Intent(MainActivity.this,
                        SnapDroid.class);
                myIntent.putExtra("client", clientS.isChecked());
                myIntent.putExtra("server", serverS.isChecked());
                myIntent.putExtra("IP", et.getText().toString());
                startActivity(myIntent);
            }
        });

        clientS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    et.setEnabled(true);
                else
                    et.setEnabled(false);
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}
