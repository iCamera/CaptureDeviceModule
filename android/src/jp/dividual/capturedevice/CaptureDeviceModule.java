/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiContext;

@Kroll.module(name="CaptureDeviceModule", id="jp.dividual.capturedevice")
public class CaptureDeviceModule extends KrollModule
{
	public CaptureDeviceModule(TiContext tiContext) {
		super(tiContext);
	}
}
