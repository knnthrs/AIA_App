package com.example.signuploginrealtime.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.WorkoutExercise;

import java.util.List;

public class WorkoutExerciseAdapter extends RecyclerView.Adapter<WorkoutExerciseAdapter.ExerciseViewHolder> {

    private final Context context;
    private final List<WorkoutExercise> exerciseList;
    private final boolean isCoachView; // true = coach can edit, false = client view only

    // --- Listener interface for updates ---
    public interface OnWorkoutChangedListener {
        void onWorkoutChanged();
    }

    private OnWorkoutChangedListener listener;

    public void setOnWorkoutChangedListener(OnWorkoutChangedListener listener) {
        this.listener = listener;
    }

    public WorkoutExerciseAdapter(Context context, List<WorkoutExercise> exerciseList, boolean isCoachView) {
        this.context = context;
        this.exerciseList = exerciseList;
        this.isCoachView = isCoachView;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_editable_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        WorkoutExercise exercise = exerciseList.get(position);

        // --- Bind name ---
        if (exercise.getExerciseInfo() != null) {
            holder.exerciseName.setText(exercise.getExerciseInfo().getName());
            if (exercise.getExerciseInfo().getInstructions() != null &&
                    !exercise.getExerciseInfo().getInstructions().isEmpty()) {
                holder.exerciseInstructions.setText(exercise.getExerciseInfo().getInstructions().get(0));
                holder.exerciseInstructions.setVisibility(View.VISIBLE);
            } else {
                holder.exerciseInstructions.setVisibility(View.GONE);
            }
        }

        // --- Bind sets & reps ---
        holder.setsCount.setText(String.valueOf(exercise.getSets()));
        holder.repsCount.setText(String.valueOf(exercise.getReps()));

        if (isCoachView) {
            // ✅ Coach can edit

            holder.increaseSets.setOnClickListener(v -> {
                exercise.setSets(exercise.getSets() + 1);
                holder.setsCount.setText(String.valueOf(exercise.getSets()));
                if (listener != null) listener.onWorkoutChanged();
            });

            holder.decreaseSets.setOnClickListener(v -> {
                if (exercise.getSets() > 0) {
                    exercise.setSets(exercise.getSets() - 1);
                    holder.setsCount.setText(String.valueOf(exercise.getSets()));
                    if (listener != null) listener.onWorkoutChanged();
                }
            });

            holder.increaseReps.setOnClickListener(v -> {
                exercise.setReps(exercise.getReps() + 1);
                holder.repsCount.setText(String.valueOf(exercise.getReps()));
                if (listener != null) listener.onWorkoutChanged();
            });

            holder.decreaseReps.setOnClickListener(v -> {
                if (exercise.getReps() > 0) {
                    exercise.setReps(exercise.getReps() - 1);
                    holder.repsCount.setText(String.valueOf(exercise.getReps()));
                    if (listener != null) listener.onWorkoutChanged();
                }
            });

            holder.removeExercise.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    exerciseList.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, exerciseList.size());
                    if (listener != null) listener.onWorkoutChanged();
                }
            });

        } else {
            // ❌ Client cannot edit
            holder.increaseSets.setVisibility(View.GONE);
            holder.decreaseSets.setVisibility(View.GONE);
            holder.increaseReps.setVisibility(View.GONE);
            holder.decreaseReps.setVisibility(View.GONE);
            holder.removeExercise.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    public static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName, setsCount, repsCount, exerciseInstructions;
        ImageView increaseSets, decreaseSets, increaseReps, decreaseReps, removeExercise;
        CardView card;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            setsCount = itemView.findViewById(R.id.sets_count);
            repsCount = itemView.findViewById(R.id.reps_count);
            exerciseInstructions = itemView.findViewById(R.id.exercise_instructions);

            increaseSets = itemView.findViewById(R.id.increase_sets_button);
            decreaseSets = itemView.findViewById(R.id.decrease_sets_button);
            increaseReps = itemView.findViewById(R.id.increase_reps_button);
            decreaseReps = itemView.findViewById(R.id.decrease_reps_button);
            removeExercise = itemView.findViewById(R.id.remove_exercise_button);

            card = (CardView) itemView;
        }
    }
}
