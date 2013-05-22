exports.definition = {
	config: {
		columns: {
			"title": "string",
		    "imagePath": "string",
		    "created_at": "datetime"
		},
		adapter: {
			// type: "sculeJSAdapter",
			type: "properties",
			collection_name: "Entry"
		}
	},		
	extendModel: function(Model) {		
		_.extend(Model.prototype, {
			// extended functions and properties go here
		});
		
		return Model;
	},
	extendCollection: function(Collection) {		
		_.extend(Collection.prototype, {
			// extended functions and properties go here
		});
		
		return Collection;
	}
}

