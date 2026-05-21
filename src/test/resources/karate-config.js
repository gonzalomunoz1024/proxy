function fn() {
  var port = karate.properties['proxy.port'] || '8080';
  var config = {
    baseUrl: 'http://127.0.0.1:' + port
  };
  karate.configure('connectTimeout', 20000);
  karate.configure('readTimeout', 20000);
  return config;
}
