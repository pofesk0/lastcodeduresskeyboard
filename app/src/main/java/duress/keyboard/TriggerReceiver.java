package duress.keyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TriggerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent activityIntent = new Intent(context, LockActivity.class);
        activityIntent.addFlags(
			Intent.FLAG_ACTIVITY_NEW_TASK |
			Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        context.startActivity(activityIntent);
    }
}