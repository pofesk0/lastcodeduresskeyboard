# DuressKeyboard

<details open>
  <summary>🇬🇧 English Description:
  </summary>  &ensp;
  
This is a keyboard implementing the Duress Password feature.  
When used, it can wipe data (reset the phone settings) upon entry of a special pre-configured reset code on the lock screen.  
This app can help if someone ever tries to force you to enter a screen unlock password.
In this case, instead of it, you enter the reset code and press the Enter arrow (⏎).  
Also, this app has other security and data reset features.

<details>
<summary>Download  
</summary>  

Github:  
https://github.com/pofesk0/lastcodeduresskeyboard/releases/latest  
F-droid:   
https://f-droid.org/packages/duress.keyboard/
</details>

<details>
<summary>  
Can't install?  
</summary>  
  &ensp;  
  
Rename apk-file in your phone's Download folder to app-release.apk and use this ADB command:

```
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk && adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell rm /data/local/tmp/app.apk
```

Or if Play-Protect blocks installation, use this ADB command:

```
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk ; adb shell pm disable-user --user 0 com.android.vending ; adb shell pm disable-user --user 0 com.google.android.gms ; adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell pm enable com.android.vending ; adb shell pm enable com.google.android.gms ; adb shell rm /data/local/tmp/app.apk
```

<details>
  
<summary>How to use ADB</summary>
&ensp;  

Go to your phone's Settings, find 'Build Number' and tap 10 times. Then find 'Developer Options'. 

To use ADB via Wi-Fi:  
Enable Wireless debugging in Developer Options, install [Shizuku](https://github.com/RikkaApps/Shizuku/releases/latest), then pair and start Shizuku service.
Then install [aShell](https://f-droid.org/packages/in.sunilpaulmathew.ashell/). Open it, allow access to Shizuku, and use the required ADB command.

To use ADB via PC:  
Enable USB debugging in Developer Options.

Windows:  
Then, download [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) on PC and extract to C:\adb.  
Connect phone to PC via USB cable, open cmd and run: ```cd C:\adb```, then ```adb devices```, and allow access on the phone screen. Then use the required ADB command.

Linux:  
Then, download [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) on PC and extract to ~/adb.  
Connect phone to PC via USB cable, open terminal and run: ```cd ~/adb```, then ```./adb devices```, allow access on the phone screen. Then use the required ADB command.

</details>

<details>
  
<summary>How to install without ADB</summary>
&ensp;  

if you don't want to use ADB, just disable the Google Play (Store) and Google Play Services apps in [your phone's app settings](https://pofesk0.github.io/open-app-settings-proxy.html) during installation. However, this will only work if the installation block is not at the Android system level. It can be at the Android level, for example, if the app is too outdated for the current system, not just for Play-Protect. Or if Google tightens the restrictions in the future. In that case, you will have to go back to the ADB option.

If you still can't install and the reason is outdated target SDK (it is intentionally low here because of policy changes in new SDKs regarding wipe-data), then use Lite version:

https://github.com/pofesk0/DuressKeyboardLite/releases/latest  
https://f-droid.org/packages/duress.keyboard.lite/

It has a higher target SDK, but fewer features, and reset there doesn't delete FRP. Therefore, it is strongly recommended to not bind backups to Google accounts, because their IDs can remain after reset if your phone has FRP.

</details>
</details>
</details>
<br>
<details open>
  <summary>
  🏳️‍ Описание на русском:
  </summary>  &ensp;
  
Это клавиатура реализующая функцию Duress Password.  
Когда используется, может стирать данные (сбрасывать настройки телефона) при вводе специального заранее заданного кода сброса на экране блокировки.  
Это приложение может помочь, если кто-то попытается заставить вас ввести пароль разблокировки экрана. В таком случае вместо него вы вводите код сброса и нажимаете стрелку Enter (⏎).  
Также это приложение имеет другие функции безопасности и сброса данных.

<details>
<summary>Скачать  
</summary>  

Github:  
https://github.com/pofesk0/lastcodeduresskeyboard/releases/latest  
F-droid:   
https://f-droid.org/packages/duress.keyboard/
</details>

<details>
<summary>Не можете установить?  
</summary>  
  &ensp;  
  
Переименуйте apk-файл в папке Download вашего телефона на app-release.apk и используйте эту ADB команду:

```
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk && adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell rm /data/local/tmp/app.apk
```

Или если Play-Protect блокирует установку, используйте эту ADB команду:  

```
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk ; adb shell pm disable-user --user 0 com.android.vending ; adb shell pm disable-user --user 0 com.google.android.gms ; adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell pm enable com.android.vending ; adb shell pm enable com.google.android.gms ; adb shell rm /data/local/tmp/app.apk
```

<details>
  
<summary>Как использовать ADB</summary>
&ensp;  

Перейдите в Настройки вашего телефона, найдите 'Номер сборки' и нажмите 10 раз. Затем найдите 'Для разработчиков'. 

Чтобы использовать ADB по Wi-Fi:  
Включите беспроводную отладку в настройках Для разработчиков, установите [Shizuku](https://github.com/RikkaApps/Shizuku/releases/latest), затем установите сопряжение и запустите службу Shizuku.
Затем установите [aShell](https://f-droid.org/packages/in.sunilpaulmathew.ashell/). Откройте его, разрешите доступ к Shizuku и используйте нужную ADB команду.

Чтобы использовать ADB через ПК:  
Включите отладку по USB в настройках Для разработчиков.

Windows:  
Затем скачайте [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) на ПК и распакуйте в C:\adb.  
Подключите телефон к ПК через USB-кабель, откройте cmd и выполните: ```cd C:\adb```, затем ```adb devices```, разрешите доступ на экране телефона. Затем используйте нужную ADB команду.

Linux:  
Затем скачайте [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) на ПК и распакуйте в ~/adb.  
Подключите телефон к ПК через USB-кабель, откройте терминал и выполните: ```cd ~/adb```, затем ```./adb devices```, и разрешите доступ на экране телефона. Затем используйте нужную ADB команду.

</details>

<details>
  
<summary>Как установить без ADB</summary>
&ensp;  

Или если не хотите использовать ADB, то просто отключите приложения Google Play (Маркет) и Сервисы Google Play в [настройках приложений на вашем телефоне](https://pofesk0.github.io/open-app-settings-proxy.html) на время установки. Но это сработает только если блокировка установки не на уровне Android. А она может быть на уровне Android, например если приложение слишком устаревшее для текущей системы, а не только для Play-Protect. Или если в будущем Google закрутит гайки. Тогда вам придется вернуться к варианту с ADB.

Если вы все ещё не можете установить и причина - устаревший target SDK (он здесь специально низкий из-за изменения политик в новых SDK по отношению к wipe-data), то тогда используйте Lite версию:

https://github.com/pofesk0/DuressKeyboardLite/releases/latest  
https://f-droid.org/packages/duress.keyboard.lite/

Она имеет более высокий target SDK, но меньше функций и сброс там не удаляет FRP, поэтому не привязывайте бекапы к Google аккаунтам (очень рекомендую). Ведь их id могут остаться после сброса, если у вас в телефоне есть FRP. 
&ensp;

</details>

</details>

</details>
