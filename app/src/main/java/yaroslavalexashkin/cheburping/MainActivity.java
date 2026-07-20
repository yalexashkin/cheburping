package yaroslavalexashkin.cheburping;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    final int REQUEST_NOTIFICATION_PERMS = 123;
    Button btnSvcToggle = null;
    EditText ipField = null;
    EditText timeoutField = null;
    EditText intervalField = null;
    Switch swLaunchOnBoot = null;
    CheckBox checkbox_5g = null;
    CheckBox checkbox_4g = null;
    CheckBox checkbox_3g = null;
    CheckBox checkbox_2g = null;
    CheckBox checkbox_wifi = null;
    CheckBox checkbox_other = null;
    ScrollView logsScroll = null;
    TextView logs_text = null;
    Thread logRefresh = null;
    boolean threadShouldStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this); fixme this??
        setContentView(R.layout.activity_main);
        btnSvcToggle = findViewById(R.id.btnSvcToggle);
        ipField = findViewById(R.id.ipField);
        timeoutField = findViewById(R.id.timeoutField);
        intervalField = findViewById(R.id.intervalField);
        swLaunchOnBoot = findViewById(R.id.swLaunchOnBoot);
        checkbox_5g = findViewById(R.id.checkbox_5g);
        checkbox_4g = findViewById(R.id.checkbox_4g);
        checkbox_3g = findViewById(R.id.checkbox_3g);
        checkbox_2g = findViewById(R.id.checkbox_2g);
        checkbox_wifi = findViewById(R.id.checkbox_wifi);
        checkbox_other = findViewById(R.id.checkbox_other);
        logs_text = findViewById(R.id.logs_text);
        logsScroll = findViewById(R.id.logsScroll);

        ipField.setText(SettingsStorage.getIp(this));
        timeoutField.setText("" + SettingsStorage.getTimeoutSeconds(this));
        intervalField.setText("" + SettingsStorage.getScanIntervalSeconds(this));
        swLaunchOnBoot.setChecked(SettingsStorage.getLaunchOnBoot(this));
        int netMask = SettingsStorage.getAllowedNetworksMask(this);
        checkbox_5g.setChecked((netMask & ConnectionType.GEN5) > 0);
        checkbox_4g.setChecked((netMask & ConnectionType.GEN4) > 0);
        checkbox_3g.setChecked((netMask & ConnectionType.GEN3) > 0);
        checkbox_2g.setChecked((netMask & ConnectionType.GEN2) > 0);
        checkbox_wifi.setChecked((netMask & ConnectionType.WIFI) > 0);
        checkbox_other.setChecked((netMask & ConnectionType.OTHER) > 0);
        threadShouldStop = false;
        logRefresh = new Thread(() -> {
            while (!threadShouldStop) {
                this.runOnUiThread(() -> {
                    if (CheburpingService.serviceRunning)
                        btnSvcToggle.setText("Stop");
                    else
                        btnSvcToggle.setText("Run");
                    //boolean isEnd = !logsScroll.canScrollVertically(1);
                    logs_text.setText(new Logger(this).readAll());
                    //if (isEnd)
                    //    logsScroll.fullScroll(View.FOCUS_DOWN); todo doesnt work
                });
                try {
                    Thread.sleep(333);
                } catch (InterruptedException ignored) {
                }
            }
        });
        logRefresh.start();
    }

    @Override
    protected void onDestroy() {
        threadShouldStop = true;
        if (logRefresh != null) {
            try {
                logRefresh.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logRefresh = null;
        super.onDestroy();
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_NOTIFICATION_PERMS)
            return;
    }*/

    public void run(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications permission required!", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMS);
                return;
            }
        }
        startService(new Intent(this, CheburpingService.class));
    }

    public void stop(View view) {
        stopService(new Intent(this, CheburpingService.class));
    }

    public void btnSvcToggle(View view) {
        if (CheburpingService.serviceRunning)
            stop(view);
        else
            run(view);
    }

    public void apply(View view) {
        String ip = ipField.getText().toString();
        String timeout = timeoutField.getText().toString();
        String interval = intervalField.getText().toString();
        int timeoutInt = 0;
        int intervalInt = 0;
        try {
            timeoutInt = Integer.parseInt(timeout);
            intervalInt = Integer.parseInt(interval);
        } catch (NumberFormatException ignored) {}
        if (timeoutInt < 1 || intervalInt < 1) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            return;
        }
        SettingsStorage.setIp(this, ip);
        SettingsStorage.setTimeoutSeconds(this, timeoutInt);
        SettingsStorage.setScanIntervalSeconds(this, intervalInt);
        SettingsStorage.setLaunchOnBoot(this, swLaunchOnBoot.isChecked());
        int netMask = 0;
        if (checkbox_5g.isChecked())
            netMask |= ConnectionType.GEN5;
        if (checkbox_4g.isChecked())
            netMask |= ConnectionType.GEN4;
        if (checkbox_3g.isChecked())
            netMask |= ConnectionType.GEN3;
        if (checkbox_2g.isChecked())
            netMask |= ConnectionType.GEN2;
        if (checkbox_wifi.isChecked())
            netMask |= ConnectionType.WIFI;
        if (checkbox_other.isChecked())
            netMask |= ConnectionType.OTHER;
        SettingsStorage.setAllowedNetworksMask(this, netMask);
        boolean prevSvcRunning = CheburpingService.serviceRunning;
        stop(null);
        if (prevSvcRunning)
            run(null);
    }
}