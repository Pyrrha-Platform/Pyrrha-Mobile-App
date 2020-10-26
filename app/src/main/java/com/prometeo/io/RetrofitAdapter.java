package com.prometeo.io;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitAdapter {

    Retrofit retrofit;

    public RetrofitAdapter(){
    }

    public Retrofit getAdapter(){
        retrofit = new Retrofit.Builder()
                .baseUrl("http://159.122.217.91/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
