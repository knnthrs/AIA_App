package com.example.signuploginrealtime.api;

import com.example.signuploginrealtime.models.WgerExerciseResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WgerApiService {

    @GET("exercise/")
    Call<WgerExerciseResponse> getExercises(
            @Query("language") int language,
            @Query("limit") int limit,
            @Query("offset") int offset
    );
}