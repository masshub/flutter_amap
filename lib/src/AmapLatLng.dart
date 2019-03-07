
part of flutter_amap;
/**
 * Created by Maker on 2019/3/7.
 * Describe:
 */

class AmapLatLng{
  final double latitude;
  final double longitude;

  const AmapLatLng(this.latitude, this.longitude);

  static AmapLatLng fromMap(Map map) {
    return new AmapLatLng(map["latitude"], map["longitude"]);
  }

  Map toMap() {
    return {"latitude": this.latitude, "longitude": this.longitude};
  }

  @override
  String toString() {
    return 'LatLng{latitude: $latitude, longitude: $longitude}';
  }
}