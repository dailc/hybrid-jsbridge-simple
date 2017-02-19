package com.hybridjsbridgesimple.jsbridge;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dailc on 2017/2/19 0019.
 *
 *
 */

public class JSBridge {
    /**
     * scheme字符串，也可以存储到静态类中
     * 约定的schem值-可以自行约定
     */
    private static final String EJS_SCHEME = "SimpleJSBridge";
    /**
     * 注册方法缓存对象
     */
    private static Map<String, HashMap<String, Method>> exposedMethods = new HashMap<>();

    /**
     * 将api注册到缓存中
     *
     * @param exposedName
     * @param clazz
     */
    public static void register(String exposedName, Class<? extends IBridge> clazz) {
        if (!exposedMethods.containsKey(exposedName)) {
            try {
                exposedMethods.put(exposedName, getAllMethod(clazz));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取框架api类中所有符合要求的api
     *
     * @param injectedCls
     * @return
     * @throws Exception
     */
    private static HashMap<String, Method> getAllMethod(Class injectedCls) throws Exception {
        HashMap<String, Method> mMethodsMap = new HashMap<>();
        Method[] methods = injectedCls.getDeclaredMethods();
        for (Method method : methods) {
            String name;
            if (method.getModifiers() != (Modifier.PUBLIC | Modifier.STATIC) || (name = method.getName()) == null) {
                continue;
            }
            Class[] parameters = method.getParameterTypes();
            if (null != parameters && parameters.length == 4) {
                if (parameters[1] == WebView.class && parameters[2] == JSONObject.class && parameters[3] == Callback.class) {
                    mMethodsMap.put(name, method);
                }
            }
        }
        return mMethodsMap;
    }

    public static String callJava(Activity webLoader, WebView webView, String uriString) {
        String methodName = "";
        String apiName = "";
        String param = "{}";
        String port = "";
        String error;

        if (TextUtils.isEmpty(uriString)) {
            return "uri不能为空";
        }

        Uri uri = Uri.parse(uriString);
        if (uri == null) {
            return "参数不合法";
        }

        apiName = uri.getHost();
        param = uri.getQuery();
        port = uri.getPort() + "";
        methodName = uri.getPath();

        if (TextUtils.isEmpty(apiName)) {
            return "API_Name不能为空";
        }
        if (TextUtils.isEmpty(port)) {
            return "callbackId不能为空";
        }
        methodName = methodName.replace("/", "");
        if (TextUtils.isEmpty(methodName)) {
            return "handlerName不能为空";
        }

        if (uriString.contains("#")) {
            error = "参数中不能有#";
            new Callback(webView, port).apply(getFailJSONObject(error));
            return error;
        }
        if (!uriString.startsWith(EJS_SCHEME)) {
            error = "SCHEME不正确";
            new Callback(webView, port).apply(getFailJSONObject(error));
            return error;
        }

        if (exposedMethods.containsKey(apiName)) {
            HashMap<String, Method> methodHashMap = exposedMethods.get(apiName);
            if (methodHashMap != null && methodHashMap.size() != 0 && methodHashMap.containsKey(methodName)) {
                Method method = methodHashMap.get(methodName);
                if (method != null) {
                    try {
                        method.invoke(null, webLoader, webView, new JSONObject(param), new Callback(webView, port));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            //未注册API
            error = apiName + "未注册";
            new Callback(webView, port).apply(getFailJSONObject(error));
            return error;
        }
        return null;
    }


    /**
     * 获取callback返回数据json对象
     *
     * @param code   1：成功 0：失败 2:下拉刷新回传code值 3:页面刷新回传code值
     * @param msg    描述
     * @param result
     * @return
     */
    public static JSONObject getJSONObject(int code, String msg, JSONObject result) {
        JSONObject object = new JSONObject();
        try {
            object.put("code", code);
            if (!TextUtils.isEmpty(msg)) {
                object.put("msg", msg);
            }
            object.putOpt("result", result == null ? "" : result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    /**
     * 获取调用成功后callback返回数据json对象
     *
     * @return
     */
    public static JSONObject getSuccessJSONObject() {
        return getJSONObject(1, "", null);
    }

    /**
     * 获取调用成功后callback返回数据json对象
     *
     * @param result
     * @return
     */
    public static JSONObject getSuccessJSONObject(JSONObject result) {
        return getJSONObject(1, "", result);
    }

    /**
     * 获取调用失败后callback返回数据json对象
     *
     * @return
     */
    public static JSONObject getFailJSONObject() {
        return getJSONObject(0, "API调用失败", null);
    }

    /**
     * 获取调用失败后callback返回数据json对象
     *
     * @param msg
     * @return
     */
    public static JSONObject getFailJSONObject(String msg) {
        return getJSONObject(0, msg, null);
    }
}
