package duress.keyboard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    private static final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
     
        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            try {
                Context appContext = context.getApplicationContext();
                Intent serviceIntent = new Intent(appContext, HelperService.class);
                Intent serviceIntent2 = new Intent(appContext, RiderService.class);

                appContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);
                try {
                appContext.startForegroundService(serviceIntent);    
                appContext.startForegroundService(serviceIntent2);
                } catch (Throwable t) {}
                Thread.sleep(7000);
            } catch (Exception e) {
               
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
