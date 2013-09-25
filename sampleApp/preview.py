# -*- coding: utf-8 -*-

import commands, os

result = os.system("../iphone/build.py")
if result != 0:
	print( "モジュールのビルドに失敗しました。" )
	exit()


os.system("cp ../iphone/jp.dividual.capturedevice-iphone-1.0.zip .")
result = os.system('titanium build -p ios -b -T device -D development -V "Koichi Yamamoto (F5E22YQX2X)" -P D8B5ACBD-7964-495A-8774-766D1B013F09')
if result != 0:
	print( "ビルド失敗しました。" )
	exit()

result = os.system('./transporter_chief.rb -v build/iphone/build/Debug-iphoneos/CaptureDeviceApp.app')
if result != 0:
	print( "転送失敗しました。リトライするには ./transporter_chief.rb -v build/iphone/build/Debug-iphoneos/CaptureDeviceApp.app" )
	os.system('terminal-notifier -message "デバイスへのインストールに失敗しました"')
	exit()

os.system('terminal-notifier -message "デバイスへのインストールが完了しました"')

