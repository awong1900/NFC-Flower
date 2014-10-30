package com.alljoyn.display;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class TvDisplay extends Activity {
	TextView hum;
	TextView tem;
	TextView moi;
	byte[] flower = { 55, 24, 80 };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.slide_out_right);

		hum = (TextView) findViewById(R.id.hum);
		tem = (TextView) findViewById(R.id.tem);
		moi = (TextView) findViewById(R.id.moi);

		hum.setText("空气湿度：" + flower[0] + "%");
		tem.setText("温度：" + flower[1] + "°C");
		moi.setText("土壤湿度：" + flower[2] + "%");

		TimerTask task = new TimerTask() {
			public void run() {
				finish();
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 10000);

	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = getIntent();
		if (intent.getAction().equals("com.alljoyn.flower")) {
			
			byte[] flower = intent.getByteArrayExtra("flower");
			hum.setText("空气湿度：" + flower[0] + "%");
			tem.setText("温度：" + flower[1] + "°C");
			moi.setText("土壤湿度：" + flower[2] + "%");

			
			TimerTask task = new TimerTask() {
				public void run() {
					finish();
				}
			};
			Timer timer = new Timer();
			timer.schedule(task, 10000);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}
}
