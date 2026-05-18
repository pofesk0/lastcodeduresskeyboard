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
	private EditText commandInput; 
    private static final String KEY_LANG_RU = "lang_ru";
    private static final String KEY_LANG_EN = "lang_en";
    private static final String KEY_LANG_SYM = "lang_sym";
    private static final String KEY_LANG_EMOJI = "lang_emoji";
    private static final String KEY_LANG_ES = "lang_es";
	private static int e= 0;


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
			}
		} catch (Exception ignored) {}

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
			"Don't help? Reinstall app.\nDon't help? Reboot the phone.\nDon't help? Use:\n\nadb shell appops set duress.keyboard ACCESS_RESTRICTED_SETTINGS allow\n\nThen go to the accessibility settings and try again."
		);
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			t5.setText(
					"Не помогло? Переустановите приложение.\nНе помогло? Перезагрузите телефон.\nНе помогло? Используйте:\n\nadb shell appops set duress.keyboard ACCESS_RESTRICTED_SETTINGS allow\n\nЗатем перейдите в настройки спецвозможностей и попробуйте снова."
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

			return;
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
				Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
				intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
				String explanation;
				if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
					explanation = "Дайте разрешение Администратора. Необходимо для работы функции стирания данных. Стирает данные когда вы введете код сброса на экране блокировки используя клавитуру этого приложения и нажмёте стрелку Enter (⏎). Также опционально вы можете включить сброс данных при других событиях. Также опционально может блокировать экран.";
				} else {
					explanation = "Grant Administrator permission. This is required for the data wipe feature to work. Data will be wiped when you enter the reset code on the lock screen using the app's keyboard and press the Enter arrow (⏎). You can also optionally enable data reset on other events. Also optionally can lock the screen.";
				}
				intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
				startActivity(intent);
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
		builder.setTitle(isRussianDevice ? "Выберите языки сервиса клавиатуры" : "Select keyboard service languages")
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
								   isRussianDevice ? "Языки сервиса клавиатуры сохранены" : "Keyboard service languages saved",
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
			? "При каждом включении экрана запускать окно с кнопками ✅, ❌. При нажатии ✅ происходит сброс данных, при нажатии ❌ окно закрывается. Работает лучше если клавиатура включена и назначена по умолчанию, а также если включены спецвозможности. Потому что это дает право на запуск Activity из фона."
			: "Every time the screen is turned on, launch a window with buttons ✅, ❌. Pressing ✅ wipes data, pressing ❌ closes the window. Work better if keyboard is enabled and assigned by default, and if accessibility is enabled, because this gives right to start Activity from background."
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
			? "Запускать фейковое поле ввода пароля при каждом включении экрана / перезагрузке в BFU, чтобы в случае чего вы могли ввести туда код сброса данных. Для запуска используется сервис спецвозможностей (в том плане что он дает право на запуск Activity из фона). Включайте это как альтернативу клавиатуре, если она не работает у вас на экране блокировки (что бывает на некоторых китайских телефонах, например: Realme)."
			: "Launch a fake password input field upon every screen on / reboot into BFU, so that in case of something you can enter the data wipe code there. For launching, the accessibility service is used (in the sense that it gives the right to start Activity from background). Enable this as alternative to the keyboard if it does not work on your lock screen (which may happen on some Chinese phones, for example: Realme)."
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
			? "Стирать данные при переключении на другую виртуальную клавиатуру. Может не работать в безопасном режиме, поэтому лучше просто отключать другие клавиатуры."
			: "Wipe data when switching to another virtual keyboard. It may not work in safe mode, so it's best to just disable other keyboards."
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

								AlertDialog infoDialog = new AlertDialog.Builder(MainActivity.this)
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

		final Button readInstructionsButton = new Button(this);
		readInstructionsButton.setText(isRussianDevice ? "Прочитать подробную инструкцию" : "Read detailed instructions");

		readInstructionsButton.setOnClickListener(new View.OnClickListener() {


				private static final String in_ru="Это приложение-клавиатура, которое стирает данные с телефона при вводе специального кода. Пригодится на случай если вас кто-то будет принуждать ввести пароль (а это может случиться в любом месте и в любое время, даже в возле парка или тогового центра, и даже в лесу, причем в не зависимости от вашего возраста и пола). Настроить приложение надо заранее, до подобных ситуаций. Это удобная клавиатура и для обычного использования, так что она вам не будет мешать, поддерживает русский, английский, символы и смайлики. Долгое нажатие на \"      \" даёт переключение между языками, обычное — просто пробел, \"!#?\" и \"abc\" — переключение на символы и обратно на буквы, долгое нажатие на \"е\" даёт \"ё\", на \"ь\" даёт \"ъ\", долгое нажатие на \"⌫\" быстро стирает текст, обычное: стирает 1 букву. 🌐 — Ещё 1 вариант переключения языков. Если хотите чтобы под принуждением можно было ввести код сброса данных (вводить нужно только на экране блокировки, ведь только там он и сработает), — то заранее настройте приложение так: дайте приложению права Администратора (даёт право сброса данных), задайте код сброса данных и сохраните его, перейдите в настройки клавиатур, включите нашу клавиатуру, установите её кавиатурой по умолчанию, если это доступно в настройках, иначе через выбор клавиатуры на экране блокировки, а затем чтобы вас не могли заставить переключиться на другие клавиатуры — в тех же настройках отключте другие клавиатуры, либо если это нельзя (например они системные), отключите приложения этих клавитур через adb shell pm disable-user --user 0 имя.пакета.нужной.программы. Если не находите имя пакета или даже сама программа скрыта в настройках, то используйте приложение Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) для поиска. Если вы не можете использовать ADB через отладку по USB (например у вас нет компьютера), то используйте отладку по WiFi и программы Shizuku и aShell (https://github.com/RikkaApps/Shizuku/releases и https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). После этого убедитесь, что вы не можете переключиться на другие клавиатуры на экране блокировки. Отключить нужно все. Код сброса срабатывает только на экране блокировки при вводе чистого кода (если в строке только он) и нажатии стрелки Enter (⏎). \n\nВнимание! На некоторых китайских телефонах, например Realme, клавиатура может не отображаться поверх экрана блокировки, ведь там используется системная, поэтому код сброса может не работать, в таком случае используйте последнюю опцию в дополнительных параметрах.\n";

				private static final String in_en="This is a keyboard app that erases data from your phone when you enter a special code. It's useful if someone try force you to enter a password (this can happen anywhere and anytime, even near a park or shopping center, or even in the forest, regardless of your age and gender). You should set up the app in advance, before such situations occur. This is a keyboard not only for wipe, for general use too, it is convenient and therefore it won't get in your way. It supports English, Spanish, symbols, and emoji. Long-pressing \"   \" switches between languages, a regular press is just a space, \"!#?\" and \"abc\" switch to symbols and back to letters, long-pressing \"⌫\" quickly erases text, and a regular press erases one letter. 🌐 — Another option for switching languages. If you want in an emergency enter wipe code (you must enter it only on the lock screen, since that is the only place where it will work), — then configure the app in advance as follows: grant the app Administrator privileges (Administrator rights give the right to reset data), set a reset code and save it, go to the keyboard settings, enable our keyboard, set it as the default keyboard if this action available in the settings, otherwise, by selecting a keyboard on the lock screen. And then to prevent attackers' ability to force you to switch to other keyboards — in the same settings disable other keyboards. Or, if this is not possible (for example, they are system keyboards), disable the applications for these keyboards using adb shell pm disable-user --user 0 package.name.of.needed.program. If you can't find the package name, or even if the program itself is hidden in the settings, use the Package Manager app (https://f-droid.org/en/packages/com.smartpack.packagemanager) to search.  If you can't use ADB via USB debugging (for example, you don't have a computer), then use WiFi debugging and the Shizuku and aShell programs (https://github.com/RikkaApps/Shizuku/releases and https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). After that, make sure you cannot switch to other keyboards on the lock screen. You must disable them all. The reset code work only on lockscreen by entering a clear code (if only this code in current line) and pressing the Enter arrow (⏎). \n\nAttention! On some Chinese phones, for example Realme, the keyboard may not be displayed over the lock screen, since a system one is used there, therefore the reset code may not work, in such a case use the last feature in additional options.\n";


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
		keyboardSettingsButton.setText(isRussianDevice ? "Открыть настройки клавиатур чтобы включить нашу." : "Open keyboard settings to enable our.");
		keyboardSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
				}
			});


		final Button chooseKeyboardButton = new Button(this);
		chooseKeyboardButton.setText(isRussianDevice ? "Выбрать нашу клавиатуру если включена" : "Choose our keyboard if enabled");
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
			? "Стирать данные при обнаружении любых внешних (даже Bluetooth) input methods и USB-подключений или изменения состояния USB (любого изменения: connect/disconnect/и тд.), за исключением зарядки от обычного зарядного блока. Включайте это для защиты от атак через USB кабель. Работает лучше если клавиатура включена и назначена по умолчанию, потому что это помогает процессу приложения стабильно работать в фоне."
			: "Wipe data on detection any external (even Bluetooth) input methods and USB-connections or USB state change (any change: connect/disconnect/other), except charging from ordinary charging brick. Enable this to protect against attacks via USB cable. Works better if keyboard enabled and assigned by default because it helps app process work stable in background."
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


		EsimWipeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsWipeEsim.edit().putBoolean(KEY_WIPE_ESIM, isChecked).apply();

					Toast.makeText(
						MainActivity.this,
						isRussianDevice
                        ? (isChecked ? "ВКЛЮЧЕН СБРОС ESIM/EXTERNAL" : "ВЫКЛЮЧЕНО")
                        : (isChecked ? "ENABLED WIPE ESIM/EXTERNAL" : "DISABLED"),
						Toast.LENGTH_SHORT
					).show();

				}
			});



		///////////////////////////////////////////

		Context dpContextWipeScrOFF = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsWipeScrOFF = dpContextWipeScrOFF.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch ScrOFFWipeSwitch = new Switch(this);
		ScrOFFWipeSwitch.setText(
			isRussianDevice
			? "СБРОС ДАННЫХ ПРИ ВЫКЛЮЧЕНИИ ЭКРАНА (работает лучше если клавиатура включена и назначена по умолчанию, потому что это помогает процессу приложения стабильно работать в фоне)"
			: "WIPE DATA ON SCREEN OFF (works better if keyboard enabled and assigned by default because it helps app process work stable in background)"
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
			? "Стирать данные при зарядке. Может защитить от USB атак, где атакующий притворяется зарядным устройством. Но отключайте эту опцию перед обычной зарядкой или просто отключайте телефон. Пока телефон отключён, приложение не активно. Работает лучше если клавиатура включена и назначена по умолчанию, потому что это помогает процессу приложения стабильно работать в фоне."
			: "Wipe data on charging. May protect against USB attacks where the attacker tries to simulate a charger. But please disable this option before regular charging or just turn off the phone. While the phone is turned off, this app is not active. Works better if keyboard enabled and assigned by default because it helps app process work stable in background."
		);


		boolean savedChargingBlockState = prefsUsb.getBoolean(KEY_BLOCK_CHARGING, false);
		chargingBlockSwitch.setChecked(savedChargingBlockState);


		chargingBlockSwitch.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						final boolean currentState = chargingBlockSwitch.isChecked();

						if (!currentState) {
							new AlertDialog.Builder(MainActivity.this)
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
			? "Сброс если нет мобильной сети больше 3 минут и НЕ включён режим полёта. Это способ детектирования пакета Фарадея. Отключайте когда едите там где сеть может пропадать без причины. ! Запускает активити 'черный экран' каждые 30 секунд пока сеть отключена и при выключении экрана чтобы предотвратить сон устройства, потому что если сеть отключена, во время сна нельзя стереть данные. Также блокирует экран при первом запуске для большей защиты. Необходимо разрешение 'Телефон' (READ_PHONE_STATE) для отслеживания сети. Работает лучше если клавиатура включена и назначена по умолчанию а также включены спецвозможности, потому что это помогает процессу приложения лучше работать в фоне и дает право на запуск Activity из фона."
			: "Reset if there's no mobile network connection for more than 3 minutes and the phone isn't in airplane mode. This is a Faraday bug detection method. Disable this when traveling to places where network connection may drop out without reason. ! Starts 'black screen' activity every 30 seconds while network is off and when the screen turns off to block device sleep, because if network disabled, in sleep wipe data may not work. Also locks the screen on first launch for better protection. The 'Phone' (READ_PHONE_STATE) permission is required to monitor the network. Works better if keyboard enabled and assigned by default and acessibility is enabled because it helps app process work better in background and gives right to launch Activity from the background."
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
					) * 0.020f;

					if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
						textPx = (float) Math.sqrt(
							dm.widthPixels * dm.heightPixels
						) * 0.019f;
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

					final Button AdditionalOptionsBack = new Button(MainActivity.this);
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

							}
						});
					layout.addView(AdditionalOptionsBack);
					setContentView(layout);
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
