package com.shantanoo.stockwatch;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "MainActivity";

    private static final String API_KEY = "pk_ab02406ce6c34caf85b5f61aa7c983bf";
    private static final String BASE_URL = "https://cloud.iexapis.com/stable/stock/";
    private static final String QUOTE_TOKEN = "quote?token=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }
}