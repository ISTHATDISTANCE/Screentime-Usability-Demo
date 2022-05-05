# Screentime-Usability-Demo
This demo is an app that manipluates some common settings of Android phones. Now the settings include brightness, volume, animation scale, system dark mode and color mode.

## Installation
1. Clone the repo and open it with [[Android Studio](https://developer.android.com/studio)].
2. Root an phone on API level 23 through 28. An emualtor is recommended.
3. Run the app on the phone or emulator. Follow the popup instructions to grant the permission.

## Note
- Brightness needs WRITE_SETTINGS permission, which we will pop up an alert to ask for your grant.
- Volume is a system service. It does not require any permission.
- Animation scale is a [[global setting](https://developer.android.com/reference/android/provider/Settings.Global)], which can only be adjusted in developer mode. So root permission is needed.
- System dark mode is a system service, but it can only be changed freely on API level 23 through 28. See the issue [[here](https://issuetracker.google.com/issues/173628055?pli=1)]
- System color mode is a system hidden setting. It does not work above API level 28, too.

## Demo
https://drive.google.com/file/d/1s1y44Pq7wQosgV0i6uYcuX6hGwnRh14a/view?usp=sharing
