package com.kaiserware.sailingrace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.net.ssl.HttpsURLConnection;

/**
 * Helper Class to integrate the output from the SailTimerAPI app into this app to
 * fetch the AWA and AWS from the SailTimer, Inc. Wind Anemometer (windex).
 *
 * Created by Volker Petersen in February 2016, modified in Mar 2017 to work with
 * new SailTimerAPI version.
 *
 */
public class SailTimerAPI extends BroadcastReceiver {
    private static SailTimerAPI mInstance = null;
    static final String LOG_TAG = SailTimerAPI.class.getSimpleName();
    public final static String ACTION_DATA_AVAILABLE = "com.ST.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String AWD_DATA = "com.ST.bluetooth.le.AWD_DATA";
    public final static String AWS_DATA = "com.ST.bluetooth.le.AWS_DATA";
    public final static String CUSTOM_NEMA = "com.ST.bluetooth.le.PSTW";
    //public final static String MWD_NEMA = "com.ST.bluetooth.le.WIMWD";
    //public final static String MWV_NEMA = "com.ST.bluetooth.le.WIMWV";
    public String hashCode = "";

    private GlobalParameters para;
    private static fifoQueueDouble AWD;
    private static fifoQueueDouble AWS;

    public SailTimerAPI() {
        para = GlobalParameters.getInstance();  // initialize our Global Parameter singleton class
        hashCode = para.getHashCode();  // MainActivity created the HashCode and stored it in GlobalParameters.
        //Log.d(LOG_TAG, "Initialized SailTimerAPI using hashCode = "+hashCode);
    }

    public static SailTimerAPI getInstance(fifoQueueDouble AWDfifo, fifoQueueDouble AWSfifo) {
        AWD = AWDfifo;
        AWS = AWSfifo;
        if (mInstance == null) {
            mInstance = new SailTimerAPI();
        }
        return mInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        double aws_value;
        double awd_value;
        String awd_decrypted = "n/a";
        String aws_decrypted = "n/a";

        final String action = intent.getAction();
        //Log.d(LOG_TAG, "SailTimerAPI intent action: " + action);
        if (ACTION_DATA_AVAILABLE.equals(action)) {
            para.setSailtimerStatus(true);

            /**
            // test code to identify all keys in this intent
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Set<String> keys = bundle.keySet();
                Iterator<String> it = keys.iterator();
                Log.d(LOG_TAG,"Dumping Intent start");
                while (it.hasNext()) {
                    String key = it.next();
                    Log.d(LOG_TAG,"[" + key + "=" + bundle.get(key)+"]");
                }
                Log.d(LOG_TAG,"Dumping Intent end");
            }
            */

            // old version caused occasional Null Pointer exception if (intent.getExtras().containsKey(AWS_DATA)) {
            if (intent.hasExtra(AWS_DATA)) {
                try {
                    String aws = intent.getStringExtra(AWS_DATA);
                    //Log.d(LOG_TAG, "Windex speed raw: " + aws);
                    //Log.d(LOG_TAG, "Windex hashCode: " + hashCode);
                    aws_decrypted = decryptIt(aws, hashCode);
                    aws_value = Double.parseDouble(aws_decrypted);
                    if (Double.isNaN(aws_value)) {
                        Log.e(LOG_TAG, "onReceive() error for AWS: "+aws_decrypted);
                    } else {
                        //Log.d(LOG_TAG, "==> Windex AWS: " + aws_value);
                        AWS.add(aws_value);
                    }
                } catch (Exception e) {
                    Log.d(LOG_TAG, "onReceive() speed failed: " + e.getMessage());
                    para.setSailtimerStatus(false);
                }
            }
            // old version caused occasional Null Pointer exception if (intent.getExtras().containsKey(AWD_DATA)) {
            if (intent.hasExtra(AWD_DATA)) {
                try {
                    String awd = intent.getStringExtra(AWD_DATA);
                    //Log.d(LOG_TAG, "Windex angle raw: " + awd);
                    //Log.d(LOG_TAG, "Windex has code: " + hashCode);
                    awd_decrypted = decryptIt(awd, hashCode);
                    awd_value = Double.parseDouble(awd_decrypted);
                    if (Double.isNaN(awd_value)) {
                        Log.e(LOG_TAG, "onReceive() error for AWD: " + awd_decrypted);
                    } else {
                        //Log.d(LOG_TAG, "==> Windex AWD (Magnetic): " + awd_value);
                        // convert Magnetic to True Compass angle using the negative declination
                        awd_value = NavigationTools.TrueToMagnetic(awd_value, -para.getDeclination());
                        AWD.add(awd_value);
                        //Log.d(LOG_TAG, "==> Windex AWD (True)    : " + awd_value);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "onReceive() angle failed: " + e.getMessage());
                    para.setSailtimerStatus(false);
                }
            }
            //if (intent.getExtras().containsKey(CUSTOM_NEMA)) {
            if (intent.hasExtra(CUSTOM_NEMA)) {
                try {
                    String customNEMA = intent.getStringExtra(CUSTOM_NEMA);
                    String customNEMA_decrypted = decryptIt(customNEMA, hashCode);
                    //Log.d(LOG_TAG, "==> Windex Custom NEMA: " + customNEMA_decrypted);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "onReceive() angle failed: " + e.getMessage());
                }
            }

            //Log.d(LOG_TAG, "onReceive() results AWD="+awd_decrypted+" | AWS="+aws_decrypted+" | bat="+batteryStr+" | sig="+signalStr);
            //Log.d(LOG_TAG, "onReceive() results AWD="+awd_decrypted+" | AWS="+aws_decrypted);
        } else {
            para.setSailtimerStatus(false);
            Log.d(LOG_TAG, "Windex Broadcast Receiver didn't receive ACTION_DATA_AVAILABLE");
        }
        return;
    }

    /**
      - register the BroadcastReceiver in Fragment_RaceInfo.java Class using:
        private final BroadcastReceiver WindexBroadcastReceiver = new SailTimerAPI(para, appContext);

      - put this code into the Fragment_RaceInfo onResume() method
        registerReceiver(WindexBroadcastReceiver, WindexBroadcastReceiverIntentFilter());

      - put this code into the Fragment_RaceInfo onPause() method
        unregisterReceiver(WindexBroadcastReceiver);
    */

    public static IntentFilter WindexBroadcastReceiverIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public static String decryptIt(String value, String cryptoPass) {
        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);
            byte[] encrypedPwdBytes = Base64.decode(value, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));
            String decrypedValue = new String(decrypedValueBytes);
            return decrypedValue;
        } catch (InvalidKeyException e) {
            Log.e(LOG_TAG, "decryptIT() error 1: "+e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "decryptIT() error 2: "+e.getMessage());
        } catch (InvalidKeySpecException e) {
            Log.e(LOG_TAG, "decryptIT() error 3: "+e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "decryptIT() error 4: "+e.getMessage());
        } catch (BadPaddingException e) {
            Log.e(LOG_TAG, "decryptIT() error 5 (can indicate invalid key):"+e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.e(LOG_TAG, "decryptIT() error 6: "+e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.e(LOG_TAG, "decryptIT() error 7: "+e.getMessage());
            e.printStackTrace();
        }
        return value;
    }
}

