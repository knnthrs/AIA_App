package com.example.signuploginrealtime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;

import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private Context context;
    private List<WorkoutExercise> exercises;

    // Constructor
    public WorkoutAdapter(Context context, List<WorkoutExercise> exercises) {
        this.context = context;
        this.exercises = exercises;
    }

    // Setter (to refresh list)
    public void setExercises(List<WorkoutExercise> newExercises) {
        this.exercises = newExercises;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exercise_card, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutExercise workoutExercise = exercises.get(position);
        ExerciseInfo info = workoutExercise.getExerciseInfo();

        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvName.setText(info != null && info.getName() != null ? info.getName() : "Unknown");
        holder.tvDetails.setText(info != null && info.getDescription() != null ? info.getDescription() : "No description");
        holder.tvRest.setText("Rest: " + workoutExercise.getRestSeconds() + "s");

        // ✅ Show muscles and equipment using helper methods
        if (info != null) {
            List<String> muscles = info.getMuscleNames();
            List<String> equipment = info.getEquipmentNames();

            holder.tvMuscles.setText(muscles.isEmpty() ? "Muscles: None" : "Muscles: " + muscles);
            holder.tvEquipment.setText(equipment.isEmpty() ? "Equipment: None" : "Equipment: " + equipment);
        } else {
            holder.tvMuscles.setText("Muscles: Unknown");
            holder.tvEquipment.setText("Equipment: Unknown");
        }
    }

    @Override
    public int getItemCount() {
        return exercises != null ? exercises.size() : 0;
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvName, tvDetails, tvRest, tvMuscles, tvEquipment;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tv_exercise_number);
            tvName = itemView.findViewById(R.id.tv_exercise_name);
            tvDetails = itemView.findViewById(R.id.tv_exercise_details);
            tvRest = itemView.findViewById(R.id.tv_exercise_rest);
            tvMuscles = itemView.findViewById(R.id.tv_exercise_muscles);
            tvEquipment = itemView.findViewById(R.id.tv_exercise_equipment);
        }
    }
}
