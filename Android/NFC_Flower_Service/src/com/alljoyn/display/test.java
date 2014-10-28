package com.alljoyn.display;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class test extends Activity {
	Intent service;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		service = new Intent(test.this,
				org.alljoyn.bus.flower.simpleservice.alljoynService.class);

		Button onService = (Button) findViewById(R.id.onService);
		Button offService = (Button) findViewById(R.id.offService);
		onService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(service);
			}
		});

		offService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopService(service);
			}
		});

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
