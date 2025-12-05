package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.widget.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

private static final String KEY_AUTORUN = "auto_run";
	
 @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

		 Context dpContext = context.getApplicationContext().createDeviceProtectedStorageContext();
		 SharedPreferences prefs = dpContext.getSharedPreferences("SimpleKeyboardPrefs", Context.MODE_PRIVATE);

		 boolean wipeOnReboot = prefs.getBoolean("wipe_on_reboot", false);

		 if (wipeOnReboot == true) {

			 DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
			 try {
				 dpm.wipeData(0);
			 } catch (Exception e) {}
		 }
		 
		 boolean autoRunEnabled = prefs.getBoolean(KEY_AUTORUN, false);
		 if (autoRunEnabled) {
		
			PackageManager pm = context.getPackageManager();
			ComponentName cn = new ComponentName(context, InputActivity.class);

			pm.setComponentEnabledSetting(
				cn,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP
			);
            scheduleExactAlarm(context);
        }}
    }

    private void scheduleExactAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, InputActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
			context,
			0,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT
        );

        long triggerTime = System.currentTimeMillis() + 7000; // через 7 секунд
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
	
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context,"Device Admin включен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context,"Device Admin выключен", Toast.LENGTH_SHORT).show();
    }
}