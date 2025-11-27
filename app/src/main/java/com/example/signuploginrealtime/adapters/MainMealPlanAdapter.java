package com.example.signuploginrealtime.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserMealPlan;

import java.util.List;

public class MainMealPlanAdapter extends RecyclerView.Adapter<MainMealPlanAdapter.ViewHolder> {

    private List<UserMealPlan> mealList;
    private OnMealClickListener listener;

    public interface OnMealClickListener {
        void onRemoveClick(UserMealPlan meal);
    }

    public MainMealPlanAdapter(List<UserMealPlan> mealList, OnMealClickListener listener) {
        this.mealList = mealList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_main_meal_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserMealPlan meal = mealList.get(position);

        holder.tvFoodName.setText(meal.getFoodName());
        holder.tvMealType.setText(meal.getMealType());
        holder.tvCalories.setText(meal.getCalories() + " cal");

        String macros = String.format("P: %.0fg • C: %.0fg • F: %.0fg",
                meal.getProtein(), meal.getCarbs(), meal.getFats());
        holder.tvMacros.setText(macros);

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvMealType, tvCalories, tvMacros;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvMealType = itemView.findViewById(R.id.tvMealType);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvMacros = itemView.findViewById(R.id.tvMacros);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}

