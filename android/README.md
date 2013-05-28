# CaptureDeviceModule (Android)

## 下準備

1. ``/path/to/android/sdk/tools/android sdk`` を実行し、Android 4.0 (API 14) の SDK Platform と Google APIs をインストールします。(*)
1. [Java Development Tools][1] を Titanium Studio にインストールします。
1. [Android NDK][2] をインストールします。
1. Titanium Studio の File > Import... → Existing Mobile Project →
[Next] → このファイルがある android フォルダを選択 → [Finish]
1. `build.properties.sample` を `build.properties` に、 `classpath.sample` を `.classpath` にコピーして、パス
を自分の環境に合わせます。`~` は使えないようです。

(*) Android 4.0 以降でのみ使える API を利用するために必要です。

[1]: http://docs.appcelerator.com/titanium/latest/#!/guide/Installing_the_Java_Development_Tools
[2]: http://docs.appcelerator.com/titanium/latest/#!/guide/Installing_the_Android_NDK

## ビルド

1. 画面左側 App Explorer の最上部、茶色い箱のアイコンを押して、Package - Android Module を選択します。
1. パッケージ先として Mobile Project: Blink を選び、Finish します。
1. Blink 側の modules/android/jp.dividual.capturedevice/ にビルドの成果物が展開されます。

### ビルドするとエラーが出るが、ログが足りない時

android フォルダで `ant` を実行すると、より詳細なログが見られます。

### 完全にクリーンするには

Titanium Studio からのクリーンでは生成されたファイルが完全には削除されないので、
まれにビルドが通らなくなることがあります。そういう時は一時ファイル置き場と `build` フォルダの両方を削除します。
一時ファイル置き場のパスは `ant` のログで確認できます。

```
rm -rf ./build/
rm -rf /private/var/folders/vx/3d12m4r91xb9ktx3v6f7gy440000gq/T/ento/android-generated/
````
