package duress.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class FakeSmsActivity extends Activity {

    private boolean isRussian;
    private FrameLayout root;
    private boolean isComposeScreen = false;
    private boolean isRecentScreen = false;
    private int navBarHeight = 120;
    private boolean isShiftPressed = false;
    private LinearLayout keyboardLayout;
    private EditText messageEdit;
    private EditText recipientEdit;

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Locale locale = Locale.getDefault();
        isRussian = locale != null && "ru".equalsIgnoreCase(locale.getLanguage());

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        navBarHeight = (int) (56 * getResources().getDisplayMetrics().density);

        showMainScreen();
    }

    

    @Override
    public void onBackPressed() {
        if (keyboardLayout != null && keyboardLayout.getVisibility() == View.VISIBLE) {
            hideKeyboard();
        } else if (isRecentScreen) {
            showMainScreen();
        } else if (isComposeScreen) {
            showMainScreen();
        } else {
            super.onBackPressed();
        }
    }

    

    private void showMainScreen() {
        isComposeScreen = false;
        isRecentScreen = false;
        hideKeyboard();
        root.removeAllViews();

        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(contentContainer, contentParams);

        TextView emptyText = new TextView(this);
        emptyText.setText(isRussian ? "Нет сообщений" : "No messages");
        emptyText.setTextSize(18);
        emptyText.setTextColor(Color.GRAY);
        emptyText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams =
            new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;

        contentContainer.addView(emptyText, textParams);

        Button plusButton = new Button(this);
        plusButton.setText("+");
        plusButton.setTextSize(32);
        plusButton.setTextColor(Color.WHITE);

        GradientDrawable plusBg = new GradientDrawable();
        plusBg.setShape(GradientDrawable.OVAL);
        plusBg.setColor(Color.parseColor("#4CAF50"));
        plusButton.setBackgroundDrawable(plusBg);

        FrameLayout.LayoutParams plusParams =
            new FrameLayout.LayoutParams(160, 160);
        plusParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        plusParams.rightMargin = 32;
        plusParams.bottomMargin = 32 + navBarHeight;

        plusButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showComposeScreen();
				}
			});

        contentContainer.addView(plusButton, plusParams);

        addNavigationBar();
    }

    

    private void showComposeScreen() {
        isComposeScreen = true;
        isRecentScreen = false;
        hideKeyboard();
        root.removeAllViews();

        final FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(contentContainer, contentParams);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        

        recipientEdit = new EditText(this);
        recipientEdit.setHint(isRussian ? "Получатель" : "Recipient");
        recipientEdit.setSingleLine(true);
        recipientEdit.setTextSize(16);
        recipientEdit.setPadding(24, 24, 24, 24);
        recipientEdit.setFocusable(false);
        recipientEdit.setFocusableInTouchMode(false);

        GradientDrawable recipientBg = new GradientDrawable();
        recipientBg.setColor(Color.parseColor("#F1F3F4"));
        recipientBg.setCornerRadius(48);
        recipientEdit.setBackgroundDrawable(recipientBg);

        LinearLayout.LayoutParams recipientParams =
            new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        recipientParams.bottomMargin = 24;

        recipientEdit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showNumericKeyboardForRecipient();
				}
			});

        

        messageEdit = new EditText(this);
        messageEdit.setHint(isRussian ? "Сообщение" : "Message");
        messageEdit.setTextSize(16);
        messageEdit.setMinLines(5);
        messageEdit.setGravity(Gravity.TOP);
        messageEdit.setPadding(24, 24, 24, 24);
        messageEdit.setFocusable(false);
        messageEdit.setFocusableInTouchMode(false);

        GradientDrawable messageBg = new GradientDrawable();
        messageBg.setColor(Color.parseColor("#F1F3F4"));
        messageBg.setCornerRadius(32);
        messageEdit.setBackgroundDrawable(messageBg);

        LinearLayout.LayoutParams messageParams =
            new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.bottomMargin = 24;

        messageEdit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showKeyboard();
				}
			});

        

        Button sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setTextSize(22);
        sendButton.setTextColor(Color.WHITE);

        GradientDrawable sendBg = new GradientDrawable();
        sendBg.setShape(GradientDrawable.OVAL);
        sendBg.setColor(Color.parseColor("#1A73E8"));
        sendButton.setBackgroundDrawable(sendBg);

        LinearLayout.LayoutParams sendParams =
            new LinearLayout.LayoutParams(140, 140);
        sendParams.gravity = Gravity.RIGHT;

        sendButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String r = recipientEdit.getText().toString().trim();
					String m = messageEdit.getText().toString().trim();

					if (TextUtils.isEmpty(r) || TextUtils.isEmpty(m)) {
						Toast.makeText(
							FakeSmsActivity.this,
							isRussian ? "Заполните все поля" : "Please fill in all fields",
							Toast.LENGTH_SHORT
						).show();
						return;
					}

					if (!isValidPhone(r)) {
						Toast.makeText(
							FakeSmsActivity.this,
							isRussian
							? "Нет такого номера или контакта"
							: "No such number or contact",
							Toast.LENGTH_SHORT
						).show();
						return;
					}

					Toast.makeText(
						FakeSmsActivity.this,
						isRussian ? "Отправлено!" : "Sent!",
						Toast.LENGTH_SHORT
					).show();

					showMainScreen();
				}
			});

        layout.addView(recipientEdit, recipientParams);
        layout.addView(messageEdit, messageParams);
        layout.addView(sendButton, sendParams);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );

        contentContainer.addView(layout, layoutParams);

        addNavigationBar();
    }


    private void showNumericKeyboardForRecipient() {
        if (keyboardLayout != null && keyboardLayout.getParent() != null) {
            ((FrameLayout) keyboardLayout.getParent()).removeView(keyboardLayout);
        }

        keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        keyboardLayout.setPadding(12, 16, 12, 16);

        
        String[][] numericKeys = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"},
            {"*", "0", "#"},
            {"+", "⌫", "⏎"}
        };

        
        String[][] numberLabels = {
            {"", "ABC", "DEF"},
            {"GHI", "JKL", "MNO"},
            {"PQRS", "TUV", "WXYZ"},
            {"", "", ""},
            {"", "", ""}
        };

        for (int i = 0; i < numericKeys.length; i++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);

            for (int j = 0; j < numericKeys[i].length; j++) {
                final String key = numericKeys[i][j];
                String label = (i < numberLabels.length && j < numberLabels[i].length) 
                    ? numberLabels[i][j] : "";

                LinearLayout keyContainer = createNumericKey(key, label);
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                    0,
                    (int) (96 * getResources().getDisplayMetrics().density)
                );
                keyParams.weight = 1;
                keyParams.setMargins(6, 6, 6, 6);

                keyContainer.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							handleKeyPress(key, recipientEdit);
						}
					});

                rowLayout.addView(keyContainer, keyParams);
            }

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            keyboardLayout.addView(rowLayout, rowParams);
        }

        FrameLayout.LayoutParams keyboardParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        keyboardParams.gravity = Gravity.BOTTOM;
        keyboardParams.bottomMargin = navBarHeight;

        root.addView(keyboardLayout, keyboardParams);
    }

    private LinearLayout createNumericKey(String mainText, String subText) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);

        
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(16);
        bg.setStroke(2, Color.parseColor("#E0E0E0"));
        container.setBackgroundDrawable(bg);

        
        TextView mainTextView = new TextView(this);
        mainTextView.setText(mainText);
        mainTextView.setTextSize(32);
        mainTextView.setTextColor(Color.BLACK);
        mainTextView.setGravity(Gravity.CENTER);

        
        if (!TextUtils.isEmpty(subText)) {
            TextView subTextView = new TextView(this);
            subTextView.setText(subText);
            subTextView.setTextSize(10);
            subTextView.setTextColor(Color.parseColor("#757575"));
            subTextView.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            subParams.topMargin = -8;
            container.addView(mainTextView);
            container.addView(subTextView, subParams);
        } else {
            LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            mainParams.gravity = Gravity.CENTER;
            container.addView(mainTextView, mainParams);
        }

        
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        return container;
    }

    

    private void showKeyboard() {
        if (keyboardLayout != null && keyboardLayout.getParent() != null) {
            ((FrameLayout) keyboardLayout.getParent()).removeView(keyboardLayout);
        }

        keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardLayout.setBackgroundColor(Color.parseColor("#1A1A1A"));
        keyboardLayout.setPadding(8, 8, 8, 8);

        String[][] keys = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"},
            {"⇪", "z", "x", "c", "v", "b", "n", "m", "⌫"},
            {",", " ", ".", "⏎"}
        };

        for (String[] row : keys) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);

            for (final String key : row) {
                Button keyButton = createKeyButton(key);
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                    0,
                    (int) (48 * getResources().getDisplayMetrics().density)
                );
                keyParams.weight = 1;
                keyParams.setMargins(2, 2, 2, 2);

                if (key.equals("⇪") || key.equals("⌫") || key.equals("⏎")) {
                    keyParams.weight = 1.5f;
                } else if (key.equals(" ")) {
                    keyParams.weight = 4;
                }

                keyButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							handleKeyPress(key, messageEdit);
						}
					});

                rowLayout.addView(keyButton, keyParams);
            }

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            keyboardLayout.addView(rowLayout, rowParams);
        }

        FrameLayout.LayoutParams keyboardParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        keyboardParams.gravity = Gravity.BOTTOM;
        keyboardParams.bottomMargin = navBarHeight;

        root.addView(keyboardLayout, keyboardParams);
    }

    private Button createKeyButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(20);
        button.setTextColor(Color.WHITE);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#333333"));
        bg.setCornerRadius(8);
        bg.setStroke(1, Color.parseColor("#555555"));
        button.setBackgroundDrawable(bg);

        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);

        return button;
    }

    private void handleKeyPress(String key, EditText targetEditText) {
        if (targetEditText == null) return;

        int cursorPos = targetEditText.getSelectionStart();
        if (cursorPos < 0) cursorPos = targetEditText.getText().length();

        String currentText = targetEditText.getText().toString();

        switch (key) {
            case "⌫":
                if (cursorPos > 0 && currentText.length() > 0) {
                    String newText = currentText.substring(0, cursorPos - 1) + 
						currentText.substring(cursorPos);
                    targetEditText.setText(newText);
                    targetEditText.setSelection(Math.max(0, cursorPos - 1));
                }
                break;

            case "⇪":
                isShiftPressed = !isShiftPressed;
                updateKeyboardCase();
                break;

            case "⏎":
                String newText = currentText.substring(0, cursorPos) + "\n" + 
					currentText.substring(cursorPos);
                targetEditText.setText(newText);
                targetEditText.setSelection(cursorPos + 1);
                break;

            case " ":
                String textWithSpace = currentText.substring(0, cursorPos) + " " + 
					currentText.substring(cursorPos);
                targetEditText.setText(textWithSpace);
                targetEditText.setSelection(cursorPos + 1);
                break;

            default:
                String charToInsert = key;
                if (isShiftPressed && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    charToInsert = key.toUpperCase();
                }

                String textWithChar = currentText.substring(0, cursorPos) + charToInsert + 
					currentText.substring(cursorPos);
                targetEditText.setText(textWithChar);
                targetEditText.setSelection(cursorPos + 1);

                if (isShiftPressed && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    isShiftPressed = false;
                    updateKeyboardCase();
                }
                break;
        }
    }

    private void updateKeyboardCase() {
        if (keyboardLayout == null) return;

        for (int i = 0; i < keyboardLayout.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) keyboardLayout.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                Button button = (Button) row.getChildAt(j);
                String text = button.getText().toString();

                if (text.length() == 1 && Character.isLetter(text.charAt(0))) {
                    if (isShiftPressed) {
                        button.setText(text.toUpperCase());
                    } else {
                        button.setText(text.toLowerCase());
                    }
                }
            }
        }
    }

    private void hideKeyboard() {
        if (keyboardLayout != null && keyboardLayout.getParent() != null) {
            ((FrameLayout) keyboardLayout.getParent()).removeView(keyboardLayout);
            keyboardLayout = null;
        }
    }


    private void addNavigationBar() {
        final LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Color.parseColor("#E61A1A1A"));

        final FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            navBarHeight
        );
        navParams.gravity = Gravity.BOTTOM;

        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					navParams.gravity = Gravity.BOTTOM;
					navBar.setLayoutParams(navParams);
				}
			});

        root.addView(navBar, navParams);

        Button backButton = createNavButton("<");
        backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent(FakeSmsActivity.this, 
												   Class.forName("duress.keyboard.LauncherActivity"));
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						Toast.makeText(FakeSmsActivity.this, 
									   isRussian ? "LauncherActivity не найден" : "LauncherActivity not found", 
									   Toast.LENGTH_SHORT).show();
					}
				}
			});

        Button homeButton = createNavButton("□");
        homeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent(FakeSmsActivity.this, 
												   Class.forName("duress.keyboard.LauncherActivity"));
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						Toast.makeText(FakeSmsActivity.this, 
									   isRussian ? "LauncherActivity не найден" : "LauncherActivity not found", 
									   Toast.LENGTH_SHORT).show();
					}
				}
			});

        Button recentButton = createNavButton("|||");
        recentButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isRecentScreen) {
						showMainScreen();
					} else {
						showRecentAppsScreen();
					}
				}
			});

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        buttonParams.weight = 1;

        navBar.addView(backButton, buttonParams);
        navBar.addView(homeButton, buttonParams);
        navBar.addView(recentButton, buttonParams);
    }

    private Button createNavButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(24);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        return button;
    }


    private void showRecentAppsScreen() {
        isRecentScreen = true;
        isComposeScreen = false;
        hideKeyboard();
        root.removeAllViews();

        FrameLayout recentContainer = new FrameLayout(this);
        recentContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));
        FrameLayout.LayoutParams recentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(recentContainer, recentParams);

        TextView noRecentText = new TextView(this);
        noRecentText.setText(isRussian ? "Нет последних приложений" : "No recent applications");
        noRecentText.setTextSize(20);
        noRecentText.setTextColor(Color.parseColor("#CCCCCC"));
        noRecentText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;

        recentContainer.addView(noRecentText, textParams);

        addNavigationBar();
    }

    

    private boolean isValidPhone(String s) {
        if (TextUtils.isEmpty(s)) return false;

        if (s.startsWith("+")) {
            if (s.length() == 1) return false;
            s = s.substring(1);
        }

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}