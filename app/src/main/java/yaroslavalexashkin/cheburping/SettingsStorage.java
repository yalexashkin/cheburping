package yaroslavalexashkin.cheburping;

import android.content.Context;

public class SettingsStorage {
    public static String getIp(Context c) {
        return c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("ip", "8.8.8.8");
    }

    public static int getTimeoutSeconds(Context c) {
        return c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("timeout", 10);
    }

    public static int getScanIntervalSeconds(Context c) {
        return c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("scanInterval", 60);
    }

    public static boolean getLaunchOnBoot(Context c) {
        return c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("launchOnBoot", false);
    }

    public static int getAllowedNetworksMask(Context c) {
        return c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("allowedNetworksMask", ~ConnectionType.NONE);
    }

    public static void setIp(Context c, String ip) {
        c.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString("ip", ip).apply();
    }

    public static void setTimeoutSeconds(Context c, int timeout) {
        c.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("timeout", timeout).apply();
    }

    public static void setScanIntervalSeconds(Context c, int scanInterval) {
        c.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("scanInterval", scanInterval).apply();
    }

    public static void setLaunchOnBoot(Context c, boolean val) {
        c.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("launchOnBoot", val).apply();
    }

    public static void setAllowedNetworksMask(Context c, int mask) {
        c.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("allowedNetworksMask", mask).apply();
    }
}
