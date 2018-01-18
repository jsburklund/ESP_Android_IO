package com.jsburklund.esp32_io_performance;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private TextView statustextview, consoletextview;
    private ScrollView consolescrollview;
    private ToggleButton testtoggle, serverclienttoggle;

    private Thread serverthread, clientthread, testconsolethread;
    private SafeShutdownRunnable serverrunnable, clientrunnable, testconsolerunnable;

    private final String ESP_IP = "192.168.4.1";
    private final int ESP_PORT = 4567;

    private final String TAG = "ESP32_IO_Performance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statustextview = (TextView) findViewById(R.id.StatusTextView);
        consoletextview = (TextView) findViewById(R.id.ConsoleTextView);
        consolescrollview = (ScrollView) findViewById(R.id.ConsoleScrollView);
        testtoggle = (ToggleButton) findViewById(R.id.StartStopToggle);
        serverclienttoggle = (ToggleButton) findViewById(R.id.ServerClientToggle);

        testconsolerunnable = new TestConsoleThread();
        testconsolethread = new Thread(testconsolerunnable);
        serverrunnable = new ServerThread();
        serverthread = new Thread(serverrunnable);
        clientrunnable = new ClientThread();
        clientthread = new Thread(clientrunnable);
        testtoggle.setOnClickListener(new StartButtonListener());

    }

    abstract class SafeShutdownRunnable implements Runnable {
        // Method to safely signal the thread to shutdown
        protected AtomicBoolean should_shutdown = new AtomicBoolean(false);
        public void shutdown() {
            should_shutdown.set(true);
        }
    }

    class ServerThread extends SafeShutdownRunnable {
        ServerSocket socket;
        Socket iosocket;
        public void run() {
            should_shutdown.set(false);
            printConsole("Starting TCP connection as: Server");
            try {
                socket = new ServerSocket(ESP_PORT);
                printConsole(String.format("Listening on port %d\n",ESP_PORT));
                printConsole("Waiting for connection...\n");
                iosocket = socket.accept();
                printConsole("Connected\n");
                InputStream istream = iosocket.getInputStream();
                while (!should_shutdown.get()) {
                    int val = istream.read();
                    if (val < 0) {
                        break;
                    }
                    printConsole(String.format("%d, ", val));
                }
                iosocket.close();
                socket.close();
            }
            catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    class ClientThread extends SafeShutdownRunnable {
        private Socket socket;
        public void run() {
            should_shutdown.set(false);
            printConsole("Start TCP Connection as: Client");
            try {
                socket = new Socket(ESP_IP, ESP_PORT);
                while(!socket.isConnected()) {
                    Log.d(TAG,"Waiting for socket to connect");
                    Thread.sleep(500);
                }
            } catch (Exception e)  { Log.e(TAG, e.getMessage()); return; }
            try {
                if (socket==null) {
                    Log.e(TAG, "Socket is null");
                }
                InputStream istream = socket.getInputStream();
                while (!should_shutdown.get()) {
                    int val = istream.read();
                    if (val < 0) {
                        break;  //Found the end of the input stream
                    } else {
                        // Valid character received
                        printConsole(String.format("%d, ",val));
                    }
                }
                socket.close();
            } catch (IOException e) { Log.e(TAG, e.getMessage()); }
        }
    }

    class TestConsoleThread extends SafeShutdownRunnable {
        public void run() {
            should_shutdown.set(false);
            while(!should_shutdown.get()) {
                //Print a line every so often
                printConsole("Hello World"+(System.currentTimeMillis()/1000.0)+'\n');
                try { Thread.sleep(500); } catch (InterruptedException e) { }
            }
        }
    }

    class StartButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!testtoggle.isChecked()) {
                //Execute the Server Thread
                //Kill an existing thread if it is already running
                if (serverthread != null && serverthread.isAlive()) {
                    serverrunnable.shutdown();
                    while(serverthread.isAlive()) {}
                }
                serverthread.start();
            } else {
                //Execute the Client Thread
                //Kill an existing thread if it is already running
                if (clientthread != null && clientthread.isAlive()) {
                    clientrunnable.shutdown();
                    while(clientthread.isAlive()) {}
                }
                clientthread.start();
            }
        }
    }

    // Convenience function for print stuff to the on screen console
    private void printConsole(final String text) {
        if (consoletextview==null) {
            return;
        }
        consoletextview.post(new Runnable() {
            public void run() {
                consoletextview.append(text);
            }
        });
    }

}
