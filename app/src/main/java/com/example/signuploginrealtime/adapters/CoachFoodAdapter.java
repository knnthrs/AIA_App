package com.example.signuploginrealtime.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.FoodRecommendation;

import java.util.List;

public class CoachFoodAdapter extends RecyclerView.Adapter<CoachFoodAdapter.FoodViewHolder> {

    private List<FoodRecommendation> foodList;
    private OnFoodActionListener listener;

    public interface OnFoodActionListener {
        void onEdit(FoodRecommendation food);
        void onDelete(FoodRecommendation food);
    }

    public CoachFoodAdapter(List<FoodRecommendation> foodList, OnFoodActionListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coach_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodRecommendation food = foodList.get(position);

        holder.tvFoodName.setText(food.getName());
        holder.tvCalories.setText(food.getCalories() + " cal");
        holder.tvMacros.setText(String.format("P: %.1fg | C: %.1fg | F: %.1fg",
                food.getProtein(), food.getCarbs(), food.getFats()));

        // Show tags
        if (food.getTags() != null && !food.getTags().isEmpty()) {
            holder.tvTags.setText(String.join(", ", food.getTags()));
            holder.tvTags.setVisibility(View.VISIBLE);
        } else {
            holder.tvTags.setVisibility(View.GONE);
        }

        // Show notes if available
        if (food.getNotes() != null && !food.getNotes().isEmpty()) {
            holder.tvNotes.setText("\"" + food.getNotes() + "\"");
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(food));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(food));
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvCalories, tvMacros, tvTags, tvNotes;
        ImageButton btnEdit, btnDelete;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvMacros = itemView.findViewById(R.id.tvMacros);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

