/**
 * Copyright 2014 sam, xiao_nie@163.com
 * https://play.google.com/store/apps/details?id=com.flower.nfcaction More info
 * http://www.elecfreaks.com
 */

package com.flower.nfcaction;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import com.flower.nfcaction.R;

public class WriteTag extends Activity {
    // URL share
    public static final String WEB_URL_ADDRESS = "WEB_URL_ADDRESS";
    
    // Theme share
    public static final String KONKA_THEME = "KONKA_THEME";
    
    NfcAdapter nfcAdapter;
    
    private boolean WriteMode = false;
    
    String flower_type;
    
    int flower_value;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write_tag);
        initData();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }
    
    private void initData() {
        Intent dataIntent = getIntent();
        flower_type = dataIntent.getStringExtra("flower");
        flower_value = dataIntent.getIntExtra(flower_type, 0);
        Log.d("sam test", "flower_type = " + flower_type);
        Log.d("sam test", "flower_value = " + flower_value);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        Log.d("sam test", "WriteTag onNewIntent action = " + intent.getAction());
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (writeTag(createNdefMessage(), detectedTag)) {
                Toast.makeText(getApplicationContext(), getString(R.string.write_succ),
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.write_failed),
                        Toast.LENGTH_LONG).show();
            }
            
        }
    }
    
    public NdefMessage createNdefMessage() {
        
        NdefRecord[] records = new NdefRecord[3];
        
        // URI
        Uri uri = Uri.parse("http://www.elecfreaks.com");
        records[0] = NdefRecord.createUri(uri);
        
        // record2
        byte bs[] = {(byte) flower_value};
        Log.d("sam test", "flower_value = " + bs[0]);
        records[1] = NdefRecord.createExternal("flower", flower_type, bs);
        
        records[2] = NdefRecord.createApplicationRecord("com.flower.nfcaction");
        /** NDEF Message **/
        NdefMessage message = new NdefMessage(records);
        return message;
        
    }
    
    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("sam test", "nfc WriteMode = " + WriteMode);
        enableTagWriteMode();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if ((isFinishing()) && (nfcAdapter != null)) {
            WriteMode = false;
            Log.d("sam test", "nfc disableForegroundDispatch");
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }
    
    private void enableTagWriteMode() {
        if (!WriteMode && nfcAdapter != null && nfcAdapter.isEnabled()) {
            WriteMode = true;
            Log.d("sam test", "nfc enableForegroundDispatch");
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] mWriteTagFilters = new IntentFilter[] {tagDetected};
            // Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
            PendingIntent mNfcPendingIntent =
                    PendingIntent.getActivity(this, 0,
                            new Intent(this, getClass()).addFlags(603979776), 0);
            nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
        }
    }
}
