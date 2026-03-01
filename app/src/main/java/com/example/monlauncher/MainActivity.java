package com.example.monlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextClock;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout homeScreen;
    private View appsContainer;
    private EditText appSearch;
    private List<AppInfo> appsList;
    private List<AppInfo> fullAppsList;
    private AppAdapter appAdapter;

    private float x1, x2, y1, y2;
    private static final int MIN_DISTANCE = 150;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        recyclerView = findViewById(R.id.apps_list);
        homeScreen = findViewById(R.id.home_screen);
        appsContainer = findViewById(R.id.apps_container);
        appSearch = findViewById(R.id.app_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        refreshAppsList();
        setupScrollToDismiss();

        appSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        homeScreen.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
            lastClickTime = clickTime;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (appsContainer.getVisibility() == View.VISIBLE) {
                    if (appSearch.getText().length() > 0) {
                        appSearch.setText("");
                    } else {
                        showHomeScreen();
                    }
                }
            }
        });
    }

    private void setupScrollToDismiss() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(-1) && dy < -50) {
                    showHomeScreen();
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;

                if (appsContainer.getVisibility() == View.GONE) {
                    if (deltaY < -MIN_DISTANCE && Math.abs(deltaX) < 200) {
                        showAppsList();
                        return true;
                    }
                    if (Math.abs(deltaX) > MIN_DISTANCE && Math.abs(deltaY) < 200) {
                        if (deltaX < 0) handleGesture("swipe_left_action");
                        else handleGesture("swipe_right_action");
                        return true;
                    }
                } else {
                    if (deltaY > MIN_DISTANCE && !recyclerView.canScrollVertically(-1)) {
                        showHomeScreen();
                        return true;
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void showAppsList() {
        appsContainer.setVisibility(View.VISIBLE);
        appsContainer.setAlpha(0f);
        appsContainer.setTranslationY(500f);

        appsContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    appSearch.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(appSearch, InputMethodManager.SHOW_IMPLICIT);
                })
                .start();

        homeScreen.animate().alpha(0f).setDuration(200).start();
    }

    private void showHomeScreen() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(appSearch.getWindowToken(), 0);

        appsContainer.animate()
                .alpha(0f)
                .translationY(500f)
                .setDuration(250)
                .withEndAction(() -> {
                    appsContainer.setVisibility(View.GONE);
                    homeScreen.setVisibility(View.VISIBLE);
                    homeScreen.setAlpha(1f);
                    appSearch.setText("");
                })
                .start();
    }

    private void applySettingsAndFavorites() {
        SharedPreferences config = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
        SharedPreferences favs = getSharedPreferences("Favs", Context.MODE_PRIVATE);
        SharedPreferences customNames = getSharedPreferences("CustomNames", Context.MODE_PRIVATE);

        String alignPref = config.getString("alignment", "Gauche");
        int gravity;
        switch (alignPref) {
            case "Centré": gravity = Gravity.CENTER_HORIZONTAL; break;
            case "Droite": gravity = Gravity.END; break;
            default: gravity = Gravity.START; break;
        }

        TextClock clock = findViewById(R.id.home_clock);
        TextClock dateView = findViewById(R.id.home_date);

        homeScreen.removeAllViews();

        if (clock != null) {
            if (clock.getParent() != null) ((android.view.ViewGroup) clock.getParent()).removeView(clock);
            setupClockShortcut(clock);
            clock.setGravity(gravity);
            homeScreen.addView(clock);
        }

        if (dateView != null) {
            if (dateView.getParent() != null) ((android.view.ViewGroup) dateView.getParent()).removeView(dateView);
            dateView.setGravity(gravity);
            homeScreen.addView(dateView);
        }

        View spacer = new View(this);
        homeScreen.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1.0f));

        int count = config.getInt("fav_count", 4);
        for (int i = 1; i <= count; i++) {
            final String key = "fav" + i;
            TextView tv = new TextView(this);
            String pkg = favs.getString(key + "_pkg", "");
            String savedFavName = favs.getString(key + "_name", "Sélectionner " + i);
            String displayName = customNames.getString(pkg, savedFavName);

            tv.setText(displayName);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(26);
            tv.setPadding(0, 25, 0, 25);

            tv.setGravity(gravity);
            // CORRECTION ICI : Utilisation de LinearLayout.LayoutParams
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(params);

            tv.setOnClickListener(v -> {
                if (!pkg.isEmpty()) {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (intent != null) startActivity(intent);
                } else openAppPicker(key);
            });
            tv.setOnLongClickListener(v -> { openAppPicker(key); return true; });
            homeScreen.addView(tv);
        }
        homeScreen.addView(new View(this), new LinearLayout.LayoutParams(1, 100));
    }

    private void handleGesture(String configKey) {
        SharedPreferences config = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
        String action = config.getString(configKey, "Aucun");

        if ("Appareil Photo".equals(action)) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.GoogleCamera");
            if (intent == null) {
                intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            }
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Erreur appareil photo", Toast.LENGTH_SHORT).show();
            }
        } else if ("Téléphone".equals(action)) {
            try { startActivity(new Intent(Intent.ACTION_DIAL)); } catch (Exception e) {}
        }
    }

    private void setupClockShortcut(TextClock clock) {
        clock.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
            String pkg = prefs.getString("clock_shortcut_pkg", "");
            if (!pkg.isEmpty()) startActivity(getPackageManager().getLaunchIntentForPackage(pkg));
            else openAppPicker("clock_shortcut");
        });
        clock.setOnLongClickListener(v -> { openAppPicker("clock_shortcut"); return true; });
    }

    private void openAppPicker(String key) {
        Intent intent = new Intent(this, AppPickerActivity.class);
        intent.putExtra("TARGET_KEY", key);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAppsList();
        applySettingsAndFavorites();
    }

    private void filterApps(String query) {
        List<AppInfo> filtered = new ArrayList<>();
        if (fullAppsList != null) {
            for (AppInfo app : fullAppsList) {
                if (app.getDisplayName().toLowerCase().contains(query.toLowerCase())) filtered.add(app);
            }
        }
        if (appAdapter != null) appAdapter.updateList(filtered);
    }

    private void refreshAppsList() {
        loadApps();
        fullAppsList = new ArrayList<>(appsList);
        if (appAdapter == null) {
            appAdapter = new AppAdapter(appsList);
            recyclerView.setAdapter(appAdapter);
        } else appAdapter.updateList(appsList);
    }

    private void loadApps() {
        appsList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        SharedPreferences customNames = getSharedPreferences("CustomNames", Context.MODE_PRIVATE);

        Intent intent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : activities) {
            AppInfo app = new AppInfo();
            app.packageName = ri.activityInfo.packageName;
            app.label = ri.loadLabel(pm).toString();
            app.customLabel = customNames.getString(app.packageName, null);
            if (!listContainsPackage(appsList, app.packageName)) appsList.add(app);
        }

        List<android.content.pm.PackageInfo> packages = pm.getInstalledPackages(0);
        for (android.content.pm.PackageInfo packageInfo : packages) {
            String pkgName = packageInfo.packageName.toLowerCase();
            if (pkgName.contains("webapk") || pkgName.startsWith("com.vivaldi")) {
                if (!listContainsPackage(appsList, packageInfo.packageName)) {
                    AppInfo app = new AppInfo();
                    app.packageName = packageInfo.packageName;
                    app.label = packageInfo.applicationInfo.loadLabel(pm).toString();
                    if (!app.label.equalsIgnoreCase("Vivaldi")) appsList.add(app);
                }
            }
        }
        Collections.sort(appsList, (a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
    }

    private boolean listContainsPackage(List<AppInfo> list, String pkg) {
        for (AppInfo info : list) { if (info.packageName.equals(pkg)) return true; }
        return false;
    }
}