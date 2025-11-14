package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.Locale;
import android.content.DialogInterface;
import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private static final String KEY_CUSTOM_COMMAND = "custom_wipe_command";

   
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

    @Override
    protected void onResume() {
        super.onResume();

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
				explanation = "Дайте разрешение Администратора. Необходимо для работы функции стирания данных. Стирает данные только когда вы зададите и введете свой код или 'wipe' используя клавитуру этого приложения и нажмёте стрелку Enter (⏎). Подробнее далее.";
			} else {
				explanation = "Grant Administrator permission. This is required for the data wipe feature to work. Data will only be wiped when you set and enter your code or 'wipe' using the app's keyboard and press the Enter arrow (⏎). More details in the description.";
			}
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
			startActivity(intent);
        }
    }

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

        
        String sysLang = Locale.getDefault().getLanguage();
        final boolean isRussianDevice = "ru".equalsIgnoreCase(sysLang);

        
        initializeDefaultLayoutsIfNeeded(isRussianDevice);
        
        initializeDefaultLanguageFlagsIfNeeded(isRussianDevice);

        final EditText commandInput = new EditText(this);
        commandInput.setHint(isRussianDevice ? "Задайте команду для сброса данных" : "Set wipe data command");

        final Button saveButton = new Button(this);
        saveButton.setText(isRussianDevice ? "Сохранить команду" : "Save command");

        saveButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					String cmd = commandInput.getText().toString().trim();
					if (!cmd.isEmpty()) {
						Context deviceProtectedContext = getApplicationContext().createDeviceProtectedStorageContext();
						SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
						prefs.edit().putString(KEY_CUSTOM_COMMAND, cmd).apply();
						Toast.makeText(MainActivity.this, (isRussianDevice ? "Команда сохранена: " : "Command saved: ") + cmd, Toast.LENGTH_SHORT).show();
						commandInput.setText("");
						commandInput.clearFocus();
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(commandInput.getWindowToken(), 0);
					}
				}
			});
			
		Button keyboardSettingsButton = new Button(this);
		keyboardSettingsButton.setText(isRussianDevice ? "Открыть настройки клавиатур чтобы включить нашу." : "Open keyboard settings to enable our.");
		keyboardSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
				}
			});
			

		Button chooseKeyboardButton = new Button(this);
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

        
        Button selectLanguagesButton = new Button(this);
		selectLanguagesButton.setText(isRussianDevice ? "Выбрать языки сервиса клавиатуры" :
									  "Select keyboard service languages");
		selectLanguagesButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLanguageSelectionDialog();
				}
			});
		
		TextView instructionText = new TextView(this);
		if (isRussianDevice) {
			instructionText.setText("Подробная инструкция (можно листать как статью):\nЭто приложение-клавиатура, которое стирает данные с телефона при вводе специального кода. Пригодится на случай если вас кто-то будет принуждать ввести пароль (а это может случиться в любом месте и в любое время, даже в возле парка или тогового центра, и даже в лесу, причем в не зависимости от вашего возраста и пола, а если вы находитесь в северной стране — опасность ещё выше). Настроить приложение надо заранее, до подобных ситуаций. Это удобная клавиатура и для обычного использования, так что она вам не будет мешать, поддерживает русский, английский, символы и смайлики. Долгое нажатие на \"      \" даёт переключение между языками, обычное — просто пробел, \"!#?\" и \"abc\" — переключение на символы и обратно на буквы, долгое нажатие на \"е\" даёт \"ё\", на \"ь\" даёт \"ъ\", долгое нажатие на \"⌫\" быстро стирает текст, обычное: стирает 1 букву. 🌐 — Ещё 1 вариант переключения языков. Если хотите чтобы под принуждением можно было ввести код сброса данных, в том числе на экране блокировки, то заранее настройте приложение так: дайте приложению права Администратора (даёт право сброса данных), задайте код сброса данных и сохраните его, перейдите в настройки клавиатур, включите нашу клавиатуру, установите её кавиатурой по умолчанию, если это доступно в настройках, иначе через выбор клавиатуры на экране блокировки, а затем в тех же настройках отключте другие клавиатуры, либо если это нельзя (например они системные), отключите приложения этих клавитур через adb shell pm disable-user --user 0 имя.пакета.нужной.программы. Если не находите имя пакета или даже сама программа скрыта в настройках, то используйте приложение Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) для поиска. Если вы не можете использовать ADB через отладку по USB (например у вас нет компьютера), то используйте отладку по WiFi и программы Shizuku и aShell (https://github.com/RikkaApps/Shizuku/releases и https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). Последнее нужно чтобы вас не заставили переключиться на другие клавиатуры (с того же экрана блокировки) из-за возможного знания злоумышленника о нашей клавитуре, которую он попытается заставить вас обойти при наличии других клавиатур, поэтому нужно их отключить. Код сброса срабатывает только при вводе чистого кода (если в строке только он) и нажатии стрелки Enter (⏎). Помимо вашего кода, работает код \"wipe\" на случай если вы забудите свой. Важно понимать: защита данных заключается не в том чтобы случано не потерять или не стиреть их, а в том чтобы никто посторонний не получил к ним доступ, ведь это гораздо опаснее. И для подобной защиты мы делаем всё. Именно поэтому 2 кода: 'wipe' и ваш собственный. Конечно про 'wipe' может знать и злоумышленник, но априори если ваш телефон попал в чужие руки, то защиты уже нет, потому что взломать его легко при физическом доступе, поэтому если он сотрёт данные — будет даже лучше, чем если он получит к ним доступ. К тому же вы можете забыть свой код в эстренной сиуации, а 'wipe' запомнить легко. Но тогда почему мы не оставили только 'wipe', а дали вам возможность задать ещё и свой код? Потому что если злоумышленник хочет получить ваши данные и заставляет вас ввести пароль, при этом зная о коде 'wipe', то тогда он не даст вам ввести код 'wipe', а ваш код будет отличаться и вы сможее ввести его, так как о нём никто не будет знать. Тоесть у вас есть 2 кода одновременно на выбор: 'wipe' и ваш код. Когда я делал данную клавитуру, я брал пример с другого похожего приложения от другого разработчика (я про приложение Duress с именем пакета me.lucky.duress (https://f-droid.org/en/packages/me.lucky.duress)), но оно использовало спецвозможности для отслеживания ввода команд сброса, а это не так надёжно, как данная клавиатура, ведь Android иногда автоматически отключает подобные спецвозможности через несколько дней после активации из-за их 'подозрительности', соответственно это плохо, ведь код сброса может не сработать в экстренной ситуации, а вот данная клавиаура сработает гарантированно, потому что это клавиатура и она напрямую (без спецвозможностей) реагирует на код.\n\n\n");
			} else {
				instructionText.setText("Detailed instructions (you can scroll through them like an article):\nThis is a keyboard app that erases data from your phone when you enter a special code. It's useful if someone try force you to enter a password (and this can happen anywhere and anytime, even near a park or shopping center, or even in the forest, regardless of your age and gender, and if you live in a northern country, the risk is even higher). You should set up the app in advance, before such situations occur. This is a keyboard not only for wipe, for general use too, it is convenient and therefore it won't get in your way. It supports English, Spanish, symbols, and emoji. Long-pressing \"   \" switches between languages, a regular press is just a space, \"!#?\" and \"abc\" switch to symbols and back to letters, long-pressing \"⌫\" quickly erases text, and a regular press erases one letter. 🌐 — Another option for switching languages. If you want in an emergency enter wipe code, including on the lock screen, configure the app in advance as follows: grant the app Administrator privileges (Administrator rights give the right to reset data), set a reset code and save it, go to the keyboard settings, enable our keyboard, set it as the default keyboard if this action available in the settings, otherwise, by selecting a keyboard on the lock screen. Then, in the same settings, disable other keyboards. Or, if this is not possible (for example, they are system keyboards), disable the applications for these keyboards using adb shell pm disable-user --user 0 package.name.of.needed.program. If you can't find the package name, or even if the program itself is hidden in the settings, use the Package Manager app (https://f-droid.org/en/packages/com.smartpack.packagemanager) to search.  If you can't use ADB via USB debugging (for example, you don't have a computer), then use WiFi debugging and the Shizuku and aShell programs (https://github.com/RikkaApps/Shizuku/releases and https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). The latter is necessary to prevent you from being forced to switch to other keyboards (from the same lock screen) because the attacker might know about our keyboard, and he will try to force you to bypass this if other keyboards are present, so you should disable them. The reset code is only triggered by entering a clear code (if only this code in current line) and pressing the Enter arrow (⏎). In addition to your code, a code named \"wipe\" is also available in case you forget yours. It's important to understand: data protection is not about this is not protection from accidentally losing or erasing data, it about preventing unauthorized access to data, which is much more dangerous. And we do everything to prevent unauthorized access to data. That's why there are two codes: 'wipe' and your own code. Of course, an attacker might know about 'wipe', but if your phone falls into the wrong hands, there's no protection, because it's easy to hack with physical access, so if they erase the data, it's even better than if they gain access. Besides, you can forget your code in an emergency, but 'wipe' is easy to remember. So why didn't we just leave 'wipe'? Because if an attacker wants to get your data and forces you to enter your password, knowing the 'wipe' code, they won't let you enter the 'wipe' code, but your code will be different, and you'll be able to enter it because no one will know it except you. In other words, you have two codes to choose from: 'wipe' and your own code.  When I made this keyboard, I took another similar app from another developer as an example (I'm talking about the Duress app with the package name me.lucky.duress (https://f-droid.org/en/packages/me.lucky.duress)), but it used accessibility features to track the reset command input, and this is not as reliable as this keyboard, because Android sometimes automatically disables such accessibility features a few days after activation due to their 'suspiciousness'. Accordingly, this is bad, because the reset code may not work in an emergency, but this keyboard will work guaranteed, because it is a keyboard and it directly (without accessibility features) responds to the code.\n\n\n");
		}

		instructionText.setTextColor(0xFF000000);
		instructionText.setTextSize(5*getResources().getDisplayMetrics().density);

		int paddingDp = (int) (16 * getResources().getDisplayMetrics().density);
		instructionText.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);

		instructionText.setTextIsSelectable(true);
		instructionText.setFocusable(true);

		instructionText.setLayoutParams(new LinearLayout.LayoutParams(
											LinearLayout.LayoutParams.MATCH_PARENT,
											LinearLayout.LayoutParams.WRAP_CONTENT
										));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(commandInput);
		
        layout.addView(saveButton);
		layout.addView(keyboardSettingsButton);
		layout.addView(chooseKeyboardButton);
        layout.addView(selectLanguagesButton);
		layout.addView(instructionText);
        setContentView(layout);
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

    
    public static String getCustomCommand(Context context) {
        Context deviceProtectedContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_CUSTOM_COMMAND, "");
    }
}
