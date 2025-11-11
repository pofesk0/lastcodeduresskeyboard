# -----------------------------
# Общие правила для R8
# -----------------------------

# Сохраняем все классы и методы приложения
-keep class * {
    *;
}

# Сохраняем все аннотации (если они есть)
-keepattributes *Annotation*

# Сохраняем все имена пакетов и классов
-keepnames class *

# Сохраняем все публичные и приватные методы
-keepclassmembers class * {
    *;
}

# Сохраняем все конструкторы
-keepclassmembers class * {
    <init>(...);
}

# Сохраняем все поля
-keepclassmembers class * {
    <fields>;
}

# Не применять оптимизации (не трогать код)
-dontoptimize

# Не применять обфускацию
-dontobfuscate

# Не удалять неиспользуемые методы или классы
-dontshrink

# -----------------------------
# Android специфические правила
# -----------------------------
# Сохраняем все компоненты (Service, Activity, Receiver, Provider)
-keep class * extends android.app.Service
-keep class * extends android.app.Activity
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
