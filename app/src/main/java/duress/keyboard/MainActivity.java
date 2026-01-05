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
/*
 Приложение использует DPS, а не Android Keystore,
 потому что на нескоторых устройствах
 даже если setUserAuthenticationRequired(false)), 
 Android Keystore может быть недоступен
 в BFU, а данное приложение являсь клавиатурой
 должно работать в BFU.
 */

/*
 The app uses DPS instead of Android Keystore,
 because on some devices, 
 even if setUserAuthenticationRequired(false), 
 Android Keystore may not be available in BFU, 
 but this app, being a keyboard, should work in BFU.
 */

public class MainActivity extends Activity {


	private android.app.AlertDialog accessibilityDialog;
	private static boolean main=true;
	boolean accessibilityEnabled = false;
    private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private static final String KEY_CUSTOM_COMMAND = "custom_wipe_command";
	private BroadcastReceiver screenOffReceiver;
	private static final String KEY_WIPE_ON_REBOOT = "wipe_on_reboot";
	private static final String KEY_AUTORUN = "auto_run";
	private static final String KEY_WIPE2 = "wipe2";
	private static final String KEY_SCREEN_ON_WIPE_PROMPT = "screen_on_wipe_prompt";
	private SharedPreferences prefsNetwork;
	private static final String KEY_FAKE_HOME = "fake_home_enabled";
	
	private Switch noNetworkWipeSwitch;
	private static final String KEY_WIPE_ON_NO_NETWORK = "wipe_on_no_network";
	private static final String KEY_USB_BLOCK = "usb_block_enabled";
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

// ...


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

		// =======================
		// 4. ЕСЛИ НЕ ВКЛЮЧЕНЫ → ПОКАЗАТЬ ОДИН РАЗ
		// =======================
		if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
			return; // уже показано
		}


		// ---------- UI ----------
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
			"Go to accessibility settings and enable them for our app."
		);

		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {

			t2.setText(
				"Перейдите в настройки спецвозможностей и там включите их для нашего приложения."
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
			"Don't help? Reinstall app."
		);
		if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			t5.setText(
					"Не помогло? Переустановите приложение.\nНе помогло? Перезагрузите телефон.\nНе помогло? Используйте:\n\nadb shell appops set duress.keyboard ACCESS_RESTRICTED_SETTINGS allow\n\nadb shell settings put secure enabled_accessibility_services duress.keyboard/MyAccessibilityService"
			);}
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

		// =======================
		// 3. ЕСЛИ ВКЛЮЧЕНЫ → УБРАТЬ ОКНО
		// =======================


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
					explanation = "Дайте разрешение Администратора. Необходимо для работы функции стирания данных. Стирает данные только когда вы зададите и введете свой код на экране блокировки используя клавитуру этого приложения и нажмёте стрелку Enter (⏎). Также опционально вы можете включить сброс данных при пропадании сети, подключении USB (ПК, флешка, USB мышка и тд), или даже при зарядке и перезагрузке. Также опционально может блокировать экран, вы увидите в описании опции перед включением.";
				} else {
					explanation = "Grant Administrator permission. This is required for the data wipe feature to work. Data will only be wiped when you set and enter your code on the lock screen using the app's keyboard and press the Enter arrow (⏎). You can also optionally enable data reset when the network is lost, when a USB connection (PC, flash drive, USB mouse, etc.) is connected, or even when charging and reboot. Also optionally can lock the screen, you'll see this in the option description before enabling it.";
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
        registerReceiver(screenOffReceiver, filter);

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

// ...


		final Button saveButton = new Button(this);
		saveButton.setText(isRussianDevice ? "Сохранить команду" : "Save command");

		saveButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					String cmd = commandInput.getText().toString().trim();
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
			? "Вместо сброса данных при вводе кода сброса запускать фейковый домашний экран."
			: "Instead of resetting data, when entering the wipe code, launch a fake home screen."
		);


		final Switch screenOnWipeSwitch = new Switch(this);
		screenOnWipeSwitch.setText(
			isRussianDevice
			? "При каждом включении экрана запускать окно с кнопками ✅, ❌. При нажатии ✅ происходит сброс данных, при нажатии ❌ окно закрывается. Работает только если клавиатура включена и назначена по умолчанию."
			: "Every time the screen is turned on, launch a window with buttons ✅, ❌. Pressing ✅ wipes data, pressing ❌ closes the window. Works only if the keyboard is enabled and set as default"
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
			? "Запускать фейковое поле ввода пароля при каждом включении экрана, чтобы в случае чего вы могли ввести туда код сброса данных. Для запуска используется сервис спецвозможностей вместо клавиатуры. Включайте это как альтернативу клавиатуре, если она не работает у вас на экране блокировки (что бывает на некоторых китайских телефонах, например Realme)."
			: "Launch a fake password input field every time the screen turns on, so that in case of something you can enter a data reset code there. For launching, an accessibility service is used instead of the keyboard. Enable this as an alternative to the keyboard if it does not work on your lock screen (which happens on some Chinese phones, for example Realme)."
		);



		aetest();
		ae.setChecked(accessibilityEnabled);


		ae.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

					if (!accessibilityEnabled) {				
				
						aetest();
						ais(); 
						
					}


					if (accessibilityEnabled){
						aetest();

						Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
						startActivity(intent);
						finish();

					}


				}
			});


		final Switch wipeOnImeSwitch = new Switch(this);

		wipeOnImeSwitch.setText(
			isRussianDevice
			? "Стирать данные при переключении на другую виртуальную клавиатуру. Работает только если перед этим данная клавиатура была включена и назначена по умолчанию. Может не работать в безопасном режиме, поэтому лучше просто отключать другие клавиатуры."
			: "Wipe data when switching to another virtual keyboard. Work only if this keyboard was enabled before it and assigned by default. It may not work in safe mode, so it's best to just disable other keyboards."
		);

		boolean savedImeWipeState = prefsIme.getBoolean(KEY_WIPE2, false);
		wipeOnImeSwitch.setChecked(savedImeWipeState);

		wipeOnImeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsIme.edit().putBoolean(KEY_WIPE2, isChecked).apply();

					Toast.makeText(
						MainActivity.this,
						isRussianDevice
						? (isChecked
						? "Стирание данных при переключении на другую виртуальную клавиатуру включено"
						: "Стирание данных при переключении на другую виртуальную клавиатуру выключено")
						: (isChecked
						? "Wipe data when switching to another virtual keyboard is enabled"
						: "Wipe data when switching to another virtual keyboard is disabled"),
						Toast.LENGTH_SHORT
					).show();

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


		Context dpContextAUTORUN = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsAUTORUN = dpContextAUTORUN.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final Switch AutoRunSwitch = new Switch(this);
		AutoRunSwitch.setText(
			isRussianDevice
			? "Автозапуск экрана с полем ввода после перезагрузки (для запуска клавиатуры, чтобы сразу начать реагировать на тригеры). Рекомендуется включить эту опцию. Может однократно блокировать экран после перезагрузки чтобы избежать ошибок наложения на экран блокировки. Это не проблема — экран после перезагрузки и так заблокирован. И то что Activity Exported тоже не проблема — блокировка будет однократной при любом количестве запусков."
			: "AutoLaunch the input field screen after reboot (to launch the keyboard so it immediately begins responding to triggers). Recommended to enable this option. May lock the screen once after reboot to avoid overlay errors on the lock screen. This is not a problem, as the screen is already locked after reboot. The fact that Activity is Exported is not an issue too — the lock will be one-time, regardless of the number of launches."
		);

		boolean savedAutoRunState = prefsAUTORUN.getBoolean(KEY_AUTORUN, false);
		AutoRunSwitch.setChecked(savedAutoRunState);

		AutoRunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					prefsAUTORUN.edit().putBoolean(KEY_AUTORUN, isChecked).apply();


					PackageManager pm = MainActivity.this.getPackageManager();
					ComponentName cn = new ComponentName(MainActivity.this, InputActivity.class);

					pm.setComponentEnabledSetting(
						cn,
						PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
						PackageManager.DONT_KILL_APP
					);

					Toast.makeText(
						MainActivity.this,
						isRussianDevice
						? (isChecked ? "Автозапуск включён" : "Автозапуск выключен")
						: (isChecked ? "AutoRun Enabled" : "AutoRun Disabled"),
						Toast.LENGTH_SHORT
					).show();

				}
			});	

		final Button readInstructionsButton = new Button(this);
		readInstructionsButton.setText(isRussianDevice ? "Прочитать подробную инструкцию" : "Read detailed instructions");

		readInstructionsButton.setOnClickListener(new View.OnClickListener() {


				private static final String in_ru="Подробная инструкция (можно листать как статью):\nЭто приложение-клавиатура, которое стирает данные с телефона при вводе специального кода. Пригодится на случай если вас кто-то будет принуждать ввести пароль (а это может случиться в любом месте и в любое время, даже в возле парка или тогового центра, и даже в лесу, причем в не зависимости от вашего возраста и пола, а если вы находитесь в северной стране — опасность ещё выше). Настроить приложение надо заранее, до подобных ситуаций. Это удобная клавиатура и для обычного использования, так что она вам не будет мешать, поддерживает русский, английский, символы и смайлики. Долгое нажатие на \"      \" даёт переключение между языками, обычное — просто пробел, \"!#?\" и \"abc\" — переключение на символы и обратно на буквы, долгое нажатие на \"е\" даёт \"ё\", на \"ь\" даёт \"ъ\", долгое нажатие на \"⌫\" быстро стирает текст, обычное: стирает 1 букву. 🌐 — Ещё 1 вариант переключения языков. Если хотите чтобы под принуждением можно было ввести код сброса данных, работает только на экране блокировки, то заранее настройте приложение так: дайте приложению права Администратора (даёт право сброса данных), задайте код сброса данных и сохраните его, перейдите в настройки клавиатур, включите нашу клавиатуру, установите её кавиатурой по умолчанию, если это доступно в настройках, иначе через выбор клавиатуры на экране блокировки, а затем в тех же настройках отключте другие клавиатуры, либо если это нельзя (например они системные), отключите приложения этих клавитур через adb shell pm disable-user --user 0 имя.пакета.нужной.программы. Если не находите имя пакета или даже сама программа скрыта в настройках, то используйте приложение Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) для поиска. Если вы не можете использовать ADB через отладку по USB (например у вас нет компьютера), то используйте отладку по WiFi и программы Shizuku и aShell (https://github.com/RikkaApps/Shizuku/releases и https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). Последнее нужно чтобы вас не заставили переключиться на другие клавиатуры (с того же экрана блокировки) из-за возможного знания злоумышленника о нашей клавитуре, которую он попытается заставить вас обойти при наличии других клавиатур, поэтому нужно их отключить. Код сброса срабатывает только на экране блокировки при вводе чистого кода (если в строке только он) и нажатии стрелки Enter (⏎). Когда я делал данную клавитуру, я брал пример с другого похожего приложения от другого разработчика (я про приложение Duress с именем пакета me.lucky.duress (https://f-droid.org/en/packages/me.lucky.duress)), но оно использовало спецвозможности для отслеживания ввода команд сброса, а это не так надёжно, как данная клавиатура, ведь Android иногда автоматически отключает подобные спецвозможности через несколько дней после активации из-за их 'подозрительности', соответственно это плохо, ведь код сброса может не сработать в экстренной ситуации, а вот данная клавиаура сработает гарантированно, потому что это клавиатура и она напрямую (без спецвозможностей) реагирует на код.\n\nВнимание! На некоторых китайских телефонах, например Realme, клавиатура может не отображаться поверх экрана блокировки, ведь там используется системная, поэтому код сброса может не работать, в таком случае используйте функцию окна с кнопками ✅❌ для экстренного сброса, она находится в дополнительных опциях. Для неё также требуется чтобы клаваитура была включена и назначена по умолчнию, ведь даже если она не видна на экране блокировки, в целом это клавиатура по умолчанию, и она будет работать в фоне, что необходимо для запуска окна с ✅❌. Но это не будет работать после перезагрузки до первого использования клавиатуры, так как она отвечает за запуск окна, что в данном случае возможно только после разблокировки, ведь использовать несистемную клавиатуру на заблокированном экране нельзя на телефонах наподобие Realme. Проверьте работает ли у вас эта клавиатура поверх экрана блокировки! Это важно. Где с наибольшей вероятностью будет работать данная клавиатура? На телефонах Samsung и Google Pixel. Но на последнем данное приложение и не требуется — лучше установите GrapheneOS, там есть функция Duress Password, которая работает в не зависимости от клавиатуры, а также есть блокировка подключений с зарядному порту. Если у вас китайский телефон, то есть другие способы защиты данных. Обычно там можно скрыть приложения и открывать по коду (на Realme) или создать пустое второе пространство (Second Space, System Cloner на некоторых Xiaomi и Realme), затем переключаться на него когда вас заставляют показать содержимое телефона. Важно: после перезагрузки до первой разблокировки на второе пространство нельзя переключиться, можно только если основной пользователь был хоть раз разблокирован.\n\nТакже в дополнительных параметрах есть функция \"Запускать фейковое поле ввода пароля при каждом включении экрана, чтобы в случае чего вы могли ввести туда код сброса данных. Для запуска используется сервис спецвозможностей вместо клавиатуры. Включайте это как альтернативу клавиатуре, если она не работает у вас на экране блокировки (что бывает на некоторых китайских телефонах, например Realme).\". \nРаботает даже в BFU на телефонах Realme, так как не требует первоначального фокуса сервиса клавиатуры. Минус лишь в том, что спецвозможности в любой момент могут быть отозваны системой. Но это все равно стабильнее, чем \"Duress\", потому что не следит за другими полями паролей, а значит шанс отзыва ниже. К тому же, если вы используете наш сервис клавиатуры помимо спецвозможностей (даже если он не отображается поверх экрана блокировки, он несёт много других функций, которые можно включить в дополнительных параметрах и которые могут работать в фоне), в таком случае шанс отзыва спецвозможностей ещё ниже, ведь ситема видит, что приложение активно используется.\n\n\n";

				private static final String in_en="Detailed instructions (you can scroll through them like an article):\nThis is a keyboard app that erases data from your phone when you enter a special code. It's useful if someone try force you to enter a password (and this can happen anywhere and anytime, even near a park or shopping center, or even in the forest, regardless of your age and gender, and if you live in a northern country, the risk is even higher). You should set up the app in advance, before such situations occur. This is a keyboard not only for wipe, for general use too, it is convenient and therefore it won't get in your way. It supports English, Spanish, symbols, and emoji. Long-pressing \"   \" switches between languages, a regular press is just a space, \"!#?\" and \"abc\" switch to symbols and back to letters, long-pressing \"⌫\" quickly erases text, and a regular press erases one letter. 🌐 — Another option for switching languages. If you want in an emergency enter wipe code, work only on the lock screen, configure the app in advance as follows: grant the app Administrator privileges (Administrator rights give the right to reset data), set a reset code and save it, go to the keyboard settings, enable our keyboard, set it as the default keyboard if this action available in the settings, otherwise, by selecting a keyboard on the lock screen. Then, in the same settings, disable other keyboards. Or, if this is not possible (for example, they are system keyboards), disable the applications for these keyboards using adb shell pm disable-user --user 0 package.name.of.needed.program. If you can't find the package name, or even if the program itself is hidden in the settings, use the Package Manager app (https://f-droid.org/en/packages/com.smartpack.packagemanager) to search.  If you can't use ADB via USB debugging (for example, you don't have a computer), then use WiFi debugging and the Shizuku and aShell programs (https://github.com/RikkaApps/Shizuku/releases and https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). The latter is necessary to prevent you from being forced to switch to other keyboards (from the same lock screen) because the attacker might know about our keyboard, and he will try to force you to bypass this if other keyboards are present, so you should disable them. The reset code work only on lockscreen by entering a clear code (if only this code in current line) and pressing the Enter arrow (⏎). When I made this keyboard, I took another similar app from another developer as an example (I'm talking about the Duress app with the package name me.lucky.duress (https://f-droid.org/en/packages/me.lucky.duress)), but it used accessibility features to track the reset command input, and this is not as reliable as this keyboard, because Android sometimes automatically disables such accessibility features a few days after activation due to their 'suspiciousness'. Accordingly, this is bad, because the reset code may not work in an emergency, but this keyboard will work guaranteed, because it is a keyboard and it directly (without accessibility features) responds to the code.\n\nAttention! On some Chinese phones, for example Realme, the keyboard may not be displayed over the lock screen, since a system one is used there, therefore the reset code may not work, in such a case use the function of a window with buttons ✅❌ for emergency reset, it is located in additional options. For it it is also required that the keyboard is enabled and set as default, since even if it is not visible on the lock screen, overall this is the default keyboard, and it will work in the background, which is necessary for launching the window with ✅❌. But this will not work after a reboot until the first use of the keyboard, since it is responsible for launching the window, which in this case is possible only after unlocking, since using the keyboard on the locked screen is impossible on phones like Realme. Check whether this keyboard works for you over the lock screen! This is important. Where with the greatest probability will this keyboard work? On Samsung and Google Pixel phones. But on the latter this application is not required — it is better to install GrapheneOS, there is a Duress Password function, which works regardless of the keyboard, and there is also blocking of connections to the charging port. If you have a Chinese phone, there are other ways of data protection. Usually there you can hide applications and open them by code (on Realme) or create an empty second space (Second Space, System Cloner on some Xiaomi and Realme), then switch to it when you are forced to show the contents of the phone. Important: after a reboot until the first unlock it is not possible to switch to the second space, it is possible only if the main user was unlocked at least once.\n\nAlso in the Additional Options there is a function \"Launch a fake password input field every time the screen turns on, so that in case of something you can enter a data reset code there. For launching, an accessibility service is used instead of the keyboard. Enable this as an alternative to the keyboard if it does not work on your lock screen (which happens on some Chinese phones, for example Realme).\". \nWorks even in BFU on Realme phones, since it does not require initial focus of the keyboard service. The only minus is that accessibility at any moment can be revoked by the system. But this is still more stable than \"Duress\", because it does not watch other password fields, and that means the chance of revocation is lower. Also, if you use our keyboard service besides accessibility (even if it is not displayed over the lock screen, it carries many other functions, which can be enabled in the \"Additional Options\" and which can work in the background), in that case the chance of accessibility revocation is even lower, because the system sees that the application is actively used.\n\n\n";


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
			? "Стирать данные при обнаружении любых внешних (даже Bluetooth) input methods и USB-подключений, за исключением зарядки от обычного зарядного блока. Работает преимущественно если включена клавиатура и назначена по умолчанию"
			: "Wipe data on detection any external (even Bluetooth) input methods and USB-connections, except charging from ordinary charging brick. Work predominantly if keyboard enabled and assigned by default"
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



        final Button selectLanguagesButton = new Button(this);
		selectLanguagesButton.setText(isRussianDevice ? "Выбрать языки сервиса клавиатуры" :
									  "Select keyboard service languages");
		selectLanguagesButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLanguageSelectionDialog();
				}
			});


		final Switch chargingBlockSwitch = new Switch(this);
		chargingBlockSwitch.setText(
			isRussianDevice
			? "Стирать данные при зарядке. Работает преимущественно если включена клавиатура и назначена по умолчанию. Теоретически, может защитить от сложных USB-exploits. Но отключайте это перед обычной зарядкой или отключайте телефон чтобы остановить это приложение."
			: "Wipe data on charging. Work predominantly if keyboard enabled and assigned by default. Theoretically, it can protect against complex USB-exploits. But please disable this before regular charging or turn off the phone to temporarily stop this app."
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
			? "Сброс если нет мобильной сети больше 3 минут и выключен режим полёта. Работает только если клавиатура включена и назначена по умолчанию. Это способ детектирования пакета Фарадея. Отключайте когда едите там где сеть может пропадать без причины. ! Запускает активити 'черный экран' каждые 30 секунд пока сеть отключена и при выключении экрана чтобы предотвратить сон устроства, потому что если сеть отключена, во время сна нельзя стереть данные. Также блокирует экран при первом запуске для большей защиты. Необходимо разрешение 'Телефон' (READ_PHONE_STATE) для отслеживания сети."
			: "Reset if there's no mobile network connection for more than 3 minutes and the phone isn't in airplane mode. Works only if the keyboard is enabled and set as default. This is a Faraday bug detection method. Disable this when traveling to places where network connection may drop out without reason. ! Starts 'black screen' activity every 30 seconds while network off and when the screen turns for block device sleep, because if network disabled, in sleep wipe data not work. Also locks the screen on first launch for better protection. The 'Phone' (READ_PHONE_STATE) permission is required to monitor the network."
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
						) * 0.021f;
					}

					usbBlockSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					chargingBlockSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					noNetworkWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					rebootWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					wipeOnImeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					AutoRunSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					fakeHomeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					screenOnWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					ae.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					layout.addView(usbBlockSwitch);
					layout.addView(chargingBlockSwitch);
					layout.addView(noNetworkWipeSwitch); 
					layout.addView(rebootWipeSwitch);
					layout.addView(wipeOnImeSwitch);
					layout.addView(AutoRunSwitch);
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
			//No password on device. Pass. (Нет пароля на телефоне. Пропустим.)
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
