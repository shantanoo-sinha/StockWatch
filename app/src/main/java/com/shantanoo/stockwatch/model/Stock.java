package com.shantanoo.stockwatch.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Shantanoo on 10/2/2020.
 */
public class Stock implements Serializable, Comparable<Stock> {

    private String stockSymbol;
    private String stockName;
    private double stockPrice;
    private double stockPriceChange;
    private double stockPriceChangePercentage;

    public Stock() {
    }

    public Stock(String stockSymbol, String stockName, double stockPrice, double stockPriceChange, double stockPriceChangePercentage) {
        this.stockSymbol = stockSymbol;
        this.stockName = stockName;
        this.stockPrice = stockPrice;
        this.stockPriceChange = stockPriceChange;
        this.stockPriceChangePercentage = stockPriceChangePercentage;
    }

    public Stock(String stockSymbol, String stockName) {
        this.stockSymbol = stockSymbol;
        this.stockName = stockName;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getStockPrice() {
        return stockPrice;
    }

    public void setStockPrice(double stockPrice) {
        this.stockPrice = stockPrice;
    }

    public double getStockPriceChange() {
        return stockPriceChange;
    }

    public void setStockPriceChange(double stockPriceChange) {
        this.stockPriceChange = stockPriceChange;
    }

    public double getStockPriceChangePercentage() {
        return stockPriceChangePercentage;
    }

    public void setStockPriceChangePercentage(double stockPriceChangePercentage) {
        this.stockPriceChangePercentage = stockPriceChangePercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return stockSymbol.equals(stock.stockSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockSymbol);
    }

    @Override
    public String toString() {
        return "Stock{" +
                "stockSymbol='" + stockSymbol + '\'' +
                ", stockName='" + stockName + '\'' +
                ", stockPrice=" + stockPrice +
                ", stockPriceChange=" + stockPriceChange +
                ", stockPriceChangePercentage=" + stockPriceChangePercentage +
                '}';
    }

    @Override
    public int compareTo(Stock o) {
        if (o.getStockSymbol() == null)
            return 0;
        return o.getStockSymbol().compareTo(this.getStockSymbol());
    }
}
