package com.example.signuploginrealtime.api;

import com.example.signuploginrealtime.models.ExerciseInfo;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface WgerApiService {
    // Fetch exercises from Wger
    @GET("exercise/")
    Call<ExerciseResponse> getExercises(
            @Query("language") int languageId, // e.g. 2 = English
            @Query("limit") int limit
    );

    // Wrapper class for the JSON response
    class ExerciseResponse {
        public List<ExerciseInfo> results;
    }
}
