package com.example.signuploginrealtime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.models.WorkoutHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder> {

    private final Context context;
    private List<WorkoutHistory> workouts;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public WorkoutHistoryAdapter(Context context, List<WorkoutHistory> workouts) {
        this.context = context;
        this.workouts = workouts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_workout_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutHistory workout = workouts.get(position);

        // Format date
        String dateStr = formatDate(workout.getTimestamp());
        holder.tvDate.setText(dateStr);

        // Duration
        holder.tvDuration.setText(workout.getDuration() + " mins");

        // Exercises count
        holder.tvExercisesCount.setText(String.valueOf(workout.getExercisesCount()));

        // Calories
        holder.tvCalories.setText(String.valueOf(workout.getCaloriesBurned()));

        // Weight
        holder.tvWeight.setText(String.format("%.1f", workout.getWeight()));

        // BMI
        holder.tvBmi.setText(String.format("%.1f", workout.getBmi()));

        // Body Focus
        if (workout.getBodyFocus() != null && !workout.getBodyFocus().isEmpty()) {
            holder.tvBodyFocus.setVisibility(View.VISIBLE);
            holder.tvBodyFocus.setText("🎯 Focus: " + String.join(", ", workout.getBodyFocus()));
        } else {
            holder.tvBodyFocus.setVisibility(View.GONE);
        }

        // View details click
        holder.btnViewDetails.setOnClickListener(v -> {
            Log.d("WorkoutHistoryAdapter", "📝 Opening detail for workout:");
            Log.d("WorkoutHistoryAdapter", "  ID: " + workout.getWorkoutId());
            Log.d("WorkoutHistoryAdapter", "  Timestamp: " + workout.getTimestamp());
            Log.d("WorkoutHistoryAdapter", "  Calories: " + workout.getCaloriesBurned());

            Intent intent = new Intent(context, WorkoutHistoryDetailActivity.class);
            intent.putExtra("workoutId", workout.getWorkoutId());
            intent.putExtra("timestamp", workout.getTimestamp());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    public void updateData(List<WorkoutHistory> newWorkouts) {
        this.workouts = newWorkouts;
        notifyDataSetChanged();
    }

    private String formatDate(long timestamp) {
        Date date = new Date(timestamp);
        Date now = new Date();

        // Check if today
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        if (dayFormat.format(date).equals(dayFormat.format(now))) {
            return "Today, " + timeFormat.format(date);
        }

        // Check if yesterday
        Date yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
        if (dayFormat.format(date).equals(dayFormat.format(yesterday))) {
            return "Yesterday, " + timeFormat.format(date);
        }

        // Otherwise show full date
        return dateFormat.format(date);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDuration, tvExercisesCount, tvCalories, tvWeight, tvBmi, tvBodyFocus, btnViewDetails;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvExercisesCount = itemView.findViewById(R.id.tv_exercises_count);
            tvCalories = itemView.findViewById(R.id.tv_calories);
            tvWeight = itemView.findViewById(R.id.tv_weight);
            tvBmi = itemView.findViewById(R.id.tv_bmi);
            tvBodyFocus = itemView.findViewById(R.id.tv_body_focus);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }
    }
}

