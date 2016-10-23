package com.detwelsoft.multishare;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    public static final int SERVER_PORT = 41033;
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
                try {
                    mMessageBuffer = URLEncoder.encode("Multishare", "ASCII").getBytes();

                    InetAddress address =
                            InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, (byte) 0, (byte) 255});

                    DatagramPacket mDatagramPacket = new DatagramPacket(
                            mMessageBuffer, mMessageBuffer.length);

                    DatagramSocket mDatagramSocket = new DatagramSocket(SERVER_PORT);

                    mDatagramSocket.setBroadcast(true);
                    mDatagramSocket.send(mDatagramPacket);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
