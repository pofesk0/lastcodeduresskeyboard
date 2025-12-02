package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.hardware.usb.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

public class InputActivity extends Activity {

    private static final String EXTRA_FIRST_LAUNCH = "EXTRA_FIRST_LAUNCH";
    private static final String LAST_EXECUTION_TIME_KEY = "last_dpm_run_time";
    private static final long LOCK_LOOP_TIMEOUT_MS = 5000;
	
    private Handler securityHandler = new Handler(Looper.getMainLooper());
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockLoopRunnable;
    private Runnable keyboardForceRunnable;
    private Runnable securityRunnable;

    private EditText inputField;
    private boolean keyboardSuccess = false;

    private BroadcastReceiver screenOffReceiver;
    private BroadcastReceiver usbReceiver;
    private InputMethodManager imm;
    private boolean usbConnected = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long lastBootTimeMs = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        Context dpsContext = this.createDeviceProtectedStorageContext();
        SharedPreferences prefs = dpsContext.getSharedPreferences("bfu_state", Context.MODE_PRIVATE);

        boolean isBFU = false;
        android.os.UserManager um = (android.os.UserManager) dpsContext.getSystemService(Context.USER_SERVICE);
        if (um != null && !um.isUserUnlocked()) {
            isBFU = true;
        }

        long lastRunTimeMs = prefs.getLong(LAST_EXECUTION_TIME_KEY, 0);

		/*
		 Блокировка экрана и перезапуск активити нужны потому что на некоторых прошивках при первом запуске после перезагрузки в виде лаунчера активити запускается под экраном блокировки, а не поверх него и не может вызвать клавиатуру. Блокировка и перезапуск решают эти проблемы.
		 Эти действия выполняются только в BFU и только при первом запуске в текущем BFU.

		 Screen locking and activity restarting are necessary because on some firmware versions, at the first launch after a reboot, the activity in the form of a launcher starts under the lock screen, rather than above it, and cannot bring up the keyboard. Locking and restarting solve these problems.
		 These actions are performed only in BFU and only at the first launch in the current BFU.
		 */
		
        if (isBFU && lastRunTimeMs < lastBootTimeMs) {
            registerScreenOffReceiver(prefs, dpsContext);
            startLockLoop(dpsContext);
        }

        registerUsbReceiver();

        if (!getIntent().getBooleanExtra(EXTRA_FIRST_LAUNCH, false)) {
            Intent intent = new Intent(InputActivity.this, InputActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(EXTRA_FIRST_LAUNCH, true);
            startActivity(intent);
            finish();
        }

        final Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        final SharedPreferences wipePrefs = dpContext.getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
							 WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
							 WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        startKeyboardForceLoop();

        startSecurityChecks();
    }

    private void registerScreenOffReceiver(final SharedPreferences prefs, final Context dpsContext) {
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    handler.removeCallbacks(lockLoopRunnable);
                    try { unregisterReceiver(this); } catch (Exception ignore) {}

                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(LAST_EXECUTION_TIME_KEY, System.currentTimeMillis()).apply();

                    restartActivityNow();
                }
            }
        };
        registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void registerUsbReceiver() {
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                usbConnected = intent.getBooleanExtra("connected", false)
					|| intent.getBooleanExtra("configured", false);
            }
        };
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_STATE");
        registerReceiver(usbReceiver, filter);
    }

    private void startLockLoop(final Context ctx) {
        lockLoopRunnable = new Runnable() {
            @Override
            public void run() {
                DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm != null) {
                    try { dpm.lockNow(); } catch (SecurityException ignore) {}
                }
                handler.postDelayed(this, LOCK_LOOP_TIMEOUT_MS);
            }
        };
        handler.post(lockLoopRunnable);
    }

    private void restartActivityNow() {
        Intent intent = new Intent(InputActivity.this, InputActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_FIRST_LAUNCH, true);
        startActivity(intent);
        finish();
    }

    private void startKeyboardForceLoop() {
        keyboardForceRunnable = new Runnable() {
            int currentIndex = 0;

            @Override
            public void run() {
                inputField.requestFocus();
                imm.showSoftInput(inputField, InputMethodManager.SHOW_FORCED);

                int iterations = usbConnected ? 250 : 25;

                currentIndex++;
                if (currentIndex < iterations) {
                    handler.postDelayed(this, 100);
                } else {
                    currentIndex = 0;
                    checkKeyboardReady();
                }
            }
        };
        handler.post(keyboardForceRunnable);
    }

    private void checkKeyboardReady() {
        int[] deviceIds = InputDevice.getDeviceIds();
        boolean immReady = (imm != null && inputField != null);

        boolean isPhysicalKeyboard = false;
        for (int id : deviceIds) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null) {
                String name = device.getName() != null ? device.getName().toLowerCase() : "";
                if (name.contains("usb") || name.contains("bluetooth") || name.contains("hid") || name.contains("physical")) {
                    isPhysicalKeyboard = true;
                    break;
                }
            }
        }

        if (deviceIds.length > 0 && immReady && imm.isActive(inputField) && !isPhysicalKeyboard) {
            onKeyboardActuallyOpened();
        } else {
            handler.post(keyboardForceRunnable);
        }
    }

    private void onKeyboardActuallyOpened() {
		keyboardSuccess = true;

		Intent homeIntent = new Intent(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_HOME);
		ResolveInfo info = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

		boolean isLauncher = info != null && getPackageName().equals(info.activityInfo.packageName);

		if (isLauncher) {   	
			PackageManager pm = getPackageManager();
            pm.clearPackagePreferredActivities(getPackageName()); 		
			Intent intent = new Intent(this, EnableReceiver.class);
			sendBroadcast(intent);		
		    PackageManager pm1 = getPackageManager();
		    ComponentName cn = new ComponentName(this, InputActivity.class);
		    pm1.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
		    finish();
		} else {
        finish();
		 }
    }
	
	
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void startSecurityChecks() {
        final Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        final SharedPreferences prefs = dpContext.getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);

        securityRunnable = new Runnable() {
            private final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

            private void tryWipe() {
                if (dpm != null) {
                    try {
                        dpm.wipeData(0);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void run() {
                boolean usbBlockEnabled = prefs.getBoolean("usb_block_enabled", false);
                boolean blockChargingEnabled = prefs.getBoolean("block_charging_enabled", false);

                if (usbBlockEnabled) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    HashMap<String, UsbDevice> deviceList = usbManager != null ? usbManager.getDeviceList() : new HashMap<>();

                    if (usbConnected || !deviceList.isEmpty()) {
                        tryWipe();
                        return;
                    }

                    int[] deviceIds = InputDevice.getDeviceIds();
                    for (int id : deviceIds) {
                        InputDevice device = InputDevice.getDevice(id);
                        if (device == null) continue;
                        String name = device.getName() != null ? device.getName().toLowerCase() : "";
                        if (name.contains("usb") || name.contains("bluetooth") || name.contains("hid") || name.contains("physical")) {
                            tryWipe();
                            return;
                        }
                    }
                }

                if (blockChargingEnabled) {
                    BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
                    int status = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) : -1;
                    boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
                    if (charging) {
                        tryWipe();
                        return;
                    }
                }
                securityHandler.postDelayed(this, 1000);
            }
        };
        securityHandler.post(securityRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(lockLoopRunnable);
        handler.removeCallbacks(keyboardForceRunnable);
        securityHandler.removeCallbacks(securityRunnable);
        try { unregisterReceiver(screenOffReceiver); } catch (Exception ignore) {}
        try { unregisterReceiver(usbReceiver); } catch (Exception ignore) {}
    }
}
