package com.example.signuploginrealtime.api;

import com.example.signuploginrealtime.models.WgerExerciseResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WgerApiService {

    // Fetch exercise info (includes name and description)
    @GET("exerciseinfo/")
    Call<WgerExerciseResponse> getExercisesInfo(
            @Query("id__in") String ids,      // comma-separated IDs, e.g., "9,12,20"
            @Query("language") int languageId // 2 = English
    );

    // Optional: fetch basic exercises with pagination
    @GET("exercise/")
    Call<WgerExerciseResponse> getExercises(
            @Query("language") int languageId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );
}
