part of flutter_amap;

class AMapController extends ChangeNotifier{

  static const String CHANNEL = "com.flutter.amap";
  static const String AMAP_ON_MAP_LOAD = "amap_on_map_load";
  static const String AMAP_ON_CAMERA_CHANGE = "amap_on_camera_change";
  static const String AMAP_CHANGE_CAMERA = "amap_change_camera";
  static const String AMAP_ADD_MARKER = "amap_add_marker";
  static const String AMAP_UPDATE_MARKER = "amap_update_marker";

  final int _id;

  AMapController._(this._id)
      : assert(_id != null)
  ,
        _channel = new MethodChannel(CHANNEL + _id.toString())
  {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  final MethodChannel _channel;

  static AMapController init(int id) {
    assert(id != null);
    return AMapController._(id);
  }


  /// 地图状态发生变化的监听接口。
  final ArgumentCallbacks<CameraPosition> onCameraChanged = ArgumentCallbacks<
      CameraPosition>();

  /// 地图状态发生变化的监听接口。
  final ArgumentCallbacks onMapLoaded = ArgumentCallbacks();

  /// Marker集合
  Set<Marker> get markers => Set<Marker>.from(_markers.values);
  final Map<String, Marker> _markers = <String, Marker>{};


  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case AMAP_ON_MAP_LOAD:
        onMapLoaded.call(null);
        break;
      case AMAP_ON_CAMERA_CHANGE:
        CameraPosition cameraPosition = CameraPosition.fromMap(
            call.arguments['position']);
        onCameraChanged.call(cameraPosition);
        break;
      default:
        throw MissingPluginException();
    }
  }


  /// 地图操作
  void changeCamera(CameraPosition cameraPosition, bool isAnimate) {
    if (_channel != null) {
      _channel.invokeMethod(
          AMAP_CHANGE_CAMERA, [cameraPosition._toMap(), isAnimate]);
    }
  }


  /// 覆盖物添加
  Future<Marker> addMarker(MarkerOptions options) async {

    final MarkerOptions effectiveOptions =
    MarkerOptions.defaultOptions.copyWith(options);
    final String markerId = await _channel.invokeMethod(
      AMAP_ADD_MARKER,
      <String, dynamic>{
        'options': effectiveOptions._toJson(),
      },
    );
    final Marker marker = Marker(markerId, effectiveOptions);
    _markers[markerId] = marker;
    notifyListeners();

    return marker;
  }

  ///
  /// 更新Marker内容 转换成内容，各平台再根据id去更新
  ///
  Future<void> updateMarker(Marker marker, MarkerOptions changes) async {
    assert(marker != null);
    assert(_markers[marker._id] == marker);
    assert(changes != null);
    await _channel.invokeMethod(AMAP_UPDATE_MARKER, <String, dynamic>{
      'marker': marker._id,
      'options': changes._toJson(),
    });
    marker._options = marker._options.copyWith(changes);

    notifyListeners();
  }


///
/// 工具转换

}