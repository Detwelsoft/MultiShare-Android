package com.detwelsoft.multishare;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.Charset;

interface OnMessageReceiveListener {
    void OnMessageReceive(String data);
}

public class MainActivity extends AppCompatActivity implements OnMessageReceiveListener {

    public static final int SERVER_PORT = 49015;
    public static final int SERVER_TCP_PORT = 49016;
    public static final int MESSAGE_BUFFER = 8192;
    public static byte[] mMessageBuffer;
    private Button sendUdp;
    private boolean isCreated = false;

    private ClipboardManager mClipboardManager;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        sendUdp = (Button) findViewById(R.id.sendUdp);

        sendUdp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoInMainThread thread = new NoInMainThread(mWifiManager.getConnectionInfo().getMacAddress());
                thread.start();

                if (!isCreated) {
                    TcpServer server = new TcpServer(MainActivity.this);
                    server.start();
                    isCreated = true;
                }
            }
        });
    }

    @Override
    public void OnMessageReceive(final String message) {
        ClipData clip = ClipData.newPlainText("MultiShare", message);
        mClipboardManager.setPrimaryClip(clip);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public class NoInMainThread extends Thread {
        private DatagramSocket mDatagramSocket;
        private byte[] mDeviceMacData = new byte[6];

        public NoInMainThread(String deviceMAC) {
            String[] macAddressParts = deviceMAC.split(":");

            for (int i = 0; i < 6; i++) {
                Integer hex = Integer.parseInt(macAddressParts[i], 16);
                mDeviceMacData[i] = hex.byteValue();
            }
        }

        @Override
        public void run() {
            try {
                mMessageBuffer = new byte[16];
                byte[] helloMessage = URLEncoder.encode("MultiShare", "ASCII").getBytes();
                System.arraycopy(helloMessage, 0, mMessageBuffer, 0, helloMessage.length);
                System.arraycopy(mDeviceMacData, 0, mMessageBuffer, helloMessage.length, mDeviceMacData.length);

                InetAddress address = InetAddress.getByAddress(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255});

                DatagramPacket mDatagramPacket = new DatagramPacket(
                        mMessageBuffer, mMessageBuffer.length, address, SERVER_PORT);

                mDatagramSocket = new DatagramSocket(SERVER_PORT);

                mDatagramSocket.setBroadcast(true);
                mDatagramSocket.send(mDatagramPacket);

                mDatagramSocket.disconnect();
                mDatagramSocket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                if (mDatagramSocket != null) {
                    mDatagramSocket.disconnect();
                    mDatagramSocket.close();
                }
            }
        }
    }

    public class TcpServer extends Thread {
        private Socket s;
        private OnMessageReceiveListener onMessageReceiveListener;

        public TcpServer(OnMessageReceiveListener onMessageReceiveListener) {
            this.onMessageReceiveListener = onMessageReceiveListener;
        }

        @Override
        public void run() {
            try {
                ServerSocket server = new ServerSocket(SERVER_TCP_PORT);
                System.out.println("server is started");
                s = server.accept();

                InputStreamReader isr = new InputStreamReader(s.getInputStream(), Charset.forName("UTF-8"));

                char buf[] = new char[MESSAGE_BUFFER];
                int numReadBytes = isr.read(buf);

                String data = new String(buf, 0, numReadBytes);
                if (!data.contentEquals("MultiShare")) {
                    throw new Exception("Invalid initial message!");
                }

                while ((numReadBytes = isr.read(buf)) >= 0) {
                    if (numReadBytes == 1) {
                        numReadBytes = isr.read(buf);
                    }

                    data = new String(buf, 0, numReadBytes);

                    System.out.print(data);
                    if (onMessageReceiveListener != null) {
                        onMessageReceiveListener.OnMessageReceive(data);
                    }
                }

                isr.close();
                s.close();
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
                //TODO обработка отключения сервера
                try {
                    s.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
