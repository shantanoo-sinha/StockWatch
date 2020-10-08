package com.shantanoo.stockwatch;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.shantanoo.stockwatch.adapter.StocksAdapter;
import com.shantanoo.stockwatch.database.DatabaseHandler;
import com.shantanoo.stockwatch.model.Stock;
import com.shantanoo.stockwatch.service.StockDataService;
import com.shantanoo.stockwatch.service.StockMasterService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "MainActivity";

    private static final String API_KEY = "pk_ab02406ce6c34caf85b5f61aa7c983bf";
    private static final String BASE_URL = "https://cloud.iexapis.com/stable/stock/";
    private static final String QUOTE_TOKEN = "quote?token=";

    private List<Stock> stocks;
    private Map<String, String> stockMaster;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StocksAdapter stocksAdapter;

    private DatabaseHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.rvStocks);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        stocks = new ArrayList<>();
        stockMaster = new HashMap<>();
        //setData();

        stocksAdapter = new StocksAdapter(stocks, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "reload: STARTED");
                swipeRefreshLayout.setRefreshing(false);

                if(isConnectedToNetwork()) {
                    ArrayList<Stock> stocksList = dbHandler.loadStocks();
                    for (int i = 0; i < stocksList.size(); i++) {
                        String symbol = stocksList.get(i).getStockSymbol();
                        new StockDataService(MainActivity.this).execute(symbol);
                    }
                    Log.d(TAG, "reload: COMPLETED");
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                builder.setTitle(R.string.no_network_connection_message_title);
                builder.setMessage(R.string.no_network_connection_message);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        new StockMasterService(this).execute();
        dbHandler = new DatabaseHandler(this);
        stocks.addAll(dbHandler.loadStocks());
        Collections.sort(stocks);
        stocksAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Shutting down DB");
        dbHandler.shutDown();
    }

    /*private void setData() {
        stocks.add(new Stock("TSLA", "Tesla", 123.01, 23.01, 23.0));
        stocks.add(new Stock("AMZN", "Amazon", 123.01, -23.01, 23.0));
        stocks.add(new Stock("FCBK", "Facebook", 123.01, 23.01, 23.0));
        stocks.add(new Stock("APPL", "Apple", 123.01, -23.01, 23.0));
        stocks.add(new Stock("GOGL", "Google", 123.01, 23.01, 23.0));
        stocks.add(new Stock("MSFT", "Microsoft", 123.01, -23.01, 23.0));
    }*/


    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: ");
        int position = recyclerView.getChildLayoutPosition(v);
        Stock stock = stocks.get(position);
        Toast.makeText(this, "On Click", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onLongClick(View v) {
        Log.d(TAG, "onLongClick: ");
        int position = recyclerView.getChildLayoutPosition(v);
        Stock stock = stocks.get(position);
        Toast.makeText(this, "On Long Click", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.mnuAddStock:
                addNewStock();
                break;
            default:
                Log.d(TAG, "Unknown menu item: " + item.getItemId());
                Toast.makeText(this, R.string.unknown_item, Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewStock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getApplicationContext().getString(R.string.stock_selection));
        builder.setMessage(getApplicationContext().getString(R.string.add_stock_message));
        builder.setCancelable(false);

        final EditText etStockSymbol = new EditText(this);
        etStockSymbol.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        etStockSymbol.setGravity(Gravity.CENTER_HORIZONTAL);
        builder.setView(etStockSymbol);

        // OK Button
        builder.setPositiveButton(getApplicationContext().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String stockSymbol = etStockSymbol.getText().toString().trim();
                if(TextUtils.isEmpty(stockSymbol)) {
                    Log.d(TAG, "setPositiveButton => onClick: Stock symbol empty");
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.empty_stock_message), Toast.LENGTH_SHORT).show();
                }
            }
        });
/*        builder.setPositiveButton(getApplicationContext().getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Checking for internet connection
                if (!checkConnectivity())
                    noConnectivity("Add Stock");
                else if (stockSymbolET.getText().toString().trim().isEmpty()) // In input field was empty
                    Toast.makeText(MainActivity.this, "Input field was empty.\nTry again enter stock symbol like TSLA..", Toast.LENGTH_LONG).show();
                else {
                    // Search for a possible stock with the given name
                    ArrayList<String> stockResults = searchStock(stockSymbolET.getText().toString().trim());
                    if (!stockResults.isEmpty()) {
                        ArrayList<String> stockOptions = new ArrayList<>(stockResults);

                        if (stockOptions.size() == 1) { // If only one stock option was found
                            if (checkDuplicate(stockOptions.get(0))) // Check if stock is already part of the list
                                duplicateStockDialog(stockSymbolET.getText().toString());
                            else // New stock found will be added to the stock list
                                addNewStock(stockOptions.get(0));
                        } else // If more than stock was found
                            multipleStocksFound(stockSymbolET.getText().toString(), stockOptions, stockOptions.size());
                    } else // If no stock options were found
                        stockNotFound(stockSymbolET.getText().toString());
                }
            }
        });*/

        // CANCEL Button
        builder.setNegativeButton(getApplicationContext().getString(R.string.cancel), new DialogInterface.OnClickListener() { // NEGATIVE Button
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Don't do anything
                Log.d(TAG, "onClick: Stock Add cancelled");
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void populateStockData(HashMap<String, String> stockMaster) {
        if (stockMaster != null && !stockMaster.isEmpty())
            this.stockMaster = stockMaster;
    }

    private ArrayList<String> searchStock(String stock) {
        Log.d(TAG, "searchStock: STARTED");
        ArrayList<String> stockList = new ArrayList<>();
        if (stockMaster != null && !stockMaster.isEmpty())
            return stockList;

        for (String symbol : stockMaster.keySet()) {
            String name = stockMaster.get(symbol);
            if (symbol.toUpperCase().contains(stock.toUpperCase()) || name.toUpperCase().contains(stock.toUpperCase()))
                stockList.add(symbol + " - " + name);
        }
        Log.d(TAG, "searchStock: COMPLETED");
        return stockList;
    }

    // Check if internet connectivity is established
    private boolean isConnectedToNetwork() {
        Log.d(TAG, "isConnectedToNetwork: Checking network connectivity");
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.d(TAG, "isConnectedToNetwork: Network not connected");
            Toast.makeText(this, "Cannot access ConnectivityManager", Toast.LENGTH_SHORT).show();
            return false;
        }

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            Log.d(TAG, "isConnectedToNetwork: Network connected");
            return true;
        }
        Log.d(TAG, "isConnectedToNetwork: Network not connected");
        return false;
    }

    public void populateStockFinancialData(Stock stock) {

    }
}