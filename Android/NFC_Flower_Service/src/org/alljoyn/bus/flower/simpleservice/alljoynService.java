package org.alljoyn.bus.flower.simpleservice;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusMethod;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class alljoynService extends Service {
	/* Load the native alljoyn_java library. */
	static {
		System.loadLibrary("alljoyn_java");
	}

	private static final String TAG = "SimpleService";

	private static final int MESSAGE_PING = 1;
	private static final int MESSAGE_PING_REPLY = 2;
	private static final int MESSAGE_POST_TOAST = 3;
	private static final int MESSAGE_POST_ACTIVITY = 4;

	// private WifiDirectAutoAccept mWfdAutoAccept;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_PING:
				break;
			case MESSAGE_PING_REPLY:
				break;
			case MESSAGE_POST_TOAST:
				Toast.makeText(getApplicationContext(), (String) msg.obj,
						Toast.LENGTH_LONG).show();
				break;
			case MESSAGE_POST_ACTIVITY:
				Intent intent = new Intent("com.alljoyn.flower");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("flower", (byte[]) msg.obj);
				startActivity(intent);
				break;
			default:
				break;
			}
		}
	};

	/* The AllJoyn object that is our service. */
	private SimpleService mSimpleService;

	/* Handler used to make calls to AllJoyn methods. See onCreate(). */
	private Handler mBusHandler;

	private IBinder binder = new CounterBinder();

	public class CounterBinder extends Binder {
		public alljoynService getService() {
			return alljoynService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		/*
		 * Prepare the auto-accept object. It will not automatically accept any
		 * connections until its intercept() method is called.
		 */
		// mWfdAutoAccept = new WifiDirectAutoAccept(getApplicationContext());

		/*
		 * Make all AllJoyn calls through a separate handler thread to prevent
		 * blocking the UI.
		 */
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new BusHandler(busThread.getLooper());

		/* Start our service. */
		mSimpleService = new SimpleService();
		mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
	}

	@Override
	public IBinder onBind(Intent intent) {

		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(TAG, "onDestroy!");

		// mWfdAutoAccept.intercept(false);

		/* Disconnect to prevent any resource leaks. */
		mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
	}

	/* The class that is our AllJoyn service. It implements the SimpleInterface. */
	class SimpleService implements SimpleInterface, BusObject {
		/*
		 * This is the code run when the client makes a call to the Ping method
		 * of the SimpleInterface. This implementation just returns the received
		 * String to the caller.
		 * 
		 * This code also prints the string it received from the user and the
		 * string it is returning to the user to the screen.
		 */
		@Override
		public String Ping(String inStr) {
			sendUiMessage(MESSAGE_PING, inStr);

			/* Simply echo the ping message. */
			sendUiMessage(MESSAGE_PING_REPLY, inStr);
			return inStr;
		}

		/* Helper function to send a message to the UI thread. */
		private void sendUiMessage(int what, Object obj) {
			mHandler.sendMessage(mHandler.obtainMessage(what, obj));
		}

		@Override
		@BusMethod
		public String flower(byte[] data) throws BusException {
			Log.d(TAG, "flower");
			Log.d(TAG, "data0:" + data[0]);
			Log.d(TAG, "data1:" + data[1]);
			Log.d(TAG, "data2:" + data[2]);

			sendUiMessage(MESSAGE_POST_ACTIVITY, data);
			return "ok";
		}
	}

	/* This class will handle all AllJoyn calls. See onCreate(). */
	class BusHandler extends Handler {
		/*
		 * Name used as the well-known name and the advertised name. This name
		 * must be a unique name both to the bus and to the network as a whole.
		 * The name uses reverse URL style of naming.
		 */
		private static final String SERVICE_NAME = "org.alljoyn.bus.flower.simple";
		private static final short CONTACT_PORT = 42;

		private BusAttachment mBus;

		/* These are the messages sent to the BusHandler from the UI. */
		public static final int CONNECT = 1;
		public static final int DISCONNECT = 2;

		public BusHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			/* Connect to the bus and start our service. */
			case CONNECT: {
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
				mBus.registerBusListener(new BusListener());

				/*
				 * To make a service available to other AllJoyn peers, first
				 * register a BusObject with the BusAttachment at a specific
				 * path.
				 * 
				 * Our service is the SimpleService BusObject at the
				 * "/SimpleService" path.
				 */
				Status status = mBus.registerBusObject(mSimpleService,
						"/SimpleService");
				logStatus("BusAttachment.registerBusObject()", status);
				if (status != Status.OK) {
					// finish();
					return;
				}

				/*
				 * The next step in making a service available to other AllJoyn
				 * peers is to connect the BusAttachment to the bus with a
				 * well-known name.
				 */
				/*
				 * connect the BusAttachement to the bus
				 */
				status = mBus.connect();
				logStatus("BusAttachment.connect()", status);
				if (status != Status.OK) {
					// finish();
					return;
				}

				/*
				 * Create a new session listening on the contact port of the
				 * chat service.
				 */
				Mutable.ShortValue contactPort = new Mutable.ShortValue(
						CONTACT_PORT);

				SessionOpts sessionOpts = new SessionOpts();
				sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
				sessionOpts.isMultipoint = false;
				sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;

				/*
				 * Explicitly add the Wi-Fi Direct transport into our
				 * advertisements. This sample is typically used in a "cable-
				 * replacement" scenario and so it should work well over that
				 * transport. It may seem odd that ANY actually excludes Wi-Fi
				 * Direct, but there are topological and advertisement/
				 * discovery problems with WFD that make it problematic to
				 * always enable.
				 */
				sessionOpts.transports = SessionOpts.TRANSPORT_ANY
						+ SessionOpts.TRANSPORT_WFD;

				status = mBus.bindSessionPort(contactPort, sessionOpts,
						new SessionPortListener() {
							@Override
							public boolean acceptSessionJoiner(
									short sessionPort, String joiner,
									SessionOpts sessionOpts) {
								if (sessionPort == CONTACT_PORT) {
									return true;
								} else {
									return false;
								}
							}
						});
				logStatus(String.format(
						"BusAttachment.bindSessionPort(%d, %s)",
						contactPort.value, sessionOpts.toString()), status);
				if (status != Status.OK) {
					// finish();
					return;
				}

				/*
				 * request a well-known name from the bus
				 */
				int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING
						| BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;

				status = mBus.requestName(SERVICE_NAME, flag);
				logStatus(String.format(
						"BusAttachment.requestName(%s, 0x%08x)", SERVICE_NAME,
						flag), status);
				if (status == Status.OK) {
					/*
					 * If we successfully obtain a well-known name from the bus
					 * advertise the same well-known name
					 */
					status = mBus.advertiseName(SERVICE_NAME,
							sessionOpts.transports);
					logStatus(String.format("BusAttachement.advertiseName(%s)",
							SERVICE_NAME), status);
					if (status != Status.OK) {
						/*
						 * If we are unable to advertise the name, release the
						 * well-known name from the local bus.
						 */
						status = mBus.releaseName(SERVICE_NAME);
						logStatus(String.format(
								"BusAttachment.releaseName(%s)", SERVICE_NAME),
								status);
						// finish();
						return;
					}
				}

				break;
			}

			/* Release all resources acquired in connect. */
			case DISCONNECT: {
				/*
				 * It is important to unregister the BusObject before
				 * disconnecting from the bus. Failing to do so could result in
				 * a resource leak.
				 */
				mBus.unregisterBusObject(mSimpleService);
				mBus.disconnect();
				mBusHandler.getLooper().quit();
				break;
			}

			default:
				break;
			}
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

}
