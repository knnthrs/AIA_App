package com.example.signuploginrealtime.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserMealPlan;

import java.util.List;

public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.ViewHolder> {

    private List<UserMealPlan> mealPlanList;
    private OnMealPlanActionListener listener;

    public interface OnMealPlanActionListener {
        void onRemoveFromMealPlan(UserMealPlan mealPlan);
    }

    public MealPlanAdapter(List<UserMealPlan> mealPlanList, OnMealPlanActionListener listener) {
        this.mealPlanList = mealPlanList;
        this.listener = listener;
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
        UserMealPlan mealPlan = mealPlanList.get(position);

        holder.tvFoodName.setText(mealPlan.getFoodName());
        holder.tvServingSize.setText(mealPlan.getServingSize() != null ? mealPlan.getServingSize() : "100g");
        holder.tvCalories.setText(mealPlan.getCalories() + " cal");
        holder.tvProtein.setText("P: " + String.format("%.1f", mealPlan.getProtein()) + "g");
        holder.tvCarbs.setText("C: " + String.format("%.1f", mealPlan.getCarbs()) + "g");
        holder.tvFats.setText("F: " + String.format("%.1f", mealPlan.getFats()) + "g");

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveFromMealPlan(mealPlan);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealPlanList.size();
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

