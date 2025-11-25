package com.example.signuploginrealtime.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.FoodRecommendation;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class UserFoodAdapter extends RecyclerView.Adapter<UserFoodAdapter.FoodViewHolder> {

    private List<FoodRecommendation> foodList;
    private OnFoodActionListener listener;

    public interface OnFoodActionListener {
        void onAddToMealPlan(FoodRecommendation food);
        void onViewDetails(FoodRecommendation food);
    }

    public UserFoodAdapter(List<FoodRecommendation> foodList, OnFoodActionListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodRecommendation food = foodList.get(position);

        holder.tvFoodName.setText(food.getName());
        holder.tvCalories.setText(food.getCalories() + " cal");
        holder.tvProtein.setText(String.format("Protein: %.1fg", food.getProtein()));
        holder.tvCarbs.setText(String.format("Carbs: %.1fg", food.getCarbs()));
        holder.tvFats.setText(String.format("Fats: %.1fg", food.getFats()));

        // Show source badge
        if (food.getCoachId() != null) {
            holder.tvSource.setText("👤 Coach Recommended");
            holder.tvSource.setBackgroundColor(Color.parseColor("#4CAF50"));
            holder.tvSource.setVisibility(View.VISIBLE);
        } else {
            holder.tvSource.setText("📊 Nutrition Database");
            holder.tvSource.setBackgroundColor(Color.parseColor("#2196F3"));
            holder.tvSource.setVisibility(View.VISIBLE);
        }

        // Show tags as chips
        holder.chipGroup.removeAllViews();
        if (food.getTags() != null && !food.getTags().isEmpty()) {
            holder.chipGroup.setVisibility(View.VISIBLE);
            for (String tag : food.getTags()) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(tag);
                chip.setClickable(false);
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(Color.parseColor("#1976D2"));
                holder.chipGroup.addView(chip);
            }
        } else {
            holder.chipGroup.setVisibility(View.GONE);
        }

        // Show coach notes if available
        if (food.getNotes() != null && !food.getNotes().isEmpty()) {
            holder.tvCoachNotes.setText("💬 " + food.getNotes());
            holder.tvCoachNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvCoachNotes.setVisibility(View.GONE);
        }

        holder.btnAddToMealPlan.setOnClickListener(v -> listener.onAddToMealPlan(food));
        holder.cardView.setOnClickListener(v -> listener.onViewDetails(food));
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvFoodName, tvCalories, tvProtein, tvCarbs, tvFats, tvSource, tvCoachNotes;
        ChipGroup chipGroup;
        Button btnAddToMealPlan;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvProtein = itemView.findViewById(R.id.tvProtein);
            tvCarbs = itemView.findViewById(R.id.tvCarbs);
            tvFats = itemView.findViewById(R.id.tvFats);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvCoachNotes = itemView.findViewById(R.id.tvCoachNotes);
            chipGroup = itemView.findViewById(R.id.chipGroup);
            btnAddToMealPlan = itemView.findViewById(R.id.btnAddToMealPlan);
        }
    }
}

