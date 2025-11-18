package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;
import android.view.*;

public class FixActivity extends Activity {
  
	private static int a=1;
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {    
                restart();         
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

   
    public void restart() {
        Intent i = new Intent(this, FixActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

		boolean begin = getIntent().getBooleanExtra("begin", false);
		
		
		if (begin && a==1) {
            begin = false;
			a=0;
			DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
			try {
				dpm.lockNow(); //Protect from situtions when someone snatched your phone and put in Faraday bug to prevent lock and immediatelly get data, but this help to lock.
			} catch (SecurityException e) {
			}
            
        }
		
		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(screenReceiver); }
        catch (Exception e) {}
    }
}
