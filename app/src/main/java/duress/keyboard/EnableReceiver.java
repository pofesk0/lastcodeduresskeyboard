package duress.keyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;

public class EnableReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        PackageManager pm = context.getPackageManager();
        ComponentName cn = new ComponentName(context, InputActivity.class);

        pm.setComponentEnabledSetting(
			cn,
			PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
			PackageManager.DONT_KILL_APP
        );
    }
}
