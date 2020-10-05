package com.shantanoo.stockwatch.adapter;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shantanoo.stockwatch.MainActivity;
import com.shantanoo.stockwatch.R;
import com.shantanoo.stockwatch.model.Stock;
import com.shantanoo.stockwatch.viewholder.ViewHolder;

import java.util.List;

/**
 * Created by Shantanoo on 10/2/2020.
 */
public class StocksAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final String TAG = "StocksAdapter";

    private List<Stock> stocks;
    private MainActivity mainActivity;

    public StocksAdapter(List<Stock> stocks, MainActivity mainActivity) {
        this.stocks = stocks;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: Creating View Holder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stock_row, parent, false);
        view.setOnClickListener(mainActivity);
        view.setOnLongClickListener(mainActivity);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: Binding View Holder");
        Stock stock = stocks.get(position);
        if (stock.getStockPriceChange() < 0) {
            holder.bind(stock, Color.RED, R.drawable.arrow_down);
            return;
        }
        holder.bind(stock, Color.GREEN, R.drawable.arrow_up);
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }
}