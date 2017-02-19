/**
 * @description  JSBridge前端交互库
 * @author dailc
 * @version 1.0
 * @time 2017-02-19
 * https://github.com/dailc
 */
(function() {
	(function(exports) {
		function detect(ua) {
			this.os = {};
			this.os.name = 'browser';
			var funcs = [
				function() { //android
					var android = ua.match(/(Android);?[\s\/]+([\d.]+)?/);
					if(android) {
						this.os.android = true;
						this.os.version = android[2];
						this.os.isBadAndroid = !(/Chrome\/\d/.test(window.navigator.appVersion));
						this.os.name += '_' + 'Android';
						this.os.name += '_' + 'mobile';
					}
					return this.os.android === true;
				},
				function() { //ios
					var iphone = ua.match(/(iPhone\sOS)\s([\d_]+)/);
					if(iphone) { //iphone
						this.os.ios = this.os.iphone = true;
						this.os.version = iphone[2].replace(/_/g, '.');
						this.os.name += '_' + 'iphone';
						this.os.name += '_' + 'mobile';
					} else {
						var ipad = ua.match(/(iPad).*OS\s([\d_]+)/);
						if(ipad) { //ipad
							this.os.ios = this.os.ipad = true;
							this.os.version = ipad[2].replace(/_/g, '.');
							this.os.name += '_' + 'iOS';
							this.os.name += '_' + 'mobile';
						}

					}
					return this.os.ios === true;
				}
			];
			[].every.call(funcs, function(func) {
				return !func.call(exports);
			});
		}
		detect.call(exports, navigator.userAgent);
	})(window.CommonTools = window.CommonTools||{});
	(function(exports) {
		function detect(ua) {
			this.os = this.os || {};
			//比如 EpointEJS/6.1.1  也可以/(EpointEJS)\/([\d\.]+)/i
			var hybrid = ua.match(/SimleJsbridgeScheme/i); //TODO ejs
			if(hybrid) {
				this.os.hybrid = true;
				this.os.name += '_' + 'hybrid';
			}
			//阿里的钉钉 DingTalk/3.0.0 
			var dd = ua.match(/DingTalk/i); //TODO dingding
			if(dd) {
				this.os.dd = true;
				this.os.name += '_' + 'dd';
			}
		}
		detect.call(exports, navigator.userAgent);
	})(window.CommonTools = window.CommonTools||{});
	(function() {
		var hasOwnProperty = Object.prototype.hasOwnProperty;
		var JSBridge = window.JSBridge || (window.JSBridge = {});
		//jsbridge协议定义的名称
		var CUSTOM_PROTOCOL_SCHEME = 'SimpleJSBridge';
		//最外层的api名称
		var API_Name = 'simple_bridge';
		//进行url scheme传值的iframe
		var messagingIframe = document.createElement('iframe');
		messagingIframe.style.display = 'none';
		messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + API_Name;
		document.documentElement.appendChild(messagingIframe);

		//定义的回调函数集合,在原生调用完对应的方法后,会执行对应的回调函数id
		var responseCallbacks = {};
		//唯一id,用来确保每一个回调函数的唯一性
		var uniqueId = 1;
		//本地注册的方法集合,原生只能调用本地注册的方法,否则会提示错误
		var messageHandlers = {};

		//实际暴露给原生调用的对象
		var Inner = {
			/**
			 * @description 注册本地JS方法通过JSBridge给原生调用
			 * 我们规定,原生必须通过JSBridge来调用H5的方法
			 * 注意,这里一般对本地函数有一些要求,要求第一个参数是data,第二个参数是callback
			 * @param {String} handlerName 方法名
			 * @param {Function} handler 对应的方法
			 */
			registerHandler: function(handlerName, handler) {
				messageHandlers[handlerName] = handler;
			},
			/**
			 * @description 调用原生开放的方法
			 * @param {String} handlerName 方法名
			 * @param {JSON} data 参数
			 * @param {Function} callback 回调函数
			 */
			callHandler: function(handlerName, data, callback) {
				//如果没有 data
				if(arguments.length == 3 && typeof data == 'function') {
					callback = data;
					data = null;
				}
				
				_doSend({
					handlerName: handlerName,
					data: data
				}, callback);
			},
			/**
			 * @description 原生调用H5页面注册的方法,或者调用回调方法
			 * @param {String} messageJSON 对应的方法的详情,需要手动转为json
			 */
			_handleMessageFromNative: function(messageJSON) {
				setTimeout(_doDispatchMessageFromNative);
				/**
				 * @description 处理原生过来的方法
				 */
				function _doDispatchMessageFromNative() {
					var message;
					try {
						if(typeof messageJSON === 'string') {
							message = JSON.parse(messageJSON);
						} else {
							message = messageJSON;
						}
					} catch(e) {
						//TODO handle the exception
						console.error("原生调用H5方法出错,传入参数错误");
						return;
					}

					//回调函数
					var responseCallback;
					if(message.responseId) {
						//这里规定,原生执行方法完毕后准备通知h5执行回调时,回调函数id是responseId
						responseCallback = responseCallbacks[message.responseId];
						if(!responseCallback) {
							return;
						}
						//执行本地的回调函数
						responseCallback(message.responseData);
						delete responseCallbacks[message.responseId];
					} else {
						//否则,代表原生主动执行h5本地的函数
						//从本地注册的函数中获取
						var handler = messageHandlers[message.handlerName];
						if(!handler) {
							//本地没有注册这个函数
						} else {
							//执行本地函数,按照要求传入数据和回调
							handler(message.data);
						}
					}
				}
			}

		};
		/**
		 * @description JS调用原生方法前,会先send到这里进行处理
		 * @param {JSON} message 调用的方法详情,包括方法名,参数
		 * @param {Function} responseCallback 调用完方法后的回调
		 */
		function _doSend(message, responseCallback) {
			if(responseCallback) {
				//取到一个唯一的callbackid
				var callbackId = Util.getCallbackId();
				//回调函数添加到集合中
				responseCallbacks[callbackId] = responseCallback;
				//方法的详情添加回调函数的关键标识
				message['callbackId'] = callbackId;
			}
			
			//获取 触发方法的url scheme
			var uri = Util.getUri(message);
			//android和ios的url scheme调用有所区别
			//hybrid环境判断的UA需要与原生中设置的一致
			if(CommonTools.os.hybrid) {
				if(CommonTools.os.ios) {
					//ios采用iframe跳转scheme的方法
					messagingIframe.src = uri;
					//console.log("ios:触发uri:"+uri);
				} else {
					window.top.prompt(uri, "");
				}
			} else {
				//浏览器
				console.error("浏览器中JSBridge无效,scheme:" + uri);
			}
		}

		var Util = {
			getCallbackId: function() {
				//如果无法解析端口,可以换为Math.floor(Math.random() * (1 << 30));
				//return 'cb_' + (uniqueId++) + '_' + new Date().getTime();
				//目前采用端口直接解析，因此需要数字
				return Math.floor(Math.random() * (1 << 30)) + '';
			},
			//获取url scheme
			//第二个参数是兼容android中的做法
			//android中由于原生不能获取JS函数的返回值,所以得通过协议传输
			getUri: function(message) {
				
				var uri = CUSTOM_PROTOCOL_SCHEME + '://' + API_Name;
				if(message) {
					//回调id作为端口存在
					var callbackId, method, params;
					if(message.callbackId) {
						//第一种:h5主动调用原生
						callbackId = message.callbackId;
						method = message.handlerName;
						params = message.data;
					} else if(message.responseId) {
						//第二种:原生调用h5后,h5回调
						//这种情况下需要原生自行分析传过去的port是否是它定义的回调
						callbackId = message.responseId;
						method = message.handlerName;
						params = message.responseData;
					}
					//参数转为字符串
					params = this.getParam(params);
					//uri 补充
					uri += ':' + callbackId + '/' + method + '?' + params;
				}

				return uri;
			},
			getParam: function(obj) {
				if(obj && typeof obj === 'object') {
					return JSON.stringify(obj);
				} else {
					return obj || '';
				}
			}
		};
		for(var key in Inner) {
			if(!hasOwnProperty.call(JSBridge, key)) {
				JSBridge[key] = Inner[key];
			}
		}

	})();

	/*
	 ***************************API********************************************
	 * 开放给外界调用的api
	 * */
	window.jsapi = {};
	/**
	 ***app 模块 
	 * 一些特殊操作
	 */
	jsapi.app = {
		/**
		 * @description 测试函数
		 */
		testNativeFunc: function(msg,callback) {
			//调用一个测试函数
			JSBridge.callHandler('testNativeFunc', {
				"param1": msg
			}, function(res) {
				callback && callback(res);
			});
		},
		/**
		 * @description 通知原生
		 */
		callH5Func: function(handleName) {
			//调用一个测试函数
			JSBridge.callHandler('callH5Func', {
				"handleName": handleName
			}, function(res) {
				//callback && callback(res);
			});
		},
	};
	//注册一个测试函数
	JSBridge.registerHandler('testH5Func', function(data) {
		alert('测试函数接收到数据:' + JSON.stringify(data));
	});
})();