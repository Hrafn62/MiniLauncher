package com.example.monlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextClock;
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
    private static final int MIN_DISTANCE = 100;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // On garde juste le wallpaper derrière
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
                if (appsContainer.getVisibility() == View.VISIBLE) showHomeScreen();
            }
        });
    }

    private void setupScrollToDismiss() {
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            private float startY = 0;
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = e.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float deltaY = e.getY() - startY;
                        if (!rv.canScrollVertically(-1) && deltaY > 150) {
                            showHomeScreen();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void applySettingsAndFavorites() {
        SharedPreferences config = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
        SharedPreferences favs = getSharedPreferences("Favs", Context.MODE_PRIVATE);
        SharedPreferences customNames = getSharedPreferences("CustomNames", Context.MODE_PRIVATE);

        TextClock clock = findViewById(R.id.home_clock);
        TextClock dateView = findViewById(R.id.home_date);

        homeScreen.removeAllViews();

        if (clock != null) {
            if (clock.getParent() != null) ((android.view.ViewGroup) clock.getParent()).removeView(clock);
            setupClockShortcut(clock);
            homeScreen.addView(clock);
        }

        if (dateView != null) {
            if (dateView.getParent() != null) ((android.view.ViewGroup) dateView.getParent()).removeView(dateView);
            homeScreen.addView(dateView);
        }

        // Espaceur pour pousser les favoris vers le bas
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1.0f);
        spacer.setLayoutParams(spacerParams);
        homeScreen.addView(spacer);

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
            tv.setGravity(Gravity.START);

            tv.setOnClickListener(v -> {
                if (!pkg.isEmpty()) {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (intent != null) startActivity(intent);
                } else {
                    openAppPicker(key);
                }
            });
            tv.setOnLongClickListener(v -> {
                openAppPicker(key);
                return true;
            });
            homeScreen.addView(tv);
        }

        View bottomMargin = new View(this);
        homeScreen.addView(bottomMargin, new LinearLayout.LayoutParams(1, 100));
    }

    private void setupClockShortcut(TextClock clock) {
        clock.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
            String shortcutPkg = prefs.getString("clock_shortcut_pkg", "");
            if (!shortcutPkg.isEmpty()) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(shortcutPkg);
                if (launchIntent != null) startActivity(launchIntent);
            } else {
                openAppPicker("clock_shortcut");
            }
        });
        clock.setOnLongClickListener(v -> {
            openAppPicker("clock_shortcut");
            return true;
        });
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
        List<AppInfo> filteredList = new ArrayList<>();
        if (fullAppsList != null) {
            for (AppInfo app : fullAppsList) {
                if (app.getDisplayName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(app);
                }
            }
        }
        if (appAdapter != null) appAdapter.updateList(filteredList);
    }

    private void refreshAppsList() {
        loadApps();
        fullAppsList = new ArrayList<>(appsList);
        if (appAdapter == null) {
            appAdapter = new AppAdapter(appsList);
            recyclerView.setAdapter(appAdapter);
        } else {
            appAdapter.updateList(appsList);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (appsContainer.getVisibility() == View.GONE) {
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
                    if (Math.abs(deltaX) > MIN_DISTANCE && Math.abs(deltaY) < 150) {
                        if (deltaX < 0) handleGesture("swipe_left_action");
                        else handleGesture("swipe_right_action");
                        return true;
                    } else if (deltaY < -MIN_DISTANCE && Math.abs(deltaX) < 150) {
                        showAppsList();
                        return true;
                    }
                    break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void handleGesture(String configKey) {
        SharedPreferences config = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
        String action = config.getString(configKey, "Aucun");
        if ("Appareil Photo".equals(action)) {
            try { startActivity(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)); } catch (Exception e) {}
        } else if ("Téléphone".equals(action)) {
            try { startActivity(new Intent(Intent.ACTION_DIAL)); } catch (Exception e) {}
        }
    }

    private void showAppsList() {
        appsContainer.setVisibility(View.VISIBLE);
        homeScreen.setVisibility(View.GONE);
        appSearch.setText("");
        appSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(appSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    private void showHomeScreen() {
        appsContainer.setVisibility(View.GONE);
        homeScreen.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(appSearch.getWindowToken(), 0);
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
        Collections.sort(appsList, (a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
    }

    private boolean listContainsPackage(List<AppInfo> list, String pkg) {
        for (AppInfo info : list) { if (info.packageName.equals(pkg)) return true; }
        return false;
    }
}