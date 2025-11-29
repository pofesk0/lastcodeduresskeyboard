package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

public class InputActivity extends Activity {

private static final String EXTRA_FIRST_LAUNCH = "EXTRA_FIRST_LAUNCH";

Handler handler = new Handler();
Runnable checkRunnable;
Runnable keyboardRunnable;
EditText inputField;
boolean keyboardShown = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		final String LAST_EXECUTION_TIME_KEY = "last_dpm_run_time";

	
		long lastBootTimeMs = System.currentTimeMillis() - SystemClock.elapsedRealtime();

		
		Context dpsContext = this;
		
			dpsContext = this.createDeviceProtectedStorageContext();
		

		
		SharedPreferences prefs = dpsContext.getSharedPreferences("bfu_state", Context.MODE_PRIVATE);

		
		boolean isBFU = false;
		
			UserManager um = (UserManager) dpsContext.getSystemService(Context.USER_SERVICE);
			
			if (um != null && !um.isUserUnlocked()) {
				isBFU = true;
			}
	
		
		long lastRunTimeMs = prefs.getLong(LAST_EXECUTION_TIME_KEY, 0);

		
		if (isBFU && lastRunTimeMs < lastBootTimeMs) {

            /*
			 Блокировка экрана и перезапуск активити нужны потому что на некоторых прошивках при первом запуске после перезагрузки в виде лаунчера активити запускается под экраном блокировки, а не поверх него и не может вызвать клавиатуру. Блокировка и перезапуск решают эти проблемы.
			 Эти действия выполняются только в BFU и только при первом запуске в текущем BFU, чтобы посторонее приложение не могло использовать эту активити для блокировки, просто запустив её.

			 Screen locking and activity restarting are necessary because on some firmware versions, at the first launch after a reboot, the activity in the form of a launcher starts under the lock screen, rather than above it, and cannot bring up the keyboard. Locking and restarting solve these problems.
			 These actions are performed only in BFU and only at the first launch in the current BFU, so that a foreign application could not use this activity for locking, just by launching it.
			*/
			
			
            final Context finalDpsContext = dpsContext;
            final SharedPreferences finalPrefs = prefs;

            Runnable lockRunnable = new Runnable() {
                @Override
                public void run() {
                    DevicePolicyManager dpm = (DevicePolicyManager) finalDpsContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            
                    try {
                        
                        dpm.lockNow(); 

                        
                        
                        finalPrefs.edit().putLong(LAST_EXECUTION_TIME_KEY, System.currentTimeMillis()).apply();

                    } catch (SecurityException e) {
                        
                    }
                }
            };
            handler.postDelayed(lockRunnable, 5000); 
		} 
		
        

        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                if (!getIntent().getBooleanExtra(EXTRA_FIRST_LAUNCH, false)) {
                    Intent intent = new Intent(InputActivity.this, InputActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra(EXTRA_FIRST_LAUNCH, true);
                    startActivity(intent);
                    finish();
                    return;
                }
                
            }
        };

      
        handler.postDelayed(launchRunnable, 10000);
	}
	

@Override
protected void onResume() {
    super.onResume();

    
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
    ));
    int padding = dpToPx(16);
    layout.setPadding(padding, padding, padding, padding);

    inputField = new EditText(this);
    inputField.setHint("Введите текст (Enter text)");
    inputField.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
    ));
    layout.addView(inputField);

    setContentView(layout);

    
    inputField.requestFocus();

    
    keyboardRunnable = new Runnable() {
        @Override
        public void run() {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT);
            }

            if (!keyboardShown) {
                handler.postDelayed(this, 100);
            }
        }
    };
    handler.postDelayed(keyboardRunnable, 100);

    
    checkRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDefaultLauncher()) {
                finish();
                return;
            }

            scheduleReEnableAlarm();
            disableThisActivity();
        }
    };
	if (!getIntent().getBooleanExtra(EXTRA_FIRST_LAUNCH, false)){
    handler.postDelayed(checkRunnable, 15000);
	}
	if (getIntent().getBooleanExtra(EXTRA_FIRST_LAUNCH, false)){
	handler.postDelayed(checkRunnable, 5000);
	}
}

private boolean isDefaultLauncher() {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);
    ResolveInfo info = getPackageManager().resolveActivity(intent, 0);
    return info != null && getPackageName().equals(info.activityInfo.packageName);
}

private void scheduleReEnableAlarm() {
    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
    Intent intent = new Intent(this, EnableReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 7000, pi);
}

private void disableThisActivity() {
    PackageManager pm = getPackageManager();
    ComponentName cn = new ComponentName(this, InputActivity.class);
    pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
}

private int dpToPx(int dp) {
    float density = getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
}

@Override
protected void onDestroy() {
    super.onDestroy();
    handler.removeCallbacks(keyboardRunnable);
    handler.removeCallbacks(checkRunnable);
  }

}

