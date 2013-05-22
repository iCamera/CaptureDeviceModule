// dumpObj(arguments[0])

var rowData = arguments[0].$model

var entry1 = rowData.entryModel1
var entry2 = rowData.entryModel2
var entry3 = rowData.entryModel3
var entry4 = rowData.entryModel4

$.thumbView1.update( entry1, 1 )
$.thumbView2.update( entry2, 2 )
$.thumbView3.update( entry3, 3 )
$.thumbView4.update( entry4, 4 )



function thumbClicked( e ){
	var entryModel;
	switch( e ){
		case 1:
			entryModel = entry1
			break;
		case 2:
			entryModel = entry2
			break;
		case 3:
			entryModel = entry3
			break;
		case 4:
			entryModel = entry4
			break;
	}
	if( entryModel ){
		$.row.fireEvent( "thumbClick", {"entryModel":entryModel} )
	}
}
