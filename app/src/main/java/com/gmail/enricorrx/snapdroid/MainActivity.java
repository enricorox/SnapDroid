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

/*
	First Activity, retrieve information about user's intentions
*/
public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Find widget in R.id
		final Button runB = findViewById(R.id.run);
		final Switch clientS = findViewById(R.id.switchClient);
		final Switch serverS = findViewById(R.id.switchServer);
		final EditText et = findViewById(R.id.editText);
		// Set button listener
		runB.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				// Check if this is a valid IP
				if(!et.getText().toString()
						.matches("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}")) {
					Toast.makeText(MainActivity.this,"Valid IP required!", Toast.LENGTH_SHORT).show();
					return;
				}
				// Check if network is available
				if(!isNetworkAvailable()){
					Toast.makeText(MainActivity.this,"Check your WiFi!", Toast.LENGTH_SHORT).show();
					return;
				}
				// Put variables in the intent
				Intent myIntent = new Intent(MainActivity.this, SnapDroid.class);
				myIntent.putExtra("client", clientS.isChecked());
				myIntent.putExtra("server", serverS.isChecked());
				myIntent.putExtra("IP", et.getText().toString());
				// Start SnapDroid activity
				startActivity(myIntent);
			}
		});

		clientS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Enable/Disable EditText field
				if(isChecked)
					et.setEnabled(true);
				else
					et.setEnabled(false);
			}
		});
	}

	private boolean isNetworkAvailable() {
		// Get an instance of Connectivity Manager
		ConnectivityManager connectivityManager
				= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// Get network info
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
