package com.example.monlauncher;

public class AppInfo {
    public String label;
    public String customLabel;
    public String packageName;

    // Cette méthode permet d'afficher le nom personnalisé s'il existe
    public String getDisplayName() {
        if (customLabel != null && !customLabel.isEmpty()) {
            return customLabel;
        }
        return label;
    }
}