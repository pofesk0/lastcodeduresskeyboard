package duress.keyboard;

import android.content.*;
import android.content.pm.*;
import android.os.*;

public class EnableReceiver extends BroadcastReceiver {
	
private static final String KEY_AUTORUN = "auto_run";
	
    @Override
    public void onReceive(final Context context, Intent intent) {

        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					PackageManager pm = context.getPackageManager();
					ComponentName cn = new ComponentName(context, InputActivity.class);
					
					Context dpContext = context.getApplicationContext().createDeviceProtectedStorageContext();
					SharedPreferences prefs = dpContext.getSharedPreferences("SimpleKeyboardPrefs", Context.MODE_PRIVATE);
					
					
					boolean autoRunEnabled = prefs.getBoolean(KEY_AUTORUN, false);
					if (autoRunEnabled) {
					
					pm.setComponentEnabledSetting(
						cn,
						PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
						PackageManager.DONT_KILL_APP
					);
				}}
			}, 350);
    }
}
