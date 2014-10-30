/**
 * Copyright 2014 sam, xiao_nie@163.com
 * https://play.google.com/store/apps/details?id=com.flower.nfcaction More info :
 * http://www.elecfreaks.com
 */

package com.flower.nfcaction;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import com.flower.alljoyn.client.SimpleInterface;
import com.flower.nfcaction.alljoyn.R;

public class MainActivity extends Activity {
	/* Load the native alljoyn_java library. */
	static {
		System.loadLibrary("alljoyn_java");
	}

	private static final String TAG = "MainActivity";
	NfcAdapter mNfcAdapter;

	private ViewPager navigationView;
	private View[] panels = new View[3];
	private String[] titles;
	public BusHandler mBusHandler;
	public byte[] flower = { 40, 26, 70 }; // hum, tem, moi

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_main);
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		initView();

		navigationView.setAdapter(new NavigationAdapter());
		titles = getResources().getStringArray(R.array.titles);

		/*
		 * Make all AllJoyn calls through a separate handler thread to prevent
		 * blocking the UI.
		 */
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new BusHandler(busThread.getLooper());

		/* Connect to an AllJoyn object. */
		mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
		mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
	}

	@Override
	public void startActivity(Intent intent) {
		Log.d("sam test", "startActivity");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		super.startActivity(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		Intent intent = null;
		intent = getIntent();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			Parcelable[] rawMsgs = intent
					.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
				// process user-defined task
				user_definded_task(msgs[0]);
			}
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		/* Disconnect to prevent resource leaks. */
		mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
	}

	private void user_definded_task(NdefMessage msgs) {

		NdefRecord[] records = msgs.getRecords();
		for (int i = 0; i < records.length; i++) {
			records[i] = records[i];
			Log.d("sam test", "records[" + i + "] =" + records[i].toString());
		}
		if (Arrays.equals(records[1].getType(), "flower:humidity".getBytes())) {
			Log.d("sam test", "equals flower:humidity");
			byte[] humidity = records[1].getPayload();
			for (byte b : humidity) {
				Log.d("sam test", "b=" + Integer.toHexString(b));
			}

			// display theme
			TextView hum = (TextView) panels[0]
					.findViewById(R.id.humidity_textview_info);
			hum.setText(humidity[0] + "%");
			// hum.setText(Integer.toHexString(humidity[1]) + "%");

			flower[0] = humidity[0];
		}
		if (Arrays
				.equals(records[2].getType(), "flower:temperature".getBytes())) {
			Log.d("sam test", "equals flower:temperature");
			byte[] temperature = records[2].getPayload();
			for (byte b : temperature) {
				Log.d("sam test", "b=" + Integer.toHexString(b));
			}

			// display theme
			TextView tem = (TextView) panels[0]
					.findViewById(R.id.temperature_textview_info);
			tem.setText(temperature[0] + "°C");

			flower[1] = temperature[0];
		}
		if (Arrays.equals(records[3].getType(), "flower:moisture".getBytes())) {
			Log.d("sam test", "equals flower:moisture");
			byte[] moisture = records[3].getPayload();
			for (byte b : moisture) {
				Log.d("sam test", "b=" + Integer.toHexString(b));
			}

			// display theme
			TextView moi = (TextView) panels[0]
					.findViewById(R.id.moisture_textview_info);
			moi.setText(moisture[0] + "%");

			flower[2] = moisture[0];
		}

		/* Call the remote object's Ping method. */
		 Message msg = mBusHandler.obtainMessage(BusHandler.FLOWER, flower);
		 mBusHandler.sendMessage(msg);
	}

	private void initView() {
		navigationView = (ViewPager) findViewById(R.id.navigations);

		getLayoutInflater();

		LayoutInflater lf = LayoutInflater.from(this);

		// info panel
		panels[0] = lf.inflate(R.layout.info, null);
		// button click
		Button watherBtn = (Button) panels[0].findViewById(R.id.wather_btn);
		watherBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// water flower
				String value = "water";
				Intent intent = new Intent(MainActivity.this, WriteTag.class);
				intent.putExtra("flower", value);
				intent.putExtra("water", 1);
				startActivity(intent);
				// Toast.makeText(getApplicationContext(), "water flower",
				// Toast.LENGTH_SHORT).show();
			}
		});

		// button click
		Button infoToTV = (Button) panels[0].findViewById(R.id.tv_btn);
		infoToTV.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Message msg = mBusHandler.obtainMessage(BusHandler.FLOWER,
						flower);
				mBusHandler.sendMessage(msg);

			}
		});

		// setting panel
		panels[1] = lf.inflate(R.layout.setttings, null);
		final TextView humiditySetting = (TextView) panels[1]
				.findViewById(R.id.humidity_text_display);
		final SeekBar humiditySeekBar = (SeekBar) panels[1]
				.findViewById(R.id.humidity_seekbar);
		humiditySeekBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {

						humiditySetting.setText(progress + "%");
					}
				});

		// button click
		Button watherBtn1 = (Button) panels[1].findViewById(R.id.wather_btn);
		watherBtn1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// water flower
				String type = "setting";
				Intent intent = new Intent(MainActivity.this, WriteTag.class);
				intent.putExtra("flower", type);
				intent.putExtra(type, humiditySeekBar.getProgress());
				startActivity(intent);
				// Toast.makeText(getApplicationContext(),
				// "Setting humidity warn!", Toast.LENGTH_SHORT).show();
			}
		});

		// about panel
		panels[2] = lf.inflate(R.layout.about, null);
	}

	private class NavigationAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return panels.length;
		}

		@Override
		public Object instantiateItem(View view, int position) {
			((ViewPager) view).addView(panels[position]);
			return panels[position];
		}

		@Override
		public void destroyItem(View view, int arg1, Object obj) {
			((ViewPager) view).removeView((View) obj);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/* This class will handle all AllJoyn calls. See onCreate(). */
	public class BusHandler extends Handler {
		/*
		 * Name used as the well-known name and the advertised name of the
		 * service this client is interested in. This name must be a unique name
		 * both to the bus and to the network as a whole.
		 * 
		 * The name uses reverse URL style of naming, and matches the name used
		 * by the service.
		 */
		private static final String SERVICE_NAME = "org.alljoyn.bus.flower.simple";
		private static final short CONTACT_PORT = 42;

		private BusAttachment mBus;
		private ProxyBusObject mProxyObj;
		private SimpleInterface mSimpleInterface;

		private int mSessionId;
		private boolean mIsInASession;
		private boolean mIsConnected;
		private boolean mIsStoppingDiscovery;

		/* These are the messages sent to the BusHandler from the UI. */
		public static final int CONNECT = 1;
		public static final int JOIN_SESSION = 2;
		public static final int DISCONNECT = 3;
		public static final int PING = 4;
		public static final int FLOWER = 5;

		public BusHandler(Looper looper) {
			super(looper);

			mIsInASession = false;
			mIsConnected = false;
			mIsStoppingDiscovery = false;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			/*
			 * Connect to a remote instance of an object implementing the
			 * SimpleInterface.
			 */
			case CONNECT: {
				Log.d(TAG, "CONNECT!");

				org.alljoyn.bus.alljoyn.DaemonInit
						.PrepareDaemon(getApplicationContext());
				/*
				 * All communication through AllJoyn begins with a
				 * BusAttachment.
				 * 
				 * A BusAttachment needs a name. The actual name is unimportant
				 * except for internal security. As a default we use the class
				 * name as the name.
				 * 
				 * By default AllJoyn does not allow communication between
				 * devices (i.e. bus to bus communication). The second argument
				 * must be set to Receive to allow communication between
				 * devices.
				 */
				mBus = new BusAttachment(getPackageName(),
						BusAttachment.RemoteMessage.Receive);

				/*
				 * Create a bus listener class
				 */
				mBus.registerBusListener(new BusListener() {
					@Override
					public void foundAdvertisedName(String name,
							short transport, String namePrefix) {
						logInfo(String
								.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)",
										name, transport, namePrefix));
						/*
						 * This client will only join the first service that it
						 * sees advertising the indicated well-known name. If
						 * the program is already a member of a session (i.e.
						 * connected to a service) we will not attempt to join
						 * another session. It is possible to join multiple
						 * session however joining multiple sessions is not
						 * shown in this sample.
						 */
						if (!mIsConnected) {
							Message msg = obtainMessage(JOIN_SESSION);
							msg.arg1 = transport;
							msg.obj = name;
							sendMessage(msg);
						}
					}
				});

				/*
				 * To communicate with AllJoyn objects, we must connect the
				 * BusAttachment to the bus.
				 */
				Status status = mBus.connect();
				logStatus("BusAttachment.connect()", status);
				if (Status.OK != status) {
					finish();
					return;
				}

				/*
				 * Now find an instance of the AllJoyn object we want to call.
				 * We start by looking for a name, then connecting to the device
				 * that is advertising that name.
				 * 
				 * In this case, we are looking for the well-known SERVICE_NAME.
				 */
				status = mBus.findAdvertisedName(SERVICE_NAME);
				logStatus(String.format(
						"BusAttachement.findAdvertisedName(%s)", SERVICE_NAME),
						status);
				if (Status.OK != status) {
					finish();
					return;
				}

				break;
			}
			case (JOIN_SESSION): {
				Log.d(TAG, "JOIN_SESSION!");

				/*
				 * If discovery is currently being stopped don't join to any
				 * other sessions.
				 */
				if (mIsStoppingDiscovery) {
					break;
				}

				/*
				 * In order to join the session, we need to provide the
				 * well-known contact port. This is pre-arranged between both
				 * sides as part of the definition of the chat service. As a
				 * result of joining the session, we get a session identifier
				 * which we must use to identify the created session
				 * communication channel whenever we talk to the remote side.
				 */
				short contactPort = CONTACT_PORT;
				SessionOpts sessionOpts = new SessionOpts();
				sessionOpts.transports = (short) msg.arg1;
				Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

				Status status = mBus.joinSession((String) msg.obj, contactPort,
						sessionId, sessionOpts, new SessionListener() {
							@Override
							public void sessionLost(int sessionId, int reason) {
								mIsConnected = false;
								logInfo(String
										.format("MyBusListener.sessionLost(sessionId = %d, reason = %d)",
												sessionId, reason));
								mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
							}
						});
				logStatus("BusAttachment.joinSession() - sessionId: "
						+ sessionId.value, status);

				if (status == Status.OK) {
					/*
					 * To communicate with an AllJoyn object, we create a
					 * ProxyBusObject. A ProxyBusObject is composed of a name,
					 * path, sessionID and interfaces.
					 * 
					 * This ProxyBusObject is located at the well-known
					 * SERVICE_NAME, under path "/SimpleService", uses sessionID
					 * of CONTACT_PORT, and implements the SimpleInterface.
					 */
					mProxyObj = mBus.getProxyBusObject(SERVICE_NAME,
							"/SimpleService", sessionId.value,
							new Class<?>[] { SimpleInterface.class });

					/*
					 * We make calls to the methods of the AllJoyn object
					 * through one of its interfaces.
					 */
					mSimpleInterface = mProxyObj
							.getInterface(SimpleInterface.class);

					mSessionId = sessionId.value;
					mIsConnected = true;
					mHandler.sendEmptyMessage(MESSAGE_STOP_PROGRESS_DIALOG);
				}
				break;
			}

			/* Release all resources acquired in the connect. */
			case DISCONNECT: {
				Log.d(TAG, "DISCONNECT!");

				mIsStoppingDiscovery = true;
				if (mIsConnected) {
					Status status = mBus.leaveSession(mSessionId);
					logStatus("BusAttachment.leaveSession()", status);
				}
				mBus.disconnect();
				getLooper().quit();
				break;
			}

			/*
			 * Call the service's Ping method through the ProxyBusObject.
			 * 
			 * This will also print the String that was sent to the service and
			 * the String that was received from the service to the user
			 * interface.
			 */
			case PING: {
				Log.d(TAG, "PING!");

				try {
					if (mSimpleInterface != null) {
						sendUiMessage(MESSAGE_PING, msg.obj);
						String reply = mSimpleInterface.Ping((String) msg.obj);
						sendUiMessage(MESSAGE_PING_REPLY, reply);
					}
				} catch (BusException ex) {
					logException("SimpleInterface.Ping()", ex);
				}
				break;
			}

			case FLOWER: {
				Log.d(TAG, "FLOWER!");

				try {
					if (mSimpleInterface != null) {
						// sendUiMessage(MESSAGE_PING, msg.obj);
						// String reply = mSimpleInterface.Ping((String)
						// msg.obj);
						// sendUiMessage(MESSAGE_PING_REPLY, reply);

						byte[] data = (byte[]) msg.obj;
						String reply = mSimpleInterface.flower(data);
						sendUiMessage(MESSAGE_POST_TOAST, reply);
					}
				} catch (BusException ex) {
					logException("SimpleInterface.Flower()", ex);
				}
				break;
			}
			default:
				break;
			}
		}

		/* Helper function to send a message to the UI thread. */
		private void sendUiMessage(int what, Object obj) {
			mHandler.sendMessage(mHandler.obtainMessage(what, obj));
		}
	}

	private void logStatus(String msg, Status status) {
		String log = String.format("%s: %s", msg, status);
		if (status == Status.OK) {
			Log.i(TAG, log);
		} else {
			Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
			mHandler.sendMessage(toastMsg);
			Log.e(TAG, log);
		}
	}

	private void logException(String msg, BusException ex) {
		String log = String.format("%s: %s", msg, ex);
		Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
		mHandler.sendMessage(toastMsg);
		Log.e(TAG, log, ex);
	}

	/*
	 * print the status or result to the Android log. If the result is the
	 * expected result only print it to the log. Otherwise print it to the error
	 * log and Sent a Toast to the users screen.
	 */
	private void logInfo(String msg) {
		Log.i(TAG, msg);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_PING:
				// String ping = (String) msg.obj;
				// mListViewArrayAdapter.add("Ping:  " + ping);
				break;
			case MESSAGE_PING_REPLY:
				// String ret = (String) msg.obj;
				// mListViewArrayAdapter.add("Reply:  " + ret);
				// mEditText.setText("");
				break;
			case MESSAGE_POST_TOAST:
				Toast.makeText(getApplicationContext(), (String) msg.obj,
						Toast.LENGTH_LONG).show();
				break;
			case MESSAGE_START_PROGRESS_DIALOG:
				mDialog = ProgressDialog.show(MainActivity.this, "",
						"正在连接TV服务器.\n请稍候...", true, true);
				break;
			case MESSAGE_STOP_PROGRESS_DIALOG:
				mDialog.dismiss();
				break;
			default:
				break;
			}
		}
	};

	private static final int MESSAGE_PING = 1;
	private static final int MESSAGE_PING_REPLY = 2;
	private static final int MESSAGE_POST_TOAST = 3;
	private static final int MESSAGE_START_PROGRESS_DIALOG = 4;
	private static final int MESSAGE_STOP_PROGRESS_DIALOG = 5;
	private ProgressDialog mDialog;

}
