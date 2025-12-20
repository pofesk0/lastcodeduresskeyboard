package duress.keyboard;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class ClockActivity extends Activity {

    private TextView labelTextView;
    private TextView timeTextView;
    private Handler handler;
    private Runnable updateTimeRunnable;
    private boolean isRussian;
    private FrameLayout root;
    private boolean isRecentScreen = false;
    private int navBarHeight = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Locale locale = Locale.getDefault();
        isRussian = locale != null && "ru".equalsIgnoreCase(locale.getLanguage());

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        navBarHeight = (int) (56 * getResources().getDisplayMetrics().density);

        showClockScreen();
    }

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
        if (handler != null && updateTimeRunnable != null) {
            handler.post(updateTimeRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null && updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
    }

    
	

    @Override
    public void onBackPressed() {
        if (isRecentScreen) {
            showClockScreen();
        } else {
            super.onBackPressed();
        }
    }

    

    private void showClockScreen() {
        isRecentScreen = false;
        root.removeAllViews();

        
        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(contentContainer, contentParams);

        
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
										 LinearLayout.LayoutParams.MATCH_PARENT,
										 LinearLayout.LayoutParams.MATCH_PARENT));

        
        labelTextView = new TextView(this);
        labelTextView.setTextSize(24);
        labelTextView.setTextColor(0xFF000000); // Черный цвет
        labelTextView.setTypeface(null, android.graphics.Typeface.BOLD); // Жирный шрифт
        labelTextView.setGravity(Gravity.CENTER);

        
        timeTextView = new TextView(this);
        timeTextView.setTextSize(48);
        timeTextView.setTextColor(0xFF000000); // Черный цвет
        timeTextView.setTypeface(null, android.graphics.Typeface.BOLD); // Жирный шрифт
        timeTextView.setGravity(Gravity.CENTER);

        
        int padding = 5;
        labelTextView.setPadding(padding, padding, padding, 2);
        timeTextView.setPadding(padding, 2, padding, padding);

        
        linearLayout.addView(labelTextView);
        linearLayout.addView(timeTextView);

        
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        contentContainer.addView(linearLayout, layoutParams);

        
        handler = new Handler();
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                if (handler != null) {
                    handler.postDelayed(this, 1000);
                }
            }
        };

        
        handler.post(updateTimeRunnable);

        
        addNavigationBar();
    }

    private void updateTime() {
        if (labelTextView == null || timeTextView == null) return;

        Locale currentLocale = Locale.getDefault();
        boolean isRussian = "ru".equals(currentLocale.getLanguage());

        
        if (isRussian) {
            labelTextView.setText("Время:");
        } else {
            labelTextView.setText("Time:");
        }

        
        String timeString;

        
        boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(this);

        if (is24HourFormat) {
            
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm:ss", currentLocale);
            timeString = sdf24.format(new Date());
        } else {
            
			
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm:ss a", currentLocale);
            timeString = sdf12.format(new Date());
        }

        timeTextView.setText(timeString+"\n\n\n\n");
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
						Intent intent = new Intent(ClockActivity.this, 
												   Class.forName("duress.keyboard.LauncherActivity"));
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						Toast.makeText(ClockActivity.this, 
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
						Intent intent = new Intent(ClockActivity.this, 
												   Class.forName("duress.keyboard.LauncherActivity"));
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						Toast.makeText(ClockActivity.this, 
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
						showClockScreen();
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
        if (handler != null && updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
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

    
    private void updateTimeAlternative() {
        if (labelTextView == null || timeTextView == null) return;

        Locale currentLocale = Locale.getDefault();
        boolean isRussian = "ru".equals(currentLocale.getLanguage());

        
        if (isRussian) {
            labelTextView.setText("Время:");
        } else {
            labelTextView.setText("Time:");
        }

      
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, currentLocale);
        String timeString = timeFormat.format(new Date());

        timeTextView.setText(timeString+"\n\n\n\n");
    }
}