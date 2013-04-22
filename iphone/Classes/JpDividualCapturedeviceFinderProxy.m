/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013年 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "JpDividualCapturedeviceFinderProxy.h"
#import "JpDividualCapturedeviceModule.h"
#import "TiUtils.h"
#import <AVFoundation/AVFoundation.h>
#import <AssetsLibrary/AssetsLibrary.h>

@implementation JpDividualCapturedeviceFinderProxy

BOOL captureStarted = false;
AVCaptureVideoPreviewLayer* previewLayer;
AVCaptureStillImageOutput* imageOutput;



-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds {
	NSLog( @"frameSizeChanged in proxy %@ %@", NSStringFromCGRect(frame), NSStringFromCGRect(bounds) );
    previewLayer.frame = bounds;
}


-(void)start:(id)args{
	NSLog( @"started" );
}

// UIView を UIImage に draw
- (UIImage *)drawViewToImage:(UIView *)source_view{
    UIGraphicsBeginImageContext(source_view.frame.size);
    [source_view.layer renderInContext:UIGraphicsGetCurrentContext()];
    UIImage *screenImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return screenImage;
}

//UIImageをリサイズするクラス
- (UIImage*)resizeImage:(UIImage *)img rect:(CGRect)rect{
    UIGraphicsBeginImageContext(rect.size);
    [img drawInRect:rect];
    UIImage* resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return resizedImage;
}

-(void)takePhoto:(id)args{
    NSLog( @"takePhoto" );
	AVCaptureConnection *connection = [[imageOutput connections] lastObject];
	[imageOutput captureStillImageAsynchronouslyFromConnection:connection
                                             completionHandler:^( CMSampleBufferRef imageDataSampleBuffer, NSError *error ){
                                                 NSLog( @"シャッターを切りました" );
                                                 
                                                 NSData *data = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
                                                 UIImage *image = [UIImage imageWithData:data];
                                                 
                                                 // 送信用データ(縦1096pxサイズ)を作成
                                                 UIImage* content_img = [self resizeImage:image rect:self.view.bounds];
                                                 NSData* content_data = UIImageJPEGRepresentation( content_img, 0.1 );
                                                 TiBlob* content_blob = [[[TiBlob alloc] initWithData:content_data mimetype:@"image/jpeg"] autorelease];
                                                 
                                                 // サムネイルのjpegデータを作成
                                                 UIImage* thumbnail_img = [self resizeImage:image rect:self.view.bounds];
                                                 NSData *thumbnail_data = UIImageJPEGRepresentation( thumbnail_img, 0.1 );
                                                 TiBlob* thumbnail_blob = [[[TiBlob alloc] initWithData:thumbnail_data mimetype:@"image/jpeg"] autorelease];
                                                 
                                                 // イベント発行
                                                 NSDictionary *dic = @{@"content":content_blob, @"thumbnail":thumbnail_blob, @"key2":@"value2"};
                                                 [self fireEvent:@"shutter" withObject:dic];
                                                 
                                                 
                                                 ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
                                                 [library writeImageToSavedPhotosAlbum:image.CGImage orientation:image.imageOrientation completionBlock:^(NSURL *assetURL, NSError *error){}];
                                             }
     ];
}

-(void)viewDidAttach{
    NSLog( @"viewDidAttach!!!!!" );
    //    self.view.backgroundColor = [UIColor redColor];
    
    
    AVCaptureDevice *captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    // 入力の初期化
    NSError *error = nil;
    AVCaptureInput *videoInput = [AVCaptureDeviceInput deviceInputWithDevice:captureDevice error:&error];
    if (!videoInput) {
        NSLog(@"ERROR:%@", error);
        return;
    }
    // セッション初期化
    AVCaptureSession* captureSession = [[AVCaptureSession alloc] init];
    [captureSession addInput:videoInput];
    [captureSession beginConfiguration];
    captureSession.sessionPreset = AVCaptureSessionPresetPhoto;
    [captureSession commitConfiguration];
    
    previewLayer = [AVCaptureVideoPreviewLayer layerWithSession:captureSession];
    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    previewLayer.frame = self.view.bounds;
    NSLog( @"%@", NSStringFromCGRect(previewLayer.frame) );
    [self.view.layer insertSublayer:previewLayer atIndex:0];
    
    // 出力の初期化
    imageOutput = [[AVCaptureStillImageOutput alloc] init];
    [captureSession addOutput:imageOutput];
    
    // セッション開始
    [captureSession startRunning];
}



-(void)viewWillAttach{
    NSLog( @"viewWillAttach!!!!!" );
}

-(void)willChangeLayout{
    NSLog( @"willChangeLayout!!!!!" );
}

-(void)willChangePosition{
    NSLog( @"willChangePosition" );
}

-(void)willChangeSize{
    NSLog( @"willChangeSize" );    
}


@end
