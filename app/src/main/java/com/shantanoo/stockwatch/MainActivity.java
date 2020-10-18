package com.shantanoo.stockwatch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
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
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
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
import com.shantanoo.stockwatch.service.StockFinancialDataDownloaderService;
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
    private static final int DUPLICATE_STOCK = 4;
    private static final int STOCK_NOT_FOUND = 5;

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

        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "reload: STARTED");
                swipeRefreshLayout.setRefreshing(false);

                if (isConnectedToNetwork()) {
                    ArrayList<Stock> stocksList = dbHandler.loadStocks();
                    for (int i = 0; i < stocksList.size(); i++) {
                        String symbol = stocksList.get(i).getStockSymbol();
                        new Thread(new StockFinancialDataDownloaderService(MainActivity.this, symbol)).start();
                    }
                    Log.d(TAG, "reload: COMPLETED");
                    return;
                }

                onNetworkDisconnected(REFRESH);
                Log.d(TAG, "reload: COMPLETED");
            }
        });

        new Thread(new StockNameDownloaderService(MainActivity.this)).start();
        dbHandler = new DatabaseHandler(MainActivity.this);
        stocks.addAll(dbHandler.loadStocks());
        Collections.sort(stocks);
        stocksAdapter.notifyDataSetChanged();
        if (isConnectedToNetwork()) {
            for (int i = 0; i < stocks.size(); i++) {
                String symbol = stocks.get(i).getStockSymbol();
                new Thread(new StockFinancialDataDownloaderService(MainActivity.this, symbol)).start();
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
        Log.d(TAG, "onDestroy: Shutting down DB");
        dbHandler.shutDown();
        super.onDestroy();
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
        builder.setMessage(String.format("%s %s%s", getString(R.string.delete_stock_message), stock.getStockSymbol(), getString(R.string.question)));

        // DELETE button
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHandler.deleteStock(stock.getStockSymbol());
                stocks.remove(position);
                stocksAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), String.format(getString(R.string.stock_remove_toast_message), stock.getStockSymbol()), Toast.LENGTH_SHORT).show();
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
            new Thread(new StockNameDownloaderService(this)).start();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.stock_selection));
        builder.setIcon(R.drawable.outline_search_black_48);
        builder.setMessage(getString(R.string.add_stock_message));
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
                        showDialog(STOCK_NOT_FOUND, stockSymbol);
                    else if (stockList.size() == 1) {
                        String stockOption = stockList.get(0);
                        if (checkDuplicate(stockOption))
                            showDialog(DUPLICATE_STOCK, stockSymbol);
                        else
                            addNewStock(stockOption);
                    } else {
                        showMultipleStocksFoundDialog(stockList);
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
        new Thread(new StockFinancialDataDownloaderService(MainActivity.this, symbol)).start();
        Stock stock = new Stock(symbol, stockNamesMaster.get(symbol));
        dbHandler.addStock(stock);
        Toast.makeText(this, String.format("Stock %s added to watchlist", selection), Toast.LENGTH_SHORT).show();
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
                stockList.add(String.format("%s %s %s", symbol, getString(R.string.hyphen), name));
        }
        Collections.sort(stockList);
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
        builder.setIcon(R.drawable.outline_cloud_off_black_48);
        String message = "";
        switch (action) {
            case ON_LOAD:
                message = getString(R.string.on_load_no_network_message);
                break;
            case ADD_STOCK:
                message = getString(R.string.add_stock_no_network_message);
                break;
            case REFRESH:
                message = getString(R.string.refresh_no_network_message);
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

    private void showDialog(int activityCode, String symbol) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.baseline_warning_black_48);
        AlertDialog dialog;
        switch (activityCode) {
            case DUPLICATE_STOCK:
                Log.d(TAG, "showDuplicateStockDialog:");
                builder.setTitle(getString(R.string.duplicate_stock_title));
                builder.setMessage(String.format(getString(R.string.duplicate_stock_message), symbol));
                dialog = builder.create();
                dialog.show();
                break;
            case STOCK_NOT_FOUND:
                Log.d(TAG, "showStockNotFoundDialog:");
                builder.setTitle(String.format(getString(R.string.stock_not_found_title), symbol));
                builder.setMessage(R.string.stock_not_found_message);
                dialog = builder.create();
                dialog.show();
                break;
            default:
                Toast.makeText(getApplicationContext(), "Failed to open Alert Dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMultipleStocksFoundDialog(ArrayList<String> stockOptions) {
        Log.d(TAG, "showMultipleStocksFoundDialog:");
        final String[] options = stockOptions.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.baseline_list_black_48);
        builder.setTitle(getString(R.string.make_a_selection));

        // Set the available options for selection
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkDuplicate(options[which]))
                    showDialog(DUPLICATE_STOCK, options[which]);
                else
                    addNewStock(options[which]);
            }
        });

        // Don't do anything if "NEVER MIND" is clicked
        builder.setNegativeButton(getString(R.string.never_mind), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "showMultipleStocksFoundDialog: Stock Selection cancelled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ListView listView = ((AlertDialog) dialog).getListView();

                listView.setDivider(new ColorDrawable(Color.GRAY));
                listView.setDividerHeight(1);

                final ListAdapter listViewAdapter = listView.getAdapter();
                listView.setAdapter(new ListAdapter() {
                    @Override
                    public boolean areAllItemsEnabled() {
                        return listViewAdapter.areAllItemsEnabled();
                    }

                    @Override
                    public boolean isEnabled(int position) {
                        return listViewAdapter.isEnabled(position);
                    }

                    @Override
                    public void registerDataSetObserver(DataSetObserver observer) {
                        listViewAdapter.registerDataSetObserver(observer);
                    }

                    @Override
                    public void unregisterDataSetObserver(DataSetObserver observer) {
                        listViewAdapter.unregisterDataSetObserver(observer);
                    }

                    @Override
                    public int getCount() {
                        return listViewAdapter.getCount();
                    }

                    @Override
                    public Object getItem(int position) {
                        return listViewAdapter.getItem(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return listViewAdapter.getItemId(position);
                    }

                    @Override
                    public boolean hasStableIds() {
                        return listViewAdapter.hasStableIds();
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = listViewAdapter.getView(position, convertView, parent);
                        TextView textView = (TextView) view;
                        textView.setTextSize(14);
                        return view;
                    }

                    @Override
                    public int getItemViewType(int position) {
                        return listViewAdapter.getItemViewType(position);
                    }

                    @Override
                    public int getViewTypeCount() {
                        return listViewAdapter.getViewTypeCount();
                    }

                    @Override
                    public boolean isEmpty() {
                        return listViewAdapter.isEmpty();
                    }
                });
            }
        });

        alertDialog.show();
    }
}