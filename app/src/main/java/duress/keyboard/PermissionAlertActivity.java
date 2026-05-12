package duress.keyboard;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class PermissionAlertActivity extends Activity {

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
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
    );}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dpToPx(12);

        TextView t1 = new TextView(this);
        t1.setText(isRu 
            ? "Спецвозможности предоставлены, теперь предоставьте приложению DuressKeyboard разрешение на отображение поверх других окон на случай если спецвозможности перестанут работать. Также, напоминание: клавиатура всегда должна быть назначена по умолчанию, даже если вы ей не пользуетесь на заблокированном экране. Ведь это помогает сервису стабильнее работать в фоне."
            : "The Accessibility is granted. Now, please grant permission to DuressKeyboard to display over other apps to guarantee the execution of its functionality even if the accessibility stops working. Also, a reminder: this keyboard should always be assigned by default, even if you don't use it on the lock screen. Because this helps the service maintain stable work in the background.");        
            root.addView(t1, lp);

        Button b1 = new Button(this);
        b1.setText(isRu ? "Открыть Настройки Разрешений" : "Open Permission Settings");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                }
                finish();
            }
        });
        root.addView(b1, lp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isRu ? "Нужно разрешение" : "Permission Required")
                .setView(root)
                .setCancelable(false)
                .create();

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp2 = window.getAttributes();
            lp2.gravity = Gravity.CENTER;
            window.setAttributes(lp2);
        }
    }
}
