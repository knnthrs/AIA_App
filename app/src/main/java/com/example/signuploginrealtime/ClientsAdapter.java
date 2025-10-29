package com.example.signuploginrealtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ClientViewHolder> {

    private Context context;
    private List<coach_clients.Client> clientsList;
    private OnClientLongClickListener longClickListener;

    public interface OnClientLongClickListener {
        void onClientLongClick(coach_clients.Client client);
    }

    public ClientsAdapter(Context context, List<coach_clients.Client> clientsList) {
        this.context = context;
        this.clientsList = clientsList;
    }

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

        String profilePictureUrl = client.getProfilePictureUrl();

        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            holder.clientProfileImage.setVisibility(View.VISIBLE);
            holder.clientAvatar.setVisibility(View.GONE);

            Glide.with(context)
                    .load(profilePictureUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.clientProfileImage);
        } else {
            holder.clientProfileImage.setVisibility(View.GONE);
            holder.clientAvatar.setVisibility(View.VISIBLE);

            if (client.getName() != null && !client.getName().isEmpty()) {
                holder.clientAvatar.setText(String.valueOf(client.getName().charAt(0)).toUpperCase());
            }
        }

        // ✅ SIMPLIFIED: Only "Active" status exists (green badge)
        String status = client.getStatus();
        if (status != null && status.equals("Active")) {
            holder.clientStatus.setBackgroundResource(R.drawable.status_active);
        } else {
            // Fallback (shouldn't happen since all clients with membership are active)
            holder.clientStatus.setBackgroundResource(R.drawable.status_unknown);
        }

        holder.clientCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, Client_workouts_details.class);
            intent.putExtra("client_uid", client.getUid());
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });

        holder.clientCard.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onClientLongClick(client);
                return true;
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
        ImageView clientProfileImage;

        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            clientCard = itemView.findViewById(R.id.client_card);
            clientName = itemView.findViewById(R.id.client_name);
            clientEmail = itemView.findViewById(R.id.client_email);
            clientStatus = itemView.findViewById(R.id.client_status);
            clientAvatar = itemView.findViewById(R.id.client_avatar);
            clientProfileImage = itemView.findViewById(R.id.client_profile_image);
        }
    }
}