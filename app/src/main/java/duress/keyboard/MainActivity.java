package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.text.method.*;
import android.text.style.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.nio.charset.*;
import java.security.*;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends Activity {

	private android.app.AlertDialog accessibilityDialog;
	private AlertDialog deadHandDialog;
	private android.widget.Switch switchDH;
	private static final String KEY_DEAD_HAND_MODE = "dead_hand_mode";	
	private android.app.AlertDialog chargingWarningDialog;
	private android.app.AlertDialog confirmWipeFlagsDialog;
	private android.app.AlertDialog adminErrorDialog;
	private android.app.AlertDialog infoDialog;
	private android.app.AlertDialog AdditionalOptionsWarning;
	private Button AdditionalOptionsBack;
	private static boolean main=true;
	boolean accessibilityEnabled = false;
    private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private static final String KEY_CUSTOM_COMMAND = "custom_wipe_command";
	private BroadcastReceiver screenOffReceiver;
	private static final String KEY_WIPE_ON_REBOOT = "wipe_on_reboot";	
	private static final String KEY_WIPE2 = "wipe2";
	static final String KEY_WIPE_ESIM = "WIPE_ESIM";
	static final String KEY_WIPE_SCROFF = "WIPE_SCROFF";
	private static final String KEY_SCREEN_ON_WIPE_PROMPT = "screen_on_wipe_prompt";
	private SharedPreferences prefsNetwork;
	private static final String KEY_FAKE_HOME = "fake_home_enabled";
	
	private Switch noNetworkWipeSwitch;
	private static final String KEY_WIPE_ON_NO_NETWORK = "wipe_on_no_network";
	static final String KEY_USB_BLOCK = "usb_block_enabled";
    private static final String KEY_BLOCK_CHARGING = "block_charging_enabled";
    private static final String KEY_LAYOUT_RU = "layout_ru";
    private static final String KEY_LAYOUT_EN = "layout_en";
    private static final String KEY_LAYOUT_SYM = "layout_sym";
    private static final String KEY_LAYOUT_EMOJI = "layout_emoji";
    private static final String KEY_LAYOUT_ES = "layout_es";
	private static boolean RESULT = false;
	private static int isPendingAdmin = 0;
	private EditText commandInput; 
    private static final String KEY_LANG_RU = "lang_ru";
    private static final String KEY_LANG_EN = "lang_en";
    private static final String KEY_LANG_SYM = "lang_sym";
    private static final String KEY_LANG_EMOJI = "lang_emoji";
    private static final String KEY_LANG_ES = "lang_es";
	private static int e= 0;

	private void openKeyboardSettings() {

	try { 
		Intent std = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);									
		startActivity(std); 
	    return;	
	} catch (Throwable t1) {}	
		
	try {	
        Intent intent = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$KeyboardSettingsActivity"));
        intent.putExtra(":settings:fragment_args_key", "virtual_keyboard_pref");    
        startActivity(intent);
		return;
    } catch (Throwable t2) {}

	try {	
        Intent internal = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$KeyboardSettingsActivity"));									
	    startActivity(internal);	
		return;
    } catch (Throwable t3) {}
				
	}	

	private void showAdditionalOptionsWarning(Button AdditionalOptionsBack) {
    aetest();
    String defaultIme = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.DEFAULT_INPUT_METHOD);
    boolean isDefaultIme = defaultIme != null && defaultIme.startsWith(getPackageName() + "/");
    boolean canDraw = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && android.provider.Settings.canDrawOverlays(this);

	if (accessibilityEnabled && isDefaultIme && !canDraw && AdditionalOptionsWarning != null && AdditionalOptionsWarning.isShowing()) {
	    AdditionalOptionsWarning.dismiss();
		AdditionalOptionsWarning = null;	
	} else if (accessibilityEnabled && isDefaultIme && canDraw && AdditionalOptionsWarning != null && AdditionalOptionsWarning.isShowing()) {
	   AdditionalOptionsWarning.dismiss();
	   AdditionalOptionsWarning = null;
	   return;	
	} else {
	   if (accessibilityEnabled && isDefaultIme && canDraw) return;
       if (AdditionalOptionsWarning != null && AdditionalOptionsWarning.isShowing()) return;	
	}		    

    final boolean isRussian = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());
    final LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.bottomMargin = dpToPx(12);

    TextView msg = new TextView(this);
    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

    if (!accessibilityEnabled || !isDefaultIme) {
        
		msg.setText(isRussian ? 
		"Привет. Это предупреждение о работе дополнительных параметров. Многие из функций требуют возможности работать в фоне или запускать окна из фона для реализации своего функционала. На новых версиях Android фоновая работа становится всё более ограниченной. Поэтому, просьба: включите пожалуйста спецвозможности для этого приложения и назначьте эту клавиатуру по умолчанию. Это даст больше шансов на стабильную работу в фоне. Для некоторых опций это может быть обязательным. Вы видите это окно потому что не сделали что-то из вышеперчисленного (тогда сделайте это) или спецвозможности были отключены из-за системного сбоя (в таком случае перезагрузите телефон)." : 
		"Hello. This is a warning about the operation of additional options. Many of the features require the ability to work in the background or to launch windows from the background to implement their functionality. On new Android versions, background work is becoming increasingly restricted. Therefore, a request: please enable Accessibility for this application and set this keyboard as default. This will give more chances for stable work in the background. For some options, this may be mandatory. You see this window because you have not done some of the above (then do it) or Accessibility was disabled due to a system error (in this case reboot your phone).");

		root.addView(msg, lp);

        Button b1 = new Button(this);
		b1.setText(isRussian ? "Перейти в главное меню чтобы включить клавиатуру" : "Go to main menu to enable keyboard");
		b1.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (AdditionalOptionsWarning != null && AdditionalOptionsWarning.isShowing()) {
                AdditionalOptionsWarning.dismiss();
				AdditionalOptionsWarning = null;
            }
            AdditionalOptionsBack.performClick();
        }});
		root.addView(b1, lp);
	   
        Button b3 = new Button(this);
		b3.setText(isRussian ? "Включить Спецвозможности" : "Enable Accessibility");
		b3.setOnClickListener(v -> {
		ais();
		if (accessibilityEnabled) {
        Toast.makeText(MainActivity.this, isRussian ? "Спецвозможности уже включены." : "Accessibility is already enabled.", Toast.LENGTH_SHORT).show();
		}});
		root.addView(b3, lp);

    } else {
        msg.setText(isRussian ? "Привет это предупреждение о работе некоторых функций. Вы включили спецвозможности, но не включили разрешение на наложение поверх других окон. Пожалуйста включите его тоже. Это запасной вариант на случай если спецвозможности перестануть работать." : "Hello, this is a warning about the operation of some features. You have enabled accessibility, but have not enabled the overlay permission. Please enable it too. This is a fallback, just in case accessibility stops working.");
        root.addView(msg, lp);

        Button b1 = new Button(this);
        b1.setText(isRussian ? "Включить разрешение на наложение поверх других окон" : "Enable overlay permission");
        b1.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Throwable e) {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            }
        });
        root.addView(b1, lp);
    }

    Button bClose = new Button(this);
    bClose.setText(isRussian ? "Закрыть и продолжить" : "Close and continue");
    root.addView(bClose, lp);

    builder.setTitle(isRussian ? "Предупреждение" : "Warning").setView(root).setCancelable(false);
    AdditionalOptionsWarning = builder.create();
    bClose.setOnClickListener(v -> {
	   AdditionalOptionsWarning.dismiss();
	   AdditionalOptionsWarning = null; 
	});
    AdditionalOptionsWarning.show();

    android.view.Window window = AdditionalOptionsWarning.getWindow();
    if (window != null) {
        android.view.WindowManager.LayoutParams lp2 = window.getAttributes();
        lp2.gravity = android.view.Gravity.CENTER;
        lp2.x = 0;
        lp2.y = 0;
        window.setAttributes(lp2);
    }
	}


	private static android.app.AlertDialog emergencyModeAlertDialog;

	private void showEmergencyModeAlertDialog() {
    if (emergencyModeAlertDialog != null && emergencyModeAlertDialog.isShowing()) return;

    final SharedPreferences prefs = getApplicationContext()
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

    if (prefs.getBoolean("emergency_acknowledged", false)) return;

    final boolean isRussian = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

    final LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    lp.bottomMargin = dpToPx(12);

    TextView t1 = new TextView(this);
    t1.setText(isRussian 
        ? "В этом приложении есть экстренный режим. Если вы нажмете на уведомление этого приложения, вы запустите его. Убедитесь что приложение имеет разрешение на отображение уведомлений, иначе оно не появится. Этот режим блокирует экран и просит систему стирать данные в случае ввода любого неверного пароля на экране блокировки (если вы введёте более 4 символов и допустите хотя-бы 1 ошибку). Сброс через экстренный режим может выполнять сама система при неверном вводе пароля, поэтому это может быть без удаления FRP и без использования других параметров удаления. Уведомление для включения экстренного режима будет видно только на разблокированном экране. Это режим не имеет срока отключения. Чтобы изменить число попыток ввода пароля для сброса данных, зайдите в настройки автосброса в приложении."
        : "This app has the emergency mode. If you tap the app's notification, you will start it. Ensure if the app has permission to show notifications, otherwise it won't appear. This mode locks the screen and asks the system to wipe data in case of any incorrect password entry on the lock screen (if you enter more than 4 characters and make at least 1 mistake). The reset via emergency mode can be performed by the system itself upon an incorrect password entry, therefore this can be without removing FRP and without using other deletion parameters. The notification for enabling the emergency mode will be visible only on an unlocked screen. This mode has no time limit for deactivation. To change the number of password failed attempts for data reset, go to auto-wipe settings in the app.");
    root.addView(t1, lp);

    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
    builder.setTitle(isRussian ? "Предупреждение об экстренном режиме" : "Emergency Mode Alert")
           .setView(root)
           .setCancelable(false);

    emergencyModeAlertDialog = builder.create();

    Button b1 = new Button(this);
    b1.setText(isRussian ? "Открыть Настройки уведомлений" : "Open Notification Settings");
    b1.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
			Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);           
			intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
			startActivity(intent);
        }
    });
    root.addView(b1, lp);

    Button b2 = new Button(this);
    b2.setText(isRussian ? "Закрыть предупреждение" : "Close alert");
    b2.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            prefs.edit().putBoolean("emergency_acknowledged", true).apply();
            if (emergencyModeAlertDialog != null && emergencyModeAlertDialog.isShowing()) {
                emergencyModeAlertDialog.dismiss();
            }
            emergencyModeAlertDialog = null;
        }
    });
    root.addView(b2, lp);

    emergencyModeAlertDialog.show();

    android.view.Window window = emergencyModeAlertDialog.getWindow();
    if (window != null) {
        android.view.WindowManager.LayoutParams lp2 = window.getAttributes();
        lp2.gravity = android.view.Gravity.CENTER;
        lp2.x = 0;
        lp2.y = 0;
        window.setAttributes(lp2);
    }
	}

    
	private void AllowAdmin() {
	ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);				
    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
	intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
	String explanation;
	if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			explanation = "Привет, это приложение DuressKeyboard. Оно стирает данные при вводе задаваемого вами кода сброса через его клавиатуру на экране блокировки и нажатии стрелки Enter (⏎). Также имеет другие функции сброса данных и блокировки экрана. Дайте права Администратора для их работы.";
	} else {
			explanation = "Hi, this is the DuressKeyboard app. It wipes phone data upon entering the reset code through it on the lock screen and pressing the Enter arrow (⏎). It also has other data wipe and screen lock features. Give Administrator rights for their work.";
	}
	intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
	startActivity(intent);
	}
	
	private void Detalis() {
    startActivity(
	new Intent(
		android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts(
						"package",
						getApplicationContext().getPackageName(),
						null
                        )
					)
			);
	}    


	private void ShowAdminErrorDialog() {
    final boolean isRussian = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

    final LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    lp.bottomMargin = dpToPx(12);

    TextView t1 = new TextView(this);
    if (isRussian) {
        t1.setText("Вы, либо система, отменили активацию прав администратора. Если это были вы, например вы случайно нажали \"отмена\", попробуйте снова.");
    } else {
        t1.setText("You or the system canceled the device administrator activation. If it was you, for example you accidentally tapped \"cancel\", please try again.");
    }
    root.addView(t1, lp);
    
    final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
    String title = isRussian ? "Ошибка активации" : "Activation Error";
    
    builder.setTitle(title)
           .setView(root)
           .setCancelable(false);
           
    adminErrorDialog = builder.create();

    Button b1 = new Button(this);
    b1.setText(isRussian ? "Попробовать снова" : "Try again");
    root.addView(b1, lp);
    b1.setOnClickListener(new View.OnClickListener() {
        @Override 
        public void onClick(View v) {            
			isPendingAdmin = 1;
			adminErrorDialog.dismiss();
            AllowAdmin();
        }
    });

    TextView t2 = new TextView(this);
    if (isRussian) {
        t2.setText("Если это была система, перейдите в настройки приложения, нажмите 3 точки в правом верхнем углу, затем \"разрешить ограниченные настройки\". После чего вернитесь сюда и попробуйте снова.");
    } else {
        t2.setText("If it was the system, go to the app settings, tap the 3 dots in the upper right corner, then \"allow restricted settings\". Then return here and try again.");
    }
    root.addView(t2, lp);

    Button b2 = new Button(this);
    b2.setText(isRussian ? "Перейти в настройки приложения" : "Go to app settings");
    root.addView(b2, lp);
    b2.setOnClickListener(new View.OnClickListener() {
        @Override 
        public void onClick(View v) {
            Detalis();
        }
    });

    TextView t3 = new TextView(this);
    if (isRussian) {
        t3.setText("Если 3 точек нет, значит окно активации прав администратора не является ограниченной настройкой. Тогда вернитесь наверх и попробуйте снова. Или перейдите в Настройки Администраторов, если не помогло.");
    } else {
        t3.setText("If there are no 3 dots, it means the admin activation window is not a restricted setting. Then return to the top and try again. Or go to Admin Settings if it didn't help.");
    }
    root.addView(t3, lp);

	Button b3 = new Button(this);
    b3.setText(isRussian ? "Открыть Настройки Администраторов" : "Go to Admin Settings");
    root.addView(b3, lp);
    b3.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
            android.content.Intent intent = new android.content.Intent();
			intent.setComponent(new android.content.ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"));
            startActivity(intent);
			isPendingAdmin = 1;	
			adminErrorDialog.dismiss();	
            } catch (Throwable e) {}
        }
    });	

    adminErrorDialog.show();

    android.view.Window window = adminErrorDialog.getWindow();
    if (window != null) {
        android.view.WindowManager.LayoutParams lp2 = window.getAttributes();
        lp2.gravity = android.view.Gravity.CENTER;
        lp2.x = 0;
        lp2.y = 0;
        window.setAttributes(lp2);
    }
  }


	private void aetest(){

		try {
			int enabled = android.provider.Settings.Secure.getInt(
                getContentResolver(),
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
			);

			if (enabled == 1) {
				String services = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
				);

				if (services != null) {
					String myService =
                        new android.content.ComponentName(
						this,
						MyAccessibilityService.class
					).flattenToString();

					accessibilityEnabled = services.contains(myService);
				}
			} else {accessibilityEnabled=false;}
		} catch (Throwable ignored) {accessibilityEnabled=false;}

	}

	private LinearLayout layout;

	private int dpToPx(int dp) {    
		float density = getResources().getDisplayMetrics().density;    
		return (int) (dp * density + 0.5f);    
	}  

	private String getAllowedCharacters(Context context) {
		Set<String> charSet = new HashSet<>();
		Context dpContext = context.getApplicationContext().createDeviceProtectedStorageContext();
		SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		String[] keys = {KEY_LAYOUT_RU, KEY_LAYOUT_EN, KEY_LAYOUT_ES, KEY_LAYOUT_SYM, KEY_LAYOUT_EMOJI};

		for (String key : keys) {
			String jsonString = prefs.getString(key, "[]");
			try {
				JSONArray outer = new JSONArray(jsonString);
				for (int i = 0; i < outer.length(); i++) {
					JSONArray inner = outer.getJSONArray(i);
					for (int j = 0; j < inner.length(); j++) {
						String symbol = inner.getString(j);

						if (symbol.length() == 1 || symbol.length() > 1 && Character.isSurrogatePair(symbol.charAt(0), symbol.charAt(1))) {
							charSet.add(symbol);
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}


		charSet.remove(" "); 


		charSet.remove("⇪"); // Shift
		charSet.remove("⌫"); // Backspace
		charSet.remove("!#?"); // Sym switch
		charSet.remove("abc"); // Alpha switch
		charSet.remove("🌐"); // Lang switch
		charSet.remove("⏎"); // Enter/Wipe trigger


		StringBuilder sb = new StringBuilder();
		for (String s : charSet) {
			sb.append(s);
		}
		return sb.toString();
	}


	private String generateSalt() {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt);
	}



	private String hashKeyWithSalt(String salt, String cmd) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hashBytes = digest.digest((salt + cmd).getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hashBytes);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		RESULT=false;
		isPendingAdmin = 0;
		AdditionalOptionsBack=null;

		if (deadHandDialog != null) {
		   if (deadHandDialog.isShowing()) {
               deadHandDialog.dismiss();
		   }
		    deadHandDialog=null;	
		}
		
		if (chargingWarningDialog != null) {
			if (chargingWarningDialog.isShowing()) {
				chargingWarningDialog.dismiss();
			}
			chargingWarningDialog = null;
		}	
		
		if (AdditionalOptionsWarning != null) {
			if (AdditionalOptionsWarning.isShowing()) {
				AdditionalOptionsWarning.dismiss();
			}
			AdditionalOptionsWarning = null;
		}

		if (confirmWipeFlagsDialog != null) {
			if (confirmWipeFlagsDialog.isShowing()) {
				confirmWipeFlagsDialog.dismiss();
			}
			confirmWipeFlagsDialog = null;
		}

		if (infoDialog != null) {
			if (infoDialog.isShowing()) {
				infoDialog.dismiss();
			}
			infoDialog = null;
		}
		
		if (accessibilityDialog != null) {
            if (accessibilityDialog.isShowing()) {
                accessibilityDialog.dismiss();
            }
            accessibilityDialog = null;
        }

        if (adminErrorDialog != null) {
            if (adminErrorDialog.isShowing()) {
                adminErrorDialog.dismiss();
            }
            adminErrorDialog = null;
        }

        if (emergencyModeAlertDialog != null) {
            if (emergencyModeAlertDialog.isShowing()) {
                emergencyModeAlertDialog.dismiss();
            }
            emergencyModeAlertDialog = null;
        }
		
		if (screenOffReceiver != null) {
			unregisterReceiver(screenOffReceiver);
			screenOffReceiver = null;
		}
	}

	private void ais() {



		aetest();


		if (accessibilityEnabled) {
			if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
				accessibilityDialog.dismiss();
				accessibilityDialog = null;
			}

			return;
		}

		
		if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
			return; // уже показано
		}


		final LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

		LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		);
		lp.bottomMargin = dpToPx(12);



		TextView t1 = new TextView(this);

		t1.setText(
			"Give accessibility permission to the app "
		);

		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {

			t1.setText(
				"Дайте приложению спецвозможности"
			);}

		root.addView(t1, lp);


		TextView t2 = new TextView(this);

		t2.setText(
			"Go to accessibility settings and enable them for our app. Or tap there to our app (even if gray color) to continue to the next step."
		);

		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {

			t2.setText(
				"Перейдите в настройки спецвозможностей и там включите их для нашего приложения. Или нажмите там на наше приложение, даже если цвет серый, чтобы перейти к следующему шагу."
			);}
		root.addView(t2, lp);

		Button b1 = new Button(this);
		b1.setText("Go to accessibility settings");
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			b1.setText("Перейти в настроки спецвозможностей");}
		root.addView(b1, lp);
		b1.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					startActivity(
						new Intent(
                            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
						)
					);
				}
			});

		TextView t3 = new TextView(this);
		t3.setText(
			"If you're told in Accessibility settings that this is a restricted setting, go to the app settings, tap the three dots in the upper right corner, and then tap Allow restricted settings."
		);
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			t3.setText(
				"Если вам в настройках спецвозможностей сказали, что это ограниченная настройка, то перейдите в настройки приложения, нажмите три точки в правом верхнем углу и затем нажмите разрешить ограниченные настройки."
			);}
		root.addView(t3, lp);

		Button b2 = new Button(this);
		b2.setText("Go to the app settings");
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			b2.setText("Перейти в настройки приложения");
		}
		root.addView(b2, lp);
		b2.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					startActivity(
						new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts(
								"package",
								getApplicationContext().getPackageName(),
								null
                            )
						)
					);
				}
			});

		TextView t4 = new TextView(this);

		t4.setText(
			"Then go back to the accessibility settings and enable them for our app."
		);
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			t4.setText(
				"Затем снова перейдите в настройки спецвозможностей и включите их для нашего приложения."
			);}
		root.addView(t4, lp);

		Button b3 = new Button(this);
		b3.setText("Go to the accessibility settings ");
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			b3.setText("Перейти в настройки спецвозможностей");
		}
		root.addView(b3, lp);
		b3.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					startActivity(
						new Intent(
                            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
						)
					);
				}
			});


		TextView t5 = new TextView(this);

		t5.setText(
			"Didn't help? Reboot the phone.\nDidn't help? Use:\n\nadb shell appops set duress.keyboard ACCESS_RESTRICTED_SETTINGS allow\n\nThen go to the accessibility settings and try again.\nDidn't help? Reinstall app."
		);
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			t5.setText(
					"Не помогло? Перезагрузите телефон.\nНе помогло? Используйте:\n\nadb shell appops set duress.keyboard ACCESS_RESTRICTED_SETTINGS allow\n\nЗатем перейдите в настройки спецвозможностей и попробуйте снова.\nНе помогло? Переустановите приложение."
			);}
		t5.setTextIsSelectable(true);
		root.addView(t5, lp);
		
		String ef = "Accessibility required";
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {

			ef = "Требуются спецвозможности";
		}
		accessibilityDialog =
            new android.app.AlertDialog.Builder(this)

			.setTitle(ef)
			.setView(root)
			.setCancelable(false)
			.create();

		accessibilityDialog.show();		
		
		android.view.Window window = accessibilityDialog.getWindow();
		if (window != null) {
		android.view.WindowManager.LayoutParams lp2 = window.getAttributes();
    
        lp2.gravity = android.view.Gravity.CENTER;
        
        lp2.x = 0; 
        lp2.y = 0;
    
        window.setAttributes(lp2);
	   }

	}

    @Override
    protected void onResume() {
        super.onResume();



		boolean accessibilityEnabled = false;


		try {
			int enabled = android.provider.Settings.Secure.getInt(
                getContentResolver(),
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
			);

			if (enabled == 1) {
				String services = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
				);

				if (services != null) {
					String myService =
                        new android.content.ComponentName(
						this,
						MyAccessibilityService.class
					).flattenToString();

					accessibilityEnabled = services.contains(myService);
				}
			}
		} catch (Exception ignored) {}

		if (accessibilityEnabled) {
			if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
				accessibilityDialog.dismiss();
				accessibilityDialog = null;
			}

		}

		if (RESULT==true){
			getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);

			ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
			DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

			if (!dpm.isAdminActive(adminComponent)) {
			if (isPendingAdmin==0) {
			isPendingAdmin=1;	
			AllowAdmin(); 
			} else if (isPendingAdmin==1) {
			isPendingAdmin=2;
			ShowAdminErrorDialog(); }
			} else {
			showEmergencyModeAlertDialog();
			if (AdditionalOptionsWarning != null && AdditionalOptionsBack != null) showAdditionalOptionsWarning(AdditionalOptionsBack);					   
			}			

		}}

	private void showLanguageSelectionDialog() {
		Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final boolean isRussianDevice = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());


		final String[] languages = new String[] {
			"Русский (Russian)",
			"English (English)",
			"Español (Spanish)",
			isRussianDevice ? "Символы (!#?)": "Symbols (!#?)",
			isRussianDevice ? "Эмодзи (😡🤡👍)" : "Emoji (😡🤡👍)"
		};

		final String[] keys = {KEY_LANG_RU, KEY_LANG_EN, KEY_LANG_ES, KEY_LANG_SYM, KEY_LANG_EMOJI};
		final boolean[] checkedItems = new boolean[languages.length];


		for (int i = 0; i < keys.length; i++) {
			checkedItems[i] = prefs.getBoolean(keys[i], false);
		}



		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(isRussianDevice ? "Выберите языки клавиатуры" : "Select keyboard languages")
			.setMultiChoiceItems(languages, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					checkedItems[which] = isChecked;
				}
			})
			.setPositiveButton(isRussianDevice ? "Сохранить" : "Save", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences.Editor ed = prefs.edit();
					for (int i = 0; i < keys.length; i++) {
						ed.putBoolean(keys[i], checkedItems[i]);
					}
					ed.apply();



					Toast.makeText(MainActivity.this,
								   isRussianDevice ? "Языки клавиатуры сохранены" : "Keyboard languages saved",
								   Toast.LENGTH_SHORT).show();



				}
			})
			.setNegativeButton(isRussianDevice ? "Отмена" : "Cancel", null)
			.show();
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
					RESULT = false;
                    finish();
                }
            }
        };
		if (Build.VERSION.SDK_INT >= 34) {
       registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
       } else {
        registerReceiver(screenOffReceiver, filter);
         }

        String sysLang = Locale.getDefault().getLanguage();
        final boolean isRussianDevice = "ru".equalsIgnoreCase(sysLang);

		


        initializeDefaultLayoutsIfNeeded(isRussianDevice);

        initializeDefaultLanguageFlagsIfNeeded(isRussianDevice);


		commandInput = new EditText(this);
		commandInput.setHint(isRussianDevice ? "Задайте команду для сброса данных" : "Set wipe data command");

		final String allowedChars = getAllowedCharacters(this);


		InputFilter filter1 = new InputFilter.LengthFilter(50);


		InputFilter filterChars = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end, 
									   Spanned dest, int dstart, int dend) {


				for (int i = start; i < end; i++) {
					if (allowedChars.indexOf(source.charAt(i)) == -1) {
						return ""; // Отклонить символ
					}
				}
				return null; // Принять ввод
			}
		};


		commandInput.setFilters(new InputFilter[] { filter1, filterChars });


		final Button saveButton = new Button(this);
		saveButton.setText(isRussianDevice ? "Сохранить команду" : "Save command");

		saveButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					String cmd = commandInput.getText().toString().trim();
					if (cmd.length() < 4) {
                    Toast.makeText(MainActivity.this,
                    isRussianDevice ? "Нужно минимум 4 символа" : "Minimum 4 characters required",
                    Toast.LENGTH_SHORT).show();
                    return;
                    }
					if (!cmd.isEmpty()) {
						try {

							String salt = generateSalt();
							String commandHash = hashKeyWithSalt(salt, cmd);


							Context deviceProtectedContext = getApplicationContext().createDeviceProtectedStorageContext();
							SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

							prefs.edit()
								.putString(KEY_CUSTOM_COMMAND, commandHash)
								.putString("command_salt", salt)
								.apply();


							String inputHash="";

							try
							{
								MessageDigest digest = MessageDigest.getInstance("SHA-256");
								byte[] hashBytes = digest.digest((salt + cmd).getBytes(StandardCharsets.UTF_8));
								inputHash = Base64.getEncoder().encodeToString(hashBytes);

							}
							catch (Exception e)
							{}  


							if (commandHash.equals(inputHash)) {

								Toast.makeText(MainActivity.this, 
											   (isRussianDevice ? "Команда сохранена: " : "Command saved: ") + cmd, 
											   Toast.LENGTH_SHORT).show();
							} 

							if (!commandHash.equals(inputHash)) {

								Toast.makeText(MainActivity.this, 
											   (isRussianDevice ? "Ошибка! Хеши не совпадают!" : "Error! Hashes Not Match!"),
											   Toast.LENGTH_SHORT).show();		   				   
							}




							commandInput.setText("");
							commandInput.clearFocus();
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.hideSoftInputFromWindow(commandInput.getWindowToken(), 0);

						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
							Toast.makeText(MainActivity.this, "Ошибка хеширования", Toast.LENGTH_SHORT).show();
						}
					}
				}
			});


		Context dpContextForIme = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsIme = dpContextForIme.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch fakeHomeSwitch = new Switch(MainActivity.this);
		fakeHomeSwitch.setText(
			isRussianDevice
			? "Вместо сброса данных при вводе кода сброса запускать фейковый домашний экран. Даже если не включено, эта опция будет автоиспользована если сброс данных не сработает. Если вы включаете это, вы просто отключаете сброс данных."
			: "Instead of resetting data, when entering the wipe code, launch a fake home screen. Even if not enabled, this option will be autoused if at some moment wipe data doesn't work. If you enable it, you just disable wipe data."
		);


		final Switch screenOnWipeSwitch = new Switch(this);
		screenOnWipeSwitch.setText(
			isRussianDevice
			? "При каждом включении экрана запускать окно с кнопками ✅, ❌. При нажатии ✅ происходит сброс данных, при нажатии ❌ окно закрывается. Требует Спецвозможности."
			: "Every time the screen turns on, launch a window with buttons ✅, ❌. Pressing ✅ wipes data, pressing ❌ closes the window. Requires Accessibility."
		);


		Context dpContextScreen = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsScreen = dpContextScreen.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		screenOnWipeSwitch.setChecked(prefsScreen.getBoolean(KEY_SCREEN_ON_WIPE_PROMPT, false));

		screenOnWipeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        prefsScreen.edit().putBoolean(KEY_SCREEN_ON_WIPE_PROMPT, isChecked).apply();
        
        Toast.makeText(MainActivity.this, 
            isRussianDevice ? (isChecked ? "Включено" : "Выключено") : (isChecked ? "Enabled" : "Disabled"), 
            Toast.LENGTH_SHORT).show();

        if (isChecked) {
            aetest();
            if (!accessibilityEnabled) {
                ais();
            }
        }
    }
});



		boolean savedFakeHomeState = prefsIme.getBoolean(KEY_FAKE_HOME, false);
		fakeHomeSwitch.setChecked(savedFakeHomeState);

		fakeHomeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsIme.edit().putBoolean(KEY_FAKE_HOME, isChecked).apply();
					Toast.makeText(
						MainActivity.this,
						isRussianDevice
						? (isChecked ? "Включено" : "Выключено")
						: (isChecked ? "Enabled" : "Disabled"),
						Toast.LENGTH_SHORT
					).show();
				}
			});


		final Switch ae = new Switch(this);

		ae.setText(
			isRussianDevice
			? "Запускать фейковое поле ввода пароля при каждом включении экрана и первоначальной загрузке системы, чтобы в случае чего вы могли ввести туда код сброса данных. Для запуска использует спецвозможности или разрешение на наложение поверх других окон. Включайте это как альтернативу клавиатуре, если она не работает у вас на экране блокировки (что бывает на некоторых китайских телефонах, например: Realme)."
			: "Launch a fake password input field upon every screen on and initial system boot, so that in case of something you can enter the data wipe code there. For launching uses Accessibility or overlay permission. Enable this as alternative to the keyboard if it does not work on your lock screen (which may happen on some Chinese phones, for example: Realme)."
		);



		aetest();
		Context dpContextAe = getApplicationContext().createDeviceProtectedStorageContext();
		boolean savedAeState = dpContextAe.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("key_fake_password_enabled", false);
		ae.setChecked(savedAeState);



		ae.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean("key_fake_password_enabled", isChecked)
            .apply();

        if (isChecked) {
            aetest();
            if (!accessibilityEnabled) {
                ais(); 
            }
        }
    }
});



		final Switch wipeOnImeSwitch = new Switch(this);

		wipeOnImeSwitch.setText(
			isRussianDevice
			? "Стирать данные при переключении на другую виртуальную клавиатуру. Может не работать в безопасном режиме, поэтому лучше также отключать другие клавиатуры или включить режим Мёртвой Руки."
			: "Wipe data when switching to another virtual keyboard. It may not work in safe mode, so it's best also to disable other keyboards or enable Dead Hand mode."
		);

		boolean savedImeWipeState = prefsIme.getBoolean(KEY_WIPE2, false);
		wipeOnImeSwitch.setChecked(savedImeWipeState);

		wipeOnImeSwitch.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						final boolean willEnable = !wipeOnImeSwitch.isChecked();

						if (willEnable) {
							String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
							if (defaultIme == null || !defaultIme.startsWith(getPackageName() + "/")) {
								
								final LinearLayout alertRoot = new LinearLayout(MainActivity.this);
								alertRoot.setOrientation(LinearLayout.VERTICAL);
								alertRoot.setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15));

								LinearLayout.LayoutParams lpAlert = new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT
								);
								lpAlert.bottomMargin = dpToPx(12);

								TextView messageTv = new TextView(MainActivity.this);
								messageTv.setText(isRussianDevice 
									? "Назначьте данную клавиатуру по умолчанию прежде чем включать эту опцию."
									: "Please assign this keyboard by default before enabling this option.");
								alertRoot.addView(messageTv, lpAlert);

								infoDialog = new AlertDialog.Builder(MainActivity.this)
									.setTitle(isRussianDevice ? "Опция недоступна" : "Option not available")
									.setView(alertRoot)
									.setCancelable(false)
									.setPositiveButton("OK", null)
									.create();

								infoDialog.show();

								Window window = infoDialog.getWindow();
								if (window != null) {
									WindowManager.LayoutParams lp2 = window.getAttributes();
									lp2.gravity = Gravity.CENTER;
									lp2.x = 0;
									lp2.y = 0;
									window.setAttributes(lp2);
								}
								
								return true;
							}

							wipeOnImeSwitch.setChecked(true);
							prefsIme.edit().putBoolean(KEY_WIPE2, true).apply();
						} else {
							wipeOnImeSwitch.setChecked(false);
							prefsIme.edit().putBoolean(KEY_WIPE2, false).apply();
						}

						Toast.makeText(
							MainActivity.this,
							isRussianDevice
							? (willEnable
							? "Стирание данных при переключении на другую виртуальную клавиатуру включено"
							: "Стирание данных при переключении на другую виртуальную клавиатуру выключено")
							: (willEnable
							? "Wipe data when switching to another virtual keyboard is enabled"
							: "Wipe data when switching to another virtual keyboard is disabled"),
							Toast.LENGTH_SHORT
						).show();
					}
					return true;
				}
			});


		Context dpContextForReboot = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsReboot = dpContextForReboot.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch rebootWipeSwitch = new Switch(this);
		rebootWipeSwitch.setText(
			isRussianDevice
			? "Стирать данные при перезагрузке"
			: "Wipe data on reboot"
		);

		boolean savedRebootWipeState = prefsReboot.getBoolean(KEY_WIPE_ON_REBOOT, false);
		rebootWipeSwitch.setChecked(savedRebootWipeState);

		rebootWipeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsReboot.edit().putBoolean(KEY_WIPE_ON_REBOOT, isChecked).apply();

					Toast.makeText(
						MainActivity.this,
						isRussianDevice
						? (isChecked ? "Сброс при перезагрузке включён" : "Сброс при перезагрузке выключен")
						: (isChecked ? "Wipe on reboot enabled" : "Wipe on reboot disabled"),
						Toast.LENGTH_SHORT
					).show();

				}
			});

		
		Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsDH = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		
		switchDH = new android.widget.Switch(this);
		switchDH.setText(isRussianDevice ? "Режим Мертвой руки" : "Dead Hand Mode");
		switchDH.setTextSize(16);
		switchDH.setChecked(prefsDH.getBoolean(KEY_DEAD_HAND_MODE, false));

		 switchDH.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
        if (deadHandDialog != null && deadHandDialog.isShowing()) {
            deadHandDialog.dismiss();
        }

        final boolean isChecked = switchDH.isChecked();
        final boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());
        float density = getResources().getDisplayMetrics().density;
        int p16 = (int) (16 * density + 0.5f);
        int p12 = (int) (12 * density + 0.5f);

        LinearLayout root = new LinearLayout(MainActivity.this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p16, p16, p16, p16);

        TextView messageText = new TextView(MainActivity.this);
        messageText.setTextSize(14);
        String titleE;
		if (!isChecked) {
			titleE = isRu ? "Отключить Режим Мертвой руки" : "Disable Dead Hand Mode";
            messageText.setText(isRu 
                ? "Вы уверены что хотите отключить режим мертвой руки? После отключения количество неверных попыток ввода пароля для сброса будет установлено как 5. Чтобы изменить это, перейдите в настройки Авто-Сброса." 
                : "Are you sure you want to disable Dead Hand Mode? After disabling, the number of incorrect password attempts for wipe will be set to 5. To change this, go to the Auto-Wipe settings.");
        } else {
			titleE = isRu ? "Включить Режим Мертвой руки" : "Enable Dead Hand Mode";            
            messageText.setText(isRu 
                ? "Хотите включить режим мертвой руки?\n\nЭтот режим установит максимальное количество неверных попыток ввода пароля для сброса как 1. Это количество будет сбрасываться до 5 после ввода пароля перед отправкой, если это не DuressPassword и не включен Экстренный Режим (а он может, до следующей разблокировки), a после нее сразу заново устанавливаться как 1.\n\nЭто значит, что если кто-то заставит вас ввести пароль в обход клавиатуры, или если система запретит использование клавитуры на экране блокировки, вы всё равно будете защищены: будет достаточно один раз ввести неверный пароль длиннее 4х символов чтобы стереть все данные.\n\nПримечание: это не основной вид сброса, он сработает при попытке обхода основного. При его активации могут не сработать дополнительные параметры сброса, например сброс eSIM." 
                : "Want to enable Dead Hand Mode?\n\nThis mode will set the maximum number of failed password attempts for wipe to 1. This number will be reset to 5 after entering the password before sending it if this is not DuressPassword and Emergency Mode is not enabled (but it can, until next unlock), and after sending it will immediately set it back to 1.\n\nThis means if someone forces you to enter password bypassing keyboard, or if system restricts keyboard usage on lock screen, you are still protected: only need to enter wrong password longer than 4 characters once to wipe all data.\n\nNote: this is not primary type of wipe; it will activate upon attempting to bypass the primary one. Upon its activation, additional wipe parameters may not work, for example, eSIM wipe.");
        }
        
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textLp.bottomMargin = p16;
        root.addView(messageText, textLp);

        LinearLayout buttonsLayout = new LinearLayout(MainActivity.this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.END);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Button btnCancel = new Button(MainActivity.this);
        btnCancel.setText(isRu ? "Отмена" : "Cancel");
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchDH.setChecked(!isChecked);
                if (deadHandDialog != null) deadHandDialog.dismiss();
            }
        });
        buttonsLayout.addView(btnCancel, btnLp);

        View spacer = new View(MainActivity.this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(p12, 1);
        buttonsLayout.addView(spacer, spacerLp);

        Button btnAction = new Button(MainActivity.this);
        btnAction.setText(isChecked ? (isRu ? "Включить" : "Enable") : (isRu ? "Выключить" : "Disable"));
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefsDH.edit().putBoolean(KEY_DEAD_HAND_MODE, isChecked).apply();
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName adminComponent = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
                if (dpm.isAdminActive(adminComponent)) {
                    dpm.setMaximumFailedPasswordsForWipe(adminComponent, isChecked ? 1 : 5);
                }
                if (deadHandDialog != null) deadHandDialog.dismiss();
            }
        });
        buttonsLayout.addView(btnAction, btnLp);

        root.addView(buttonsLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        deadHandDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(titleE)
                .setView(root)
                .setCancelable(false)
                .create();

        deadHandDialog.show();

        Window window = deadHandDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp2 = window.getAttributes();
            lp2.gravity = Gravity.CENTER;
            lp2.x = 0;
            lp2.y = 0;
            window.setAttributes(lp2);
        }
    } });

		final Button readInstructionsButton = new Button(this);
		readInstructionsButton.setText(isRussianDevice ? "Прочитать подробную инструкцию" : "Read detailed instructions");

		readInstructionsButton.setOnClickListener(new View.OnClickListener() {


				private static final String in_ru="Это приложение-клавиатура, которое стирает данные с телефона при вводе специального кода на экране блокировки. Пригодится на случай если вас кто-то будет принуждать ввести пароль (а это может случиться в любом месте и в любое время, даже в возле парка или тогового центра, и даже в лесу, причем в не зависимости от вашего возраста и пола). Настроить приложение надо заранее, до подобных ситуаций. Это удобная клавиатура и для обычного использования, так что она вам не будет мешать, поддерживает русский, английский, символы и смайлики. Долгое нажатие на \"      \" даёт переключение между языками, обычное — просто пробел, \"!#?\" и \"abc\" — переключение на символы и обратно на буквы, долгое нажатие на \"е\" даёт \"ё\", на \"ь\" даёт \"ъ\", долгое нажатие на \"⌫\" быстро стирает текст, обычное: стирает 1 букву. 🌐 — Ещё 1 вариант переключения языков. Если хотите чтобы под принуждением можно было ввести код сброса данных на экране блокировки, — то заранее настройте приложение так: дайте приложению права Администратора (даёт право сброса данных), задайте код сброса данных и сохраните его, перейдите в настройки клавиатур, включите нашу клавиатуру, установите её кавиатурой по умолчанию, если это доступно в настройках, иначе через выбор клавиатуры на экране блокировки, а затем чтобы вас не могли заставить переключиться на другие клавиатуры — в тех же настройках отключте другие клавиатуры, либо если это нельзя (например они системные), включите режим Мёртвой Руки сдесь в главном меню или отключите приложения этих клавитур через adb shell pm disable-user --user 0 имя.пакета. Если не находите имя пакета или даже сама программа скрыта в настройках, то используйте приложение Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) для поиска. Если вы не можете использовать ADB через отладку по USB (например у вас нет компьютера), то используйте отладку по WiFi и программы Shizuku и aShell (https://github.com/RikkaApps/Shizuku/releases и https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). Или если не хотите использовать aShell, можете отключить их прямо через Package Manager. Для этого откройте его после запуска службы Shizuku и дайте права. Затем найдите ненужное системное приложение и выберите отключение или удаление. После этого убедитесь, что вы не можете переключиться на другие клавиатуры на экране блокировки. Отключить нужно их всех.\n\nПримечание: Код сброса срабатывает только на экране блокировки при вводе чистого кода (если в строке только он) и нажатии стрелки Enter (⏎). \n\nВнимание! На некоторых китайских телефонах, например Realme, клавиатура может не отображаться поверх экрана блокировки, ведь там используется системная, поэтому код сброса может не работать, в таком случае используйте последнюю опцию в дополнительных параметрах.\n";

				private static final String in_en="This is a keyboard app that erases data from your phone when you enter a special code on the lock screen. It's useful if someone try force you to enter a password (this can happen anywhere and anytime, even near a park or shopping center, or even in the forest, regardless of your age and gender). You should set up the app in advance, before such situations occur. This is a keyboard not only for wipe, for general use too, it is convenient and therefore it won't get in your way. It supports English, Spanish, symbols, and emoji. Long-pressing \"   \" switches between languages, a regular press is just a space, \"!#?\" and \"abc\" switch to symbols and back to letters, long-pressing \"⌫\" quickly erases text, and a regular press erases one letter. 🌐 — Another option for switching languages. If you want in an emergency enter wipe code on the lock screen, — then configure the app in advance as follows: grant the app Administrator privileges (Administrator rights give the right to reset data), set a reset code and save it, go to the keyboard settings, enable our keyboard, set it as the default keyboard if this action available in the settings, otherwise, by selecting a keyboard on the lock screen. And then to prevent attackers' ability to force you to switch to other keyboards — in the same settings disable other keyboards. Or, if this is not possible (for example, they are system keyboards), Enable Dead Hand mode here in the main menu or disable the applications for these keyboards using adb shell pm disable-user --user 0 package.name. If you can't find the package name, or even if the program itself is hidden in the settings, use the Package Manager app (https://f-droid.org/en/packages/com.smartpack.packagemanager) to search.  If you can't use ADB via USB debugging (for example, you don't have a computer), then use WiFi debugging and the Shizuku and aShell programs (https://github.com/RikkaApps/Shizuku/releases and https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). Or if you don't want to use aShell, you can disable them directly from the Package Manager. To do this, open it after launching the Shizuku service and give permissions. Next, find the system app you don't need and choose to disable or uninstall. After that, make sure you cannot switch to other keyboards on the lock screen. You must disable them all.\n\nNote: The reset code work only on lockscreen by entering a clear code (if only this code in current line) and pressing the Enter arrow (⏎). \n\nAttention! On some Chinese phones, for example Realme, the keyboard may not be displayed over the lock screen, since a system one is used there, therefore the reset code may not work, in such a case use the last feature in additional options.\n";


			    @Override
				public void onClick(View v) {

					String instructions;

					if (isRussianDevice) {
						instructions = in_ru;
					} else {
						instructions = in_en;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

					ScrollView scroll = new ScrollView(MainActivity.this);
					int padding = (int) (16 * getResources().getDisplayMetrics().density);

					TextView tv = new TextView(MainActivity.this);
					tv.setText(instructions);
					tv.setTextColor(Color.BLACK);
					tv.setTextSize(16);
					tv.setPadding(padding, padding, padding, padding);
					tv.setTextIsSelectable(true); 


					String text = instructions;

					SpannableString ss = new SpannableString(text);


					Pattern pattern = Pattern.compile("(https?://[A-Za-z0-9/.:\\-_%?=&]+)");
					Matcher matcher = pattern.matcher(text);

					while (matcher.find()) {
						final String url = matcher.group();

						ss.setSpan(
							new ClickableSpan() {
								@Override
								public void onClick(View widget) {
									Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
									widget.getContext().startActivity(intent);
								}

								@Override
								public void updateDrawState(TextPaint ds) {
									super.updateDrawState(ds);
									ds.setColor(Color.BLUE);
									ds.setUnderlineText(true);
								}
							},
							matcher.start(),
							matcher.end(),
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
						);
					}

					tv.setText(ss);
					tv.setMovementMethod(LinkMovementMethod.getInstance());
					tv.setLinksClickable(true);
					tv.setTextColor(Color.BLACK);
					tv.setTextIsSelectable(true);
					scroll.addView(tv);

					builder.setTitle(isRussianDevice ? "Инструкция" : "Instructions");
					builder.setView(scroll);
					builder.setPositiveButton("OK", null);
					builder.show();
				}
			});


		final Button keyboardSettingsButton = new Button(this);
		keyboardSettingsButton.setText(isRussianDevice ? "Открыть настройки клавиатур чтобы включить эту и отключить остальные." : "Open keyboard settings to enable this and disable others."); //all others, - user can understand this.
		keyboardSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openKeyboardSettings();
				}
			});


		final Button chooseKeyboardButton = new Button(this);
		chooseKeyboardButton.setText(isRussianDevice ? "Выбрать эту клавиатуру если включена" : "Choose this keyboard if enabled");
		chooseKeyboardButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showInputMethodPicker();
					} else {
						Toast.makeText(MainActivity.this, isRussianDevice ? "Не удалось открыть выбор клавиатуры" : "Failed to open keyboard picker", Toast.LENGTH_SHORT).show();
					}
				}
			});

		Context dpContextForUsb = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsUsb = dpContextForUsb.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch usbBlockSwitch = new Switch(this);
		usbBlockSwitch.setText(
			isRussianDevice
			? "Стирать данные при обнаружении многих внешних (даже Bluetooth) input methods и USB-подключений или изменения состояния USB (любого изменения: connect/disconnect/и тд.), за исключением зарядки от обычного зарядного блока. Включайте это для защиты от атак через USB кабель."
			: "Wipe data on detection many external (even Bluetooth) input methods and USB-connections or USB state change (any change: connect/disconnect/other), except charging from ordinary charging brick. Enable this to protect against attacks via USB cable."
		);


		boolean savedUsbBlockState = prefsUsb.getBoolean(KEY_USB_BLOCK, false);
		usbBlockSwitch.setChecked(savedUsbBlockState);


		usbBlockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsUsb.edit().putBoolean(KEY_USB_BLOCK, isChecked).apply();

					Toast.makeText(
						MainActivity.this,
						isRussianDevice
                        ? (isChecked ? "USB-блокировка включена" : "USB-блокировка выключена")
                        : (isChecked ? "USB blocking enabled" : "USB blocking disabled"),
						Toast.LENGTH_SHORT
					).show();

				}
			});

		////////////////////////////////////////////
		Context dpContextWipeEsim = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsWipeEsim = dpContextWipeEsim.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch EsimWipeSwitch = new Switch(this);
		EsimWipeSwitch.setText(
			isRussianDevice
			? "СБРОС ESIM, ВНЕШНЕГО ХРАНИЛИЩА И FRP ПРИ СБРОСЕ ДАННЫХ"
			: "WIPE ESIM, EXTERNAL STORAGE & FRP WHEN WIPE DATA"
		);

		EsimWipeSwitch.setChecked(prefsWipeEsim.getBoolean(KEY_WIPE_ESIM, true));

		EsimWipeSwitch.setOnTouchListener(new android.view.View.OnTouchListener() {
        @Override
        public boolean onTouch(android.view.View v, android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            if (EsimWipeSwitch.isChecked()) {
                EsimWipeSwitch.setChecked(true);
                
                final android.widget.LinearLayout root = new android.widget.LinearLayout(MainActivity.this);
                root.setOrientation(android.widget.LinearLayout.VERTICAL);
                root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dpToPx(12);

                android.widget.TextView msg = new android.widget.TextView(MainActivity.this);
                msg.setText(isRussianDevice 
                    ? "Вы уверены что хотите отключить флаги сброса? Без них после сброса могут остаться следы ваших данных. Например FRP может содержать id аккаунтов, Esim может содержать виртуальный номер и все что привязано к нему, а внешнее хранилище это данные sd карты." 
                    : "Are you sure you want to disable wipe flags? Without them, traces of your data may remain after the wipe. For example, FRP may contain account IDs, Esim may contain virtual number and everything attached to it, and external storage is SD card data.");
                root.addView(msg, lp);

                android.widget.Button bDisable = new android.widget.Button(MainActivity.this);
                bDisable.setText(isRussianDevice ? "Отключить" : "Disable");
                bDisable.setOnClickListener(v1 -> {
                    EsimWipeSwitch.setChecked(false);
                    prefsWipeEsim.edit().putBoolean(KEY_WIPE_ESIM, false).apply();
                    android.widget.Toast.makeText(MainActivity.this, isRussianDevice ? "ВЫКЛЮЧЕНО" : "DISABLED", android.widget.Toast.LENGTH_SHORT).show();
                    if (confirmWipeFlagsDialog != null) confirmWipeFlagsDialog.dismiss();
                });
                root.addView(bDisable, lp);

                android.widget.Button bCancel = new android.widget.Button(MainActivity.this);
                bCancel.setText(isRussianDevice ? "Отмена" : "Cancel");
                bCancel.setOnClickListener(v1 -> {
                    EsimWipeSwitch.setChecked(true);
                    if (confirmWipeFlagsDialog != null) confirmWipeFlagsDialog.dismiss();
                });
                root.addView(bCancel, lp);

                confirmWipeFlagsDialog = new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(isRussianDevice ? "Подтверждение" : "Confirmation")
                    .setView(root)
                    .setCancelable(false)
                    .create();

                confirmWipeFlagsDialog.show();

                android.view.Window window = confirmWipeFlagsDialog.getWindow();
                if (window != null) {
                    android.view.WindowManager.LayoutParams lp2 = window.getAttributes();
                    lp2.gravity = android.view.Gravity.CENTER;
                    lp2.x = 0;
                    lp2.y = 0;
                    window.setAttributes(lp2);
                }
            } else {
                EsimWipeSwitch.setChecked(true);
                prefsWipeEsim.edit().putBoolean(KEY_WIPE_ESIM, true).apply();
                android.widget.Toast.makeText(MainActivity.this, isRussianDevice ? "ВКЛЮЧЁН СБРОС ESIM/ВНЕШНЕГО ХРАНИЛИЩА/FRP" : "ENABLED WIPE ESIM/EXTERNAL STORAGE/FRP", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }});



		///////////////////////////////////////////

		Context dpContextWipeScrOFF = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsWipeScrOFF = dpContextWipeScrOFF.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch ScrOFFWipeSwitch = new Switch(this);
		ScrOFFWipeSwitch.setText(
			isRussianDevice
			? "СБРОС ДАННЫХ ПРИ ВЫКЛЮЧЕНИИ ЭКРАНА."
			: "WIPE DATA ON SCREEN OFF."
		);

		ScrOFFWipeSwitch.setChecked(prefsWipeScrOFF.getBoolean(KEY_WIPE_SCROFF, false));


		ScrOFFWipeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsWipeScrOFF.edit().putBoolean(KEY_WIPE_SCROFF, isChecked).apply();

					Toast.makeText(
						MainActivity.this,
						isRussianDevice
                        ? (isChecked ? "ВКЛЮЧЕН СБРОС ПРИ ВЫКЛ ЭКРАНА" : "ВЫКЛЮЧЕНО")
                        : (isChecked ? "ENABLED WIPE DATA ON SCREEN OFF" : "DISABLED"),
						Toast.LENGTH_SHORT
					).show();

				}
			});




		/////////////////////////////////////////



        final Button selectLanguagesButton = new Button(this);
		selectLanguagesButton.setText(isRussianDevice ? "Выбрать языки сервиса клавиатуры" :
									  "Select keyboard service languages");
		selectLanguagesButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLanguageSelectionDialog();
				}
			});

		final Button AutoWipeSettingsButton = new Button(this);
		AutoWipeSettingsButton.setText(isRussianDevice ? "Настройки Авто-Сброса" :
									  "Auto-wipe Settings");
		AutoWipeSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
							Intent intent7a = new Intent(getApplicationContext(), AdditionalOptionsActivity.class);
							intent7a.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent7a);
					} catch (Throwable ignored) {}
				}
			});


		final Switch chargingBlockSwitch = new Switch(this);
		chargingBlockSwitch.setText(
			isRussianDevice
			? "Стирать данные при зарядке. Может защитить от USB атак, где атакующий притворяется зарядным устройством. Но отключайте эту опцию перед обычной зарядкой или просто отключайте телефон. Пока телефон отключён, приложение не активно."
			: "Wipe data on charging. May protect against USB attacks where the attacker tries to simulate a charger. But please disable this option before regular charging or just turn off the phone. While the phone is turned off, this app is not active."
		);


		boolean savedChargingBlockState = prefsUsb.getBoolean(KEY_BLOCK_CHARGING, false);
		chargingBlockSwitch.setChecked(savedChargingBlockState);

		chargingBlockSwitch.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						final boolean currentState = chargingBlockSwitch.isChecked();

						if (!currentState) {
							chargingWarningDialog = new AlertDialog.Builder(MainActivity.this)
								.setTitle(isRussianDevice ? "Подтверждение" : "Confirmation")
								.setMessage(isRussianDevice
											? "Вы уверены? Если вы прямо сейчас заряжаете телефон, то данные могут стереться прямо сейчас"
											: "Are you sure? If you are charging your phone right now, data may be wiped immediately")
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										chargingBlockSwitch.setChecked(true); 
										prefsUsb.edit().putBoolean(KEY_BLOCK_CHARGING, true).apply();
										Toast.makeText(MainActivity.this,
													   isRussianDevice ? "Блокировка зарядки включена" : "Charging blocking enabled",
													   Toast.LENGTH_SHORT
													   ).show();

									}
								})
								.setNegativeButton(isRussianDevice ? "Отмена" : "Cancel", null)
								.show();
						} else { 
							chargingBlockSwitch.setChecked(false); 
							prefsUsb.edit().putBoolean(KEY_BLOCK_CHARGING, false).apply();
							Toast.makeText(MainActivity.this,
										   isRussianDevice ? "Блокировка зарядки выключена" : "Charging blocking disabled",
										   Toast.LENGTH_SHORT
										   ).show();

						}
					}
					return true; 
				}
			});


		noNetworkWipeSwitch = new Switch(this);
		noNetworkWipeSwitch.setText(
			isRussianDevice
			? "Сброс если нет мобильной сети больше 3 минут и НЕ включён режим полёта. Это способ детектирования пакета Фарадея. Отключайте когда едите там где сеть может пропадать без причины. Запускает окно 'черный экран' каждые 30 секунд пока сеть отключена и при выключении экрана чтобы предотвратить сон устройства. Также блокирует экран при первом запуске для большей защиты. Требует разрешение 'Телефон' и Спецвозможности для запуска чёрного экрана и мониторинга состояния сети."
			: "Reset if there's no mobile network connection for more than 3 minutes and the phone isn't in airplane mode. This is a Faraday bug detection method. Disable this when traveling to places where network connection may drop out without reason. Starts 'black screen' window every 30 seconds while network is off and when the screen turns off to block device sleep. Also locks the screen on first launch for better protection. Requires 'Phone' permission and Accessibility to start black screen and monitor network state."
		);

		Context dpContextForNetwork = getApplicationContext().createDeviceProtectedStorageContext();
		prefsNetwork = dpContextForNetwork.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		boolean savedNoNetworkWipeState = prefsNetwork.getBoolean(KEY_WIPE_ON_NO_NETWORK, false);
		noNetworkWipeSwitch.setChecked(savedNoNetworkWipeState);

		noNetworkWipeSwitch.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {

					if (event.getAction() == MotionEvent.ACTION_UP) {

						final boolean willEnable = !noNetworkWipeSwitch.isChecked();

						if (willEnable) {

							if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
								!= PackageManager.PERMISSION_GRANTED) {


								requestPermissions(
									new String[]{ android.Manifest.permission.READ_PHONE_STATE },
									1
								);

							} else {

								noNetworkWipeSwitch.setChecked(true);
								prefsNetwork.edit().putBoolean(KEY_WIPE_ON_NO_NETWORK, true).apply();
								Toast.makeText(MainActivity.this,
											   isRussianDevice ? "Сброс по отсутствию сети включен"
											   : "Wipe on no network enabled",
											   Toast.LENGTH_SHORT).show();
								ais();

							}

						} else {

							noNetworkWipeSwitch.setChecked(false);
							prefsNetwork.edit().putBoolean(KEY_WIPE_ON_NO_NETWORK, false).apply();
							Toast.makeText(MainActivity.this,
										   isRussianDevice ? "Сброс по отсутствию сети выключен"
										   : "Wipe on no network disabled",
										   Toast.LENGTH_SHORT).show();

						}
					}

					return true; 
				}
			});


        final Button AdditionalOptions = new Button(this);
		AdditionalOptions.setText(isRussianDevice ? "Дополнительные Параметры" : "Addidtional Options");	
		AdditionalOptions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					main=false;
					layout.removeAllViews(); 
					DisplayMetrics dm = getResources().getDisplayMetrics();

					float textPx = (float) Math.sqrt(
						dm.widthPixels * dm.heightPixels
					) * 0.023f;

					if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
						textPx = (float) Math.sqrt(
							dm.widthPixels * dm.heightPixels
						) * 0.022f;
					}

					EsimWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					ScrOFFWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					usbBlockSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					chargingBlockSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					noNetworkWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					rebootWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					wipeOnImeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);					
					fakeHomeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					screenOnWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					ae.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					layout.addView(EsimWipeSwitch);
					layout.addView(ScrOFFWipeSwitch);
					layout.addView(usbBlockSwitch);
					layout.addView(chargingBlockSwitch);
					layout.addView(noNetworkWipeSwitch); 
					layout.addView(rebootWipeSwitch);
					layout.addView(wipeOnImeSwitch);					
					layout.addView(fakeHomeSwitch);
					layout.addView(screenOnWipeSwitch);
					layout.addView(ae);

					AdditionalOptionsBack = new Button(MainActivity.this);
					AdditionalOptionsBack.setText(isRussianDevice ? "Основное Меню" : "Main Menu");	
					AdditionalOptionsBack.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								main=true;
								layout.removeAllViews(); 
								layout.setOrientation(LinearLayout.VERTICAL);
								layout.addView(commandInput);
								layout.addView(saveButton);
								layout.addView(keyboardSettingsButton);
								layout.addView(chooseKeyboardButton);
								layout.addView(selectLanguagesButton);
								layout.addView(readInstructionsButton);
								layout.addView(AutoWipeSettingsButton);
								layout.addView(AdditionalOptions);
								layout.addView(switchDH);

							}
						});
					layout.addView(AdditionalOptionsBack);
					setContentView(layout);
					showAdditionalOptionsWarning(AdditionalOptionsBack);
				}
			});


		layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(commandInput);
        layout.addView(saveButton);
		layout.addView(keyboardSettingsButton);
		layout.addView(chooseKeyboardButton);
        layout.addView(selectLanguagesButton);
		layout.addView(readInstructionsButton);
		layout.addView(AutoWipeSettingsButton);		
		layout.addView(AdditionalOptions);
		layout.addView(switchDH);

		KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

		if (keyguardManager.isKeyguardSecure()) {
			Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
				null, null
			);
			if (intent != null) {
				startActivityForResult(intent, 1337);
			}
		} else { 
			RESULT=true;
			setContentView(layout);
		}
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1337) {
			if (resultCode == RESULT_OK) {			
				RESULT=true;
		
				setContentView(layout);
			} else {
				finish();
			}
		}
	}

    private void initializeDefaultLayoutsIfNeeded(boolean isRussianDevice) {
        Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        boolean changed = false;
        if (!prefs.contains(KEY_LAYOUT_RU)) {

            String[][] russianLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"й","ц","у","к","е","н","г","ш","щ","з","х"},
                {"ф","ы","в","а","п","р","о","л","д","ж","э"},
                {"⇪","я","ч","с","м","и","т","ь","б","ю","⌫"},
                {"!#?","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_RU, string2DArrayToJson(russianLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_EN)) {
            String[][] englishLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l"},
                {"⇪","z","x","c","v","b","n","m","⌫"},
                {"!#?","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_EN, string2DArrayToJson(englishLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_SYM)) {
            String[][] symbolLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
		        {"/","\\","`","+","*","@","#","$","^","&","'"},
                {"=","|","<",">","[","]","(",")","{","}","\""},
                {"😃","~","%","-","—","_",":",";","!","?","⌫"},
                {"abc","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_SYM, string2DArrayToJson(symbolLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_EMOJI)) {
            String[][] emojiLetters = {
                {"😀","😢","😡","🤡","💩","👍","😭","🤬","😵","☠️","😄"},
                {"😁","😔","😤","😜","🤢","😆","😟","😠","😝","🤮","👎"},
                {"😂","😞","😣","😛","😷","🤣","🥰","😖","🤨","🤒","🤧"},
                {"!#?","😊","😫","🧐","🥴","💔","☹️","😩","🐷","😵‍💫","⌫"},
			    {"abc","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_EMOJI, string2DArrayToJson(emojiLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_ES)) {

            String[][] spanishLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l","ñ"},
                {"⇪","z","x","c","v","b","n","m","⌫"},
                {"!#?","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_ES, string2DArrayToJson(spanishLetters));
            changed = true;
        }
        if (changed) ed.apply();
    }

    private void initializeDefaultLanguageFlagsIfNeeded(boolean isRussianDevice) {
        Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        boolean changed = false;
        if (!prefs.contains(KEY_LANG_RU) && !prefs.contains(KEY_LANG_EN) && !prefs.contains(KEY_LANG_ES)
			&& !prefs.contains(KEY_LANG_SYM) && !prefs.contains(KEY_LANG_EMOJI)) {
            if (isRussianDevice) {
                ed.putBoolean(KEY_LANG_RU, true);
                ed.putBoolean(KEY_LANG_EN, true);
                ed.putBoolean(KEY_LANG_ES, false);
                ed.putBoolean(KEY_LANG_SYM, true);
                ed.putBoolean(KEY_LANG_EMOJI, true);
            } else {
                ed.putBoolean(KEY_LANG_RU, false);
                ed.putBoolean(KEY_LANG_EN, true);
                ed.putBoolean(KEY_LANG_ES, true);
                ed.putBoolean(KEY_LANG_SYM, true);
                ed.putBoolean(KEY_LANG_EMOJI, true);
            }
            changed = true;
        }
        if (changed) ed.apply();
    }

    private String string2DArrayToJson(String[][] arr) {
        JSONArray outer = new JSONArray();
        for (int i = 0; i < arr.length; i++) {
            JSONArray inner = new JSONArray();
            for (int j = 0; j < arr[i].length; j++) {
                inner.put(arr[i][j]);
            }
            outer.put(inner);
        }
        return outer.toString();
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == 1) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

				noNetworkWipeSwitch.setChecked(true);
				prefsNetwork.edit().putBoolean(KEY_WIPE_ON_NO_NETWORK, true).apply();
				ais();
			} else {

				noNetworkWipeSwitch.setChecked(false);
			}
		}
	}

    public static String getCustomCommand(Context context) {
        Context deviceProtectedContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_CUSTOM_COMMAND, "");
    }
}
