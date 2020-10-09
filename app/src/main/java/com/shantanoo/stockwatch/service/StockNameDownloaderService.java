package com.shantanoo.stockwatch.service;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.shantanoo.stockwatch.MainActivity;
import com.shantanoo.stockwatch.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Shantanoo on 10/7/2020.
 */
public class StockNameDownloaderService extends AsyncTask<Void, Void, String> {
    private static final String TAG = "StockNameDownloader";
    private static final String DOWNLOAD_LINK = "https://api.iextrading.com/1.0/ref-data/symbols";

    private MainActivity mainActivity;

    public StockNameDownloaderService(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Uri uri = Uri.parse(DOWNLOAD_LINK);
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(uri.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.connect();

            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader((new InputStreamReader(is)));

            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            Log.e(TAG, "doInBackground: Exception: ", e);
        }
        return sb.toString();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        mainActivity.populateStockNamesData((HashMap<String, String>) parseJSON(s));
    }

    private Map<String, String> parseJSON(String input) {
        Map<String, String> stockMaster = new HashMap<>();
        try {
            JSONArray jsonArray = new JSONArray(input);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String symbol = jsonObject.getString(mainActivity.getString(R.string.symbol));
                String name = jsonObject.getString(mainActivity.getString(R.string.name));
                stockMaster.put(symbol, name);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseJSON: Failed to parse JSON", e);
        }
        return stockMaster;
    }
}
