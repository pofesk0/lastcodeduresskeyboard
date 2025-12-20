package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class LauncherActivity extends Activity {

    private FrameLayout root;
    private boolean isRecentScreen = false;
    private int navBarHeight = 120; 
	
	
	private void showCameraErrorMessage() {
		String message = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage())
			? "В приложении \"Камера\" произошла Ошибка"
			: "In the \"Camera\" application an Error occurred";

		String buttonText = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage())
			? "✕  закрыть приложение"
			: "✕  close application";

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
			.setNegativeButton(buttonText, null)
			.setCancelable(false);

		AlertDialog dialog = builder.create();

		Window window = dialog.getWindow();
		if (window != null) {
			window.setGravity(Gravity.CENTER);
		}

		dialog.show();

		
		TextView messageView = dialog.findViewById(android.R.id.message);
		if (messageView != null) {
			messageView.setGravity(Gravity.CENTER);
			messageView.setTypeface(Typeface.DEFAULT_BOLD); // Жирный шрифт
		}

		Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeButton != null) {
			negativeButton.setTypeface(Typeface.DEFAULT);
		}
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

     }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        navBarHeight = (int) (56 * getResources().getDisplayMetrics().density);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        showMainScreen();
    }

    

    @Override
    public void onBackPressed() {
        if (isRecentScreen) {
            showMainScreen();
        } else {
            super.onBackPressed();
        }
    }

    

	private void showMainScreen() {
		isRecentScreen = false;
		root.removeAllViews();

		FrameLayout contentContainer = new FrameLayout(this);
		FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		);
		root.addView(contentContainer, contentParams);

		
		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		int padding = dp(16);
		int iconSize = dp(72);
		int iconMargin = dp(4);

		
		int iconsPerRow = Math.max(1, (screenWidth - 2 * padding) / (iconSize + 2 * iconMargin));

		
		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setPadding(padding, padding, padding, padding + navBarHeight);

		String[] icons;
		Class<?>[] activities;

		

			icons = new String[]{"📞", "💬", "🕗", "🖼️", "🧭", "📷"};
			activities = new Class<?>[]{
				PhoneActivity.class,
				FakeSmsActivity.class,
				ClockActivity.class,
				GalleryActivity.class,
				CompassActivity.class,
				null
			};
	
		
		for (int i = 0; i < icons.length; i += iconsPerRow) {
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);

			
			int iconsInThisRow = Math.min(iconsPerRow, icons.length - i);

			for (int j = 0; j < iconsInThisRow; j++) {
				int index = i + j;
				TextView icon = createIcon(icons[index]);
				final Class<?> activityClass = activities[index];

				icon.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (activityClass == null) {
								showCameraErrorMessage();
							} else {
								startActivity(new Intent(LauncherActivity.this, activityClass));
							}
						}
					});
					
				row.addView(icon);
			}

			mainLayout.addView(row);
		}

		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		);
		contentContainer.addView(mainLayout, layoutParams);

		addNavigationBar();
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
					Rect visibleRect = new Rect();
					root.getGlobalVisibleRect(visibleRect);
					navParams.gravity = Gravity.BOTTOM;
					navBar.setLayoutParams(navParams);
				}
			});

        root.addView(navBar, navParams);

        
        Button backButton = createNavButton("<");
        backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					if (isRecentScreen) {
						showMainScreen();
					}
				}
			});

        
        Button homeButton = createNavButton("□");
        homeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
			
					if (isRecentScreen) {
						showMainScreen();
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
        root.removeAllViews();

        
        FrameLayout recentContainer = new FrameLayout(this);
        recentContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));
        FrameLayout.LayoutParams recentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(recentContainer, recentParams);

        
        boolean isRussian = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

        
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

    private TextView createIcon(String emoji) {
        TextView tv = new TextView(this);
        tv.setText(emoji);
        tv.setTextSize(40);
        tv.setGravity(Gravity.CENTER);

        int size = dp(72);
        LinearLayout.LayoutParams lp =
			new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        tv.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFEDEDED);
        bg.setStroke(dp(2), 0xFF444444);
        bg.setCornerRadius(0);

        tv.setBackground(bg);

        return tv;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
