package org.pyrrha_platform.io;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitAdapter {

    Retrofit retrofit;

    public RetrofitAdapter() {
    }

    public Retrofit getAdapter() {
        retrofit = new Retrofit.Builder()
                .baseUrl("http://pyrrha-kubernetes-8877e2c915ebdcc9b5067e5cb2150b3c-0000.eu-gb.containers.appdomain.cloud/rulesdecision/")
                // .baseUrl("http://159.122.217.91/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
