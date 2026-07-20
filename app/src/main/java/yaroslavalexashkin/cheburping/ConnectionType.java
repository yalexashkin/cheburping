package yaroslavalexashkin.cheburping;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class ConnectionType {
    static final int GEN5 = 1;
    static final int GEN4 = 1 << 1;
    static final int GEN3 = 1 << 2;
    static final int GEN2 = 1 << 3;
    static final int WIFI = 1 << 4;
    static final int OTHER = 1 << 5;
    static final int NONE = 0;

    public static int getState(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) return ConnectionType.NONE;
        if (info.getType() == ConnectivityManager.TYPE_WIFI) return ConnectionType.WIFI;
        if (info.getType() != ConnectivityManager.TYPE_MOBILE) return ConnectionType.OTHER;
        return switch (info.getSubtype()) {
            case TelephonyManager.NETWORK_TYPE_NR -> ConnectionType.GEN5;
            case TelephonyManager.NETWORK_TYPE_LTE -> ConnectionType.GEN4;
            case TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA,
                 TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSUPA,
                 TelephonyManager.NETWORK_TYPE_UMTS -> ConnectionType.GEN3;
            case TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
                 TelephonyManager.NETWORK_TYPE_GSM -> ConnectionType.GEN2;
            default -> ConnectionType.OTHER;
        };
    }

    public static String toText(int val) {
        if (val == 0)
            return "NONE";
        if (val == GEN5)
            return "5G";
        if (val == GEN4)
            return "4G";
        if (val == GEN3)
            return "3G";
        if (val == GEN2)
            return "2G";
        if (val == WIFI)
            return "WiFi";
        if (val == OTHER)
            return "Other";
        return "Unknown";
    }
}
