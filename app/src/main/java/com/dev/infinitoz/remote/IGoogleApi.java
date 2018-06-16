package com.dev.infinitoz.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface IGoogleApi {

    @GET
    Call<String> getDataFromGoogleApi(@Url String url);
}
