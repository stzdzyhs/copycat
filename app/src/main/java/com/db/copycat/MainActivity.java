package com.db.copycat;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.db.copycat.server.Server1;
import com.db.copycat.server.ServerActionHandler;
import com.db.util.MyAssetManager;
import com.db.util.MyAssetManagerAndroid;
import com.db.util.Util;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;

public class MainActivity extends AppCompatActivity implements ServerActionHandler {

    Server1 server1;
    TextView eMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        eMsg = findViewById(R.id.eMsg);

        MyAssetManager am = new MyAssetManagerAndroid(this.getAssets());
        server1 = new Server1(am, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //server1.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public static String getIp(Activity act) {
        WifiManager wifiMgr = (WifiManager) act.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        return ipAddress;
    }

    public void copyToClipboard(String copyText) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(copyText);
        }
        else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("theMsg", copyText);
            clipboard.setPrimaryClip(clip);
        }
    }


    int cnt = 0;
    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String m = (String)msg.obj;
            copyToClipboard(m);
            String m2 = String.format(Locale.US, "%d: %s\n", cnt, m);

            eMsg.setText(eMsg.getText(), TextView.BufferType.EDITABLE);
            ((Editable) eMsg.getText()).insert(0, m2);

            //eMsg.append(m2);
            cnt++;
        }
    };

    @Override
    public String handle(FullHttpRequest req) throws Exception {
        String path = new URI(req.uri()).getPath();
        String resp;
        if ("/msg.do".equals(path)) {
            String theMsg = req.content().toString(CharsetUtil.UTF_8);
            try {
                Map<String,String> m = new HashMap<String,String>();
                Util.decodePost(theMsg, m);

                String msg0 = m.get("msg");
                System.out.printf("the msg: %s \n", msg0);

                Message msg = new Message();
                msg.obj = msg0;
                messageHandler.sendMessage(msg);

                resp = "";//String.format(Locale.US, HttpServerHandler.resultT, 0, "");
            }
            catch(Exception e) {
                throw e;
            }
        }
        else {
            throw new Exception("unknown action...");
        }
        return resp;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_start) {
            try {
                if(server1.isStarted()) {
                    Toast.makeText(this, R.string.start_ok, Toast.LENGTH_SHORT).show();
                    return true;
                }

                server1.start();
                Toast.makeText(this, R.string.start_ok, Toast.LENGTH_SHORT).show();


                String ip = getIp(this);
                if(ip==null) {
                    ip = "ErrorIp";
                }
                int p = server1.getPort();

                String s = String.format(Locale.US, "https//%s:%d/msg.html", ip, p);
//                if(eMsg==null) {
//                    throw new Exception("eText null ????");
//                }
                //eMsg.append("");
                eMsg.setText(s);
            }
            catch(Exception e) {
                Toast.makeText(this, this.getText(R.string.start_error) + ":" + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            return true;
        }
        else if(id==R.id.action_stop) {
            server1.stop();
            Toast.makeText(this, R.string.stop_ok, Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
        }

        return super.onOptionsItemSelected(item);
    }

}
