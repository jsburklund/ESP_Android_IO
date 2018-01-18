package com.jsburklund.esp32_io_performance;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private TextView statustextview, consoletextview;
    private ScrollView consolescrollview;
    private ToggleButton testtoggle, serverclienttoggle;

    private Thread serverthread, clientthread, testconsolethread;
    private SafeShutdownRunnable serverrunnable, clientrunnable, testconsolerunnable;

    private final String ESP_IP = "192.168.4.2";
    private final int ESP_PORT = 44567;

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
        clientthread = new Thread(clientthread);
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
        public void run() {
            should_shutdown.set(false);
            printConsole("Starting TCP connection as: Server");
            while (!should_shutdown.get()) {

            }
        }
    }

    class ClientThread extends SafeShutdownRunnable {
        public void run() {
            should_shutdown.set(false);
            printConsole("Start TCP Connection as: Client");
            while (!should_shutdown.get()) {

            }
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
            if (testtoggle.isChecked() && testconsolethread!=null) {
                if (!testconsolethread.isAlive()) {
                    testconsolethread.start();
                }
            } else if (!testtoggle.isChecked() && testconsolethread!=null) {
                if (testconsolethread.isAlive()) {
                    testconsolerunnable.shutdown();
                }
            }
        }
    }

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
