package com.shantanoo.stockwatch.service;

import android.os.AsyncTask;
import android.util.Log;

import com.shantanoo.stockwatch.MainActivity;
import com.shantanoo.stockwatch.model.Stock;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Shantanoo on 10/8/2020.
 */
public class StockDataService extends AsyncTask<String, Void, String> {

    private static final String TAG = "StockDataService";

    private MainActivity mainActivity;

    public StockDataService(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(String... strings) {
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Stock stock = parseJSON(s);
        mainActivity.populateStockFinancialData(stock);
    }

    private Stock parseJSON(String input) {
        Log.d(TAG, "parseJSON: Parsing JSON data");
        Stock stock = null;
        try {
            JSONObject object = new JSONObject(input);

            String symbol = object.getString("symbol");
            String name = object.getString("companyName");
            double price = object.getDouble("latestPrice");
            double priceChange = object.getDouble("change");
            double changePercent = object.getDouble("changePercent");

            stock = new Stock(symbol, name, price, priceChange, changePercent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stock;
    }
}
