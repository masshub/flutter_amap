import 'package:flutter/material.dart';
import 'package:flutter_amap/flutter_amap.dart';
import 'package:amap_location/amap_location.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AMap Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'AMap Demo'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  AMapController mapController;
  Marker currentMarker;
  bool _isAnimation = true;
  List<LatLng> locations = List<LatLng>();

  @override
  void initState() {
    AMapLocationClient.startup(new AMapLocationOption(
        desiredAccuracy: CLLocationAccuracy.kCLLocationAccuracyHundredMeters));
    AMapLocationClient.getLocation(true);
    initData();
    super.initState();
  }

  void initData() {
    locations.add(LatLng(30.301021, 120.316654));
    locations.add(LatLng(30.298353, 120.325752));
    locations.add(LatLng(30.318508, 120.319401));
    locations.add(LatLng(30.309765, 120.348927));
    locations.add(LatLng(30.257141, 120.358368));
    locations.add(LatLng(30.271523, 120.378452));
    locations.add(LatLng(30.27597, 120.333477));
    locations.add(LatLng(30.264851, 120.316483));
    locations.add(LatLng(30.244092, 120.356823));
    locations.add(LatLng(30.318657, 120.36266));
    locations.add(LatLng(30.280121, 120.340859));
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(widget.title),
        ),
        body: AMap(
          onMapCreated: onMapCreated,
        )
//          Row(
//            children: <Widget>[
//              FlatButton(
//                child: Text("移动到北京"),
//                onPressed: () {
//                  if (mapController != null) {
//                    mapController.changeCamera(
//                        CameraPosition(
//                            target: LatLng(39.893927, 116.405972), zoom: 10),
//                        _isAnimation);
//                  }
//                },
//              ),
//              _isAnimationButton(),
//            ],
//          ),
//          Row(
//            children: <Widget>[
//              FlatButton(
//                child: Text("添加Marker"),
//                onPressed: () {
//                  if (mapController != null) {
//                    MarkerOptions options = MarkerOptions.defaultOptions;
//
//                    mapController.addMarker(options).then((marker) {
//                      setState(() {
//                        currentMarker = marker;
//                      });
//                    });
//                  }
//                },
//              ),
//              FlatButton(
//                child: Text("修改位置"),
//                onPressed: () {
//                  if (mapController != null) {
//                    if (currentMarker != null) {
//                      //fixme 如果默认值不一样如何更新
//                      MarkerOptions markerOptions = MarkerOptions(
//                          position: LatLng(
//                              currentMarker.options.position.latitude,
//                              currentMarker.options.position.longitude +
//                                  0.001));
//                      mapController.updateMarker(currentMarker, markerOptions);
//                    } else {
//                      print("currentMarker is null");
//                    }
//                  }
//                },
//              ),
//            ],
//          ),
//          Row(
//            children: <Widget>[
//              FlatButton(
//                child: Text("添加Polyline"),
//                onPressed: () {
//                  if (mapController != null) {
//                    MarkerOptions options = MarkerOptions.defaultOptions;
//                    mapController.addMarker(options);
//                  }
//                },
//              ),
//            ],
//          ),
//        ]

    );
  }

  Widget _isAnimationButton() {
    return FlatButton(
      child: Text('${_isAnimation ? '关闭' : '开启'} 动画'),
      onPressed: () {
        setState(() {
          _isAnimation = !_isAnimation;
        });
      },
    );
  }


  void onMapCreated(AMapController controller) async {
    print("onMapCreated");
    mapController = controller;
    AMapLocation location = await AMapLocationClient.getLocation(true);
    print('经纬度${location.latitude},${location.longitude}');
    mapController.changeCamera(
        CameraPosition(
            target: LatLng(location.latitude, location.longitude), zoom: 13.0),
        true);

    MarkerOptions markerOptions = MarkerOptions(
        position: LatLng(location.latitude, location.longitude),
        icon: BitmapDescriptor.fromAsset(
            'assets/images/navi_map_gps_locked.png'));
    mapController.addMarker(markerOptions).then((marker) {
      setState(() {
        currentMarker = marker;
        print('currentMarker: $currentMarker');
      });
    });

    MarkerOptions options;
    for (int i = 0; i < locations.length; i++) {
      if (i == 1) {
        options = MarkerOptions(
            position: locations[i],
            icon: BitmapDescriptor.fromAsset('assets/images/MAGENTA.png'));
        mapController.addMarker(options);
      } else if (i > 1 && i < 4) {
        options = MarkerOptions(
            position: locations[i],
            icon: BitmapDescriptor.fromAsset('assets/images/CYAN.png'));
        mapController.addMarker(options);
      } else {
        options = MarkerOptions(
            position: locations[i],
            icon: BitmapDescriptor.fromAsset('assets/images/RED.png'));
        mapController.addMarker(options);
      }
      // 注册监听
      mapController.onMapLoaded.add(onMapLoaded);
      mapController.onCameraChanged.add(onCameraChanged);
    }
  }


    void onMapLoaded(argument) {
      print("onMapLoaded");
    }

    void onCameraChanged(CameraPosition cameraPostion) {
      AMapLocationClient.onLocationUpate.listen((AMapLocation loc) {
        if (!mounted) return;
        setState(() {
//        mapController.changeCamera(CameraPosition(target: LatLng(loc.latitude, loc.longitude)), true);
        });
      });
      print("onCameraChanged " + onCameraChanged.toString());
    }
  }
