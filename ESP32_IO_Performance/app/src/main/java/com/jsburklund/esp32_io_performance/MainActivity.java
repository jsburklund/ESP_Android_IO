package com.jsburklund.esp32_io_performance;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private TextView statustextview, consoletextview;
    private ScrollView consolescrollview;
    private ToggleButton testtoggle, serverclienttoggle;

    private Thread serverthread, clientthread;
    private SafeShutdownRunnable serverrunnable, clientrunnable;

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

        // Setup the console textview
        consoletextview.setText("Console: "+System.lineSeparator());

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

    DatagramSocket socket;
    InetAddress address;
    boolean blinkstate = false;
    final int blinktimeusec = 20*1000;
    class BlinkRunnable implements Runnable {
        @Override
        public void run() {
            try {
                String packet;
                if (blinkstate) {
                    packet = "ssnn";
                } else {
                    packet = "ssff";
                }
                socket.send(new DatagramPacket(packet.getBytes(StandardCharsets.UTF_8), packet.length(), address, ESP_PORT));
                blinkstate ^= true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerThread extends SafeShutdownRunnable {

        public void run() {
            should_shutdown.set(false);
            printLineConsole("Starting TCP connection as: Server");
            try {
                socket = new DatagramSocket(ESP_PORT);
                address = InetAddress.getByName(ESP_IP);
                printLineConsole(String.format("Listening on port %d", ESP_PORT));
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                final ScheduledFuture<?> ioHandler = scheduler.scheduleAtFixedRate(new BlinkRunnable(), 0, blinktimeusec, TimeUnit.MICROSECONDS);
                while (!should_shutdown.get()) {}
                printLineConsole("Shutting down Server thread");
                ioHandler.cancel(true);
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    class ClientThread extends SafeShutdownRunnable {
        public void run() {
            should_shutdown.set(false);
            printLineConsole("Start TCP Connection as: Client");
            try {
                socket = new DatagramSocket();
                address = InetAddress.getByName(ESP_IP);
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                final ScheduledFuture<?> ioHandler = scheduler.scheduleAtFixedRate(new BlinkRunnable(), 0, blinktimeusec, TimeUnit.MICROSECONDS);
                while (!should_shutdown.get()) {

                }
                printLineConsole("Shutting down Client thread");
                ioHandler.cancel(true);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class StartButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            // Run when the start button is clicked
            if (testtoggle.isChecked()) {
                final String servertype;
                if (!serverclienttoggle.isChecked()) {
                    //Execute the Server Thread
                    //Kill an existing thread if it is already running
                    if (serverthread != null && serverthread.isAlive()) {
                        serverrunnable.shutdown();
                        while (serverthread.isAlive()) {
                        }
                    }
                    serverthread.start();
                    servertype = "server";
                } else {
                    //Execute the Client Thread
                    //Kill an existing thread if it is already running
                    if (clientthread != null && clientthread.isAlive()) {
                        clientrunnable.shutdown();
                        while (clientthread.isAlive()) {
                        }
                    }
                    clientthread.start();
                    servertype = "client";
                }
                statustextview.post(new Runnable() {
                    @Override
                    public void run() {
                        statustextview.setText("Running "+servertype);
                    }
                });
            } else {
                //Stop button pressed
                // Shutdown the threads
                serverrunnable.shutdown();
                clientrunnable.shutdown();
                //Signify that the test is stoppped
                statustextview.post(new Runnable() {
                    @Override
                    public void run() {
                        statustextview.setText("Stopped");
                    }
                });
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

    // Convenience funciton to print a line to the on screen console
    private void printLineConsole(final String text) {
        printConsole(text+System.lineSeparator());
    }

}
