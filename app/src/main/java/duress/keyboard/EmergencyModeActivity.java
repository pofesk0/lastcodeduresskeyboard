package duress.keyboard;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class EmergencyModeActivity extends Activity {

    private AlertDialog adminErrorDialog;    

    @Override
    protected void onResume() {
        super.onResume();
        
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);

        try {
            dpm.lockNow();
			dpm.setMaximumFailedPasswordsForWipe(admin, 1);            
			SharedPreferences prefs = createDeviceProtectedStorageContext().getSharedPreferences("SimpleKeyboardPrefs", MODE_PRIVATE);
			if (!prefs.getBoolean("emergency_mode_pending_for_keyguard_unlock", false)) {
            prefs.edit().putBoolean("emergency_mode_pending_for_keyguard_unlock", true).commit();
            }
            finish();
        } catch (Throwable t) {
            if (dpm.isAdminActive(admin)) {
                ShowLogDialog(t.toString());
            } else {                                                   
				ShowEmergencyDialog();                 
            }
        }
    }

    private AlertDialog logDialog;

    private void ShowLogDialog(String error) {
        final boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        TextView tv = new TextView(this);
        tv.setText(error);
        tv.setTextIsSelectable(true);
        root.addView(tv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        logDialog = new AlertDialog.Builder(this)
                .setTitle(isRu ? "Ошибка" : "Error")
                .setView(root)
                .setCancelable(false)
                .setPositiveButton("OK", (d, i) -> finish())
                .create();

        logDialog.show();

        Window window = logDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.CENTER;
            lp.x = 0;
            lp.y = 0;
            window.setAttributes(lp);
        }
    }

	private AlertDialog emergencyModeDialog;

        
       private void ShowEmergencyDialog() {
        final boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dpToPx(12);

        TextView t1 = new TextView(this);
        t1.setText(isRu ? "Привет. Это экстренный режим. Он заблокирует экран и попросит систему стирать данные в случае ввода любого неверного пароля на экране блокировки. Достаточно, чтобы вы ввели больше 4 символов и допустили хотя бы 1 ошибку. Этот режим будет ослаблен после разблокировки экрана и лимит неверных попыток ввода пароля будет сброшен до 3. Чтобы изменить число попыток ввода пароля для сброса данных, зайдите в настройки Aвтос-Cброса в приложении. Предоставьте права администратора для запуска этой функции." 
                        : "Hello. This is the emergency mode. It will lock the screen and ask the system to wipe data in case of any incorrect password entry on the lock screen. It is enough to enter more than 4 characters and make at least 1 mistake. This mode will be disabled after unlocking the screen and the limit of incorrect password attempts will be reset to 3. To change the number of password failed attempts for data reset, go to Auto-Wipe settings in the app. Please grant Device Admin rights to start this feature.");
        root.addView(t1, lp);

        emergencyModeDialog = new AlertDialog.Builder(this)
                .setTitle(isRu ? "Экстренный режим" : "Emergency Mode")
                .setView(root)
                .setCancelable(false)
                .create();

        Button b1 = new Button(this);
        b1.setText(isRu ? "Дать права администратора" : "Grant Admin Rights");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                
                emergencyModeDialog.dismiss();
                AllowAdmin();
            }
        });
        root.addView(b1, lp);

        emergencyModeDialog.show();

        Window window = emergencyModeDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp2 = window.getAttributes();
            lp2.gravity = Gravity.CENTER;
            lp2.x = 0;
            lp2.y = 0;
            window.setAttributes(lp2);
        }
    }     

	private void Detalis() {
    startActivity(
	new Intent(
		android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts(
						"package",
						getApplicationContext().getPackageName(),
						null
                        )
					)
			);
	}    

    private void AllowAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, MyDeviceAdminReceiver.class));
        startActivity(intent);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }


	    @Override
    protected void onDestroy() {
        super.onDestroy();
                
        if (emergencyModeDialog != null) {
            if (emergencyModeDialog.isShowing()) {
                emergencyModeDialog.dismiss();
            }
            emergencyModeDialog = null;
        }
		
		if (logDialog != null) {
            if (logDialog.isShowing()) {
                logDialog.dismiss();
            }
            logDialog = null;
        }
    }

}
