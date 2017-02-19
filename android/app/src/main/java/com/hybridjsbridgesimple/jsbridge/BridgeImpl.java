package com.hybridjsbridgesimple.jsbridge;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TimePicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by dailc on 2017/2/19 0019.
 *
 * API的定义必须满足以下条件：
 * 1.实现IBridge
 * 2.方法必须是public static类型
 * 3.固定4个参数Activity，WebView，JSONObject，Callback
 * 4.回调统一采用 callback.apply(),参数通过JSBridge.getJSONObject()获取Json对象
 * 注意：
 * 耗时操作在多线程中实现
 * UI操作在主线程实现
 *
 * JSONObject 转 JsonObject
 * JsonObject jsonObject = new JsonParser().parse(param==null?"":param.toString()).getAsJsonObject();
 */

public class BridgeImpl implements IBridge {
    /**
     * 自定义原生API
     * 参数：
     * param1：参数1
     */
    public static void testNativeFunc(final Activity webLoader, WebView wv, JSONObject param, final Callback callback) {
        Log.d("ss","receivetestNativeFunc~");
        final String param1 = param.optString("param1");
        wv.post(new Runnable() {
            public void run() {
                //做一些自己的操作，操作完毕后将值通过回调回传给h5页面
                try {
                    JSONObject object = new JSONObject();
                    object.put("param1", "回传参数:"+param1);
                    callback.apply(JSBridge.getSuccessJSONObject(object));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 调用H5页面的制定方法
     * 参数：
     * param1：参数1
     */
    public static void callH5Func(final Activity webLoader, WebView wv, JSONObject param, final Callback callback) {
        final String handleName = param.optString("handleName");
        wv.post(new Runnable() {
            public void run() {
                //做一些自己的操作，操作完毕后将值通过回调回传给h5页面
                try {
                    JSONObject object = new JSONObject();
                    object.put("param1","传给h5的参数~");
                    //主动调用h5中注册的方法
                    callback.call(handleName,JSBridge.getSuccessJSONObject(object));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
