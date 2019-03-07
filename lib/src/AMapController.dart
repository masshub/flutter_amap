part of flutter_amap;

class AMapController extends ChangeNotifier {
  static const String CHANNEL = "com.flutter.amap";
  static const String AMAP_ON_MAP_LOAD = "amap_on_map_load";
  static const String AMAP_ON_CAMERA_CHANGE = "amap_on_camera_change";
  static const String AMAP_CHANGE_CAMERA = "amap_change_camera";
  static const String AMAP_ADD_MARKER = "amap_add_marker";
  static const String AMAP_UPDATE_MARKER = "amap_update_marker";
  static const String AMAP_UPDATE_LOCATION = "amap_update_location";
  static const String AMAP_KEY = "amap_key";
  static bool _apiKeySet = false;

  final int _id;

  AMapController._(this._id)
      : assert(_id != null),
        _channel = new MethodChannel(CHANNEL + _id.toString()) {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

   final MethodChannel _channel;

  StreamController<Location> _locationChangeStreamController = new StreamController.broadcast();
  Stream<Location> get onLocationUpdated =>_locationChangeStreamController.stream;

  static AMapController init(int id) {
    assert(id != null);
    return AMapController._(id);
  }

  /// 地图状态发生变化的监听接口。
  final ArgumentCallbacks<CameraPosition> onCameraChanged =
      ArgumentCallbacks<CameraPosition>();

  /// 地图状态发生变化的监听接口。
  final ArgumentCallbacks onMapLoaded = ArgumentCallbacks();

  /// 地图状态发生变化的监听接口。
  final ArgumentCallbacks<Location> onLocationChanged =
  ArgumentCallbacks<Location>();

  /// Marker集合
  Set<Marker> get markers => Set<Marker>.from(_markers.values);
  final Map<String, Marker> _markers = <String, Marker>{};

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case AMAP_ON_MAP_LOAD:
        onMapLoaded.call(null);
        break;
      case AMAP_ON_CAMERA_CHANGE:
        CameraPosition cameraPosition =
            CameraPosition.fromMap(call.arguments['position']);
        onCameraChanged.call(cameraPosition);
        break;
      case "updateLocation":
        Map args = call.arguments;
        _locationChangeStreamController.add(Location.fromMap(args));
        return new Future.value("");

        break;

      default:
        throw MissingPluginException();
    }
  }

  static setApiKey(String apiKey){
    MethodChannel c = const MethodChannel("com.flutter.amap");
    c.invokeMethod(AMAP_KEY, apiKey);
    _apiKeySet = true;
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

  static StreamController<AMapLocation> _locationUpdateStreamController =
  new StreamController.broadcast();

  /// 定位改变监听
  static Stream<AMapLocation> get onLocationUpate =>
      _locationUpdateStreamController.stream;


  /// 直接获取到定位，不必先启用监听
  /// @param needsAddress 是否需要详细地址信息
  Future<AMapLocation> getLocation(bool needsAddress) async {
    final dynamic location =
    await _channel.invokeMethod('getLocation', needsAddress);
    return AMapLocation.fromMap(location);
  }

  /// 启动系统
  /// @param options 启动系统所需选项
   Future<bool> startup(AMapLocationOption option) async {
    _channel.setMethodCallHandler(_handleMethodCall);
    return await _channel.invokeMethod("startup", option.toMap());
  }

  /// 更新选项，如果已经在监听，那么要先停止监听，再调用这个函数
   Future<bool> updateOption(AMapLocationOption option) async {
    return await _channel.invokeMethod("updateOption", option);
  }

   Future<bool> shutdown() async {
    return await _channel.invokeMethod("shutdown");
  }

  /// 启动监听位置改变
   Future<bool> startLocation() async {
    return await _channel.invokeMethod("startLocation");
  }

  /// 停止监听位置改变
  Future<bool> stopLocation() async {
    return await _channel.invokeMethod("stopLocation");
  }






  ///
  /// 工具转换

}
