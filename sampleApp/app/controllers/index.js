// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.

trace = Ti.API.info

$.win.open()

// TODO: write your module tests here
var TiCamera = require('be.k0suke.ticamera');

var cameraView = TiCamera.createView({
	width: 320,
	height: 548,
	backgroundColor: '#000',
	videoQuality: TiCamera.QUALITY_MEDIUM,
	cameraPosition: TiCamera.hasBackCamera() ? TiCamera.CAMERA_BACK : TiCamera.CAMERA_FRONT,
	frameDuration: 16
});
$.win.add(cameraView);

cameraView.startCamera()

cameraView.addEventListener( "imageProcessed", function(e){
	trace( "_onImageProcessed" )
	trace( "e.content.length = "+ Math.floor(e.content.length/1000) +"KB" )
	trace( "e.thumbnail.length = "+ e.thumbnail.length +"Bytes" )

	// 画像ファイルを一時的に保存
	var image = e.thumbnail;
	var newFile = Ti.Filesystem.getFile( Ti.Filesystem.tempDirectory, Ti.Platform.createUUID() +'.jpg' );
	newFile.createFile();
	newFile.write( image );
	trace( String(newFile.size) )
	trace( newFile.nativePath )

	preview.image = newFile.nativePath;
} )




var camera = Ti.UI.createButton({
	top: 64,
	left: 10,
	width: Ti.UI.SIZE,
	height: 44,
	title: 'toggle camera'
});
$.win.add(camera);

camera.addEventListener('click', function(){
	cameraView.toggleCamera();
});

var torch = Ti.UI.createButton({
	top: 64,
	right: 10,
	width: Ti.UI.SIZE,
	height: 44,
	title: 'toggle torch'
});
$.win.add(torch);

torch.addEventListener('click', function(){
	if (cameraView.isBackCamera()) {
		cameraView.toggleTorch();
	} else {
		alert('Do not use toggleTorch method, in front camera mode');
	}
});

var recording = Ti.UI.createButton({
	top: 10,
	right: 10,
	width: Ti.UI.SIZE,
	height: 44,
	title: 'recording'
});
$.win.add(recording);

var isRecording = false;
recording.addEventListener('click', function(){
	if (isRecording) {
		cameraView.stopRecording({
			saveToPhotoGallery: save.getValue(),	// default false
			recordingSound: shutter.getValue(),		// default true
			success: function(e){
				var player = Ti.Media.createVideoPlayer({
					media: e.media,
					autoplay: true,
					mediaControlStyle: Ti.Media.VIDEO_CONTROL_FULLSCREEN,
					scalingMode: Ti.Media.VIDEO_SCALING_ASPECT_FIT
				});
				$.win.add(player);

				player.addEventListener('complete', function(){
					$.win.remove(player);
				});
			},
			error: function(e){
				console.log(e);
			}
		});
		recording.setTitle('recording');
		isRecording = false;
	} else {
		isRecording = true;
		recording.setTitle('stop');

		cameraView.startRecording({
			recordingSound: shutter.getValue()		// default true
		});
	}
});

var capture = Ti.UI.createButton({
	top: 10,
	left: 10,
	width: Ti.UI.SIZE,
	height: 44,
	title: 'photo'
});
$.win.add(capture);

capture.addEventListener('click', function(){
	console.log("capture!!!");
	cameraView.takePicture({
		saveToPhotoGallery: save.getValue(),	// default false
		shutterSound: shutter.getValue(),		// default true
		success: function(e){
			console.log(e);
			console.log('width: ' + e.media.width);
			console.log('height: ' + e.media.height);
			console.log('mime: ' + e.media.mime);
			preview.setImage(e.media);
		},
		error: function(e){
			console.log(e);
		}
	});
});

var interval = Ti.UI.createButton({
	top: 10,
	width: Ti.UI.SIZE,
	height: 44,
	title: 'interval'
});
$.win.add(interval);

cameraView.addEventListener('interval', function(e){
	preview.setImage(e.media);
});

var isInterval = false;
interval.addEventListener('click', function(){
	if (isInterval) {
		isInterval = false;
		interval.setTitle('interval');
		cameraView.stopInterval();
	} else {
		isInterval = true;
		interval.setTitle('stop');

		cameraView.startInterval({
			saveToPhotoGallery: save.getValue(),	// default false
			shutterSound: shutter.getValue(),		// default true
			intervalDelay: 1000
		});
	}
});

var save = Ti.UI.createSwitch({
	bottom: 10,
	left: 10,
	value: false
});
$.win.add(save);

var shutter = Ti.UI.createSwitch({
	bottom: 44,
	left: 10,
	value: false
});
$.win.add(shutter);

var sepia = Ti.UI.createSwitch({
	bottom: 10,
	right: 10,
	value: false
});
$.win.add(sepia);

sepia.addEventListener('change', function(){
	cameraView.setSepia(sepia.getValue());
});

var preview = Ti.UI.createImageView({
	bottom: 10,
	width: 48,
	height: 64
});
$.win.add(preview);