package com.shantanoo.stockwatch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.shantanoo.stockwatch.adapter.StocksAdapter;
import com.shantanoo.stockwatch.database.DatabaseHandler;
import com.shantanoo.stockwatch.decoration.DividerItemDecoration;
import com.shantanoo.stockwatch.model.Stock;
import com.shantanoo.stockwatch.service.StockDataDownloaderService;
import com.shantanoo.stockwatch.service.StockNameDownloaderService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "MainActivity";

    private static final int ON_LOAD = 1;
    private static final int ADD_STOCK = 2;
    private static final int REFRESH = 3;

    private static final String MARKET_WATCH_URL = "http://www.marketwatch.com/investing/stock/";

    private List<Stock> stocks;
    private Map<String, String> stockNamesMaster;

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
        stockNamesMaster = new HashMap<>();

        stocksAdapter = new StocksAdapter(stocks, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(ContextCompat.getDrawable(getApplicationContext(), R.drawable.recycler_view_divider)));

        /*swipeRefreshLayout.setProgressViewOffset(true, 0, 200);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(android.R.color.holo_blue_bright));*/
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "reload: STARTED");
                swipeRefreshLayout.setRefreshing(false);

                if (isConnectedToNetwork()) {
                    ArrayList<Stock> stocksList = dbHandler.loadStocks();
                    for (int i = 0; i < stocksList.size(); i++) {
                        String symbol = stocksList.get(i).getStockSymbol();
                        new StockDataDownloaderService(MainActivity.this).execute(symbol);
                    }
                    Log.d(TAG, "reload: COMPLETED");
                    return;
                }

                onNetworkDisconnected(REFRESH);
                Log.d(TAG, "reload: COMPLETED");
            }
        });

        new StockNameDownloaderService(this).execute();
        dbHandler = new DatabaseHandler(this);
        stocks.addAll(dbHandler.loadStocks());
        Collections.sort(stocks);
        stocksAdapter.notifyDataSetChanged();
        if (isConnectedToNetwork()) {
            for (int i = 0; i < stocks.size(); i++) {
                String symbol = stocks.get(i).getStockSymbol();
                new StockDataDownloaderService(MainActivity.this).execute(symbol);
            }
        } else
            onNetworkDisconnected(ON_LOAD);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        stocksAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Shutting down DB");
        dbHandler.shutDown();
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: ");
        int position = recyclerView.getChildLayoutPosition(v);
        Stock stock = stocks.get(position);

        String marketWatchURL = MARKET_WATCH_URL + stock.getStockSymbol();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(marketWatchURL));
        startActivity(intent);
    }

    @Override
    public boolean onLongClick(View v) {
        Log.d(TAG, "onLongClick: ");
        final int position = recyclerView.getChildLayoutPosition(v);
        final Stock stock = stocks.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_stock_title);
        builder.setIcon(R.drawable.outline_delete_outline_black_48);
        builder.setMessage(getString(R.string.delete_stock_message) + " " + stock.getStockSymbol() + getString(R.string.question));

        // DELETE button
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHandler.deleteStock(stock.getStockSymbol());
                stocks.remove(position);
                stocksAdapter.notifyDataSetChanged();
                Log.d(TAG, "onLongClick: Stock Deleted");
            }
        });

        // CANCEL button
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                Log.d(TAG, "onLongClick: Cancelled");
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
                showAddNewStockDialog();
                break;
            default:
                Log.d(TAG, "Unknown menu item: " + item.getItemId());
                Toast.makeText(this, getString(R.string.unknown_item), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddNewStockDialog() {

        // If no network, show dialog
        if (!isConnectedToNetwork()) {
            onNetworkDisconnected(ADD_STOCK);
            return;
        }

        if (stockNamesMaster == null || stockNamesMaster.isEmpty())
            new StockNameDownloaderService(this).execute();

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
                if (TextUtils.isEmpty(stockSymbol)) {
                    Log.d(TAG, "setPositiveButton => onClick: Stock symbol empty");
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.empty_stock_message), Toast.LENGTH_SHORT).show();
                } else {
                    ArrayList<String> stockList = searchStock(stockSymbol);
                    if (stockList.isEmpty())
                        showStockNotFoundDialog(stockSymbol);
                    else if (stockList.size() == 1) {
                        String stockOption = stockList.get(0);
                        if (checkDuplicate(stockOption))
                            showDuplicateStockDialog(stockSymbol);
                        else
                            addNewStock(stockOption);
                    } else {
                        showMultipleStocksFoundDialog(stockSymbol, stockList);
                    }
                }
            }
        });

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

    private void addNewStock(String selection) {
        Log.d(TAG, "addNewStock:");
        String symbol = selection.split(getString(R.string.hyphen))[0].trim();
        new StockDataDownloaderService(MainActivity.this).execute(symbol);
        Stock stock = new Stock(symbol, stockNamesMaster.get(symbol));
        dbHandler.addStock(stock);
    }

    public void populateStockNamesData(HashMap<String, String> stockMaster) {
        Log.d(TAG, "populateStockNamesData: ");
        if (stockMaster != null && !stockMaster.isEmpty())
            this.stockNamesMaster = stockMaster;
    }

    private ArrayList<String> searchStock(String searchStock) {
        Log.d(TAG, "searchStock:");
        ArrayList<String> stockList = new ArrayList<>();
        if (TextUtils.isEmpty(searchStock) || stockNamesMaster == null || stockNamesMaster.isEmpty())
            return stockList;

        for (String symbol : stockNamesMaster.keySet()) {
            String name = stockNamesMaster.get(symbol);
            if (symbol.toUpperCase().contains(searchStock.toUpperCase()) || (name != null && name.toUpperCase().contains(searchStock.toUpperCase())))
                stockList.add(symbol + " - " + name);
        }
        return stockList;
    }

    private boolean checkDuplicate(String input) {
        Log.d(TAG, "checkDuplicate: Checking for duplicate stock");
        String symbol = input.split(getString(R.string.hyphen))[0].trim();
        Stock stock = new Stock(symbol, null);
        return stocks.contains(stock);
    }

    // Check if internet connectivity is established
    private boolean isConnectedToNetwork() {
        Log.d(TAG, "isConnectedToNetwork: Checking network connectivity");
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

    public void populateStocksData(Stock stock) {
        Log.d(TAG, "populateStocksData: ");
        if (stock != null) {
            int index = stocks.indexOf(stock);
            if (index > -1)
                stocks.remove(index);
            stocks.add(stock);
            Collections.sort(stocks);
            stocksAdapter.notifyDataSetChanged();
        }
    }

    public void onNetworkDisconnected(int action) {
        Log.d(TAG, "onNetworkDisconnected: STARTED");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.no_network_connection));
        String message = "";
        switch (action) {
            case ON_LOAD:
                message = "Stocks App Needs Network Connection To Fetch Latest Prices\nLoading Saved Stocks With Price As $0";
                break;
            case ADD_STOCK:
                message = "Stocks Cannot Be Added Without A Network Connection";
                break;
            case REFRESH:
                message = "Stocks Cannot Be Updated Without A Network Connection";
                break;
            default:
                Log.e(TAG, "onNetworkDisconnected: Unknown operation");
                break;
        }
        builder.setMessage(message);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Log.d(TAG, "onNetworkDisconnected: COMPLETED");
    }

    private void showDuplicateStockDialog(String symbol) {
        Log.d(TAG, "showDuplicateStockDialog:");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.baseline_warning_black_48);
        builder.setTitle(getString(R.string.duplicate_stock));
        builder.setMessage("Stock Symbol " + symbol + " is already displayed");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showStockNotFoundDialog(String symbol) {
        Log.d(TAG, "showStockNotFoundDialog:");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Symbol Not Found: " + symbol);
        builder.setMessage("Data for stock symbol");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showMultipleStocksFoundDialog(final String symbol, ArrayList<String> stockOptions) {
        Log.d(TAG, "showMultipleStocksFoundDialog:");
        final String[] options = stockOptions.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make a Selection");
        builder.setIcon(R.drawable.baseline_list_black_48);

        // Set the available options for selection
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkDuplicate(options[which]))
                    showDuplicateStockDialog(symbol);
                else
                    addNewStock(options[which]);
            }
        });

        // Don't do anything if "NEVER MIND" is clicked
        builder.setNegativeButton("NEVER MIND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "showMultipleStocksFoundDialog: Stock Selection cancelled");
            }
        });

        AlertDialog dialog = builder.create();
        ListView listView = dialog.getListView();
        listView.setDivider(new ColorDrawable(Color.GRAY));
        listView.setDividerHeight(1);
        dialog.show();
    }
}