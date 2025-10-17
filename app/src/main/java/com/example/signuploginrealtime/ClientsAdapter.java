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
    private OnClientLongClickListener longClickListener;

    // Interface for long click callback
    public interface OnClientLongClickListener {
        void onClientLongClick(coach_clients.Client client);
    }

    // Original constructor (for backward compatibility)
    public ClientsAdapter(Context context, List<coach_clients.Client> clientsList) {
        this.context = context;
        this.clientsList = clientsList;
    }

    // New constructor with long click listener
    public ClientsAdapter(Context context, List<coach_clients.Client> clientsList, OnClientLongClickListener longClickListener) {
        this.context = context;
        this.clientsList = clientsList;
        this.longClickListener = longClickListener;
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

        // Set avatar with first letter of name
        if (client.getName() != null && !client.getName().isEmpty()) {
            holder.clientAvatar.setText(String.valueOf(client.getName().charAt(0)).toUpperCase());
        }

        // Set status background based on client status
        String status = client.getStatus();
        if (status != null) {
            switch (status) {
                case "Active":
                    holder.clientStatus.setBackgroundResource(R.drawable.status_active);
                    break;
                case "Inactive":
                    holder.clientStatus.setBackgroundResource(R.drawable.status_inactive);
                    break;
                case "New":
                    holder.clientStatus.setBackgroundResource(R.drawable.status_new);
                    break;
                default:
                    holder.clientStatus.setBackgroundResource(R.drawable.status_unknown);
                    break;
            }
        } else {
            holder.clientStatus.setBackgroundResource(R.drawable.status_unknown);
        }

        // Regular click - Navigate to Client_workouts_details
        holder.clientCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, Client_workouts_details.class);
            intent.putExtra("client_uid", client.getUid());

            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });

        // Long press - Archive client
        holder.clientCard.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onClientLongClick(client);
                return true; // Consume the long click event
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return clientsList.size();
    }

    public static class ClientViewHolder extends RecyclerView.ViewHolder {
        CardView clientCard;
        TextView clientName, clientEmail, clientStatus, clientAvatar;

        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            clientCard = itemView.findViewById(R.id.client_card);
            clientName = itemView.findViewById(R.id.client_name);
            clientEmail = itemView.findViewById(R.id.client_email);
            clientStatus = itemView.findViewById(R.id.client_status);
            clientAvatar = itemView.findViewById(R.id.client_avatar);
        }
    }

}