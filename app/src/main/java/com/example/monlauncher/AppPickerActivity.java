package com.example.monlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppPickerActivity extends AppCompatActivity {
    private String targetKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        targetKey = getIntent().getStringExtra("TARGET_KEY");
        if (targetKey == null) targetKey = "fav1";

        RecyclerView rv = findViewById(R.id.settings_recycler);
        if (rv == null) {
            finish();
            return;
        }

        rv.setLayoutManager(new LinearLayoutManager(this));

        List<AppInfo> allApps = loadAllApps();

        // LOGIQUE MODIFIÉE ICI
        SettingsAdapter adapter = new SettingsAdapter(allApps, app -> {
            if ("clock_shortcut".equals(targetKey)) {
                // Pour l'horloge, on enregistre dans LauncherConfig
                SharedPreferences pref = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
                pref.edit()
                        .putString("clock_shortcut_pkg", app.packageName)
                        .apply();
            } else {
                // Pour les favoris classiques (fav1, fav2...), on reste dans Favs
                SharedPreferences pref = getSharedPreferences("Favs", Context.MODE_PRIVATE);
                pref.edit()
                        .putString(targetKey + "_name", app.label)
                        .putString(targetKey + "_pkg", app.packageName)
                        .apply();
            }
            finish();
        });

        rv.setAdapter(adapter);
    }

    private List<AppInfo> loadAllApps() {
        List<AppInfo> list = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

        if (activities != null) {
            for (ResolveInfo ri : activities) {
                AppInfo app = new AppInfo();
                app.label = ri.loadLabel(pm).toString();
                app.packageName = ri.activityInfo.packageName;
                list.add(app);
            }
        }
        Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return list;
    }
}