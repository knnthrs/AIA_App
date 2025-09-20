package com.example.signuploginrealtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ClientViewHolder> {

    private Context context;
    private List<coach_clients.Client> clientsList;

    public ClientsAdapter(Context context, List<coach_clients.Client> clientsList) {
        this.context = context;
        this.clientsList = clientsList;
    }

    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_client, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        coach_clients.Client client = clientsList.get(position);

        holder.clientName.setText(client.getName());
        holder.clientEmail.setText(client.getEmail());
        holder.clientStatus.setText(client.getStatus());

        // Set status color
        int statusColor;
        switch (client.getStatus()) {
            case "Active":
                statusColor = android.graphics.Color.parseColor("#4CAF50"); // Green
                break;
            case "Inactive":
                statusColor = android.graphics.Color.parseColor("#F44336"); // Red
                break;
            case "New":
                statusColor = android.graphics.Color.parseColor("#FF9800"); // Orange
                break;
            default:
                statusColor = android.graphics.Color.parseColor("#9E9E9E"); // Gray
                break;
        }
        holder.clientStatus.setTextColor(statusColor);

        // ✅ Navigate to Client_workouts_details
        holder.clientCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, Client_workouts_details.class);
            intent.putExtra("client_name", client.getName());
            intent.putExtra("client_email", client.getEmail());
            intent.putExtra("client_status", client.getStatus());
            intent.putExtra("client_weight", client.getWeight());
            intent.putExtra("client_height", client.getHeight());
            intent.putExtra("client_goal", client.getGoal());
            intent.putExtra("client_activity", client.getActivityLevel());

            // If context is not an Activity, add FLAG_ACTIVITY_NEW_TASK
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return clientsList.size();
    }

    public static class ClientViewHolder extends RecyclerView.ViewHolder {
        CardView clientCard;
        TextView clientName, clientEmail, clientStatus;

        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            clientCard = itemView.findViewById(R.id.client_card);
            clientName = itemView.findViewById(R.id.client_name);
            clientEmail = itemView.findViewById(R.id.client_email);
            clientStatus = itemView.findViewById(R.id.client_status);
        }
    }


}
