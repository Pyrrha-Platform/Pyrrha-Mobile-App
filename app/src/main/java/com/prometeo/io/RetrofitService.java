package com.prometeo.io;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitService {

    @GET("get_status")
    Call<StatusCloud> get_status(@Query("firefighter_id") String firefighter_id, @Query("timestamp_mins") String timestamp_mins);

}
