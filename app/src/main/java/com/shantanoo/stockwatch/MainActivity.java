package com.shantanoo.stockwatch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.shantanoo.stockwatch.adapter.StocksAdapter;
import com.shantanoo.stockwatch.model.Stock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "MainActivity";

    private static final String API_KEY = "pk_ab02406ce6c34caf85b5f61aa7c983bf";
    private static final String BASE_URL = "https://cloud.iexapis.com/stable/stock/";
    private static final String QUOTE_TOKEN = "quote?token=";

    private List<Stock> stocks;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private StocksAdapter stocksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.rvStocks);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        stocks = new ArrayList<>();
        setData();

        stocksAdapter = new StocksAdapter(stocks, this);
        recyclerView.setAdapter(stocksAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //get data
                Collections.shuffle(stocks);
                stocksAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getApplicationContext(), "Data Refreshed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setData() {
        stocks.add(new Stock("TSLA", "Tesla", 123.01, 23.01, 23.0));
        stocks.add(new Stock("AMZN", "Amazon", 123.01, -23.01, 23.0));
        stocks.add(new Stock("FCBK", "Facebook", 123.01, 23.01, 23.0));
        stocks.add(new Stock("APPL", "Apple", 123.01, -23.01, 23.0));
        stocks.add(new Stock("GOGL", "Google", 123.01, 23.01, 23.0));
        stocks.add(new Stock("MSFT", "Microsoft", 123.01, -23.01, 23.0));
    }


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
}