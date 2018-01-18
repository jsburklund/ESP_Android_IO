package com.jsburklund.esp32_io_performance;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private TextView statustextview, consoletextview;
    private ScrollView consolescrollview;
    private ToggleButton testtoggle, serverclienttoggle;

    Thread serverthread, clientthread, testconsolethread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statustextview = (TextView) findViewById(R.id.StatusTextView);
        consoletextview = (TextView) findViewById(R.id.ConsoleTextView);
        consolescrollview = (ScrollView) findViewById(R.id.ConsoleScrollView);
        testtoggle = (ToggleButton) findViewById(R.id.StartStopToggle);
        serverclienttoggle = (ToggleButton) findViewById(R.id.ServerClientToggle);

        testconsolethread = new Thread(new TestConsoleThread());
        testtoggle.setOnClickListener(new StartButtonListener());

    }

    class ServerThread implements Runnable {
        public void run() {

        }
    }

    class ClientThread implements Runnable {
        public void run() {

        }
    }

    class TestConsoleThread implements Runnable {
        public void run() {
            while(true) {
                //Print a line every so often
                consoletextview.post(new Runnable() {
                    public void run() {
                        consoletextview.append("Hello World"+(System.currentTimeMillis()/1000.0)+'\n');
                    }
                });
                try { Thread.sleep(500); }
                //Break the loop if interrupted
                catch (InterruptedException e) { break;}
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
                    testconsolethread.interrupt();
                }
            }
        }
    }

}
