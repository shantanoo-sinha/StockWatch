package com.shantanoo.stockwatch.viewholder;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shantanoo.stockwatch.R;
import com.shantanoo.stockwatch.model.Stock;

import java.util.Locale;

/**
 * Created by Shantanoo on 10/2/2020.
 */
public class ViewHolder extends RecyclerView.ViewHolder {

    private TextView tvStockSymbol;
    private TextView tvStockName;
    private TextView tvStockPrice;
    private TextView tvStockPriceChange;
    private TextView tvStockPriceChangePercentage;
    private ImageView ivStockChangeDirection;

    public ViewHolder(@NonNull View itemView) {
        super(itemView);

        tvStockSymbol = itemView.findViewById(R.id.tvStockSymbol);
        tvStockName = itemView.findViewById(R.id.tvStockName);
        tvStockPrice = itemView.findViewById(R.id.tvStockPrice);
        tvStockPriceChange = itemView.findViewById(R.id.tvStockPriceChange);
        tvStockPriceChangePercentage = itemView.findViewById(R.id.tvStockPriceChangePercentage);
        ivStockChangeDirection = itemView.findViewById(R.id.ivStockChangeDirection);
    }

    public void bind(Stock stock, int colorId, int imageId) {
        tvStockSymbol.setTextColor(colorId);
        tvStockName.setTextColor(colorId);
        tvStockPrice.setTextColor(colorId);
        tvStockPriceChange.setTextColor(colorId);
        tvStockPriceChangePercentage.setTextColor(colorId);

        ivStockChangeDirection.setColorFilter(colorId);
        ivStockChangeDirection.setImageResource(imageId);

        tvStockSymbol.setText(stock.getStockSymbol());
        tvStockName.setText(stock.getStockName());
        tvStockPrice.setText(String.format(Locale.US, "%.2f", stock.getStockPrice()));
        tvStockPriceChange.setText(String.format(Locale.US, "%.2f", stock.getStockPriceChange()));
        tvStockPriceChangePercentage.setText(String.format(Locale.US, "(%.2f%%)", stock.getStockPriceChangePercentage()));
    }
}
