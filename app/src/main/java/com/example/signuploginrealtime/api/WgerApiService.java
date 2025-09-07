package com.example.signuploginrealtime.api;

import com.example.signuploginrealtime.models.WgerExerciseResponse;
import com.example.signuploginrealtime.models.ExerciseImage;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.ApiResponse;

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

    // ✅ Fetch ALL exercise info (basic)
    @GET("exerciseinfo/")
    Call<ApiResponse<ExerciseInfo>> getAllExercisesInfo(
            @Query("language") int languageId
    );

    // Optional: fetch basic exercises with pagination
    @GET("exercise/")
    Call<WgerExerciseResponse> getExercises(
            @Query("language") int languageId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    // ✅ Fetch exercise images
    @GET("exerciseimage/")
    Call<ApiResponse<ExerciseImage>> getExerciseImages(
            @Query("exercise") int exerciseId
    );
}
