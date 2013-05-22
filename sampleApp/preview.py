# -*- coding: utf-8 -*-

import commands, os

result = os.system("../iphone/build.py")
if result != 0:
	print( "モジュールのビルドに失敗しました。" )
	exit()


os.system("cp ../iphone/jp.dividual.capturedevice-iphone-1.0.zip .")
result = os.system('titanium build -p ios -b -T device -D development -V "Koichi Yamamoto (F5E22YQX2X)" -P 5F348C86-D17F-4A38-8713-17A811040EC4')
if result != 0:
	print( "ビルド失敗しました。" )
	exit()

os.system('./transporter_chief.rb -v build/iphone/build/Debug-iphoneos/CaptureDeviceApp.app')

