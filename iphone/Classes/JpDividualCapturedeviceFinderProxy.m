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
                                                 
                                                 // イベント発行
                                                 [self fireEvent:@"shutter" withObject:nil];
                                                 
                                                 NSData *data = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
                                                 UIImage *image = [UIImage imageWithData:data];
                                                 
                                                 // 送信用データ(縦852pxサイズ)を作成
                                                 UIImage* content_img = [self resizeImage:image rect:CGRectMake(0, 0, 640, 852)];
                                                 NSData* content_data = UIImageJPEGRepresentation( content_img, 0.8 );
                                                 TiBlob* content_blob = [[[TiBlob alloc] initWithData:content_data mimetype:@"image/jpeg"] autorelease];
                                                 
                                                 // サムネイルのjpegデータを作成
//                                                 UIImage* thumbnail_img = [self resizeImage:content_img rect:CGRectMake(0, 0, 150, 150)];
                                                 UIImage* thumbnail_img = [self imageByScalingAndCropping:content_img ForSize:CGSizeMake(150, 150)];
                                                 NSData *thumbnail_data = UIImageJPEGRepresentation( thumbnail_img, 0.8 );
                                                 TiBlob* thumbnail_blob = [[[TiBlob alloc] initWithData:thumbnail_data mimetype:@"image/jpeg"] autorelease];
                                                 
                                                 // イベント発行
                                                 NSDictionary *dic = @{@"content":content_blob, @"thumbnail":thumbnail_blob, @"key2":@"value2"};
                                                 [self fireEvent:@"imageProcessed" withObject:dic];
                                                 
                                                 
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
