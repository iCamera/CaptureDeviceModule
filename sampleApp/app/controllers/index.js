"use strict"

var testmodule = require('jp.dividual.capturedevice');

var rootSection = Ti.UI.createListSection();
$.listView.setSections( [rootSection] );


// var irisImages = []
// for( var i=1; i<10; i++ ){
// 	irisImages.push( "/images/iris/irisOpenAnime000"+ i +".png" );
// }
// // irisImages = ["/images/cat.jpg", "/images/cat300.jpg", "/images/cat240320.jpg"]
// trace( irisImages )
// // $.iris_iv.duration = 30
// $.iris_iv.images = irisImages;
// $.iris_iv.start();



function createThumbViewTemplate( objectId ){
	return {
		type: 'Ti.UI.ImageView', // Use an image view for the image
		bindId: 'pic'+ objectId,           // Maps to a custom pic property of the item data
		properties: {            // Sets the image view  properties
		    width: '75dp',
		    height: '75dp',
		    top:"4dp",
		    left: "4dp",
		},
	}
}
var myTemplate = {
	properties: {
	    height: '79dp',
	    selectionStyle: Titanium.UI.iPhone.ListViewCellSelectionStyle.NONE,
	},
    childTemplates: [
    	{
    		type: 'Ti.UI.View',
    		properties: {
    			layout: "horizontal",
    		},
    		childTemplates: [
	    		createThumbViewTemplate( 1 ),
	    		createThumbViewTemplate( 2 ),
	    		createThumbViewTemplate( 3 ),
	    		createThumbViewTemplate( 4 ),
    		],
    	}
    ]
};

$.listView.templates = { 'temple': myTemplate },
$.listView.defaultItemTemplate = 'temple'

$.listView.addEventListener('itemclick', function(e){
	dump(e)
    // var item = section.getItemAt(e.itemIndex);
});



Alloy.Collections.Entries.on( "add", function( model, collenction ){
	trace("add")
	if( !rootSection.items || rootSection.items.length==0 ){
		updateListItems()
		return;
	}
	trace( rootSection.items[(rootSection.items.length-1)].thumbCount )
	if( rootSection.items[(rootSection.items.length-1)].thumbCount==4 ){
		// 行が追加されるタイミングに section.setItems すると画面が乱れるので、素直に appendItems する
		trace( "行を追加します" )
		dump(model)
		var data = [];
		var listItem = {
			thumbCount: 1,
			pic1: {
				image: model.get("imagePath"),
				visible: true,
			},
			pic2: {visible: false},
			pic3: {visible: false},
			pic4: {visible: false},
		}
		data.push( listItem );
		rootSection.appendItems( data, {animated:false} )
	} else {
		updateListItems()
	}

	// 最下段にスクロール
	var lastSectionId = $.listView.sectionCount - 1;
	var lastSection = $.listView.sections[lastSectionId]
	var lastItemId = lastSection.items.length - 1
	trace( "最下段にスクロールします。"+ lastSectionId +"/"+ lastItemId )
	$.listView.scrollToItem( lastSectionId, lastItemId )

} )

Alloy.Collections.Entries.fetch();
updateListItems()

function updateListItems(){
	trace( "updateListItems!!!" )
	trace( Alloy.Collections.Entries.models.length )

	var data = [];
	for (var i = 0; i < Alloy.Collections.Entries.models.length; i++) {
		if( i%4==0 ){
			var thumbCount = 0
			var listItem = {}
			if( Alloy.Collections.Entries.models[i] ){
				listItem.pic1 = {
					image: Alloy.Collections.Entries.models[i].get("imagePath"),
					visible: true,
				}
				thumbCount++
			} else {
				listItem.pic1 = { visible: false }
			}
			if( Alloy.Collections.Entries.models[i+1] ){
				listItem.pic2 = {
					image: Alloy.Collections.Entries.models[i+1].get("imagePath"),
					visible: true,
				}
				thumbCount++
			} else {
				listItem.pic2 = { visible: false }
			}
			if( Alloy.Collections.Entries.models[i+2] ){
				listItem.pic3 = {
					image: Alloy.Collections.Entries.models[i+2].get("imagePath"),
					visible: true,
				}
				thumbCount++
			} else {
				listItem.pic3 = { visible: false }
			}
			if( Alloy.Collections.Entries.models[i+3] ){
				listItem.pic4 = {
					image: Alloy.Collections.Entries.models[i+3].get("imagePath"),
					visible: true,
				}
				thumbCount++
			} else {
				listItem.pic4 = { visible: false }
			}
			listItem.thumbCount = thumbCount
			data.push( listItem );
		}
	}
	rootSection.setItems( data, {animated:false} )
}








var finder = testmodule.createFinder({
	color:"white",
	left: 0,
	bottom: 0,
	width: "240dp",
	height: "320dp",
});

finder.addEventListener( "click", function(e){
	var horizontal = 1 - ( e.x / e.source.size.width )
	var vertical = ( e.y / e.source.size.height )
	console.log( horizontal +", "+ vertical );
	finder.focusAndExposureAtPoint( {x:vertical, y:horizontal} );
} )

finder.addEventListener( "focusComplete", function(e){
	console.log( "フォーカス完了!" );
} )

finder.addEventListener( "shutter", function(e){
	finder.opacity = 0;
	finder.animate( {opacity:1, duration:200} )
} )


finder.addEventListener( "imageProcessed", function(e){
	// 画像ファイルを一時的に保存
	var image = e.thumbnail;
	var newFile = Ti.Filesystem.getFile( Ti.Filesystem.tempDirectory, guid()+'.jpg' );
	newFile.createFile();
	newFile.write( image );
	trace( String(newFile.size) )
	trace( newFile.nativePath )

	var entry = Alloy.createModel('Entry', {
		title: "hoge",
		imagePath: newFile.nativePath,
	    created_at : new Date()
	});
	Alloy.Collections.Entries.add( entry );// add new model to the global collection
	entry.save();// save the model to persistent storage
} )


function open(){
	finder.start();
	$.camera_view.add( finder )
}

function close(){
	$.camera_view.remove( finder )
	finder.stop();

}

function shutter(){
	finder.takePhoto( {saveToDevice:true} )
}

function changeToFrontCamera(){
	finder.changeToFrontCamera();
}
function changeToBackCamera(){
	finder.changeToBackCamera();
}

function setFlashModeOn(){
	finder.setFlashModeOn();
}
function setFlashModeOff(){
	finder.setFlashModeOff();
}
function setFlashModeAuto(){
	finder.setFlashModeAuto();
}

// Filter the fetched collection before rendering. Don't return the
// collection itself, but instead return an array of models 
// that you would like to render. 
function dataFilter( collection ){
	trace(collection.models.length)
	var out = []
	var len = collection.models.length
	var count = 0
	for( var i=0; i<len; i++ ){
		if( i%4==0 ){
			var _rowData = []
			_rowData.entryModel1 = collection.models[i]
			_rowData.entryModel2 = collection.models[(i+1)]
			_rowData.entryModel3 = collection.models[(i+2)]
			_rowData.entryModel4 = collection.models[(i+3)]
			out.push( _rowData )
		}
	}
	return []
	// return out
	// return collection.where({ done: whereIndex === 1 ? 0 : 1 });
}



$.index.open();









function S4() {
   return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
};
function guid() {
   return (S4()+S4()+'-'+S4()+'-'+S4()+'-'+S4()+'-'+S4()+S4()+S4());
};





















