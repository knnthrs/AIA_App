package com.example.signuploginrealtime.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.Coach;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class CoachSelectionAdapter extends RecyclerView.Adapter<CoachSelectionAdapter.CoachViewHolder> {

    private List<Coach> coaches;
    private OnCoachSelectedListener listener;

    public interface OnCoachSelectedListener {
        void onCoachSelected(Coach coach);
    }

    public CoachSelectionAdapter(List<Coach> coaches, OnCoachSelectedListener listener) {
        this.coaches = coaches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CoachViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coach_selection, parent, false);
        return new CoachViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoachViewHolder holder, int position) {
        Coach coach = coaches.get(position);
        holder.bind(coach);
    }

    @Override
    public int getItemCount() {
        return coaches.size();
    }

    class CoachViewHolder extends RecyclerView.ViewHolder {
        TextView tvCoachName, tvExperience, tvSkillsLabel;
        ChipGroup chipGroupSkills;

        public CoachViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCoachName = itemView.findViewById(R.id.tvCoachName);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvSkillsLabel = itemView.findViewById(R.id.tvSkillsLabel);
            chipGroupSkills = itemView.findViewById(R.id.chipGroupSkills);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCoachSelected(coaches.get(position));
                }
            });
        }

        public void bind(Coach coach) {
            tvCoachName.setText(coach.getFullname());

            // Show experience if available
            if (coach.getYearsOfExperience() > 0) {
                tvExperience.setVisibility(View.VISIBLE);
                tvExperience.setText(coach.getYearsOfExperience() + " years experience");
            } else {
                tvExperience.setVisibility(View.GONE);
            }

            // Clear previous chips
            chipGroupSkills.removeAllViews();

            // Add skill chips
            List<String> skills = coach.getSkills();
            if (skills != null && !skills.isEmpty()) {
                tvSkillsLabel.setVisibility(View.VISIBLE);
                for (String skill : skills) {
                    Chip chip = new Chip(itemView.getContext());
                    chip.setText(skill);
                    chip.setChipBackgroundColorResource(R.color.black);
                    chip.setTextColor(Color.WHITE);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    chip.setTextSize(12);
                    chipGroupSkills.addView(chip);
                }
            } else {
                // Show message if no skills specified
                tvSkillsLabel.setVisibility(View.VISIBLE);
                Chip chip = new Chip(itemView.getContext());
                chip.setText("No specializations yet");
                chip.setChipBackgroundColorResource(android.R.color.darker_gray);
                chip.setTextColor(Color.WHITE);
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setTextSize(12);
                chipGroupSkills.addView(chip);
            }
        }
    }
}

