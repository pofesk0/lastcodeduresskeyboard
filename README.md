# DuressKeyboard

<details open>
  <summary>🇬🇧 English Description:
  </summary>  &ensp;
  
This is a keyboard implementing the Duress Password feature.  
When used, it can wipe data (reset the phone settings) upon entry of a special pre-configured reset code on the lock screen.  
This app can help if someone ever tries to force you to enter a screen unlock password.
In this case, instead of it, you enter the reset code and press the Enter arrow (⏎).  
Also this app has other security and data reset features.

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
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk && adb shell pm disable-user --user 0 com.android.vending && adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell pm enable com.android.vending ; adb shell rm /data/local/tmp/app.apk
```
&ensp;  
Or if you don't want to use ADB, just disable the Google Play (Store) and Google Play Services apps in [your phone's app settings](https://pofesk0.github.io/open-app-settings-proxy.html) during installation. However, this will only work if the installation block is not at the Android system level. It can be at the Android level, for example, if the app is too outdated for the current system, not just for Play-Protect. Or if Google tightens the restrictions in the future. In that case, you will have to go back to the ADB option.

If you still can't install and the reason is outdated target SDK (it is here intentionally low because of policy changes in new SDKs regarding wipe-data), then use Lite version:

https://github.com/pofesk0/DuressKeyboardLite/releases/latest  
https://f-droid.org/packages/duress.keyboard.lite/

It has higher target SDK, but fewer features and reset there doesn't delete FRP, therefore don't bind backups to Google accounts (strongly recommend). Because their IDs can remain after reset, if your phone has FRP.
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
adb shell cp /storage/emulated/0/Download/app-release.apk /data/local/tmp/app.apk && adb shell pm disable-user --user 0 com.android.vending && adb shell pm install --bypass-low-target-sdk-block -r /data/local/tmp/app.apk ; adb shell pm enable com.android.vending ; adb shell rm /data/local/tmp/app.apk
```
&ensp;  
Или если не хотите использовать ADB, то просто отключите приложения Google Play (Маркет) и Сервисы Google Play в [настройках приложений на вашем телефоне](https://pofesk0.github.io/open-app-settings-proxy.html) на время установки. Но это сработает только если блокировка установки не на уровне Android. А она может быть на уровне Android, например если приложение слишком устаревшее для текущей системы, а не только для Play-Protect. Или если в будущем Google закрутит гайки. Тогда вам придется вернуться к варианту с ADB.

Если вы все ещё не можете установить и причина - устаревший target SDK (он здесь специально низкий из-за изменения политик в новых SDK по отношению к wipe-data), то тогда используйте Lite версию:

https://github.com/pofesk0/DuressKeyboardLite/releases/latest  
https://f-droid.org/packages/duress.keyboard.lite/

Она имеет более высокий target SDK, но меньше функций и сброс там не удаляет FRP, поэтому не привязывайте бекапы к Google аккаунтам (очень рекомендую). Ведь их id могут остаться после сброса, если у вас в телефоне есть FRP. 

</details>

</details>
