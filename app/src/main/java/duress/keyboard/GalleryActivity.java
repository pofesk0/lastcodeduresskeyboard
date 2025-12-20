package duress.keyboard;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class GalleryActivity extends Activity {

    private String[] smileys = {"🎩", "🌠", "🌫", "🌂", "🌌", "️️🌧"};
    private LinearLayout layout;
    private int screenWidth;
    private int screenHeight;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        // Создаем корневой FrameLayout как в FakeSmsActivity
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        navBarHeight = (int) (56 * getResources().getDisplayMetrics().density);

        showGrid();
    }


    @Override
    public void onBackPressed() {
        if (isRecentScreen) {
            showGrid();
        } else {
            super.onBackPressed();
        }
    }

    private void showGrid() {
		isRecentScreen = false;
		root.removeAllViews();
		FrameLayout contentContainer = new FrameLayout(this);
		FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		);
		root.addView(contentContainer, contentParams);
		layout = new LinearLayout(getApplicationContext());
		layout.setOrientation(LinearLayout.VERTICAL); 
		layout.setBackgroundColor(Color.BLACK);

		int cellSize = screenWidth / 2;
		int cellsPerRow = 2; 

		LinearLayout currentRow = null;
		int cellCountInRow = 0;

		for (int i = 0; i < smileys.length; i++) {
			
			if (currentRow == null || cellCountInRow >= cellsPerRow) {
				currentRow = new LinearLayout(getApplicationContext());
				currentRow.setOrientation(LinearLayout.HORIZONTAL);
				currentRow.setBackgroundColor(Color.BLACK);
				layout.addView(currentRow);
				cellCountInRow = 0;
			}

			LinearLayout cell = new LinearLayout(getApplicationContext());
			cell.setBackgroundColor(Color.BLACK);
			cell.setPadding(2, 2, 2, 2);

			TextView text = new TextView(getApplicationContext());
			text.setText(smileys[i]);
			text.setTextSize(40);
			text.setTextColor(Color.BLACK);
			text.setBackgroundColor(Color.WHITE);
			text.setGravity(Gravity.CENTER);

			cell.addView(text, cellSize - 4, cellSize - 4);

			final int index = i;
			cell.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showBig(index);
					}
				});

			currentRow.addView(cell);
			cellCountInRow++;
		}

		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT,
			FrameLayout.LayoutParams.WRAP_CONTENT
		);
		layoutParams.gravity = Gravity.TOP;
		contentContainer.addView(layout, layoutParams);

		addNavigationBar();
	}

    private void showBig(int index) {
        isRecentScreen = false;
        root.removeAllViews();

        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(contentContainer, contentParams);

        
        layout = new LinearLayout(getApplicationContext());
        layout.setBackgroundColor(Color.BLACK);

        int cellSize = screenWidth / 3;
        int bigSize = cellSize * 3;
        TextView big = new TextView(getApplicationContext());
        big.setText(smileys[index]);
        big.setTextSize(120);
        big.setTextColor(Color.BLACK);
        big.setBackgroundColor(Color.WHITE);
        big.setGravity(Gravity.CENTER);

        layout.setGravity(Gravity.CENTER);
        layout.addView(big, bigSize, bigSize);

        layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGrid();
                }
            });

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        contentContainer.addView(layout, layoutParams);
        

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
                        Intent intent = new Intent(GalleryActivity.this, 
                                                   Class.forName("duress.keyboard.LauncherActivity"));
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        Toast.makeText(GalleryActivity.this, 
                                       "LauncherActivity not found", 
                                       Toast.LENGTH_SHORT).show();
                    }
                }
            });

        Button homeButton = createNavButton("□");
        homeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(GalleryActivity.this, 
                                                   Class.forName("duress.keyboard.LauncherActivity"));
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        Toast.makeText(GalleryActivity.this, 
                                       "LauncherActivity not found", 
                                       Toast.LENGTH_SHORT).show();
                    }
                }
            });

        Button recentButton = createNavButton("|||");
        recentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isRecentScreen) {
                        showGrid();
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

        TextView noRecentText = new TextView(this);
		Locale locale = Locale.getDefault();
        isRussian = locale != null && "ru".equalsIgnoreCase(locale.getLanguage());
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
}