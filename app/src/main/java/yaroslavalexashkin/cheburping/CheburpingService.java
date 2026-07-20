package yaroslavalexashkin.cheburping;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE;

public class CheburpingService extends Service {// ping -c 1 -W 10 8.8.8.8
    static boolean serviceRunning = false;
    final int SERVICE_ID = 100;
    final int STATE_UPDATE_ID = 200;
    final String KEEPALIVE_NOTIF_CHANNEL = "keepalive";
    Notification.Builder builder = null;
    Thread bgThread = null;
    boolean shouldStop = false;

    final String NOTIF_CHANNEL_AVAILABLE = "statechange-available";
    final String NOTIF_CHANNEL_UNREACHABLE = "statechange-unreachable";
    final String NOTIF_CHANNEL_TIMEOUT = "statechange-timeout";
    final String NOTIF_CHANNEL_GENERIC_ERR = "statechange-unknown-err";

    final int INTENT_LAUNCH_SELF = 110;
    final int INTENT_LAUNCH_RADIOINFO = 111;
    final int INTENT_LAUNCH_SCANNOW = 112;
    final String CMD_SCANNOW = "scanNow";

    Logger logger = null;
    boolean scanNowTriggered = false;

    public enum State {
        UNDEFINED,
        AVAILABLE,
        UNREACHABLE,
        TIMEOUT,
        OTHER_ERR
    }

    enum StateForLog {
        undefined,
        accessible,
        unreachable,
        timeout,
        unknownErr,

        blacklistedNet,
        noNetwork
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("unsupported");
    }

    void bgThreadLoop() {
        String ip = SettingsStorage.getIp(this);
        int scanIntervalMs = SettingsStorage.getScanIntervalSeconds(this) * 1000;
        int timeout = SettingsStorage.getTimeoutSeconds(this);
        int allowedNetworkMask = SettingsStorage.getAllowedNetworksMask(this);
        builder.setContentTitle("Cheburping " + ip);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        logger.write("Service launched");

        State accessibleOld = State.UNDEFINED;
        StateForLog stateForLogOld = StateForLog.undefined;
        while (!shouldStop) {
            String now = DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now());
            State accessibleCur = State.UNDEFINED;
            StateForLog stateForLogCur = StateForLog.undefined;
            int curNetwork = ConnectionType.getState(cm);
            if ((curNetwork & allowedNetworkMask) > 0 || scanNowTriggered && curNetwork > 0) {
                Pinger p = new Pinger(ip, timeout);
                if (!p.successful)
                    builder.setContentText("process launch failed");
                else if (p.unreachable) {
                    builder.setContentText("Unreachable - " + now + " - " + ConnectionType.toText(curNetwork));
                    accessibleCur = State.UNREACHABLE;
                    stateForLogCur = StateForLog.unreachable;
                } else if (p.timeout) {
                    builder.setContentText("connection timeout - " + now + " - " + ConnectionType.toText(curNetwork));
                    accessibleCur = State.TIMEOUT;
                    stateForLogCur = StateForLog.timeout;
                } else if (p.otherErr) {
                    builder.setContentText("unknown error - " + now + " - " + ConnectionType.toText(curNetwork));
                    accessibleCur = State.OTHER_ERR;
                    stateForLogCur = StateForLog.unknownErr;
                } else {
                    builder.setContentText("Accessible - " + p.ping + " - " + now + " - " + ConnectionType.toText(curNetwork));
                    accessibleCur = State.AVAILABLE;
                    stateForLogCur = StateForLog.accessible;
                }
                getSystemService(NotificationManager.class).notify(SERVICE_ID, builder.build());
                if (accessibleOld == State.UNDEFINED)
                    accessibleOld = accessibleCur;
                if (accessibleCur != accessibleOld && accessibleCur != State.UNDEFINED) {
                    Notification.Builder builder = new Notification.Builder(this, NOTIF_CHANNEL_AVAILABLE);
                    builder.setCategory(Notification.CATEGORY_SERVICE)
                            .setSmallIcon(R.drawable.ic_launcher_foreground);
                    switch (accessibleCur) {
                        case AVAILABLE:
                            builder.setChannelId(NOTIF_CHANNEL_AVAILABLE)
                                    .setContentTitle(ip + ": accessible - " + now);
                            break;
                        case UNREACHABLE:
                            builder.setChannelId(NOTIF_CHANNEL_UNREACHABLE)
                                    .setContentTitle(ip + ": unreachable - " + now);
                            break;
                        case TIMEOUT:
                            builder.setChannelId(NOTIF_CHANNEL_TIMEOUT)
                                    .setContentTitle(ip + ": timeout - " + now);
                            break;
                        case OTHER_ERR:
                            builder.setChannelId(NOTIF_CHANNEL_GENERIC_ERR)
                                    .setContentTitle(ip + ": unknown error - " + now);
                            break;
                    }
                    getSystemService(NotificationManager.class).notify(STATE_UPDATE_ID, builder.build());
                    accessibleOld = accessibleCur;
                }
            } else {
                if (curNetwork == 0) {
                    builder.setContentText("Network unavailable");
                    stateForLogCur = StateForLog.noNetwork;
                } else {
                    builder.setContentText("Blacklisted network: " + ConnectionType.toText(curNetwork));
                    stateForLogCur = StateForLog.blacklistedNet;
                }
                getSystemService(NotificationManager.class).notify(SERVICE_ID, builder.build());
            }
            scanNowTriggered = false;
            if (stateForLogCur != stateForLogOld) {
                switch (stateForLogCur) {
                    case accessible -> logger.write("[" + ConnectionType.toText(curNetwork) + "]" + ip + ": accessible");
                    case unreachable -> logger.write("[" + ConnectionType.toText(curNetwork) + "]" + ip + ": unreachable");
                    case timeout -> logger.write("[" + ConnectionType.toText(curNetwork) + "]" + ip + ": timeout");
                    case unknownErr -> logger.write("[" + ConnectionType.toText(curNetwork) + "]" + ip + ": unknown error");
                    case blacklistedNet -> logger.write("[" + ConnectionType.toText(curNetwork) + "]" + ip + ": blacklisted");
                    case noNetwork -> logger.write("no network");
                }
                stateForLogOld = stateForLogCur;
            }
            try {

                //while ()
                Thread.sleep(scanIntervalMs);
            } catch (InterruptedException ignored) {}
        }
        logger.write("Service stopped");
    }

    @Override
    public void onCreate() {
        serviceRunning = true;
        logger = new Logger(this);
        getSystemService(NotificationManager.class)
                .createNotificationChannel(new NotificationChannel(KEEPALIVE_NOTIF_CHANNEL, "Service keepalive",
                        NotificationManager.IMPORTANCE_LOW));
        getSystemService(NotificationManager.class)
                .createNotificationChannel(new NotificationChannel(NOTIF_CHANNEL_AVAILABLE, "Available",
                        NotificationManager.IMPORTANCE_LOW));
        getSystemService(NotificationManager.class)
                .createNotificationChannel(new NotificationChannel(NOTIF_CHANNEL_UNREACHABLE, "Unreachable",
                        NotificationManager.IMPORTANCE_LOW));
        getSystemService(NotificationManager.class)
                .createNotificationChannel(new NotificationChannel(NOTIF_CHANNEL_TIMEOUT, "Timeout",
                        NotificationManager.IMPORTANCE_LOW));
        getSystemService(NotificationManager.class)
                .createNotificationChannel(new NotificationChannel(NOTIF_CHANNEL_GENERIC_ERR, "Unknown error",
                        NotificationManager.IMPORTANCE_LOW));
        builder = new Notification.Builder(this, KEEPALIVE_NOTIF_CHANNEL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            builder.setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE);
        builder.setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setChannelId(KEEPALIVE_NOTIF_CHANNEL);

        Intent radioInfoLauncher = new Intent("android.intent.action.MAIN");
        radioInfoLauncher.setClassName("com.android.phone", "com.android.phone.settings.RadioInfo");
        radioInfoLauncher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        builder.addAction(new Notification.Action.Builder(null, "Radio info", PendingIntent.getActivity(
                this,
                INTENT_LAUNCH_RADIOINFO,
                radioInfoLauncher,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        )).build());

        Intent selfLauncher = new Intent(this, MainActivity.class);
        radioInfoLauncher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        builder.addAction(new Notification.Action.Builder(null, "Settings", PendingIntent.getActivity(
                this,
                INTENT_LAUNCH_SELF,
                selfLauncher,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        )).build());

        Intent scannowCmd = new Intent(this, CheburpingService.class);
        scannowCmd.putExtra(CMD_SCANNOW, true);
        builder.addAction(new Notification.Action.Builder(null, "Scan now", PendingIntent.getService(
                this,
                INTENT_LAUNCH_SCANNOW,
                scannowCmd,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        )).build());
    }

    @Override
    public void onDestroy() {
        if (bgThread != null) {
            shouldStop = true;
            bgThread.interrupt();
            try {
                bgThread.join();
                bgThread = null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logger.quit();
        logger = null;
        builder = null;
        serviceRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (bgThread == null) {
            shouldStop = false;
            int svcType = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                svcType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            Notification notif = builder.setContentTitle("Running").setContentText("").build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(SERVICE_ID, notif, svcType);
            else startForeground(SERVICE_ID, notif);
            bgThread = new Thread(this::bgThreadLoop);
            bgThread.start();
        } else if (intent != null && intent.hasExtra(CMD_SCANNOW)) {
            builder.setContentText("Scanning...");
            scanNowTriggered = true;
            getSystemService(NotificationManager.class).notify(SERVICE_ID, builder.build());
            bgThread.interrupt();
        }
        return START_STICKY;
    }
}
