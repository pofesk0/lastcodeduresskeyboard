package duress.keyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.admin.DevicePolicyManager;

public class NucleusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("SimpleKeyboardPrefs", Context.MODE_PRIVATE).getBoolean("usb_block_enabled", false)) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            try {
                if (context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("SimpleKeyboardPrefs", Context.MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
                    dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
                } else {
                    dpm.wipeData(0);
                }
            } catch (SecurityException e) {}
        }
    }
}
