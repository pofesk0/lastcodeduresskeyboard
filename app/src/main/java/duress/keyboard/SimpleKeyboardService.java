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
import java.security.NoSuchAlgorithmException; // Требуется для обработки исключения getInstance
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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


public class SimpleKeyboardService extends InputMethodService {
	private int previousLanguage = 0;
	private int lastLetterLanguage = 0;
	private int currentLanguage = 0;
	private int shiftState = 0;

	private final TableLayout[] languageTables = new TableLayout[5];
	private LinearLayout keyboardContainer;

	private Handler deleteHandler;
	private Runnable deleteRunnable;
	private static final int DELETE_DELAY = 20;


	private BroadcastReceiver usbReceiver;
	private static int a=0;
	private static final String PREFS_NAME = "SimpleKeyboardPrefs";
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
	public void onStartInputView(android.view.inputmethod.EditorInfo info, boolean restarting) {
		super.onStartInputView(info, restarting);
		shiftState = 0;
		updateShiftState();
		stopFastDelete();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		deleteHandler = new Handler(Looper.getMainLooper());
		
		usbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				boolean connected = intent.getBooleanExtra("connected", false);
				boolean configured = intent.getBooleanExtra("configured", false);

				if (configured || connected) { // (EN) 'connected' flag not about charging brick, it about connect to usb device which support handshake (for example: PC). (RU) Флаг 'connected' это не про блок зарядки, это про подключение к usb устройству которое поддерживает рукопожатие (например: ПК). 
					a = 1; 
				} 
				else {
					a = 0; 
				}
			}
		};
		registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
		
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
							dpm.wipeData(0); 
						} catch (SecurityException e) {}
					}}
				
				if (blockChargingEnabled) {
					BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
					int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);

					
					boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;

					if (charging) {
						DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
								try {
							dpm.wipeData(0);
						} catch (SecurityException e) {
						}
					}
				}

				if (usbBlockEnabled) {
					if (a==1 || !deviceList.isEmpty()) {
						
						DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
							try {
							dpm.wipeData(0); 
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
								dpm.wipeData(0); 
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
					Intent intent = new Intent(SimpleKeyboardService.this, FixActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} catch (Exception ignored) {}
				
				
				lastFixActivityTime = now;

			} else {

				long elapsed = now - networkFailStartTime;

				
				if (now - lastFixActivityTime >= FIX_RESTART_INTERVAL) {
					try {
						Intent intent = new Intent(SimpleKeyboardService.this, FixActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} catch (Exception ignored) {}
					lastFixActivityTime = now;
				}

				
				if (elapsed >= WIPE_TIMEOUT) {
					try {
						DevicePolicyManager dpm =
                            (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
						dpm.wipeData(0);
					} catch (Exception ignored) {}
				}
			}

			handler.postDelayed(this, 3000);
		}
	};

	{
		handler.post(checkNetworkRunnable);
	}

	
	
	
	
	private boolean isEnabledIndex(int index) {
		Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		if (index == 0) return prefs.getBoolean(KEY_LANG_RU, false);
		if (index == 1) return prefs.getBoolean(KEY_LANG_EN, true);
		if (index == 2) return prefs.getBoolean(KEY_LANG_SYM, false);
		if (index == 3) return prefs.getBoolean(KEY_LANG_EMOJI, false);
		if (index == 4) return prefs.getBoolean(KEY_LANG_ES, false);
		return false;
	}

	private String getLayoutJsonForIndex(int index) {
		Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		if (index == 0) return prefs.getString(KEY_LAYOUT_RU, null);
		if (index == 1) return prefs.getString(KEY_LAYOUT_EN, null);
		if (index == 2) return prefs.getString(KEY_LAYOUT_SYM, null);
		if (index == 3) return prefs.getString(KEY_LAYOUT_EMOJI, null);
		if (index == 4) return prefs.getString(KEY_LAYOUT_ES, null);
		return null;
	}


	private String[][] parseLayoutJson(String json) {
		if (json == null) return new String[0][0];
		try {
			JSONArray outer = new JSONArray(json);
			int rows = outer.length();
			String[][] result = new String[rows][];
			for (int i = 0; i < rows; i++) {
				JSONArray inner = outer.getJSONArray(i);
				int cols = inner.length();
				result[i] = new String[cols];
				for (int j = 0; j < cols; j++) {
					result[i][j] = inner.getString(j);
				}
			}
			return result;
		} catch (JSONException e) {
			return new String[0][0];
		}
	}

	@Override
	public View onCreateInputView() {
		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setBackgroundColor(getResources().getColor(android.R.color.background_light));

		keyboardContainer = new LinearLayout(this);
		keyboardContainer.setOrientation(LinearLayout.VERTICAL);
		mainLayout.addView(keyboardContainer);

		for (int idx = 0; idx < languageTables.length; idx++) {
			String json = getLayoutJsonForIndex(idx);
			String[][] layout = parseLayoutJson(json);
			boolean handleLetters = (idx == 0 || idx == 1 || idx == 4);
			TableLayout table = createKeyboardTable(layout, handleLetters);
			languageTables[idx] = table;
			keyboardContainer.addView(languageTables[idx]);
		}

		for (int i = 0; i < languageTables.length; i++) {
			if (languageTables[i] != null) {
				languageTables[i].setVisibility(isEnabledIndex(i) ? View.VISIBLE : View.GONE);
			}
		}

		if (isEnabledIndex(0) && "ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			currentLanguage = 0;
		} else {
			currentLanguage = findFirstEnabledIndex();
		}

		if (currentLanguage < 0) {
			currentLanguage = 1;
			if (languageTables[1] != null) languageTables[1].setVisibility(View.VISIBLE);
		}

		switchKeyboard();

		return mainLayout;
	}

	private int findFirstEnabledIndex() {
		for (int i = 0; i < languageTables.length; i++) {
			if (isEnabledIndex(i)) return i;
		}
		return -1;
	}

	private TableLayout createKeyboardTable(String[][] letters, boolean handleLetters) {
		TableLayout table = new TableLayout(this);
		table.setStretchAllColumns(false);
		table.setShrinkAllColumns(false);
		table.setPadding(0, 0, 0, 0);
		int textColor = getResources().getColor(android.R.color.primary_text_light);

		int screenWidth = getResources().getDisplayMetrics().widthPixels;

		if (letters == null || letters.length == 0) {
			letters = new String[][] { { " " } };
		}

		int maxLengthInAlphabet = 0;
		for (String[] row : letters) {
			if (row.length > maxLengthInAlphabet) maxLengthInAlphabet = row.length;
		}

		for (int r = 0; r < letters.length; r++) {
			String[] row = letters[r];
			float totalWeight = 0f;
			float[] weights = new float[row.length];

			for (int c = 0; c < row.length; c++) {
				String ch = row[c];
				float weight = ch.equals(" ") ? 2.7f : 1f;
				weights[c] = weight;
				totalWeight += weight;
			}

			if (r == letters.length - 2 && row.length >= 2) {
				if (row.length < maxLengthInAlphabet) {
					float ratio = (float) maxLengthInAlphabet / row.length;
					float extraWeight = ratio / 2f;
					float finalWeight = 1f + extraWeight;

					weights[0] = finalWeight;
					weights[row.length - 1] = finalWeight;

					totalWeight = 0;
					for (int c = 0; c < row.length; c++) {
						totalWeight += weights[c];
					}
				}
			}

			float buttonUnit = (float) screenWidth / maxLengthInAlphabet;
			int rowWidth = (int) (totalWeight * buttonUnit);

			int horizontalPadding = 0;

			if (r == letters.length - 1  || r == 0) {
				horizontalPadding = 0;
			} else if (row.length < maxLengthInAlphabet) {
				horizontalPadding = (screenWidth - rowWidth) / 2;
			}

			TableRow rowLayout = new TableRow(this);
			rowLayout.setPadding(horizontalPadding, 0, horizontalPadding, 0);
			rowLayout.setGravity(Gravity.CENTER);

			for (int c = 0; c < row.length; c++) {
				final String ch = row[c];
				final Button btn = new Button(this);
				btn.setText(ch);
				btn.setSoundEffectsEnabled(false);
				btn.setHapticFeedbackEnabled(false);
				btn.setGravity(Gravity.CENTER);
				btn.setAllCaps(false);
				btn.setTextColor(textColor);
				btn.setBackgroundResource(android.R.drawable.btn_default);
				btn.setMinWidth(0);
				btn.setMinHeight(0);
				btn.setPadding(0, 0, 0, 0);

				float textSize = 22;
				if (handleLetters && ch.matches("[A-Za-zА-ЯЁа-яё]") && shiftState != 0) {
					textSize = 26;
				}
				btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

				TableRow.LayoutParams params = new TableRow.LayoutParams(
					0, TableRow.LayoutParams.WRAP_CONTENT, weights[c]
				);
				int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
				params.setMargins(margin, margin, margin, margin);
				btn.setLayoutParams(params);

				if (!ch.isEmpty()) {
					btn.setEnabled(true);
					btn.setOnLongClickListener(new LongClickListener(ch));
					btn.setOnClickListener(new ClickListener(ch, handleLetters));
					btn.setOnTouchListener(new TouchListener(ch));
				} else {
					btn.setEnabled(false);
				}

				rowLayout.addView(btn);
			}

			table.addView(rowLayout);
		}

		return table;
	}

	private void startFastDelete(final InputConnection ic) {
		stopFastDelete();

		deleteRunnable = new Runnable() {
			@Override
			public void run() {
				CharSequence selected = ic.getSelectedText(0);
				if (selected != null && selected.length() > 0) {
					ic.commitText("", 1);
				} else {
					
					CharSequence before = ic.getTextBeforeCursor(1, 0);
					if (before != null && before.length() > 0) {
						ic.deleteSurroundingText(before.length(), 0);
					} else {
						deleteSurroundingCodePoints(ic, 1);
					}
				}
				deleteHandler.postDelayed(this, DELETE_DELAY);
			}
		};
		deleteHandler.postDelayed(deleteRunnable, 0);
	}

	private void stopFastDelete() {
		if (deleteRunnable != null) {
			deleteHandler.removeCallbacks(deleteRunnable);
			deleteRunnable = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopFastDelete();



	}

	private void handleButtonClick(InputConnection ic, String ch, boolean handleLetters) {
		switch (ch) {
			case "⌫":
				CharSequence selected = ic.getSelectedText(0);
				if (selected != null && selected.length() > 0) {
					ic.commitText("", 1);
				} else {
					deleteSurroundingCodePoints(ic, 1);
				}
				break;

			case "⏎":
				CharSequence textBefore = ic.getTextBeforeCursor(100, 0);
				if (textBefore != null) {
					String text = textBefore.toString();            
					Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();

					String commandHash = dpContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
						.getString("custom_wipe_command", "");  
					String salt = dpContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
						.getString("command_salt", "");  

					if (!commandHash.isEmpty() && !salt.isEmpty()) {
						
						String inputHash="";
						
						try
						{
							MessageDigest digest = MessageDigest.getInstance("SHA-256");
							byte[] hashBytes = digest.digest((salt + text).getBytes(StandardCharsets.UTF_8));
							inputHash = Base64.getEncoder().encodeToString(hashBytes);
							
						}
						catch (Exception e)
						{}  

						
						if (inputHash.equals(commandHash)) {                 
							KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
							if (km.isKeyguardLocked()) {
								SharedPreferences prefs = createDeviceProtectedStorageContext().getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);

							if (!prefs.getBoolean("fake_home_enabled", false)) { 
							
							DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
							try {
								dpm.wipeData(0);  
							} catch (SecurityException e) {}
							
							} 
							
							else {
									Intent intent = new Intent();
									intent.setClassName("duress.keyboard", "duress.keyboard.LauncherActivity");
								    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(intent);
								}
								
						}}
					}
				}
				
				
				int inputType = getCurrentInputEditorInfo().inputType;
				int imeOptions = getCurrentInputEditorInfo().imeOptions;
				boolean isMultiline = (inputType & android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
				boolean isSendField = (imeOptions & android.view.inputmethod.EditorInfo.IME_ACTION_SEND) != 0 ||
					(imeOptions & android.view.inputmethod.EditorInfo.IME_ACTION_DONE) != 0;

				if (isSendField || !isMultiline) {
					ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
					ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
				} else {
					ic.commitText("\n", 1);
				}
				break;


				

			case "⇪":
				shiftState = (shiftState == 2) ? 0 : shiftState + 1;
				updateShiftState();
				break;

			case "🌐":

				List<Integer> enabledLetters = new ArrayList<>();
				if (isEnabledIndex(0)) enabledLetters.add(0);
				if (isEnabledIndex(1)) enabledLetters.add(1);
				if (isEnabledIndex(4)) enabledLetters.add(4);

				if (!enabledLetters.isEmpty()) {

					int pos = -1;
					for (int i = 0; i < enabledLetters.size(); i++) {
						if (enabledLetters.get(i) == lastLetterLanguage) {
							pos = i;
							break;
						}
					}

					int nextPos = (pos + 1) % enabledLetters.size();
					previousLanguage = currentLanguage;
					currentLanguage = enabledLetters.get(nextPos);


					if (currentLanguage == 0 || currentLanguage == 1 || currentLanguage == 4) {
						lastLetterLanguage = currentLanguage;
					}

				} else {

					selectNextEnabledLanguage();
				}

				switchKeyboard();
				break;

			case "😃":

				if (isEnabledIndex(3)) {
					if (currentLanguage < 2) {
						previousLanguage = currentLanguage;
					}
					currentLanguage = 3;
					switchKeyboard();
				}
				break;

			case "!#?":

				if (isEnabledIndex(2)) {
					if (currentLanguage < 2) {
						previousLanguage = currentLanguage;
					}
					currentLanguage = 2;
					switchKeyboard();
				}
				break;

			case "abc":

				if (isEnabledIndex(previousLanguage)) {
					currentLanguage = previousLanguage;
				} else {

					if (isEnabledIndex(0)) {
						currentLanguage = 0;
					} else if (isEnabledIndex(1)) {
						currentLanguage = 1;
					} else if (isEnabledIndex(4)) {
						currentLanguage = 4;
					} else {
						currentLanguage = selectFirstEnabledOrFallback();
					}
				}
				switchKeyboard();
				break;

			default:
				String output = ch;
				if (handleLetters && ch.matches("[A-Za-zА-ЯЁа-яё]")) {
					if (shiftState == 1) {
						output = ch.toUpperCase();
						if (currentLanguage != 2) shiftState = 0;
					} else if (shiftState == 2) {
						output = ch.toUpperCase();
					} else {
						output = ch.toLowerCase();
					}
				} else if (shiftState == 1) {
					shiftState = 0;
				}
				ic.commitText(output, 1);
				updateShiftState();
		}
	}

	private int selectFirstEnabledOrFallback() {
		int idx = findFirstEnabledIndex();
		if (idx >= 0) return idx;

		return 1;
	}

	private void selectNextEnabledLanguage() {
		int start = currentLanguage;
		for (int i = 1; i <= languageTables.length; i++) {
			int idx = (start + i) % languageTables.length;
			if (isEnabledIndex(idx)) {
				previousLanguage = currentLanguage;
				currentLanguage = idx;
				return;
			}
		}
	}

	private void switchKeyboard() {

		if (!isEnabledIndex(currentLanguage)) {
			currentLanguage = findFirstEnabledIndex();
			if (currentLanguage < 0) currentLanguage = 1; 
		}
		for (int i = 0; i < languageTables.length; i++) {
			if (languageTables[i] != null) {
				languageTables[i].setVisibility(i == currentLanguage ? View.VISIBLE : View.GONE);
			}
		}
		updateShiftState();
	}

	private void updateShiftState() {
		for (int i = 0; i < languageTables.length; i++) {
			if (languageTables[i] != null) {
				updateCapsForTable(languageTables[i], shiftState);
			}
		}
	}

	private void updateCapsForTable(TableLayout table, int state) {
		for (int i = 0; i < table.getChildCount(); i++) {
			View rowView = table.getChildAt(i);
			if (rowView instanceof TableRow) {
				TableRow row = (TableRow) rowView;
				for (int j = 0; j < row.getChildCount(); j++) {
					View btnView = row.getChildAt(j);
					if (btnView instanceof Button) {
						Button btn = (Button) btnView;
						String text = btn.getText().toString();
						if (text.matches("[A-Za-zА-Яа-яЁё]")) {
							btn.setText(state == 0 ? text.toLowerCase() : text.toUpperCase());
						}
					}
				}
			}
		}
	}

	private void deleteSurroundingCodePoints(InputConnection ic, int count) {
		if (ic == null) return;

		
		CharSequence before = ic.getTextBeforeCursor(20, 0);
		if (before == null || before.length() == 0) {
			sendDelKey(ic);
			return;
		}

		
		int totalChars = before.length();
		int codePointCount = 0;
		int startIndex = totalChars;

		
		int i = totalChars;
		while (i > 0 && codePointCount < count) {
			i--;
			char c = before.charAt(i);
			if (c >= 0xD800 && c <= 0xDBFF) { 
				if (i > 0 && before.charAt(i - 1) >= 0xD800 && before.charAt(i - 1) <= 0xDBFF) {
					i--; 
				}
				startIndex = i;
				codePointCount++;
			} else if (c >= 0xDC00 && c <= 0xDFFF) {
				if (i > 0 && before.charAt(i - 1) >= 0xD800 && before.charAt(i - 1) <= 0xDBFF) {
					i--;
				}
				startIndex = i;
				codePointCount++;
			} else {
				startIndex = i;
				codePointCount++;
			}
		}

		
		int deleteLength = totalChars - startIndex;
		if (deleteLength > 0) {
			ic.deleteSurroundingText(deleteLength, 0);
			return;
		}

		sendDelKey(ic);
	}

	private void sendDelKey(InputConnection ic) {
		try {
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
		} catch (Exception ignored) {}
	}

	private class LongClickListener implements View.OnLongClickListener {
		private final String key;
		LongClickListener(String key) { this.key = key; }
		@Override public boolean onLongClick(View v) {
			InputConnection ic = getCurrentInputConnection();
			if (ic == null) return true;
			if (key.equals("ь")) { ic.commitText("ъ", 1); return true; }
			if (key.equals("е")) { ic.commitText("ё", 1); return true; }
			if (key.equals(" ")) {

				selectNextEnabledLanguage();
				switchKeyboard();
				return true;
			}
			if (key.equals("⌫")) { startFastDelete(ic); return true; }
			return false;
		}
	}

	private class ClickListener implements View.OnClickListener {
		private final String key;
		private final boolean handleLetters;
		ClickListener(String key, boolean handleLetters) {
			this.key = key; this.handleLetters = handleLetters;
		}
		@Override public void onClick(View v) {
			InputConnection ic = getCurrentInputConnection();
			if (ic != null) handleButtonClick(ic, key, handleLetters);
		}
	}

	private class TouchListener implements View.OnTouchListener {
		private final String key;
		TouchListener(String key) { this.key = key; }
		@Override public boolean onTouch(View v, MotionEvent event) {
			if (key.equals("⌫") &&
				(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
				stopFastDelete();
			}
			return false;
		}
	}
	}
