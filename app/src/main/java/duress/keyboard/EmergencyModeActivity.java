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
    private static int isPendingAdmin = 0;

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
                if (isPendingAdmin == 0) {
                    isPendingAdmin = 2;
                    ShowEmergencyDialog();
                } else if (isPendingAdmin == 1) { 
                    isPendingAdmin = 2;
                    ShowAdminErrorDialog();
                }
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
                isPendingAdmin = 1;
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

	private void ShowAdminErrorDialog() {
    final boolean isRussian = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

    final LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    lp.bottomMargin = dpToPx(12);

    TextView t1 = new TextView(this);
    if (isRussian) {
        t1.setText("Вероятно, вы либо система отменили активацию прав администратора. Если это были вы или вы не знаете что произошло, например вы случайно нажали \"отмена\", попробуйте снова.");
    } else {
        t1.setText("Probably, you or the system canceled the device administrator activation. If it was you or you don't know what happened, for example you accidentally tapped \"cancel\", please try again.");
    }
    root.addView(t1, lp);
    
    final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
    String title = isRussian ? "Ошибка активации прав Администратора" : "Device Admin Activation Error";
    
    builder.setTitle(title)
           .setView(root)
           .setCancelable(false);
           
    adminErrorDialog = builder.create();

    Button b1 = new Button(this);
    b1.setText(isRussian ? "Попробовать снова" : "Try again");
    root.addView(b1, lp);
    b1.setOnClickListener(new View.OnClickListener() {
        @Override 
        public void onClick(View v) {            
			isPendingAdmin = 1;
			adminErrorDialog.dismiss();
            AllowAdmin();
        }
    });

    TextView t2 = new TextView(this);
    if (isRussian) {
        t2.setText("Если это была система, перейдите в настройки приложения, нажмите 3 точки в правом верхнем углу, затем \"разрешить ограниченные настройки\". После чего вернитесь сюда и попробуйте снова.");
    } else {
        t2.setText("If it was the system, go to the app settings, tap the 3 dots in the upper right corner, then \"allow restricted settings\". Then return here and try again.");
    }
    root.addView(t2, lp);

    Button b2 = new Button(this);
    b2.setText(isRussian ? "Перейти в настройки приложения" : "Go to app settings");
    root.addView(b2, lp);
    b2.setOnClickListener(new View.OnClickListener() {
        @Override 
        public void onClick(View v) {
            Detalis();
        }
    });

    TextView t3 = new TextView(this);
    if (isRussian) {
        t3.setText("Если 3 точек нет, значит окно активации прав администратора не является ограниченной настройкой. Тогда вернитесь наверх и попробуйте снова. Или перейдите в Настройки Администраторов, если не помогло.");
    } else {
        t3.setText("If there are no 3 dots, it means the admin activation window is not a restricted setting. Then return to the top and try again. Or go to Admin Settings if it didn't help.");
    }
    root.addView(t3, lp);

	Button b3 = new Button(this);
    b3.setText(isRussian ? "Открыть Настройки Администраторов" : "Go to Admin Settings");
    root.addView(b3, lp);
    b3.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
            android.content.Intent intent = new android.content.Intent();
			intent.setComponent(new android.content.ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"));
            startActivity(intent);
			isPendingAdmin = 1;	
			adminErrorDialog.dismiss();	
            } catch (Throwable e) {}
        }
    });	

    adminErrorDialog.show();

    android.view.Window window = adminErrorDialog.getWindow();
    if (window != null) {
        android.view.WindowManager.LayoutParams lp2 = window.getAttributes();
        lp2.gravity = android.view.Gravity.CENTER;
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
        
        if (adminErrorDialog != null) {
            if (adminErrorDialog.isShowing()) {
                adminErrorDialog.dismiss();
            }
            adminErrorDialog = null;
        }

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
