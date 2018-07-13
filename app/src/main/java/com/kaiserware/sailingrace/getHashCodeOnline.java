package com.kaiserware.sailingrace;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 * Async Task class fetch the Hash code from the SailTimer Inc. website using
 * a HttpsURLConnection call.  This Hash Code is required to decode the
 * data transmitted from the Windex.
 * @param - first parameter is a String Array passed into "doInBackground"
 * @param - second parameter is an Integer array passed into "onProgressUpdate"
 * @param - third parameter is a String passed into "onPostExecute"
 */
public class getHashCodeOnline extends AsyncTask<String, Integer, String> {
    public final String LOG_TAG = getHashCodeOnline.class.getSimpleName();

    // Runs in UI before background thread is called
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    // this is our Background thread
    @Override
    protected String doInBackground(String... msg1) {
        final String SAILTIMER_URL = "https://www.sailtimermaps.com/getHash.php";
        final GlobalParameters gp = GlobalParameters.getInstance();
        String asyncResult = "";

        try {
            JSONObject params = new JSONObject();
            params.put("user", "Volker");
            params.put("password", "K\\u8L27CLe<S:R7Q");
            URL url = new URL(SAILTIMER_URL);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setReadTimeout(15000);        // milliseconds
            urlConnection.setConnectTimeout(15000);     // milliseconds
            urlConnection.setRequestMethod("POST");			  // designate a POST request
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            OutputStream out = urlConnection.getOutputStream();
            out.write((params.toString()).getBytes("UTF-8"));
            out.close();

            int responseCode = urlConnection.getResponseCode();
            Log.d(LOG_TAG, "getHashCodeOnline() parameters="+params.toString());
            Log.d(LOG_TAG, "getHashCodeOnline() httpRequest responseCode="+responseCode);

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                try {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream())
                    );
                    String line = "";
                    StringBuffer response = new StringBuffer("");
                    while((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    gp.setHashCode(response.toString());
                    Log.d(LOG_TAG, "getHashCodeOnline() SailTimer Hash Code="+response.toString());
                    asyncResult = response.toString();
                } catch (Exception e) {
                    gp.setHashCode("");
                    Log.e(LOG_TAG, "getHashCodeOnline() httpRequest error="+e.getMessage());
                    asyncResult = "httpRequest error="+e.getMessage();
                }
            } else {
                gp.setHashCode("");
                Log.d(LOG_TAG, "getHashCodeOnline() httpRequest responseCode="+responseCode);
                asyncResult = "HTTP response code = "+responseCode;
            }
        } catch (JSONException j) {
            gp.setHashCode("");
            Log.d(LOG_TAG, "getHashCodeOnline() JSON error ="+j.getMessage());
            asyncResult = "JSON error ="+j.getMessage();
        } catch (IOException e) {
            gp.setHashCode("");
            Log.d(LOG_TAG, "getHashCodeOnline() error =" + e.getMessage());
            asyncResult = "error =" + e.getMessage();
        }
        return asyncResult;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }

}

