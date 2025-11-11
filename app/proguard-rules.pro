# ======================================================================
# FOSS TRANSPARENCY: ОТКЛЮЧЕНИЕ ОБФУСКАЦИИ И ОПТИМИЗАЦИИ
# ----------------------------------------------------------------------
# Сохраняем имена всех классов, полей и методов приложения (отключаем обфускацию).
-keep class * { *; }
-keep interface * { *; }

# Отключаем оптимизацию для упрощения аудита кода.
-dontoptimize
# ======================================================================

# 🛡️ ОБЯЗАТЕЛЬНЫЕ ПРАВИЛА СОХРАНЕНИЯ СИСТЕМНЫХ КОМПОНЕНТОВ
# Сохраняем классы, вызываемые системой Android по имени из Manifest'а.

# 1. INPUT METHOD SERVICE (Клавиатура)
-keep public class duress.keyboard.SimpleKeyboardService {
    <init>();
}

# 2. DEVICE ADMIN RECEIVER (Администратор Устройства)
-keep public class duress.keyboard.MyDeviceAdminReceiver {
    <init>();
    public *;
}

# 3. ACTIVITY (Экран настроек)
-keep public class duress.keyboard.MainActivity {
    <init>();
}

# 4. СЛУШАТЕЛИ VIEW
-keepclassmembers class * extends android.view.View {
    void setOnClickListener(android.view.View$OnClickListener);
    void setOnLongClickListener(android.view.View$OnLongClickListener);
    void setOnTouchListener(android.view.View$OnTouchListener);
}

# 5. КОНТЕКСТ ДЛЯ ЗАЩИЩЕННОГО ХРАНИЛИЩА
-keepclassmembers class android.content.Context {
    public android.content.Context createDeviceProtectedStorageContext();
}

