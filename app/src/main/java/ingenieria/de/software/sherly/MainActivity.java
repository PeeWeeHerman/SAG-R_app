package ingenieria.de.software.sherly;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MotionEventCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.Bundle;
import android.os.Handler;
import java.util.Set;
import java.util.UUID;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bluetooth:

        try
        {
            findBT();
            openBT();
            sendData("inicializa conexión");
        }
        catch (IOException ex) { }


        TextView txt = findViewById(R.id.texto);
        txt.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
                Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
                TextView txt = findViewById(R.id.texto);
                txt.setBackgroundColor(Color.BLUE);
                try {
                    sendData("Movio arriba");
                }catch (IOException ex) { }
            }

            public void onSwipeRight() {
                Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
                TextView txt = findViewById(R.id.texto);
                txt.setBackgroundColor(Color.RED);
                try {
                    sendData("Movio derecha");
                }catch (IOException ex) { }
            }

            public void onSwipeLeft() {
                Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
                TextView txt = findViewById(R.id.texto);
                txt.setBackgroundColor(Color.BLACK);
                try {
                    sendData("Movio izquierda");
                }catch (IOException ex) { }
            }

            public void onSwipeBottom() {
                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
                TextView txt = findViewById(R.id.texto);
                txt.setBackgroundColor(Color.YELLOW);
                try {
                    sendData("Movio abajo");
                }catch (IOException ex) { }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in Androidst.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // This example shows an Activity, but you would use the same approach if
// you were subclassing a View.
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                Toast.makeText(getApplicationContext(), "Action was DOWN", Toast.LENGTH_SHORT).show();
                return true;
            case (MotionEvent.ACTION_MOVE):
                Toast.makeText(getApplicationContext(), "Action was MOVE", Toast.LENGTH_SHORT).show();
                return true;
            case (MotionEvent.ACTION_UP):
                Toast.makeText(getApplicationContext(), "Action was UP", Toast.LENGTH_SHORT).show();
                return true;
            case (MotionEvent.ACTION_CANCEL):
                Toast.makeText(getApplicationContext(), "Action was CANCEL", Toast.LENGTH_SHORT).show();
                return true;
            case (MotionEvent.ACTION_OUTSIDE):

                Toast.makeText(getApplicationContext(), "Salio afuera", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onTouchEvent(event);

        }
    }




    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(getApplicationContext(),"No bluetooth adapter available",Toast.LENGTH_SHORT).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("Galaxy A30"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Toast.makeText(getApplicationContext(),"Bluetooth Device Found",Toast.LENGTH_SHORT).show();
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Toast.makeText(getApplicationContext(),"Bluetooth Opened",Toast.LENGTH_SHORT).show();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 46; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Toast.makeText(getApplicationContext(),data,Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }


    void sendData(String text) throws IOException
    {
        String msg = text;
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        Toast.makeText(getApplicationContext(),"Data send",Toast.LENGTH_SHORT).show();
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(getApplicationContext(),"Bluetooth Closed",Toast.LENGTH_SHORT).show();
    }

}