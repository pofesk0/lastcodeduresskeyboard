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
	private static final String KEY_AUTORUN = "auto_run";
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
			? "Запускать фейковое поле ввода пароля при каждом включении экрана / перезагрузке в BFU, чтобы в случае чего вы могли ввести туда код сброса данных. Для запуска используется сервис спецвозможностей вместо клавиатуры. Включайте это как альтернативу клавиатуре, если она не работает у вас на экране блокировки (что бывает на некоторых китайских телефонах, например Realme)."
			: "Launch a fake password input field upon every screen on / reboot into BFU, so that in case of something you can enter a data reset code there. For launching, an accessibility service is used instead of the keyboard. Enable this as an alternative to the keyboard if it does not work on your lock screen (which happens on some Chinese phones, for example Realme)."
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
			? "Автозапуск экрана с полем ввода после перезагрузки (для запуска клавиатуры, чтобы сразу начать реагировать на тригеры). Может не работать на новых версиях Android. Отключайте, если не работает."
			: "AutoLaunch the input field screen after reboot (to launch the keyboard so it immediately begins responding to triggers). May not work on new Android versions. Disable if not work."
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


	private static final String in_ru =
    "Что это:\n" +
    "Это приложение-клавиатура, которое стирает данные с телефона при вводе специального кода в поле ввода пароля на экране блокировки. Вводите код и нажимаете стрелку на клавиатуре, которая обозначает ввод, и данные будут стёрты.\n\n" +

    "Для чего это:\n" +
    "Для повседневного использования и готовности к ситуациям, когда вас могут заставить разблокировать телефон. Когда это может быть произойти? Почти всегда. Вы идёте в торговый центр. А там произошла крупная авария. И все кто там были попали под проверку. Вы гуляли в лесу, а там недавно кто-то пропал. И вы попали под проверку. Если вы покажете телефон, то проблемы у вас будут по другим причинам. Ваши интересы, переписки, всё это используют против вас. Именно в этом и есть настоящая цель подобных мероприятий. Если вы сотрёте данные, то это нельзя будет использовать против вас. Этой информации недостаточно, чтобы что-то с ней сделать. Факт того, что данные стерты не говорит ровно ни о чём. Вы просто не хотите чтобы кто-то лез в вашу личную жизнь. Это ваше конституционное право.\n" +
    "Почему надо именно стереть данные? Потому что это быстро и необратимо. Если просто не дадите пароль, на вас могут начать давить. У вас могут отобрать телефон и взломать с помощью специального оборудования. Не позволяйте никому добиваться своих целей давлением на вас!\n\n" +

    "Функции клавиатуры:\n" +
    "Поддерживает русский, английский, испанский символы и смайлики.\n" +
    "Переключение языков: долгое нажатие на пробел и быстрое нажатие на 🌐.\n" +
    "Переключение на символы: !#?\n" +
    "Переключение на буквы: abc.\n" +
    "Стирание буквы: ⌫ быстрое нажатие\n" +
    "Стирание нескольких букв: ⌫ долгое нажатие\n" +
    "Особенности русской раскладки: Долгое нажатие на \"е\" даёт \"ё\", на \"ь\" даёт \"ъ\".\n\n" +

    "Баги и решения:\n" +
    "Проверьте, что у вас отображается эта клавиатура на экране блокировки. Если нет, проверьте, что включили её и назначили по умолчанию. Если не помогло, то значит на вашей (вероятно, китайской) прошивке нельзя сторонним клавиатурам там работать. Тогда используйте спецвозможности как альтернативу, хотя это не так надёжно, ведь преимущество нашего приложения именно в том, что это клавиатура. Тот же Duress (без приставки Keyboard) тоже использует их, но система, видя слежку за полями ввода, часто отключает его сервис спецвозможностей. Но у нашего приложения другой подход. Мы запускаем своё поле ввода, имитирующее поле ввода пароля на такой случай. Загляните в дополнительные параметры и найдите там последнюю функцию.\n\n" +

    "Советы:\n" +
    "Убедитесь, что нет других клавиатур, на которые можно переключиться на экране блокировки. Если есть, отключите их, чтобы вас потом не заставили переключиться на них. Как это сделать?\n" +
    "Вариант 1 (если сработает): Зайдите в настройки, где включали нашу клавиатуру, и отключите остальные.\n" +
    "Вариант 2 (если не сработало, например, у вас не чистый Android, а Samsung OneUI): попробуйте найти системную клавиатуру в настройках и отключить, а если не получается, то переходим к варианту 3.\n" +
    "Вариант 3: Установите Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) и используйте его, чтобы найти имя пакета системной клавиатуры, которую нужно отключить. Установите Shizuku (https://github.com/RikkaApps/Shizuku/releases/latest) и aShell (https://f-droid.org/en/packages/in.sunilpaulmathew.ashell), затем включите режим разработчика (Настройки → О телефоне → Номер сборки → нажать 10 раз). В них включите отладку по WiFi. Затем зайдите в Shizuku и запустите её. Затем в aShell введите команду (adb shell pm disable-user --user 0 имя.пакета.лишней.клавиатуры). Либо, если не хотите использовать этот способ, то используйте отладку по USB в тех же настройках разработчика и компьютер с SDK Platform Tools для использования ADB-команды.\n\n" +
    "Совет 2: Избегайте FRP. Не храните аккаунты Google на телефоне в основном профиле. Их ID остаются после сброса, так как сброс без ввода пароля не удаляет FRP. Данные уже удалены, но ID аккаунта Google остаётся. Зная ваш ID, можно узнать вашу почту. Зная вашу почту, можно узнать ваш никнейм и так далее. А если к аккаунту Google вы решили привязать бекап, то это ещё опаснее. В общем, не делайте бекапы или не привязывайте их к аккаунту Google, а чтобы избежать самого FRP, создайте рабочий профиль через Shelter или лучше ProtectedWorkProfile (они есть на F-droid) и перенесите туда аккаунты Google — ведь оттуда аккаунты не сохраняются в FRP. Перед сбросом у вас не должно быть Google-аккаунтов в основном профиле.";

	private static final String in_en =
    "What it is:\n" +
    "This is a keyboard app that wipes your phone's data when you enter a special code in the password input field on the lock screen. You enter the code and press the arrow on the keyboard that means \"Enter,\" and the data will be wiped.\n\n" +

    "What it's for:\n" +
    "For everyday use and preparedness for situations where you might be forced to unlock your phone. When could this happen? Almost always. You go to the shopping centre. And there was a major accident. Everyone who was there came under scrutiny. You were walking in the forest, and someone recently went missing there. And you came under scrutiny. If you show your phone, you'll have problems for other reasons. Your interests, conversations — all of that can be used against you. This is the real purpose of such measures. If you wipe the data, it can't be used against you. There's not enough information here for them to do anything with it. The fact that the data is wiped means absolutely nothing. You just don't want anyone snooping through your personal life. This is your constitutional right.\n" +
    "Why wipe the data? Because it's fast and irreversible. If you just refuse to give the password, they might pressure you. They could take your phone and hack it using special equipment. Don't let anyone force their way into your personal life through pressure!\n\n" +

    "Keyboard features:\n" +
    "Supports Russian, English, Spanish characters, and emojis.\n" +
    "Language switching: Long-press the spacebar and quick-tap the 🌐.\n" +
    "Switch to symbols: !#?\n" +
    "Switch to letters: abc.\n" +
    "Delete a letter: ⌫ quick tap\n" +
    "Delete multiple letters: ⌫ long press\n" +
    "Russian layout quirks: Long-press \"е\" to get \"ё\", on \"ь\" to get \"ъ\".\n\n" +

    "Bugs and fixes:\n" +
    "Check that this keyboard is displayed on your lock screen. If not, check that you enabled it and set it as default. If that didn't help, then your (likely Chinese) firmware doesn't allow third-party keyboards to work there. Then use accessibility features as an alternative, although this is not as reliable because the advantage of our app is precisely that it's a keyboard. The same Duress (without the Keyboard suffix) also uses them, but the system, seeing monitoring of input fields, often disables its accessibility service. But our app takes a different approach. We launch our own input field that mimics a password field in such cases. Check the additional options and find the last feature there.\n\n" +

    "Tips:\n" +
    "Make sure there are no other keyboards you can switch to on the lock screen. If there are, disable them so you can't be forced to switch to them. How?\n" +
    "Option 1 (if it works): Go to the settings where you enabled our keyboard and disable the others.\n" +
    "Option 2 (if it didn't work, for example, if you don't have a clean Android but Samsung OneUI): Try to find the system keyboard in the settings and disable it, and if that doesn't work, move to Option 3.\n" +
    "Option 3: Install Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) and use it to find the package name of the system keyboard you need to disable. Install Shizuku (https://github.com/RikkaApps/Shizuku/releases/latest) and aShell (https://f-droid.org/en/packages/in.sunilpaulmathew.ashell), then enable Developer Mode (Settings → About Phone → Build Number → tap 10 times). In them, enable WiFi debugging. Then go to Shizuku and launch it. Then in aShell enter the command (adb shell pm disable-user --user 0 package.name.of.unwanted.keyboard). Or if you don't want to use this method, then use USB debugging in the same Developer settings and a computer with SDK Platform Tools to use this ADB command.\n\n" +
    "Tip 2: Avoid FRP. Don't store Google accounts on your phone in the main profile. Their IDs remain after a reset because a reset without entering the password does not remove FRP. Data is already wiped, but the Google account ID remains. Knowing your ID, they can find your email. Knowing your email, they can find your username, and so on. And if you decided to link backups to the Google account, that's even more dangerous. In general, don't make backups or don't link them to the Google account, and to avoid FRP itself, create a work profile through Shelter or better ProtectedWorkProfile (they are on F-droid) and move Google accounts there—from there the accounts are not retained in FRP. Before resetting, you must not have any Google accounts in the main profile.";
			
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
			? "Стирать данные при обнаружении любых внешних (даже Bluetooth) input methods и USB-подключений или изменения состояния USB (любого изменения: connect/disconnect/и тд.), за исключением зарядки от обычного зарядного блока. Работает преимущественно если включена клавиатура и назначена по умолчанию"
			: "Wipe data on detection any external (even Bluetooth) input methods and USB-connections or USB state change (any change: connect/disconnect/other), except charging from ordinary charging brick. Work predominantly if keyboard enabled and assigned by default"
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
			? "СБРОС ESIM И ВНЕШНЕГО ХРАНИЛИЩА ПРИ СБРОСЕ ДАННЫХ"
			: "WIPE ESIM & EXTERNAL STORAGE WHEN WIPE DATA"
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
			? "СБРОС ДАННЫХ ПРИ ВЫКЛЮЧЕНИИ ЭКРАНА (работает только если клавиатура включена и назначена по умолчанию)"
			: "WIPE DATA ON SCREEN OFF (work only if keyboard enabled and assigned by default)"
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
			? "Сброс если нет мобильной сети больше 3 минут и выключен режим полёта. Работает стабильно только если клавиатура включена и назначена по умолчанию. Это способ детектирования пакета Фарадея. Отключайте когда едите там где сеть может пропадать без причины. ! Запускает активити 'черный экран' каждые 30 секунд пока сеть отключена и при выключении экрана чтобы предотвратить сон устроства, потому что если сеть отключена, во время сна нельзя стереть данные. Также блокирует экран при первом запуске для большей защиты. Необходимо разрешение 'Телефон' (READ_PHONE_STATE) для отслеживания сети."
			: "Reset if there's no mobile network connection for more than 3 minutes and the phone isn't in airplane mode. Works stable only if the keyboard is enabled and set as default. This is a Faraday bug detection method. Disable this when traveling to places where network connection may drop out without reason. ! Starts 'black screen' activity every 30 seconds while network off and when the screen turns for block device sleep, because if network disabled, in sleep wipe data not work. Also locks the screen on first launch for better protection. The 'Phone' (READ_PHONE_STATE) permission is required to monitor the network."
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

					EsimWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					ScrOFFWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					usbBlockSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					chargingBlockSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					noNetworkWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					rebootWipeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					wipeOnImeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
					AutoRunSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
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
