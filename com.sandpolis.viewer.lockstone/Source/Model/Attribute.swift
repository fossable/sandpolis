import Foundation

class Attribute {
	var path: String
	var title: String
	var value: String?
	
	init(_ path: String, _ title: String) {
		self.path = path
		self.title = title
	}
}
