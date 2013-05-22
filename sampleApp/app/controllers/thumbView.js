var objId;

exports.update = function( entry, _objId ){
	objId = _objId;
	if( !entry ){
		$.view.visible = false
		return;
	}

	$.imageView.image = entry.get("imagePath")

}

function thumbClicked(e){
	$.trigger( 'click', objId ); 
}