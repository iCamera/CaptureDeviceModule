/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013年 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "JpDividualCapturedeviceFinder.h"
#import "JpDividualCapturedeviceFinderProxy.h"

@implementation JpDividualCapturedeviceFinder

- (void)makeRootViewFirstResponder{
    NSLog(@"makeRootViewFirstResponder");
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds {
	NSLog( @"frameSizeChanged %@ %@", NSStringFromCGRect(frame), NSStringFromCGRect(bounds) );
    [(JpDividualCapturedeviceFinderProxy*)self.proxy frameSizeChanged:frame bounds:bounds];
}


-(void)dealloc{
    NSLog( @"CapturedeviceFinder dealloc" );
    [super dealloc];
}


/*
 
 -(void)dealloc{
 RELEASE_TO_NIL(square);
 [super dealloc];
 }
 
 -(UIView*)square{
 if (square==nil){
 square = [[UIView alloc] initWithFrame:[self frame]];
 [self addSubview:square];
 }
 return square;
 }
 
 -(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds {
 if (square!=nil){
 [TiUtils setView:square positionRect:bounds];
 }
 }
 
 -(void)setColor_:(id)color{
 UIColor *c = [[TiUtils colorValue:color] _color];
 UIView *s = [self square];
 s.backgroundColor = c;
 }
 */



@end
