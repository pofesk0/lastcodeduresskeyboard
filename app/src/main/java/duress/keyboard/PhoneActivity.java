package duress.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.Random;

public class PhoneActivity extends Activity {

    private TextView input;
    private ToneGenerator tone;
    private Handler handler = new Handler();
    private boolean inCall = false;
    private AudioManager am;
    private FrameLayout root;
    private int navBarHeight = 120;
    private boolean isRecentScreen = false;
    private boolean isRussian;

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
    protected void onPause() {
        super.onPause();
		endCall();
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		if (inCall == true) {
		endCall();
		}
		
        am = (AudioManager) getSystemService(AUDIO_SERVICE);

        
        tone = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);

        
        Locale locale = Locale.getDefault();
        isRussian = locale != null && "ru".equalsIgnoreCase(locale.getLanguage());

        
        navBarHeight = (int) (56 * getResources().getDisplayMetrics().density);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        showDialer();
    }

    

    @Override
    public void onBackPressed() {
        if (isRecentScreen) {
            showDialer();
        } else {
            super.onBackPressed();
        }
    }

    

    private void showDialer() {
        inCall = false;
        isRecentScreen = false;
        root.removeAllViews();

        
        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(contentContainer, contentParams);

        LinearLayout dialerRoot = new LinearLayout(this);
        dialerRoot.setOrientation(LinearLayout.VERTICAL);
        dialerRoot.setBackgroundColor(Color.BLACK);
        dialerRoot.setPadding(20,20,20,20);
        dialerRoot.setGravity(Gravity.BOTTOM);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        input = new TextView(this);
        input.setTextSize(36);
        input.setTextColor(Color.WHITE);
        input.setGravity(Gravity.START);
        input.setPadding(10,10,10,10);

        LinearLayout.LayoutParams ip =
			new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        top.addView(input, ip);

        Button del = createRoundedButton("⌫", Color.DKGRAY, Color.WHITE);
        del.setTextSize(28);
        del.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					String t = input.getText().toString();
					if (t.length() > 0) input.setText(t.substring(0, t.length() - 1));
				}
			});
        top.addView(del);

        dialerRoot.addView(top);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setRowCount(4);
        grid.setUseDefaultMargins(true);

        String[] keys = {"1","2","3","4","5","6","7","8","9","*","0","#"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            Button btn = createRoundedButton(key, Color.DKGRAY, Color.WHITE);
            btn.setTextSize(26);
            btn.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) {
						if (inCall) return;
						input.append(key);
						checkIMEI();
					}
				});

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = GridLayout.LayoutParams.WRAP_CONTENT;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            grid.addView(btn, p);
        }

        dialerRoot.addView(grid);

        Button call = createRoundedButton("📞", Color.GREEN, Color.WHITE);
        call.setTextSize(32);
        call.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					String num = input.getText().toString();
					if (num.length() > 0 && !num.contains("*#06#")) {
						showCallScreen(num);
					}
				}
			});

        LinearLayout.LayoutParams cp =
			new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.topMargin = 20;
        dialerRoot.addView(call, cp);

        FrameLayout.LayoutParams dialerParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
        );
        dialerParams.bottomMargin = navBarHeight; 
        contentContainer.addView(dialerRoot, dialerParams);

        
        addNavigationBar();
    }

    

    private void checkIMEI() {
        String t = input.getText().toString();
        if (t.contains("*#06#")) {

            String imei = generateIMEI();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("IMEI:");
            builder.setMessage(imei+"\n\n");

            AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
				dialog.getWindow().setGravity(Gravity.CENTER);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.WHITE);

                bg.setCornerRadius(40);
                dialog.getWindow().setBackgroundDrawable(bg);
            }

            dialog.show();
            input.setText("");
        }
    }

    

    private void showCallScreen(String number) {
        inCall = true;
        isRecentScreen = false;
        root.removeAllViews();

        
        am.setMode(AudioManager.MODE_IN_CALL);

        
        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(contentContainer, contentParams);

        LinearLayout callRoot = new LinearLayout(this);
        callRoot.setOrientation(LinearLayout.VERTICAL);
        callRoot.setBackgroundColor(Color.BLACK);
        callRoot.setGravity(Gravity.CENTER);
        callRoot.setPadding(40,40,40,40);

        TextView num = new TextView(this);
        num.setText(number);
        num.setTextSize(32);
        num.setTextColor(Color.WHITE);
        num.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);

        if (isRussian) {
            status.setText("Вызов…");
        } else {
            status.setText("Calling…");
        }
        status.setTextColor(Color.GRAY);
        status.setPadding(0,20,0,60);

        Button end = createRoundedButton("📞", Color.RED, Color.WHITE);
        end.setTextSize(36);
        end.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					endCall();
				}
			});

        callRoot.addView(num);
        callRoot.addView(status);
        callRoot.addView(end);

        FrameLayout.LayoutParams callParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
        );
        callParams.bottomMargin = navBarHeight; 
        contentContainer.addView(callRoot, callParams);

        
        addNavigationBar();
        startRinging();
    }

    private void startRinging() {
        handler.post(ringRunnable);
    }

    private Runnable ringRunnable = new Runnable() {
        @Override public void run() {
            if (!inCall) return;
            tone.startTone(ToneGenerator.TONE_SUP_RINGTONE, 2000);
            handler.postDelayed(this, 4000);
        }
    };

    private void endCall() {
        inCall = false;
        handler.removeCallbacks(ringRunnable);
        tone.stopTone();

        
        am.setMode(AudioManager.MODE_NORMAL);

        showDialer();
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
					
					openLauncherActivity();
				}
			});

        
        Button homeButton = createNavButton("□");
        homeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					openLauncherActivity();
				}
			});

        
        Button recentButton = createNavButton("|||");
        recentButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isRecentScreen) {
						
						showDialer();
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

    private void openLauncherActivity() {
        try {
            Intent intent = new Intent(PhoneActivity.this,
									   Class.forName("duress.keyboard.LauncherActivity"));
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(PhoneActivity.this,
						   isRussian ? "LauncherActivity не найден" : "LauncherActivity not found",
						   Toast.LENGTH_SHORT).show();
        }
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
        root.removeAllViews();

        
        FrameLayout recentContainer = new FrameLayout(this);
        recentContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));
        FrameLayout.LayoutParams recentParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(recentContainer, recentParams);

        
        TextView noRecentText = new TextView(this);

        if (isRussian) {
            noRecentText.setText("Нет последних приложений");
        } else {
            noRecentText.setText("No recent applications");
        }
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


    private Button createRoundedButton(String text, int bgColor, int textColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(22);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(25);
        gd.setColor(bgColor);
        btn.setBackground(gd);

        return btn;
    }

    private String generateIMEI() {
        int[] digits = new int[14];
        Random r = new Random();
        for (int i = 0; i < 14; i++) digits[i] = r.nextInt(10);

        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int d = digits[i];
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
        }

        int check = (10 - sum % 10) % 10;

        StringBuilder sb = new StringBuilder();
        for (int d : digits) sb.append(d);
        sb.append(check);
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tone != null) tone.release();
    }
}
