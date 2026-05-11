package duress.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import java.util.Locale;

public class PermissionAlertActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());

        String title = isRu ? "Нужно разрешение" : "Permission Required";
        String message = isRu ? 
            "Спецвозможности предоставлены, теперь предоставьте приложению DuressKeyboard разрешение на отображение поверх других окон на случай если спецвозможности перестанут работать. Также напомним: клавитура всегда должна быть назначена по умолчанию, даже если вы ей не пользуетесь на заблокированном экране. Ведь это помогает сервису стабильнее работать в фоне." :
            "Accessibility granted. Now please grant DuressKeyboard permission to display over other apps in case accessibility stops working. Also, remember: the keyboard should always be set as default, even if you don't use it on the lock screen. This helps the service stay stable in the background.";
        
        String btnText = isRu ? "Открыть Настройки Разрешений" : "Open Permission Settings";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(btnText, (d, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Throwable e) {                        
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    }
                    finish();
                })
                .create();

        dialog.show();
    }
}
