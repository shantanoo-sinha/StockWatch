package com.shantanoo.stockwatch.service;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.shantanoo.stockwatch.MainActivity;
import com.shantanoo.stockwatch.R;
import com.shantanoo.stockwatch.model.Stock;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Shantanoo on 10/8/2020.
 */
public class StockDataDownloaderService extends AsyncTask<String, Void, String> {

    private static final String TAG = "StockDataDownloader";

    private static final String BASE_URL = "https://cloud.iexapis.com/stable/stock/";
    private static final String QUOTE_TOKEN = "/quote?token=";
    private static final String API_KEY = "pk_ab02406ce6c34caf85b5f61aa7c983bf";

    private MainActivity mainActivity;

    public StockDataDownloaderService(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(String... strings) {
        StringBuilder sb = new StringBuilder();
        Uri uri = Uri.parse(BASE_URL + strings[0] + QUOTE_TOKEN + API_KEY);
        String line;
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
        Stock stock = parseJSON(s);
        mainActivity.populateStocksData(stock);
    }

    private Stock parseJSON(String input) {
        Log.d(TAG, "parseJSON: Parsing JSON data");
        Stock stock = new Stock();
        try {
            JSONObject object = new JSONObject(input);

            String symbol = object.getString(mainActivity.getString(R.string.symbol));
            stock.setStockSymbol(symbol);

            String name = object.getString(mainActivity.getString(R.string.company_name));
            stock.setStockName(name);

            double price = object.getDouble(mainActivity.getString(R.string.latest_price));
            stock.setStockPrice(price);

            double priceChange = object.getDouble(mainActivity.getString(R.string.change));
            stock.setStockPriceChange(priceChange);

            double changePercent = object.getDouble(mainActivity.getString(R.string.change_percentage));
            stock.setStockPriceChangePercentage(changePercent);
        } catch (JSONException e) {
            Log.e(TAG, "parseJSON: Failed to parse JSON", e);
        }
        return stock;
    }
}
