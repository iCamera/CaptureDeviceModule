/**
 * BeK0sukeTicameraView.m
 */

#import "BeK0sukeTicameraView.h"
#import <AssetsLibrary/AssetsLibrary.h>

@implementation BeK0sukeTicameraView

BOOL waitingForShutter = NO;
BOOL initialized = NO;// カメラの初期設定が終わっている？
AVCaptureStillImageOutput* stillImageOutput;
AVCaptureDevice *captureDevice;

-(void)dealloc{
    RELEASE_TO_NIL(successPictureCallback);
    RELEASE_TO_NIL(errorPictureCallback);
    RELEASE_TO_NIL(successRecordingCallback);
    RELEASE_TO_NIL(errorRecordingCallback);
#ifndef __i386__
    [self.videoSession stopRunning];
#endif
    [super dealloc];
}

-(id)init{
    self = [super init];
    return self;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds{
#ifndef __i386__
    if (self.videoPreview == nil){
        self.videoPreview = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, bounds.size.width, bounds.size.height)];
        [self addSubview:self.videoPreview];
    }
#endif
}

-(void)dispatchCallback:(NSArray*)args{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	NSString *type = [args objectAtIndex:0];
	id object = [args objectAtIndex:1];
	id listener = [args objectAtIndex:2];
	[self.proxy _fireEventToListener:type withObject:object listener:listener thisObject:nil];
	[pool release];
}

-(void)startCamera:(id)args{
    NSLog( @"TicameraView setupAVCapture" );
    
#ifndef __i386__
    NSError *error = nil;
    
    if( initialized == NO ){
        
        captureDevice = [self deviceWithPosition: [TiUtils intValue:[self.proxy valueForKey:@"cameraPosition"] def:AVCaptureDevicePositionBack]];
        [captureDevice addObserver:self forKeyPath:@"adjustingFocus" options:NSKeyValueObservingOptionNew context:nil];
        self.videoInput = [[AVCaptureDeviceInput alloc] initWithDevice:captureDevice error:&error];
        
        if (!self.videoInput){
            NSLog(@"[ERROR] %@", error);
            return;
        }
        
        self.videoSession = [[AVCaptureSession alloc] init];
        [self.videoSession addInput:self.videoInput];
        
        [self.videoSession beginConfiguration];
        //self.videoSession.sessionPreset = [TiUtils intValue:[self.proxy valueForKey:@"videoQuality"] def:AVCaptureSessionPresetMedium];
        self.videoSession.sessionPreset = AVCaptureSessionPresetPhoto;
        [self.videoSession commitConfiguration];
        
        [captureDevice addObserver:self
                        forKeyPath:@"adjustingExposure"
                           options:NSKeyValueObservingOptionNew
                           context:nil];
        
        // 画像への出力を作成し、セッションに追加
        stillImageOutput = [[AVCaptureStillImageOutput alloc] init];
        [self.videoSession addOutput:stillImageOutput];
        [self.videoSession addObserver:self forKeyPath:@"running" options:NSKeyValueObservingOptionNew context:nil];
        
        // キャプチャーセッションから入力のプレビュー表示を作成
        AVCaptureVideoPreviewLayer *captureVideoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:self.videoSession];
        captureVideoPreviewLayer.frame = self.bounds;
        captureVideoPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
        
        // レイヤーをViewに設定
        CALayer *previewLayer = self.layer;
        previewLayer.masksToBounds = YES;
        [previewLayer addSublayer:captureVideoPreviewLayer];
        
        isCameraInputOutput = YES;
        initialized = YES;
    }
    
    if( [self.videoSession isRunning]==YES ){
        NSLog( @"すでにカメラ起動中です" );
        return;
    }
    
    [self.videoSession startRunning];
    
#endif
}

-(void)stopCamera:(id)args{
    if( [self.videoSession isRunning]==NO ){
        NSLog( @"すでにカメラ終了してます" );
        return;
    }
    [self.videoSession stopRunning];
}



-(void)setupAVCapture{
}



#ifndef __i386__
-(AVCaptureDevice *)deviceWithPosition:(AVCaptureDevicePosition) position{
    NSArray *Devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *Device in Devices){
        if ([Device position] == position){
            return Device;
        }
    }
    return nil;
}
#endif







-(void)takePicture:(id)args{
    NSLog( @"objc takePhoto" );

    if( waitingForShutter ){
        NSLog( @"前回のシャッターが切れるのを待っているので新しく撮影処理は開始しません" );
        return;
    }

    waitingForShutter = YES;

    // JavaScript からわたってきたパラメータを解釈
    ENSURE_SINGLE_ARG( args, NSDictionary );
    NSLog( @"%@", args );
    NSNumber* saveToDevice = [args objectForKey:@"saveToDevice"];

	AVCaptureConnection *connection = [[stillImageOutput connections] lastObject];
	[stillImageOutput captureStillImageAsynchronouslyFromConnection:connection
                                                  completionHandler:^( CMSampleBufferRef imageDataSampleBuffer, NSError *error ){
                                                      NSLog( @"シャッターを切りました" );
                                                      waitingForShutter = NO;

                                                      // イベント発行
                                                      [self.proxy fireEvent:@"shutter" withObject:nil];

                                                      NSData *data = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
                                                      UIImage *image = [UIImage imageWithData:data];

                                                      // フロントカメラならフリップ
                                                      TiBlob* original_blob;
//                                                      if( [self _isFrontCamera] ){
//                                                          NSLog( @"フリップします" );
//                                                          image = [UIImage imageWithCGImage:image.CGImage scale:1.0 orientation: UIImageOrientationLeftMirrored];
//                                                          NSData* content_data = UIImageJPEGRepresentation( image, 0.8 );
//                                                          original_blob = [[[TiBlob alloc] initWithData:content_data mimetype:@"image/jpeg"] autorelease];
//                                                      } else {
//                                                          original_blob = [[[TiBlob alloc] initWithData:data mimetype:@"image/jpeg"] autorelease];
//                                                      }
                                                      original_blob = [[[TiBlob alloc] initWithData:data mimetype:@"image/jpeg"] autorelease];

                                                      // 送信用データ(縦1136pxサイズ)を作成
//                                                      UIImage* content_img = [self resizeImage:image rect:CGRectMake(0, 0, 852, 1136)];// 重い
//                                                      NSData* content_data = UIImageJPEGRepresentation( content_img, 0.8 );
//                                                      TiBlob* content_blob = [[[TiBlob alloc] initWithData:content_data mimetype:@"image/jpeg"] autorelease];

                                                      // サムネイルのjpegデータを作成
                                                      UIImage* thumbnail_img = [self imageByScalingAndCropping:image ForSize:CGSizeMake(150, 150)];
                                                      NSData *thumbnail_data = UIImageJPEGRepresentation( thumbnail_img, 0.8 );
                                                      TiBlob* thumbnail_blob = [[[TiBlob alloc] initWithData:thumbnail_data mimetype:@"image/jpeg"] autorelease];

                                                      // イベント発行
                                                      NSDictionary *dic = @{@"original":original_blob, @"content":thumbnail_blob, @"thumbnail":thumbnail_blob, @"key2":@"value2"};
                                                      [self.proxy fireEvent:@"imageProcessed" withObject:dic];

                                                      if ([TiUtils boolValue:[args valueForKey:@"saveToPhotoGallery"] def:NO]){
                                                          ALAssetsLibrary *library = [[[ALAssetsLibrary alloc] init] autorelease];
                                                          [library writeImageToSavedPhotosAlbum:image.CGImage orientation:image.imageOrientation completionBlock:^(NSURL *assetURL, NSError *error){}];
                                                      }
                                                  }
     ];
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


// flash オン
-(void)setFlashModeOn:(id)args{
    [self _setFlashMode:AVCaptureFlashModeOn];
}
// flash オフ
-(void)setFlashModeOff:(id)args{
    [self _setFlashMode:AVCaptureFlashModeOff];
}
// flash オート
-(void)setFlashModeAuto:(id)args{
    [self _setFlashMode:AVCaptureFlashModeAuto];
}

// flashモード設定 内部処理
-(void)_setFlashMode:(AVCaptureFlashMode)flashMode{
    if( [captureDevice isFlashModeSupported:flashMode] ){
        NSError *error;
        if( [captureDevice lockForConfiguration:&error] ){
            captureDevice.flashMode = flashMode;
            [captureDevice unlockForConfiguration];
        }
    }
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



// フォーカス完了時にイベント発行
- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context{
    NSLog( @"CapturedeviceFinderProxy observeValueForKeyPath:%@ change=%@", keyPath, change );
    if ([keyPath isEqual:@"adjustingFocus"]) {
        if ([[change objectForKey:NSKeyValueChangeNewKey] boolValue] == YES ){
            [self.proxy fireEvent:@"focusStart" withObject:nil];
        } else {
            [self.proxy fireEvent:@"focusComplete" withObject:nil];
        }
    } else if( [keyPath isEqual:@"running"] ){
        if( [[change objectForKey:NSKeyValueChangeNewKey] boolValue] == YES ){
            [self.proxy fireEvent:@"sessionStart" withObject:nil];// カメラ起動完了
        } else {
            [self.proxy fireEvent:@"sessionStop" withObject:nil];// カメラ終了完了
        }
    } else if( [keyPath isEqual:@"adjustingExposure"] ){
        if (adjustingExposure){
            if ([[change objectForKey:NSKeyValueChangeNewKey] boolValue] == NO){
                adjustingExposure = NO;
                AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];

                NSError *error = nil;
                if ([device lockForConfiguration:&error]){
                    [device setExposureMode:AVCaptureExposureModeLocked];
                    [device unlockForConfiguration];
                }
            }
        }
    }
}


//UIImageをリサイズ
- (UIImage*)resizeImage:(UIImage *)img rect:(CGRect)rect{
    UIGraphicsBeginImageContext(rect.size);

    // 補完処理を省略してリサイズを高速化
    //    CGContextRef context = UIGraphicsGetCurrentContext();
    //	CGContextSetInterpolationQuality(context, kCGInterpolationNone);

    [img drawInRect:rect];
    UIImage* resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return resizedImage;
}



























/*
 -(void)takePicture:(id)args
 {
 #ifndef __i386__
 ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary);
 ENSURE_UI_THREAD(takePicture, args);

 successPictureCallback = [args objectForKey:@"success"];
 ENSURE_TYPE_OR_NIL(successPictureCallback, KrollCallback);
 [successPictureCallback retain];

 errorPictureCallback = [args objectForKey:@"error"];
 ENSURE_TYPE_OR_NIL(errorPictureCallback, KrollCallback);
 [errorPictureCallback retain];

 if ([TiUtils boolValue:[args valueForKey:@"shutterSound"] def:YES])
 {
 AudioServicesPlaySystemSound(1108);
 }

 if (self.videoPreview.image)
 {
 if ([TiUtils boolValue:[args valueForKey:@"saveToPhotoGallery"] def:NO])
 {
 UIImageWriteToSavedPhotosAlbum(self.videoPreview.image, self, nil, nil);
 }

 if (successPictureCallback != nil)
 {
 id listener = [[successPictureCallback retain] autorelease];
 NSMutableDictionary *event = [TiUtils dictionaryWithCode:0 message:nil];
 [event setObject:[[[TiBlob alloc] initWithImage:self.videoPreview.image] autorelease] forKey:@"media"];
 [NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"success", event, listener, nil]];
 }
 }
 else
 {
 if (errorPictureCallback != nil)
 {
 id listener = [[errorPictureCallback retain] autorelease];
 NSMutableDictionary *event = [TiUtils dictionaryWithCode:-1 message:@"AVCaptureConnection connect failed"];
 [NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error", event, listener, nil]];
 }
 }
 #endif
 }
 */

-(id)isFrontCamera:(id)args{
    return NUMBOOL([self _isFrontCamera]);
}

-(BOOL)_isFrontCamera{
#ifndef __i386__
    if ([[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo] count] > 1){
        NSError *error = nil;
        AVCaptureDevicePosition position = [[self.videoInput device] position];
        if (position == AVCaptureDevicePositionFront){
            return YES;
        }
    }
    return NO;
#endif
}

-(id)isBackCamera:(id)args
{
#ifndef __i386__
    if ([[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo] count] > 1)
    {
        NSError *error = nil;

        AVCaptureDevicePosition position = [[self.videoInput device] position];
        if (position == AVCaptureDevicePositionFront)
        {
            return NUMBOOL(NO);
        }
    }

    return NUMBOOL(YES);
#endif
}

-(id)isTorch:(id)args
{
#ifndef __i386__
    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    NSError *error = nil;

    if ([device hasTorch] == YES && device.torchMode == AVCaptureTorchModeOn)
    {
        return NUMBOOL(YES);
    }

    return NUMBOOL(NO);
#endif
}

-(void)toggleCamera:(id)args{
#ifndef __i386__
    if ([[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo] count] > 1){
        NSError *error = nil;

        AVCaptureDeviceInput *newVideoInput;
        AVCaptureDevicePosition position = [[self.videoInput device] position];
        if (position == AVCaptureDevicePositionBack){
            newVideoInput = [[AVCaptureDeviceInput alloc] initWithDevice:[self deviceWithPosition:AVCaptureDevicePositionFront] error:&error];
        }else{
            newVideoInput = [[AVCaptureDeviceInput alloc] initWithDevice:[self deviceWithPosition:AVCaptureDevicePositionBack] error:&error];
        }

        if (!newVideoInput){
            NSLog(@"[ERROR] %@", error);
            return;
        }

        [self.videoSession beginConfiguration];
        [self.videoSession removeInput:self.videoInput];

        if ([self.videoSession canAddInput:newVideoInput]){
            [self.videoSession addInput:newVideoInput];
            self.videoInput = newVideoInput;
        }else{
            [self.videoSession addInput:self.videoInput];
        }
        [self.videoSession commitConfiguration];
    }
#endif
}




-(void)toggleTorch:(id)args
{
#ifndef __i386__
    if ([[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo] count] > 1)
    {
        NSError *error = nil;

        AVCaptureDevicePosition position = [[self.videoInput device] position];
        if (position == AVCaptureDevicePositionFront)
        {
            NSLog(@"[ERROR] do not support torch in front camera mode");
            return;
        }
    }

    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    NSError *error = nil;

    if (![device hasTorch])
    {
        NSLog(@"[ERROR] do not support torch in this device");
        return;
    }

    if (device.torchMode == AVCaptureTorchModeOff)
    {
        [device lockForConfiguration:&error];
        device.torchMode = AVCaptureTorchModeOn;
        [device unlockForConfiguration];
    }
    else
    {
        [device lockForConfiguration:&error];
        device.torchMode = AVCaptureTorchModeOff;
        [device unlockForConfiguration];
    }
#endif
}








-(void)startRecording:(id)args
{
#ifndef __i386__
    ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary);
    ENSURE_UI_THREAD(startRecording, args);

    if (isRecording)
    {
        NSLog(@"[WARN] video recording started");
        return;
    }

    recordingFrame = 0;
    self.recordingBuffer = [[NSMutableArray array] mutableCopy];
    NSDateFormatter *format = [[NSDateFormatter alloc] init];
    format.dateFormat = @"yyyyMMddHHmmss";
    recordingUrl = [NSURL fileURLWithPath:[NSString stringWithFormat:@"%@%@_%@%@",
                                           NSTemporaryDirectory(),
                                           @"output",
                                           [format stringFromDate:[NSDate date]],
                                           @".mov"]];
    recordingSize = CGSizeMake([TiUtils intValue:[self.proxy valueForKey:@"width"]],
                               [TiUtils intValue:[self.proxy valueForKey:@"height"]]);

    NSError *error = nil;
    self.recordingWriter = [[AVAssetWriter alloc] initWithURL:recordingUrl fileType:AVFileTypeQuickTimeMovie error:&error];

    if (error != nil)
    {
        NSLog(@"[ERROR] do not recording start");
        return;
    }

    NSDictionary *settings = @{AVVideoCodecKey: AVVideoCodecH264,
                               AVVideoWidthKey: [NSNumber numberWithInt:recordingSize.width],
                               AVVideoHeightKey: [NSNumber numberWithInt:recordingSize.height]};
    self.recordingInput = [AVAssetWriterInput assetWriterInputWithMediaType:AVMediaTypeVideo outputSettings:settings];
    [self.recordingInput setExpectsMediaDataInRealTime:YES];

    CGAffineTransform transform = CGAffineTransformIdentity;
    transform = CGAffineTransformTranslate(transform, recordingSize.width * 0.5, recordingSize.height * 0.5);
    transform = CGAffineTransformRotate(transform , 90 / 180.0f * M_PI);
    transform = CGAffineTransformScale(transform, 1.0, 1.0);
    self.recordingInput.transform = transform;

    NSDictionary *bufferAttributes = @{(id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32ARGB)};
    self.recordingAdaptor = [AVAssetWriterInputPixelBufferAdaptor
                             assetWriterInputPixelBufferAdaptorWithAssetWriterInput:self.recordingInput
                             sourcePixelBufferAttributes:bufferAttributes];
    [self.recordingWriter addInput:self.recordingInput];
    [self.recordingWriter startWriting];
    [self.recordingWriter startSessionAtSourceTime:kCMTimeZero];

    isRecording = YES;

    if ([TiUtils boolValue:[args valueForKey:@"recordingSound"] def:YES])
    {
        AudioServicesPlaySystemSound(1117);
    }
#endif
}

-(void)stopRecording:(id)args{
#ifndef __i386__
    ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary);
    ENSURE_UI_THREAD(stopRecording, args);

    successRecordingCallback = [args objectForKey:@"success"];
  ENSURE_TYPE_OR_NIL(successRecordingCallback, KrollCallback);
  [successRecordingCallback retain];

    errorRecordingCallback = [args objectForKey:@"error"];
  ENSURE_TYPE_OR_NIL(errorRecordingCallback, KrollCallback);
  [errorRecordingCallback retain];

    if (!isRecording)
    {
        NSLog(@"[WARN] video recording not start");
        if (errorRecordingCallback != nil)
        {
            id listener = [[errorRecordingCallback retain] autorelease];
            NSMutableDictionary *event = [TiUtils dictionaryWithCode:-1 message:@"video recording not start"];
            [NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error", event, listener, nil]];
        }
        return;
    }

    if ([TiUtils boolValue:[args valueForKey:@"recordingSound"] def:YES])
    {
        AudioServicesPlaySystemSound(1118);
    }

    dispatch_queue_t queue = dispatch_queue_create("be.k0suke.ticamera.recordingQueue", NULL);

    [self.recordingInput requestMediaDataWhenReadyOnQueue:queue usingBlock:^{
        while ([self.recordingInput isReadyForMoreMediaData])
        {
            if ([self.recordingBuffer count] <= 1)
            {
                [self.recordingInput markAsFinished];
                [self.recordingWriter finishWritingWithCompletionHandler:^{
                    dispatch_async(dispatch_get_main_queue(), ^{
                        if ([TiUtils boolValue:[args valueForKey:@"saveToPhotoGallery"] def:NO])
                        {
                            UISaveVideoAtPathToSavedPhotosAlbum([recordingUrl path], self, nil, nil);
                        }

                        if (successRecordingCallback != nil)
                        {
                            id listener = [[successRecordingCallback retain] autorelease];
                            NSMutableDictionary *event = [TiUtils dictionaryWithCode:0 message:nil];
                            NSData *data = [NSData dataWithContentsOfURL:recordingUrl];
                            [event setObject:[[[TiBlob alloc] initWithData:data mimetype:@"video/quicktime"] autorelease] forKey:@"media"];
                            [NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"success", event, listener, nil]];
                        }
                    });
                }];
                return;
            }

            CVPixelBufferRef buffer = (CVPixelBufferRef)[self pixelBufferFromCGImage:[[self.recordingBuffer objectAtIndex:0] CGImage] size:recordingSize];
            if (buffer)
            {
                if ([self.recordingAdaptor appendPixelBuffer:buffer
                                        withPresentationTime:CMTimeMake(recordingFrame, [TiUtils intValue:[self.proxy valueForKey:@"frameDuration"] def:16])])
                {
                    recordingFrame++;
                }
                else
                {
                    NSLog(@"[ERROR] recording write failed");
                }

                CFRelease(buffer);
                buffer = nil;
            }

            [self.recordingBuffer removeObjectAtIndex:0];
        }
    }];

    isRecording = NO;
#endif
}


-(void)startInterval:(id)args{
}

-(void)intervalFlag:(NSTimer*)timer{
}

-(void)stopInterval:(id)args{
}












#ifndef __i386__
-(CVPixelBufferRef)pixelBufferFromCGImage:(CGImageRef)image size:(CGSize)size
{
    CVPixelBufferRef pxbuffer = NULL;
    CVReturn status = CVPixelBufferPoolCreatePixelBuffer(NULL, self.recordingAdaptor.pixelBufferPool, &pxbuffer);
    NSParameterAssert(status == kCVReturnSuccess && pxbuffer != NULL);

    CVPixelBufferLockBaseAddress(pxbuffer, 0);

    void *pxdata = CVPixelBufferGetBaseAddress(pxbuffer);
    NSParameterAssert(pxdata != NULL);

    CGColorSpaceRef rgbColorSpace = CGColorSpaceCreateDeviceRGB();

    CGContextRef context = CGBitmapContextCreate(pxdata, size.width, size.height, 8, 4 * size.width, rgbColorSpace, kCGImageAlphaPremultipliedFirst);
    NSParameterAssert(context);

    CGContextSetInterpolationQuality(context, kCGInterpolationLow);
    CGContextDrawImage(context, CGRectMake(0, 0, CGImageGetWidth(image), CGImageGetHeight(image)), image);

    CGColorSpaceRelease(rgbColorSpace);
    CGContextRelease(context);

    CVPixelBufferUnlockBaseAddress(pxbuffer, 0);

    return pxbuffer;
}

-(UIImage *)imageFromSampleBuffer:(CMSampleBufferRef)sampleBuffer
{
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);

    CVPixelBufferLockBaseAddress(imageBuffer, 0);

    uint8_t *baseAddress = (uint8_t *)CVPixelBufferGetBaseAddressOfPlane(imageBuffer, 0);
    size_t width = CVPixelBufferGetWidth(imageBuffer);
    size_t height = CVPixelBufferGetHeight(imageBuffer);
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();

    CGContextRef context = CGBitmapContextCreate(baseAddress,
                                                 width,
                                                 height,
                                                 8,
                                                 bytesPerRow,
                                                 colorSpace,
                                                 kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
    CGImageRef imageRef = CGBitmapContextCreateImage(context);
    CGColorSpaceRelease(colorSpace);
    CGContextRelease(context);

    UIGraphicsBeginImageContext(CGSizeMake(height, width));
    context = UIGraphicsGetCurrentContext();
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextRotateCTM(context, -M_PI_2);
    CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
    CGImageRelease(imageRef);

    return image;
}

-(void)captureOutput:(AVCaptureOutput *)captureOutput didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection{
}

-(void)tapDetected:(UITapGestureRecognizer*)tapRecognizer{
    CGPoint point = [tapRecognizer locationInView:tapRecognizer.view];
    CGPoint pointOfInterest = CGPointMake(point.y / [TiUtils floatValue:[self.proxy valueForKey:@"height"]],
                                          1.0 - point.x / [TiUtils floatValue:[self.proxy valueForKey:@"width"]]);

    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    NSError *error = nil;

    if ([device lockForConfiguration:&error]){
        if ([device isFocusPointOfInterestSupported] &&
            [device isFocusModeSupported:AVCaptureFocusModeAutoFocus])
        {
            device.focusPointOfInterest = pointOfInterest;
            device.focusMode = AVCaptureFocusModeAutoFocus;
        }

        if ([device isExposurePointOfInterestSupported] &&
            [device isExposureModeSupported:AVCaptureExposureModeContinuousAutoExposure])
        {
            adjustingExposure = YES;
            device.exposurePointOfInterest = pointOfInterest;
            device.exposureMode = AVCaptureExposureModeContinuousAutoExposure;
        }

        [device unlockForConfiguration];
    }
}


#endif

@end
