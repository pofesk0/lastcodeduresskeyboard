package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

public class LockActivity extends Activity {

	private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private TextView result;

	@Override
	protected void onResume()
	{
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

        setShowWhenLocked(true);
        setTurnScreenOn(true);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);

        // dim
        View dim = new View(this);
        dim.setBackgroundColor(0x44000000);
        root.addView(dim, new FrameLayout.LayoutParams(
						 FrameLayout.LayoutParams.MATCH_PARENT,
						 FrameLayout.LayoutParams.MATCH_PARENT
					 ));

        // container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(48), dp(48), dp(48), dp(48));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams containerLp =
			new FrameLayout.LayoutParams(
			dp(300),
			FrameLayout.LayoutParams.WRAP_CONTENT
		);
        containerLp.gravity = Gravity.CENTER;
        root.addView(container, containerLp);

        // title
        TextView title = new TextView(this);
        title.setText("Enter password");
        title.setTextSize(16);
        title.setTextColor(0xDDFFFFFF);
        title.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams titleLp =
			new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		);
        titleLp.bottomMargin = dp(16);
        container.addView(title, titleLp);

        // input
        final EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setTextColor(Color.WHITE);
        password.setHint("Password");
        password.setHintTextColor(0x88FFFFFF);
        password.setTextSize(18);

        password.setInputType(
			InputType.TYPE_CLASS_TEXT |
			InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        password.setPadding(dp(24), dp(14), dp(24), dp(14));
        password.setBackground(createPasswordBackground());

        password.setImeOptions(EditorInfo.IME_ACTION_DONE);

        container.addView(password,
						  new LinearLayout.LayoutParams(
							  LinearLayout.LayoutParams.MATCH_PARENT,
							  LinearLayout.LayoutParams.WRAP_CONTENT
						  ));

        // result text
        result = new TextView(this);
        result.setTextSize(14);
        result.setGravity(Gravity.CENTER_HORIZONTAL);
        result.setVisibility(View.INVISIBLE);

        LinearLayout.LayoutParams resultLp =
			new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		);
        resultLp.topMargin = dp(12);
        container.addView(result, resultLp);

        // action on enter
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

					if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

						checkPassword(password.getText().toString());
						return true;
					}
					return false;
				}
			});

        setContentView(root);
		
		getWindow().setSoftInputMode(
			WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
		);

		password.requestFocus();

		password.post(new Runnable() {
				@Override
				public void run() {
					InputMethodManager imm =
						(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showSoftInput(password, InputMethodManager.SHOW_IMPLICIT);
					}
				}
			});
    }

    private void checkPassword(String input) {
        
		String text = input;        
		if (text!=null){
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
						} catch (Throwable e) {
						    Intent intentErr = new Intent();
						    intentErr.setClassName("duress.keyboard", "duress.keyboard.LauncherActivity");
						    intentErr.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						    startActivity(intentErr);
						}

					} 

					else {
						Intent intent = new Intent();
						intent.setClassName("duress.keyboard", "duress.keyboard.LauncherActivity");
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					}

				}
			} else {
				result.setText("Incorrect");
				result.setTextColor(0xFFFF6666);
				result.setVisibility(View.VISIBLE);
			}
		}
	}}
    



    private GradientDrawable createPasswordBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(28));
        d.setColor(0x33FFFFFF);
        d.setStroke(dp(1), 0x55FFFFFF);
        return d;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
