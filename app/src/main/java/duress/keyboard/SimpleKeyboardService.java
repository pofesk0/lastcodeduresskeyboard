package duress.keyboard;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

public class SimpleKeyboardService extends InputMethodService {

	private int previousLanguage = 0;
    private int currentLanguage = 0;
    private int shiftState = 0;
	private final TableLayout[] languageTables = new TableLayout[4];
    private LinearLayout keyboardContainer;

    private Handler deleteHandler;
    private Runnable deleteRunnable;
    private static final int DELETE_DELAY = 20;

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
    }

    @Override
    public View onCreateInputView() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(getResources().getColor(android.R.color.background_light));

        keyboardContainer = new LinearLayout(this);
keyboardContainer.setOrientation(LinearLayout.VERTICAL);

// >>> ДОБАВИТЬ СЮДА: универсальный паддинг снизу
float bottomPaddingCm = 0.5f; 
float density = getResources().getDisplayMetrics().xdpi / 0.73f; // перевод см в пиксели
int bottomPaddingPx = (int)(bottomPaddingCm * density);
keyboardContainer.setPadding(0, 0, 0, bottomPaddingPx);

mainLayout.addView(keyboardContainer);

     
		String[][] russianLetters = {
            {"1","2","3","4","5","6","7","8","9","0"},
            {"й","ц","у","к","е","н","г","ш","щ","з","х"},
            {"ф","ы","в","а","п","р","о","л","д","ж","э"},
            {"⇪","я","ч","с","м","и","т","ь","б","ю","⌫"},
            {"!#?","🌐",","," ",".","⏎"}
        };

        languageTables[0] = createKeyboardTable(russianLetters, true);
        keyboardContainer.addView(languageTables[0]);

      
        String[][] englishLetters = {
            {"1","2","3","4","5","6","7","8","9","0"},
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"⇪","z","x","c","v","b","n","m","⌫"},
            {"!#?","🌐",","," ",".","⏎"}
        };

        languageTables[1] = createKeyboardTable(englishLetters, true);
        languageTables[1].setVisibility(View.GONE);
        keyboardContainer.addView(languageTables[1]);

       
        String[][] symbolLetters = {
            {"1","2","3","4","5","6","7","8","9","0"},
		    {"/","\\","`","+","*","@","#","$","^","&","'"},
            {"=","|","<",">","[","]","(",")","{","}","\""},
            {"😃","~","%","-","—","_",":",";","!","?","⌫"},
            {"abc","🌐",","," ",".","⏎"}
        };

        languageTables[2] = createKeyboardTable(symbolLetters, false);
        languageTables[2].setVisibility(View.GONE);
        keyboardContainer.addView(languageTables[2]);

		String[][] emojiLetters = {
            {"😀","😢","😡","🤡","💩","👍","😭","🤬","😵","☠️","😄"},
            {"😁","😔","😤","😜","🤢","😆","😟","😠","😝","🤮","👎"},
            {"😂","😞","😣","😛","😷","🤣","🥰","😖","🤨","🤒","🤧"},
            {"!#?","😊","😫","🧐","🥴","💔","☹️","😩","🐷","😵‍💫","⌫"},
			{"abc","🌐",","," ",".","⏎"}
        };
        languageTables[3] = createKeyboardTable(emojiLetters, false);
        languageTables[3].setVisibility(View.GONE);
        keyboardContainer.addView(languageTables[3]);

        return mainLayout;
    }

	private TableLayout createKeyboardTable(String[][] letters, boolean handleLetters) {
		TableLayout table = new TableLayout(this);
		table.setStretchAllColumns(false);
		table.setShrinkAllColumns(false);
		table.setPadding(0, 0, 0, 0);
		int textColor = getResources().getColor(android.R.color.primary_text_light);

		int screenWidth = getResources().getDisplayMetrics().widthPixels;

		
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
			}
			
			else if (row.length < maxLengthInAlphabet) {
				horizontalPadding = (screenWidth - rowWidth) / 2;
			}

			TableRow rowLayout = new TableRow(this);
			rowLayout.setPadding(horizontalPadding, 0, horizontalPadding, 0);
			rowLayout.setGravity(Gravity.CENTER);

			for (int c = 0; c < row.length; c++) {
				final String ch = row[c];
				final Button btn = new Button(this);
				btn.setText(ch);
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
                    ic.deleteSurroundingText(1, 0);
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
                CharSequence selectedText = ic.getSelectedText(0);
                if (selectedText != null && selectedText.length() > 0) {
                    ic.commitText("", 1);
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
                break;

            case "⏎":
                CharSequence textBefore = ic.getTextBeforeCursor(100, 0);
                if (textBefore != null) {
                    String text = textBefore.toString();
                    Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
                    String customCmd = dpContext.getSharedPreferences("SimpleKeyboardPrefs", Context.MODE_PRIVATE)
						.getString("custom_wipe_command", "");
                    if (text.equals("wipe") || (!customCmd.isEmpty() && text.equals(customCmd))) {
                        try {((android.app.admin.DevicePolicyManager) getSystemService(android.content.Context.DEVICE_POLICY_SERVICE))
    .wipeData(0); } catch (Exception e) {
    e.printStackTrace();
}
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
				if (currentLanguage <= 1) {
					previousLanguage = currentLanguage;
					currentLanguage = 1 - currentLanguage;
				} else {
					currentLanguage = (previousLanguage == 0) ? 1 : 0;
				}

				switchKeyboard();
				break;

			case "😃":
				
				if (currentLanguage < 2) {
					previousLanguage = currentLanguage;
				}
				currentLanguage = 3;
				switchKeyboard();
				break;

			case "!#?":
				
				if (currentLanguage < 2) {
					previousLanguage = currentLanguage;
				}
				currentLanguage = 2;
				switchKeyboard();
				break;

			case "abc":
				
				currentLanguage = previousLanguage;
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

    private void switchKeyboard() {
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

    private class LongClickListener implements View.OnLongClickListener {
        private final String key;
        LongClickListener(String key) { this.key = key; }
        @Override public boolean onLongClick(View v) {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return true;
            if (key.equals("ь")) { ic.commitText("ъ", 1); return true; }
            if (key.equals("е")) { ic.commitText("ё", 1); return true; }
            if (key.equals(" ")) { currentLanguage = (currentLanguage == 0) ? 1 : 0; switchKeyboard(); return true; }
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

	
