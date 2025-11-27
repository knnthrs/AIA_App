package com.example.signuploginrealtime.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.FoodRecommendation;

import java.util.List;

public class MainFoodRecoAdapter extends RecyclerView.Adapter<MainFoodRecoAdapter.ViewHolder> {

    private List<FoodRecommendation> foodList;
    private OnFoodClickListener listener;

    public interface OnFoodClickListener {
        void onAddClick(FoodRecommendation food);
    }

    public MainFoodRecoAdapter(List<FoodRecommendation> foodList, OnFoodClickListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_main_food_reco, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodRecommendation food = foodList.get(position);

        holder.tvFoodName.setText(food.getName());
        holder.tvCalories.setText(food.getCalories() + " cal");
        holder.tvProtein.setText(food.getProtein() + "g protein");

        // Show source badge
        if (food.getUserId() != null) {
            holder.tvSource.setText("👤 Personalized");
            holder.tvSource.setVisibility(View.VISIBLE);
        } else {
            holder.tvSource.setVisibility(View.GONE);
        }

        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddClick(food);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvCalories, tvProtein, tvSource;
        Button btnAdd;

        ViewHolder(View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvProtein = itemView.findViewById(R.id.tvProtein);
            tvSource = itemView.findViewById(R.id.tvSource);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}

