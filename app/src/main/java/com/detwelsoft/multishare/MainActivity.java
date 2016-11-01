package com.detwelsoft.multishare;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.InputStream;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendUdp = (Button) findViewById(R.id.sendUdp);

        sendUdp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoInMainThread thread = new NoInMainThread();
                thread.start();
            }
        });
    }

    @Override
    public void OnMessageReceive(String data) {
        final String d = data;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, d, Toast.LENGTH_LONG).show();
            }
        });
    }

    public class NoInMainThread extends Thread {
        private DatagramSocket mDatagramSocket;
        private boolean isCreated = false;

        @Override
        public void run() {
            try {
                mMessageBuffer = new byte[16];
                byte[] helloMessage = URLEncoder.encode("MultiShare", "ASCII").getBytes();
                byte[] mac = new byte[]{(byte) 200, (byte) 221, (byte) 201, (byte) 249, (byte) 147, (byte) 249};
                System.arraycopy(helloMessage, 0, mMessageBuffer, 0, helloMessage.length);
                System.arraycopy(mac, 0, mMessageBuffer, helloMessage.length, mac.length);

                InetAddress address =
                        InetAddress.getByAddress(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255});

                DatagramPacket mDatagramPacket = new DatagramPacket(
                        mMessageBuffer, mMessageBuffer.length, address, SERVER_PORT);

                mDatagramSocket = new DatagramSocket(SERVER_PORT);

                mDatagramSocket.setBroadcast(true);
                mDatagramSocket.send(mDatagramPacket);

                if (!isCreated) {
                    TcpServer server = new TcpServer(MainActivity.this);
                    server.start();
                    isCreated = true;
                }

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

                InputStream is = s.getInputStream();

                byte buf[] = new byte[MESSAGE_BUFFER];

                while (true) {
                    int r = is.read(buf);

                    String data = new String(buf, 0, r, Charset.forName("UTF-8"));

                    System.out.print(data);
                    MainActivity.this.OnMessageReceive(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
                //TODO обработка отключения сервера
                //s.close();
            }
        }
    }
}
