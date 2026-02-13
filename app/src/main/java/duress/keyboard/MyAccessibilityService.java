package duress.keyboard;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.UserManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    private BroadcastReceiver screenReceiver;

    @Override
    protected void onServiceConnected() {
        registerScreenReceiver();
        
        Context dpsContext = this.createDeviceProtectedStorageContext();
        android.os.UserManager um = (android.os.UserManager) dpsContext.getSystemService(Context.USER_SERVICE);

        if (um != null && !um.isUserUnlocked()) {
        Intent i = new Intent(dpsContext, TriggerReceiver.class);
        dpsContext.sendBroadcast(i);
        }
        
    }

    private void registerScreenReceiver() {
        if (screenReceiver != null) return;

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {

                    Intent i = new Intent(context, TriggerReceiver.class);
                    context.sendBroadcast(i);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);

        registerReceiver(screenReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            screenReceiver = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // не используется
    }

    @Override
    public void onInterrupt() {
    }
}
