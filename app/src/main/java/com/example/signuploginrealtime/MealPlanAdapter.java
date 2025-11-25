package com.example.signuploginrealtime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.models.UserMealPlan;

import java.util.List;

public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.ViewHolder> {

    private List<UserMealPlan> mealList;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(UserMealPlan mealPlan);
    }

    public MealPlanAdapter(List<UserMealPlan> mealList, OnDeleteClickListener deleteListener) {
        this.mealList = mealList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_plan_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserMealPlan meal = mealList.get(position);

        holder.tvFoodName.setText(meal.getFoodName());
        holder.tvServingSize.setText(meal.getServingSize());
        holder.tvCalories.setText(meal.getCalories() + " cal");
        holder.tvProtein.setText("P: " + String.format("%.1f", meal.getProtein()) + "g");
        holder.tvCarbs.setText("C: " + String.format("%.1f", meal.getCarbs()) + "g");
        holder.tvFats.setText("F: " + String.format("%.1f", meal.getFats()) + "g");

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvServingSize, tvCalories, tvProtein, tvCarbs, tvFats;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvServingSize = itemView.findViewById(R.id.tvServingSize);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvProtein = itemView.findViewById(R.id.tvProtein);
            tvCarbs = itemView.findViewById(R.id.tvCarbs);
            tvFats = itemView.findViewById(R.id.tvFats);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
