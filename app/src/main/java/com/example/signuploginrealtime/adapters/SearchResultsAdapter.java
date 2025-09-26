package com.example.signuploginrealtime.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.signuploginrealtime.R;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> exercises;
    private final OnAddClickListener listener;
    private final Set<String> alreadyAdded; // ✅ track added exercises

    // Constructor
    public SearchResultsAdapter(Context context,
                                List<Map<String, Object>> exercises,
                                OnAddClickListener listener) {
        this.context = context;
        this.exercises = exercises;
        this.listener = listener;
        this.alreadyAdded = new HashSet<>();
    }

    // Call this to sync already added exercises (from Firestore)
    public void setAlreadyAdded(Set<String> added) {
        alreadyAdded.clear();
        if (added != null) alreadyAdded.addAll(added);
        notifyDataSetChanged();
    }

    // ✅ NEW: Method to clear search results
    public void clearResults() {
        exercises.clear();
        notifyDataSetChanged();
    }

    // ✅ NEW: Method to update search results
    public void updateResults(List<Map<String, Object>> newExercises) {
        exercises.clear();
        if (newExercises != null) {
            exercises.addAll(newExercises);
        }
        notifyDataSetChanged();
    }

    // ✅ NEW: Check if results are empty
    public boolean isEmpty() {
        return exercises.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_exercise_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> exercise = exercises.get(position);

        // Name
        String name = (String) exercise.get("name");
        holder.exerciseName.setText(name != null ? name : "Unnamed exercise");

        // Category
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

        // ✅ Handle button state
        boolean isAdded = name != null && alreadyAdded.contains(name.toLowerCase());
        updateButtonState(holder.addButton, isAdded);

        if (!isAdded) {
            holder.addButton.setOnClickListener(v -> {
                if (listener != null && name != null) {
                    listener.onAddClicked(exercise);
                    // Mark as added immediately
                    alreadyAdded.add(name.toLowerCase());
                    notifyItemChanged(holder.getAdapterPosition());
                }
            });
        } else {
            holder.addButton.setOnClickListener(null); // disable listener
        }
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    // 🔹 Helper for button state
    private void updateButtonState(MaterialButton button, boolean isAdded) {
        if (isAdded) {
            button.setText("Added");
            button.setEnabled(false);
            button.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, android.R.color.darker_gray)
            );
        } else {
            button.setText("Add");
            button.setEnabled(true);
            button.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.black)
            );
        }
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
    public interface OnAddClickListener {
        void onAddClicked(Map<String, Object> exercise);
    }
}