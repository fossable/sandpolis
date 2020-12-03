import Foundation

struct Oid<T> {

    let namespace: Int64
    let path: String
    let title: String

    init(_ namespace: Int64, _ path: String, _ title: String) {
        self.namespace = namespace
        self.path = path
        self.title = title
    }
    
    func resolve(_ component: String) -> Oid<T> {
        return self //TODO
    }
}

class InstanceOid {
    static let uuid = Oid<String>.init(3756592246360572888, "/profile//uuid", "UUID")
    static let hostname = Oid<String>.init(3756592246360572888, "/profile//agent/hostname", "Hostname")
    static let osType = Oid<Core_Foundation_OsType>.init(3756592246360572888, "/profile//os_type", "OS Type")
    static let online = Oid<Bool>(3756592246360572888, "/profile//agent/online", "Online")
    static let ipAddress = Oid<String>(3756592246360572888, "/profile//ip_address", "IP Address")
    static let ipLocationCity = Oid<String>(3756592246360572888, "/profile//agent/ip_location/city", "City")
    static let ipLocationContinent = Oid<String>(3756592246360572888, "/profile//agent/ip_location/continent", "Continent")
    static let ipLocationCountry = Oid<String>(3756592246360572888, "/profile//agent/ip_location/country", "Country")
    static let ipLocationCountryCode = Oid<String>(3756592246360572888, "/profile//agent/ip_location/country_code", "Country Code")
    static let ipLocationCurrency = Oid<String>(3756592246360572888, "/profile//agent/ip_location/currency", "Currency")
    static let ipLocationDistrict = Oid<String>(3756592246360572888, "/profile//agent/ip_location/district", "District")
    static let ipLocationIsp = Oid<String>(3756592246360572888, "/profile//agent/ip_location/isp", "ISP")
    static let ipLocationLatitude = Oid<Float32>(3756592246360572888, "/profile//agent/ip_location/latitude", "Latitude")
    static let ipLocationLongitude = Oid<Float32>(3756592246360572888, "/profile//agent/ip_location/longitude", "Longitude")
    static let ipLocationRegion = Oid<String>(3756592246360572888, "/profile//agent/ip_location/region", "Region")
    static let instanceType = Oid<Core_Instance_InstanceType>(3756592246360572888, "/profile//instance_type", "Instance Type")
    static let instanceFlavor = Oid<Core_Instance_InstanceFlavor>(3756592246360572888, "/profile//instance_flavor", "Instance Subtype")
    static let screenshot = Oid<Data>(0, "/profile/\(uuid)/desktop/display//screenshot", "Screenshot")
}
