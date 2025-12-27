package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

public class WipeActivity extends Activity {

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
		
        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));

       
        final Button btnYes = new Button(this);
        btnYes.setText("✅");
        btnYes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        btnYes.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        btnYes.setPadding(0, dp(24), 0, dp(24));

        
        final Button btnNo = new Button(this);
        btnNo.setText("❌");
        btnNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        btnNo.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        btnNo.setPadding(0, dp(24), 0, dp(24));

       
        ((LinearLayout.LayoutParams) btnYes.getLayoutParams()).rightMargin = dp(16);
        ((LinearLayout.LayoutParams) btnNo.getLayoutParams()).leftMargin = dp(16);

       
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
				try {
					dpm.wipeData(0); 
				} catch (SecurityException e) {}
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

       
        root.addView(btnYes);
        root.addView(btnNo);

		setContentView(root);
		
		DisplayMetrics dm = getResources().getDisplayMetrics();
		
		float shift = -dm.heightPixels * 0.05f;
		btnYes.setTranslationY(shift);
		btnNo.setTranslationY(shift);
        
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }
}