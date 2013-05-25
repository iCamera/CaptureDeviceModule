# CaptureDeviceModule (android)

## 下準備

1. [Java Development Tools][1] を Titanium Studio にインストールします。
1. [Android NDK][2] をインストールします。
1. Titanium Studio の File > Import... → Existing Mobile Project →
[Next] → このファイルがある android フォルダを選択 → [Finish]
1. `build.properties.sample` を `build.properties` に、 `classpath.sample` を `.classpath` にコピーして、パス
を自分の環境に合わせます。`~` は使えないようです。

[1]: http://docs.appcelerator.com/titanium/latest/#!/guide/Installing_the_Java_Development_Tools
[2]: http://docs.appcelerator.com/titanium/latest/#!/guide/Installing_the_Android_NDK

## ビルド

1. 画面左側 App Explorer の最上部、茶色い箱のアイコンを押して、Package - Android Module を選択します。
1. パッケージ先として Mobile Project: Blink を選び、Finish します。
1. Blink 側の modules/android/jp.dividual.capturedevice/ にビルドの成果物が展開されます。

### ビルドするとエラーが出るが、ログが足りない時

android フォルダで `ant` を実行すると、より詳細なログが見られます。
