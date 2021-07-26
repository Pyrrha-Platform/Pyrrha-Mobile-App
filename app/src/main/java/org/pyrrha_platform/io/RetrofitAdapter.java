package org.pyrrha_platform.io;

import org.pyrrha_platform.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitAdapter {

    Retrofit retrofit;

    public RetrofitAdapter() {
    }

    public Retrofit getAdapter() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.FLAVOR_RULES_DECISION_SERVICE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
