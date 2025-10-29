package com.example.signuploginrealtime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ArchivedClientsAdapter extends RecyclerView.Adapter<ArchivedClientsAdapter.ArchivedClientViewHolder> {

    private Context context;
    private List<coach_clients.Client> archivedClientsList;
    private OnArchivedClientClickListener clickListener;

    public interface OnArchivedClientClickListener {
        void onRestoreClick(coach_clients.Client client);
        void onDeleteClick(coach_clients.Client client);
    }

    public ArchivedClientsAdapter(Context context, List<coach_clients.Client> archivedClientsList, OnArchivedClientClickListener clickListener) {
        this.context = context;
        this.archivedClientsList = archivedClientsList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ArchivedClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_archived_client, parent, false);
        return new ArchivedClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchivedClientViewHolder holder, int position) {
        coach_clients.Client client = archivedClientsList.get(position);

        holder.clientName.setText(client.getName());
        holder.clientEmail.setText(client.getEmail());
        holder.clientGoal.setText("Goal: " + client.getGoal());
        holder.clientLevel.setText("Level: " + client.getActivityLevel());

        // ✅ ADDED: Profile Picture Handling
        String profilePictureUrl = client.getProfilePictureUrl();

        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            // Show profile picture
            holder.clientProfileImage.setVisibility(View.VISIBLE);
            holder.clientAvatar.setVisibility(View.GONE);

            Glide.with(context)
                    .load(profilePictureUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.clientProfileImage);
        } else {
            // Show text avatar
            holder.clientProfileImage.setVisibility(View.GONE);
            holder.clientAvatar.setVisibility(View.VISIBLE);

            if (client.getName() != null && !client.getName().isEmpty()) {
                holder.clientAvatar.setText(String.valueOf(client.getName().charAt(0)).toUpperCase());
            }
        }

        holder.restoreButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRestoreClick(client);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeleteClick(client);
            }
        });
    }

    @Override
    public int getItemCount() {
        return archivedClientsList.size();
    }

    static class ArchivedClientViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView clientName, clientEmail, clientGoal, clientLevel, clientAvatar;
        ImageView clientProfileImage;  // ✅ ADDED
        Button restoreButton, deleteButton;

        public ArchivedClientViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.archived_client_card);
            clientName = itemView.findViewById(R.id.archived_client_name);
            clientEmail = itemView.findViewById(R.id.archived_client_email);
            clientGoal = itemView.findViewById(R.id.archived_client_goal);
            clientLevel = itemView.findViewById(R.id.archived_client_level);
            clientAvatar = itemView.findViewById(R.id.archived_client_avatar);  // ✅ ADDED
            clientProfileImage = itemView.findViewById(R.id.archived_client_profile_image);  // ✅ ADDED
            restoreButton = itemView.findViewById(R.id.restore_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}