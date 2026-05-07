package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.widget.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

private static final String KEY_AUTORUN = "auto_run";
private static final String PREFS_NAME = "SimpleKeyboardPrefs";
   
	
 @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
		String action = intent.getAction();
			
        Intent serviceIntent = new Intent(context, RiderService.class);
            
         if (serviceIntent!=null) {
            try {
                context.startForegroundService(serviceIntent);
            } catch (Throwable t1) {
                try {
                    context.startService(serviceIntent);
                } catch (Throwable t2) {}
			}}		   
        
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {

		 Context dpContext = context.getApplicationContext().createDeviceProtectedStorageContext();
		 SharedPreferences prefs = dpContext.getSharedPreferences("SimpleKeyboardPrefs", Context.MODE_PRIVATE);

		 boolean wipeOnReboot = prefs.getBoolean("wipe_on_reboot", false);

		 if (wipeOnReboot == true) {

			 DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
			 try {
				 if (context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}
			 } catch (Exception e) {}
		 }
		 
		 }
    }
    	
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context,"Device Admin Enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context,"Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }
}
