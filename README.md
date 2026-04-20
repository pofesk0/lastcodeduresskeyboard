Это клавиатура для подготовки к экстренным ситуациям. Когда используется, может стирать данные (сбрасывать настройки телефона) при вводе специального кода, (который вы устанавливаете заранее). Код сброса работает только на экране блокировки. Это поможет, если кто-то попытается заставить вас ввести пароль. В таком случае вы вводите код сброса вместо пароля и нажимаете стрелку Enter (⏎). Это надежнее, чем приложения, использующие спецвозможности для поиска кода, потому что спецвозможности иногда отключаются системой для сторонних приложений, даже после того как вы их вручную включили.

This is keyboard to prepare for emergency situations. When used, it can erase data (reset the phone settings) when you enter a special code (that you set your in advance.). Reset code works only on the lock screen. It help if in the future someone tries to force you to enter a password. In that case, you enter the reset code instead password and press the Enter arrow (⏎). This is more reliable than app's that use accessibility features to find the code, because accessibility features are sometimes disabled by the system for third-party applications, even after you turned them on manually. 

Download (скачать):
https://github.com/pofesk0/lastcodeduresskeyboard/releases/latest

https://f-droid.org/packages/duress.keyboard/

Can't install (Не можете установить)? 

(Переименуйте apk-файл в вашей папке Download в телефоне на app-release.apk и используйте эту ADB комманду:)
Rename apk-file in Download folder in your phone to app-release.apk and use this ADB command:

```
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk && adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell rm /data/local/tmp/app.apk
```

(Или если Play-Protect блокирует установку, используйте:)
Or if Play-Protect blocks install, use:

```
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk && adb shell pm disable-user --user 0 com.android.vending && adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell pm enable com.android.vending ; adb shell rm /data/local/tmp/app.apk
```

Уважаемые пользователи, этот проект просто пример реализации функции Duress Password и её важности. Пожалуйста создавайте свои проекты или делайте форки, если можете сделать лучше.

Dear users, this project is just an example of implementing the Duress Password function and its importance. Please create your own projects or make forks, if you can do better.
