package com.example.signuploginrealtime.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.signuploginrealtime.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Map;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> exercises;
    private final OnExerciseClickListener listener;

    // Constructor
    public SearchResultsAdapter(Context context, List<Map<String, Object>> exercises, OnExerciseClickListener listener) {
        this.context = context;
        this.exercises = exercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exercise_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> exercise = exercises.get(position);

        // Name
        String name = (String) exercise.get("name");
        holder.exerciseName.setText(name != null ? name : "Unnamed exercise");

        // Category (bodyParts list → string)
        Object bodyPartsObj = exercise.get("bodyParts");
        if (bodyPartsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> bodyParts = (List<String>) bodyPartsObj;
            holder.exerciseCategory.setText(TextUtils.join(", ", bodyParts));
        } else {
            holder.exerciseCategory.setText("No category");
        }

        // GIF preview
        String gifUrl = (String) exercise.get("gifUrl");
        if (gifUrl != null && !gifUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .asGif()
                    .load(gifUrl)
                    .into(holder.exerciseGif);
        } else {
            holder.exerciseGif.setImageResource(R.drawable.loading_placeholder);
        }

        // Add button
        holder.addButton.setOnClickListener(v -> {
            if (listener != null) listener.onExerciseClick(exercise);
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView exerciseGif;
        TextView exerciseName, exerciseCategory;
        MaterialButton addButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseGif = itemView.findViewById(R.id.iv_exercise_gif);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            exerciseCategory = itemView.findViewById(R.id.exercise_category);
            addButton = itemView.findViewById(R.id.add_exercise_button);
        }
    }

    // 👇 Define the click listener
    public interface OnExerciseClickListener {
        void onExerciseClick(Map<String, Object> exercise);
    }
}
