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
AVCaptureDevice* captureDevice;
AVCaptureSession* captureSession;
AVCaptureVideoPreviewLayer* previewLayer;
AVCaptureStillImageOutput* imageOutput;
AVCaptureDeviceInput* frontFacingCameraDeviceInput;// 前面カメラ
AVCaptureDeviceInput* backFacingCameraDeviceInput;// 背面カメラ



-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds {
	NSLog( @"CapturedeviceFinderProxy frameSizeChanged in proxy %@ %@", NSStringFromCGRect(frame), NSStringFromCGRect(bounds) );
    previewLayer.frame = bounds;
}


-(void)start:(id)args{
	NSLog( @"CapturedeviceFinderProxy started" );
}

-(void)_updateInputDevice{
    // カメラを取得して初期化
    NSError *error = nil;
    NSArray *devices = [AVCaptureDevice devices];
    for (AVCaptureDevice *device in devices) {
        NSLog(@"Device name: %@", [device localizedName]);
        if ([device hasMediaType:AVMediaTypeVideo]) {
            if( [device position] == AVCaptureDevicePositionBack ){
                NSLog(@"Device position : back");
                backFacingCameraDeviceInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
            } else {
                NSLog(@"Device position : front");
                frontFacingCameraDeviceInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
            }
        }
    }
}


// フロントカメラに切り替え
-(void)changeToFrontCamera:(id)args{
    [self _updateInputDevice];

    [captureSession beginConfiguration];
    [captureSession removeInput:captureSession.inputs[0]];
    [captureSession addInput:frontFacingCameraDeviceInput];
    [captureSession commitConfiguration];
}

// バックカメラに切り替え
-(void)changeToBackCamera:(id)args{
    [self _updateInputDevice];
    
    [captureSession beginConfiguration];
    [captureSession removeInput:captureSession.inputs[0]];
    [captureSession addInput:backFacingCameraDeviceInput];
    [captureSession commitConfiguration];
}


// 露出とフォーカスを指定した座標( 0〜1.0 )に合わせる
- (void)focusAndExposureAtPoint:(id)args{
    NSLog( @"CapturedeviceFinderProxy focusAndExposureAtPoint" );
    
    // JavaScript からわたってきたパラメータを解釈
    ENSURE_SINGLE_ARG( args, NSDictionary );
    NSLog( @"%@", args );
    NSNumber* px = [args objectForKey:@"x"];
    NSNumber* py = [args objectForKey:@"y"];
    CGPoint point = CGPointMake( px.floatValue, py.floatValue );
    
    AVCaptureDevice *device = captureDevice;
    if ([device isFocusPointOfInterestSupported] && [device isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
        NSError *error;
        if ([device lockForConfiguration:&error]) {
            // フォーカス
            [device setFocusPointOfInterest:point];
            [device setFocusMode:AVCaptureFocusModeAutoFocus];
            
            // 露出
            [device setExposurePointOfInterest:point];
            [device setExposureMode:AVCaptureExposureModeContinuousAutoExposure];
            
            [device unlockForConfiguration];
        }
    }
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context{
    NSLog( @"CapturedeviceFinderProxy observeValueForKeyPath:%@", keyPath );
    if ([keyPath isEqual:@"adjustingFocus"]) {
        if ([[change objectForKey:NSKeyValueChangeNewKey] boolValue] == NO) {
            // イベント発行
            [self fireEvent:@"focusComplete" withObject:nil];
        }
    }
}




-(void)takePhoto:(id)args{
    NSLog( @"CapturedeviceFinderProxy takePhoto" );
    
    // JavaScript からわたってきたパラメータを解釈
    ENSURE_SINGLE_ARG( args, NSDictionary );
    NSLog( @"%@", args );
    NSNumber* saveToDevice = [args objectForKey:@"saveToDevice"];
    
	AVCaptureConnection *connection = [[imageOutput connections] lastObject];
	[imageOutput captureStillImageAsynchronouslyFromConnection:connection
                                             completionHandler:^( CMSampleBufferRef imageDataSampleBuffer, NSError *error ){
                                                 NSLog( @"シャッターを切りました" );
                                                 
                                                 // イベント発行
                                                 [self fireEvent:@"shutter" withObject:nil];
                                                 
                                                 NSData *data = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
                                                 UIImage *image = [UIImage imageWithData:data];
                                                 
                                                 // 送信用データ(縦852pxサイズ)を作成
                                                 UIImage* content_img = [self resizeImage:image rect:CGRectMake(0, 0, 640, 852)];
                                                 NSData* content_data = UIImageJPEGRepresentation( content_img, 0.8 );
                                                 TiBlob* content_blob = [[[TiBlob alloc] initWithData:content_data mimetype:@"image/jpeg"] autorelease];
                                                 
                                                 // サムネイルのjpegデータを作成
                                                 UIImage* thumbnail_img = [self resizeImage:content_img rect:CGRectMake(0, 0, 150, 200)];
//                                                 UIImage* thumbnail_img = [self imageByScalingAndCropping:content_img ForSize:CGSizeMake(150, 150)];
                                                 NSData *thumbnail_data = UIImageJPEGRepresentation( thumbnail_img, 0.8 );
                                                 TiBlob* thumbnail_blob = [[[TiBlob alloc] initWithData:thumbnail_data mimetype:@"image/jpeg"] autorelease];
                                                 
                                                 // イベント発行
                                                 NSDictionary *dic = @{@"content":content_blob, @"thumbnail":thumbnail_blob, @"key2":@"value2"};
                                                 [self fireEvent:@"imageProcessed" withObject:dic];
                                                 
                                                 if( [saveToDevice isEqualToNumber:@1] ){
                                                     ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
                                                     [library writeImageToSavedPhotosAlbum:image.CGImage orientation:image.imageOrientation completionBlock:^(NSURL *assetURL, NSError *error){}];
                                                 }
                                             }
     ];
}



-(void)viewDidAttach{
    NSLog( @"CapturedeviceFinderProxy viewDidAttach" );
    //    self.view.backgroundColor = [UIColor redColor];
    
    // カメラを取得して初期化
    NSError *error = nil;
    
    captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    [captureDevice addObserver:self forKeyPath:@"adjustingFocus" options:NSKeyValueObservingOptionNew context:nil];
    AVCaptureDeviceInput *videoInput = [AVCaptureDeviceInput deviceInputWithDevice:captureDevice error:&error];

    // セッション初期化
    captureSession = [[AVCaptureSession alloc] init];
    [captureSession beginConfiguration];
    [captureSession addInput:videoInput];
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
    NSLog( @"CapturedeviceFinderProxy viewWillAttach" );
}

-(void)willChangeLayout{
    NSLog( @"CapturedeviceFinderProxy willChangeLayout" );
}

-(void)willChangePosition{
    NSLog( @"CapturedeviceFinderProxy willChangePosition" );
}

-(void)willChangeSize{
    NSLog( @"CapturedeviceFinderProxy willChangeSize" );    
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



- (UIImage*)imageByScalingAndCropping:(UIImage*)image ForSize:(CGSize)targetSize {
    UIImage *sourceImage = image;
    UIImage *newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = targetSize.width;
    CGFloat targetHeight = targetSize.height;
    CGFloat scaleFactor = 0.0;
    CGFloat scaledWidth = targetWidth;
    CGFloat scaledHeight = targetHeight;
    CGPoint thumbnailPoint = CGPointMake(0.0,0.0);
    
    if (CGSizeEqualToSize(imageSize, targetSize) == NO){
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;
        
        if (widthFactor > heightFactor)   {
            scaleFactor = widthFactor; // scale to fit height
        } else {
            scaleFactor = heightFactor; // scale to fit width
        }
        
        scaledWidth  = width * scaleFactor;
        scaledHeight = height * scaleFactor;
        
        // center the image
        if (widthFactor > heightFactor){
            thumbnailPoint.y = (targetHeight - scaledHeight) * 0.5;
        }else{
            if (widthFactor < heightFactor){
                thumbnailPoint.x = (targetWidth - scaledWidth) * 0.5;
            }
        }
    }
    
    UIGraphicsBeginImageContext(targetSize); // this will crop
    
    CGRect thumbnailRect = CGRectZero;
    thumbnailRect.origin = thumbnailPoint;
    thumbnailRect.size.width  = scaledWidth;
    thumbnailRect.size.height = scaledHeight;
    
    [sourceImage drawInRect:thumbnailRect];
    
    newImage = UIGraphicsGetImageFromCurrentImageContext();
    
    if(newImage == nil){
        NSLog(@"could not scale image");
    }
    
    //pop the context to get back to the default
    UIGraphicsEndImageContext();
    return newImage;
}




@end
