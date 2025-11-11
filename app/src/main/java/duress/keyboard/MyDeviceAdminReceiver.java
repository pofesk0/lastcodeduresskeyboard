package duress.keyboard;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if ("duress.keyboard.ACTION_WIPE_DEVICE".equals(intent.getAction())) {
            // 👉 Выполняем wipeData(0)
            try {
                DevicePolicyManager dpm = (DevicePolicyManager)
                        context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm != null) {
                    dpm.wipeData(0);
                    Toast.makeText(context, "Device wipe initiated", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            // 👉 При других событиях — запускаем MainActivity
            try {
                Intent launchIntent = new Intent(context, MainActivity.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Toast.makeText(context, "DeviceAdminReceiver active", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
