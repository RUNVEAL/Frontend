import CoreLocation
import SharedLogic

extension LatLon {
    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: lat, longitude: lon)
    }
}

extension CLLocationCoordinate2D {
    var latLon: LatLon {
        LatLon(lat: latitude, lon: longitude)
    }
}
