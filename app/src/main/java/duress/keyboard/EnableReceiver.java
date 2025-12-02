package duress.keyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

public class EnableReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {

        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					PackageManager pm = context.getPackageManager();
					ComponentName cn = new ComponentName(context, InputActivity.class);

					pm.setComponentEnabledSetting(
						cn,
						PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
						PackageManager.DONT_KILL_APP
					);
				}
			}, 350);
    }
}
