package com.example.signuploginrealtime.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.signuploginrealtime.models.Exercise;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WgerApiService {
    private static final String BASE_URL = "https://wger.de/api/v2/";
    private OkHttpClient client;
    private Gson gson;

    public WgerApiService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public List<Exercise> getExercisesByCategory(int categoryId) {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "exercise/?language=2&category=" + categoryId + "&limit=20")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();

                // Parse the response
                WgerApiResponse apiResponse = gson.fromJson(jsonResponse, WgerApiResponse.class);
                return apiResponse != null ? apiResponse.getResults() : new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Exercise> getExercisesByEquipment(int equipmentId) {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "exercise/?language=2&equipment=" + equipmentId + "&limit=20")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();

                WgerApiResponse apiResponse = gson.fromJson(jsonResponse, WgerApiResponse.class);
                return apiResponse != null ? apiResponse.getResults() : new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // Inner class for API response
    private static class WgerApiResponse {
        private List<Exercise> results;
        private String next;
        private int count;

        public List<Exercise> getResults() {
            return results != null ? results : new ArrayList<>();
        }

        public void setResults(List<Exercise> results) {
            this.results = results;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
