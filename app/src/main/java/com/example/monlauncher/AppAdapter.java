package com.example.monlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private List<AppInfo> appsList;

    public AppAdapter(List<AppInfo> appsList) {
        this.appsList = appsList;
    }

    public void updateList(List<AppInfo> newList) {
        this.appsList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Assure-toi que R.layout.item_app existe bien
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appsList.get(position);

        // Utilise getDisplayName() - Assure-toi d'avoir mis à jour AppInfo.java !
        holder.textApp.setText(app.getDisplayName());
        holder.textApp.setTextColor(Color.WHITE);
        holder.textApp.setTextSize(18);

        holder.itemView.setOnClickListener(v -> {
            Intent launchIntent = v.getContext().getPackageManager()
                    .getLaunchIntentForPackage(app.packageName);
            if (launchIntent != null) {
                v.getContext().startActivity(launchIntent);
            }
        });

        // CLIC LONG
        holder.itemView.setOnLongClickListener(v -> {
            showOptionsMenu(v.getContext(), app, position);
            return true;
        });
    }

    private void showOptionsMenu(Context context, AppInfo app, int position) {
        String[] options = {"(i) Informations", "Modifier le nom"};

        new android.app.AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.getDisplayName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + app.packageName));
                        context.startActivity(intent);
                    } else if (which == 1) {
                        showRenameDialog(context, app, position);
                    }
                })
                .show();
    }

    private void showRenameDialog(Context context, AppInfo app, int position) {
        final EditText input = new EditText(context);
        input.setText(app.getDisplayName());
        input.setTextColor(Color.WHITE);

        new android.app.AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Nouveau nom")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String newName = input.getText().toString();
                    app.customLabel = newName;

                    SharedPreferences prefs = context.getSharedPreferences("CustomNames", Context.MODE_PRIVATE);
                    prefs.edit().putString(app.packageName, newName).apply();

                    notifyItemChanged(position);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return appsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textApp;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vérifie que android.R.id.text1 correspond bien à l'ID dans ton item_app.xml
            textApp = itemView.findViewById(android.R.id.text1);
        }
    }
}