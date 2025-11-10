package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private static final String KEY_CUSTOM_COMMAND = "custom_wipe_command";

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
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Дайте разрешение Администратора. Необходимо для работы функции стирания данных. Стирает данные только когда вы зададите и введете свой код или 'wipe' используя клавитуру этого приложения и нажмёте стрелку Enter (⏎). Подробнее далее.");
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final EditText commandInput = new EditText(this);
        commandInput.setHint("Задайте команду для сброса данных");

        final Button saveButton = new Button(this);
        saveButton.setText("Сохранить команду");

        saveButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					String cmd = commandInput.getText().toString().trim();
					if (!cmd.isEmpty()) {
						// Device Protected Storage (чтобы было доступно в BFU)
						Context deviceProtectedContext = getApplicationContext().createDeviceProtectedStorageContext();
						SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
						prefs.edit().putString(KEY_CUSTOM_COMMAND, cmd).apply();
						Toast.makeText(MainActivity.this, "Команда сохранена: " + cmd, Toast.LENGTH_SHORT).show();
						commandInput.setText("");
						commandInput.clearFocus();
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(commandInput.getWindowToken(), 0);
					}
				}
			});
			
		Button keyboardSettingsButton = new Button(this);
		keyboardSettingsButton.setText("Открыть настройки клавиатур чтобы включить нашу.");
		keyboardSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
					finish();
				}
			});
			

		Button chooseKeyboardButton = new Button(this);
		chooseKeyboardButton.setText("Выбрать нашу клавиатуру если включена");
		chooseKeyboardButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showInputMethodPicker();
					} else {
						Toast.makeText(MainActivity.this, "Не удалось открыть выбор клавиатуры", Toast.LENGTH_SHORT).show();
					}
				}
			});

		
			
			
	
		TextView instructionText = new TextView(this);
		instructionText.setText("Подробная инструкция (можно листать как статью):\nЭто приложение-клавиатура, которое стирает данные с телефона при вводе специального кода. Пригодится на случай если вас кто-то будет принуждать ввести пароль (а это может случиться в любом месте и в любое время, даже в возле парка или тогового центра, и даже в лесу, причем в не зависимости от вашего возраста и пола, а если вы находитесь в северной стране — опасность ещё выше). Настроить приложение надо заранее, до подобных ситуаций. Это удобрая клавиатура и для обычного использования, так что она вам не будет мешать, поддерживает русский, английский, символы и смайлики. Долгое нажатие на \"      \" даёт переключение между языками, обычное — просто пробел, \"!#?\" и \"abc\" — переключение на символы и обратно на буквы, долгое нажатие на \"е\" даёт \"ё\", на \"ь\" даёт \"ъ\", долгое нажатие на \"⌫\" быстро стирает текст, обычное: стирает 1 букву. 🌐 — Ещё 1 вариант переключения языков. Если хотите чтобы под принуждением можно было ввести код сброса данных, в том числе на экране блокировки, то заранее настройте приложение так: дайте приложению права Администратора (даёт право сброса данных), задайте код сброса данных, перейдите в настройки клавиатур, включите нашу клавиатуру, установите её кавиатурой по умолчанию, если это доступно в настройках, иначе через выбор клавиатуры на экране блокировки, а затем в тех же настройках отключте другие клавиатуры, либо если это нельзя (например они системные), отключите приложения этих клавитур через adb shell pm disable-user --user 0 имя.пакета.нужной.программы. Если не находите имя пакета или даже сама программа скрыта в настройках, то используйте приложение Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager) для поиска. Если вы не можете использовать ADB через отладку по USB (например у вас нет компьютера), то используйте отладку по WiFi и программы Shizuku и aShell (https://github.com/RikkaApps/Shizuku/releases и https://f-droid.org/en/packages/in.sunilpaulmathew.ashell). Последнее нужно чтобы вас не заставили переключиться на другие клавиатуры (с того же экрана блокировки) из-за возможного знания злоумышленника о нашей клавитуре, которую он попытается заставить вас обойти при наличии других клавиатур, поэтому нужно их отключить. Код сброса срабатывает только при вводе чистого кода (если в строке только он) и нажатии стрелки Enter (⏎). Помимо вашего кода, работает код \"wipe\" на случай если вы забудите свой. Важно понимать: защита данных заключается не в том чтобы случано не потерять или не стиреть их, а в том чтобы никто посторонний не получил к ним доступ, ведь это гораздо опаснее. И для подобной защиты мы делаем всё. Именно поэтому 2 кода: 'wipe' и ваш собственный. Конечно про 'wipe' может знать и злоумышленник, но априори если ваш телефон попал в чужие руки, то защиты уже нет, потому что взломать его легко при физическом доступе, поэтому если он сотрёт данные — будет даже лучше, чем если он получит к ним доступ. К тому же вы можете забыть свой код в эстренной сиуации, а 'wipe' запомнить легко. Но тогда почему мы не оставили только 'wipe', а дали вам возможность задать ещё и свой код? Потому что если злоумышленник хочет получить ваши данные и заставляет вас ввести пароль, при этом зная о коде 'wipe', то тогда он не даст вам ввести код 'wipe', а ваш код будет отличаться и вы сможее ввести его, так как о нём никто не будет знать. Тоесть у вас есть 2 кода одновременно на выбор: 'wipe' и ваш код. Когда я делал данную клавитуру, я брал пример с другого похожего приложения от другого разработчика (я про приложение Duress с именем пакета me.lucky.duress (https://f-droid.org/en/packages/me.lucky.duress)), но оно использовало спецвозможности для отслеживания ввода команд сброса, а это не так надёжно, как данная клавиатура, ведь Android иногда автоматически отключает подобные спецвозможности через несколько дней после активации из-за их 'подозрительности', соответственно это плохо, ведь код сброса может не сработать в экстренной ситуации, а вот данная клавиаура сработает гарантированно, потому что это клавиатура и она напрямую (без спецвозможностей) реагирует на код.\n\n\n");

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
		layout.addView(instructionText);
        setContentView(layout);
    }

    public static String getCustomCommand(Context context) {
        Context deviceProtectedContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_CUSTOM_COMMAND, "");
    }
}