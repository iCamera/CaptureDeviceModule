# -*- coding: utf-8 -*-

import commands, os

result = os.system("../iphone/build.py")
if result != 0:
	print( "モジュールのビルドに失敗しました。" )
	exit()


os.system("cp ../iphone/jp.dividual.capturedevice-iphone-1.0.zip .")
result = os.system('titanium build -p ios -b -T device -D development -V "Koichi Yamamoto (F5E22YQX2X)" -P 9939BDB5-FEE8-4EF8-81B1-DDF443BC29A1')
if result != 0:
	print( "ビルド失敗しました。" )
	exit()

os.system('./transporter_chief.rb -v build/iphone/build/Debug-iphoneos/CaptureDeviceApp.app')

