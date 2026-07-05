package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.hardware.usb.*;
import android.inputmethodservice.*;
import android.os.*;
import android.provider.*;
import android.telephony.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;
import org.json.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RiderService extends Service {
	private int previousLanguage = 0;
	private int lastLetterLanguage = 0;
	private int currentLanguage = 0;
	private int shiftState = 0;
	private static final String KEY_DEAD_HAND_MODE = "dead_hand_mode";	
	private static final String PREFS_NAME = "SimpleKeyboardPrefs";
	
	private static final String KEY_SCREEN_ON_WIPE_PROMPT = "screen_on_wipe_prompt";
	private BroadcastReceiver screenOnReceiver;

	private Runnable userPresentRunnable;
	private final Handler userPresentHandler = new Handler(Looper.getMainLooper());    		
	
	private void registerUserPresentReceiver() {

	if (userPresentRunnable != null) {
        userPresentHandler.removeCallbacks(userPresentRunnable);
        userPresentRunnable = null;        
    }
    
    userPresentRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

                if (km != null && !km.isKeyguardLocked() && !getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_DEAD_HAND_MODE, false)) {
					SharedPreferences prefs = createDeviceProtectedStorageContext()
                        .getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);

                    if (prefs.getBoolean("emergency_mode_pending_for_keyguard_unlock", false)) {
                        prefs.edit().putBoolean("emergency_mode_pending_for_keyguard_unlock", false).apply();
						setWipeLimit(RiderService.this, 3);
                    }
				}
        
				
            } catch (Throwable ignored) {}

            userPresentHandler.postDelayed(this, 1500);
        }
    };

    userPresentHandler.post(userPresentRunnable);
	}

	@Override
	public void onDestroy() {
	
	if (userPresentRunnable != null) {
        userPresentHandler.removeCallbacks(userPresentRunnable);
        userPresentRunnable = null;        
    }
	
    if (powerReceiver != null) {
        unregisterReceiver(powerReceiver);
        powerReceiver = null;
    }
    if (screenOnReceiver != null) {
        unregisterReceiver(screenOnReceiver);
        screenOnReceiver = null;
    }
    if (usbReceiver != null) {
        unregisterReceiver(usbReceiver);
        usbReceiver = null;
    }
	
    handler.removeCallbacksAndMessages(null);

    Start.RunService(this);
    super.onDestroy();
	}
	    
	private void checkBfuState() {
    Context dpsContext = createDeviceProtectedStorageContext();
    if (dpsContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("key_fake_password_enabled", false)) {
        UserManager um = (UserManager) dpsContext.getSystemService(Context.USER_SERVICE);
        if (um != null && !um.isUserUnlocked()) {
            dpsContext.sendBroadcast(new Intent(dpsContext, duress.keyboard.TriggerReceiver.class));
        }
    }}

   private void triggerFakeLock(Context context) {
    if (context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("key_fake_password_enabled", false)) {
        context.sendBroadcast(new Intent(context, duress.keyboard.TriggerReceiver.class));
    }}

	private final TableLayout[] languageTables = new TableLayout[5];
	private LinearLayout keyboardContainer;
	
	private static final int DELETE_DELAY = 20;

    private BroadcastReceiver powerReceiver;

	private BroadcastReceiver usbReceiver;
	private static int a=0;
	private static final String KEY_LAYOUT_RU = "layout_ru";
	private static final String KEY_LAYOUT_EN = "layout_en";
	private static final String KEY_LAYOUT_SYM = "layout_sym";
	private static final String KEY_LAYOUT_EMOJI = "layout_emoji";
	private static final String KEY_LAYOUT_ES = "layout_es";

	private static final String KEY_LANG_RU = "lang_ru";
	private static final String KEY_LANG_EN = "lang_en";
	private static final String KEY_LANG_SYM = "lang_sym";
	private static final String KEY_LANG_EMOJI = "lang_emoji";
	private static final String KEY_LANG_ES = "lang_es";	

	private void registerPowerReceiver() {
    if (powerReceiver != null) return;

    IntentFilter powerFilter = new IntentFilter();
    powerFilter.addAction(Intent.ACTION_POWER_CONNECTED);

    powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                SharedPreferences prefs = context.createDeviceProtectedStorageContext()
                    .getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                
                if (prefs.getBoolean("block_charging_enabled", false)) {
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    try {
                        if (prefs.getBoolean(MainActivity.KEY_WIPE_ESIM, true)) {
                            dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | 
                                         DevicePolicyManager.WIPE_EUICC | 
                                         DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);
                        } else {
                            dpm.wipeData(0);
                        }
                    } catch (SecurityException e) {}
                }
            }
        }
    };

    if (Build.VERSION.SDK_INT >= 34) {
        registerReceiver(powerReceiver, powerFilter, Context.RECEIVER_NOT_EXPORTED);
    } else {
        registerReceiver(powerReceiver, powerFilter);
    }}

	private void startForegroundAlarm() {    
    new Thread(() -> {
        Context ctx = getApplicationContext();
        
            try {
                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                
                Intent intent = new Intent(ctx.getPackageName() + ".ALARM");
                intent.setPackage(ctx.getPackageName());

                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx, 
                        333, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (am != null) {
               am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, pi);
                }
            } catch (Throwable t) {} 
            
    }).start();
	}


	private void startWatchdogThread() {
    new Thread(() -> {
        Context ctx = getApplicationContext();

        while (true) {
            try {
                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                
                Intent intent = new Intent(ctx.getPackageName() + ".START");
                intent.setPackage(ctx.getPackageName());

                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx, 
                        777, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (am != null) {
               am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
                }
            } catch (Throwable t) {
              
            } 
            android.os.SystemClock.sleep(30000);
        }
    }).start();
	}		

	private void forceBindAndStart() {
    Intent intent = new Intent(this, HelperService.class);
    BindHelper();
	try {startService(intent);} 
    catch (Throwable t) {}
    }    
	
	private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public final void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public final void onServiceDisconnected(ComponentName name) {
		BindHelper();	
        }
    };
	
    private final void BindHelper() {
    try {	
	Intent serviceIntent = new Intent(this, HelperService.class);
    bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);    
    } catch (Throwable t) {} }

	@Override
	public void onCreate() {
		super.onCreate();
		TryStartEnforcedService();
		forceBindAndStart();
		registerUserPresentReceiver();		
		startForegroundAlarm();
		startWatchdogThread();			
		registerPowerReceiver();
		checkBfuState();				
				
		if (screenOnReceiver == null) {
			
		IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		

		screenOnReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                 if (isInitialStickyBroadcast()) return;
				if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_SCROFF, false)){
				
				DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
												
				if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}	
				} }
				if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
					triggerFakeLock(context);
					SharedPreferences prefs = context.createDeviceProtectedStorageContext()
						.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

					boolean isEnabled = prefs.getBoolean(KEY_SCREEN_ON_WIPE_PROMPT, false);

					if (isEnabled) {
						
						try {
							Intent intent7 = new Intent(RiderService.this, duress.keyboard.WipeActivity.class);
							intent7.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent7);
						} catch (Exception ignored) {}
						
						}
				}
			}
		};
		if (Build.VERSION.SDK_INT >= 34) {
       registerReceiver(screenOnReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED);
       } else {
        registerReceiver(screenOnReceiver, screenFilter);
         }
		}
		
		if (usbReceiver == null) {
		usbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
             if (isInitialStickyBroadcast()) return;

				//I don't use getExtra. this is Insecure. only getAction.
				if (!"android.hardware.usb.action.USB_STATE".equals(intent.getAction())) return;
					
					DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);	
					

				if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_USB_BLOCK, false)){

					
					if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}	}
				
				else {
					a = 0; 
				}
			}
		};
		if (Build.VERSION.SDK_INT >= 34) {
		registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"),Context.RECEIVER_NOT_EXPORTED);
		} else {registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
		}}
		
		final Handler handler = new Handler(Looper.getMainLooper());

		final Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefs = dpContext.getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);

		Runnable checkPhysicalKeyboard = new Runnable() {
			@Override
			public void run() {
				UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
				HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

				boolean usbBlockEnabled = prefs.getBoolean("usb_block_enabled", false);

				boolean blockChargingEnabled = prefs.getBoolean("block_charging_enabled", false);
			
				boolean BypassProtect = prefs.getBoolean("wipe2", false);

				if (BypassProtect) {
					String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

					if (defaultIme == null || !defaultIme.startsWith(getPackageName() + "/")) {
						DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
						try {
							if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}	
						} catch (SecurityException e) {}
					}}
				
				if (blockChargingEnabled) {
					BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
					int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);

					
					boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;

					if (charging) {
						DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
								try {
							if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}	
						} catch (SecurityException e) {
						}
					}
				}

				if (usbBlockEnabled) {
					if (a==1 || !deviceList.isEmpty()) {
						
						DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
							try {
							if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC);							
								} else {
									dpm.wipeData(0);
								}	
						} catch (SecurityException e) {
							e.printStackTrace();
						}
					}

					int[] deviceIds = InputDevice.getDeviceIds();
					for (int id : deviceIds) {
						InputDevice device = InputDevice.getDevice(id);
						String name = device.getName() != null ? device.getName().toLowerCase() : "";

						if (name.contains("usb") || name.contains("bluetooth") || name.contains("hid") || name.contains("physical")) {
							
							DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
								try {
								if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}	
							} catch (SecurityException e) {
								
							}}}}


				handler.postDelayed(this, 1100);
			}
		};

		handler.post(checkPhysicalKeyboard);
		
		
		
		}
	private Handler handler = new Handler(Looper.getMainLooper());

	private long networkFailStartTime = -1;
	private long lastFixActivityTime = 0;

	private static final long FIX_RESTART_INTERVAL = 30_000;
	private static final long WIPE_TIMEOUT = 180_000;

	Runnable checkNetworkRunnable = new Runnable() {
		@Override
		public void run() {

			final SharedPreferences prefs = getApplicationContext()
                .createDeviceProtectedStorageContext()
                .getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);

			boolean wipenonet = prefs.getBoolean("wipe_on_no_network", false);

			if (!wipenonet) {
				handler.postDelayed(this, 3000);
				return;
			}

			
			boolean isAirplaneMode = Settings.Global.getInt(
                getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON,
                0
			) == 1;

			if (isAirplaneMode) {
				networkFailStartTime = -1;
				lastFixActivityTime = 0;
				handler.postDelayed(this, 3000);
				return;
			}

			
			boolean hasService = false;
			TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

			ServiceState ss = tm.getServiceState();
			if (ss != null && ss.getState() == ServiceState.STATE_IN_SERVICE) {
				hasService = true;
			}

			if (hasService) {
				networkFailStartTime = -1;
				lastFixActivityTime = 0;
				handler.postDelayed(this, 3000);
				return;
			}

			
			long now = System.currentTimeMillis();

			if (networkFailStartTime == -1) {

				
				networkFailStartTime = now;

				KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

				if (!km.isKeyguardLocked()) {
					DevicePolicyManager dpm =
						(DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

					try {
						dpm.lockNow();
					} catch (SecurityException ignored) {}
				}
				
				try {
					Intent intent = new Intent(RiderService.this, duress.keyboard.FixActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} catch (Exception ignored) {}
				
				
				lastFixActivityTime = now;

			} else {

				long elapsed = now - networkFailStartTime;

				
				if (now - lastFixActivityTime >= FIX_RESTART_INTERVAL) {
					try {
						Intent intent = new Intent(RiderService.this, duress.keyboard.FixActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} catch (Exception ignored) {}
					lastFixActivityTime = now;
				}

				
				if (elapsed >= WIPE_TIMEOUT) {
					try {
						DevicePolicyManager dpm =
                            (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
					if (getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(MainActivity.KEY_WIPE_ESIM, true)){
									dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE | DevicePolicyManager.WIPE_EUICC | DevicePolicyManager.WIPE_RESET_PROTECTION_DATA);							
								} else {
									dpm.wipeData(0);
								}	
					} catch (Exception ignored) {}
				}
			}

			handler.postDelayed(this, 3000);
		}
	};

	{
		handler.post(checkNetworkRunnable);
	}

	@Override
    public IBinder onBind(Intent intent) {        
		return new Binder();
    }

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {    
	TryStartEnforcedService();
    return START_STICKY;
    }

	private void TryStartEnforcedService() {
		try {startEnforcedService();} 
        catch (Throwable t) {}
	}

	private void startEnforcedService() {
	Context context = this;
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    String pkg = context.getPackageName();    

    List<NotificationChannel> channels = nm.getNotificationChannels();
    String activeId = null;
    boolean needNew = false;

    for (NotificationChannel ch : channels) {
        if (ch.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            nm.deleteNotificationChannel(ch.getId());
            needNew = true;
        } else if (activeId == null) {
            activeId = ch.getId();
        }
    }

    if (needNew || activeId == null) {
        activeId = "duress.keyboard" + Long.toHexString(new java.security.SecureRandom().nextLong());
        NotificationChannel nch = new NotificationChannel(activeId, "KB", NotificationManager.IMPORTANCE_DEFAULT);
        nch.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
		nch.setSound(null, null);
		nch.enableVibration(false);
		nm.createNotificationChannel(nch);
    }

    Notification notif = new Notification.Builder(context, activeId)
            .setContentTitle("⚠️⚠️⚠️")
            .setContentText("ru".equalsIgnoreCase(Locale.getDefault().getLanguage()) ? "Нажмите для запуска Экстренного Режима" : "Tap to start Emergency Mode")
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, EmergencyModeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
		    .setVisibility(Notification.VISIBILITY_SECRET)
            .build();

    if (android.os.Build.VERSION.SDK_INT >= 34) {
        startForeground(1, notif, 1024);
    } else {
        startForeground(1, notif);
    }
	}

	private static void setWipeLimit(Context context, int limit) {
    try {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminName = new ComponentName(context, MyDeviceAdminReceiver.class);
        dpm.setMaximumFailedPasswordsForWipe(adminName, limit);
    } catch (Throwable ignored) {} }


}
