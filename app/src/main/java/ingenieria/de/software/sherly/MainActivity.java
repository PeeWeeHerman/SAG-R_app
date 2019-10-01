package ingenieria.de.software.sherly;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;

public class MainActivity extends AppCompatActivity {
    //nombre del dispositivo emparejado (en nuestro caso va a ser dinámico)
    private static final  String DEVICE_NAME = "SHERLY";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    ProgressBar spinner;
    private Button mDiscoverBtn;
    private static BroadcastReceiver  mReceiver;
    /**
     Método que se ejecuta cuando se crea un Activity o pantalla
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //seteo el xml de layout que está en /res/layout
        setContentView(R.layout.activity_main);
        spinner = (ProgressBar)findViewById(R.id.loading);
        //tomo el TextView con id "tochPanel", ver content_main.xml
        TextView txt = findViewById(R.id.touchPanel);


        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("SHERLY","Entro BB");
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    spinner.setVisibility(View.VISIBLE);
                    //discovery starts, we can show progress dialog or perform other tasks
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //discovery finishes, dismis progress dialog
                    spinner.setVisibility(View.GONE);
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    spinner.setVisibility(View.GONE);
                    //bluetooth device found
                    mmDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(DEVICE_NAME.equals(mmDevice.getName())){
                        Toast.makeText(getApplicationContext(),"Se encontró el dispositivo Bluetooth" + mmDevice.getName(),Toast.LENGTH_LONG).show();
                        try {
                            openBT();
                            sendData(" Conecto con SHERLY Bluetooth");
                        }catch(Exception e){
                            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }

                }
            }
        };

        //Buscar los dispositivos bluetooth y conectarse a uno especificado
        //Para poder realizar la conexión con Bluetooth, se debió solicitar permisos al usuario para usarlo.
        //Para ello, en el archivo AndroidManifest.xml, se agregaron los permisos Bluetooth
        try
        {
            findBT();

        }
        catch (Exception ex) { }

        //le asigno evento Swipe
        //TODO: mover lo que hace con cada Swipe a un método para no repetir código
        txt.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
                Toast.makeText(MainActivity.this, "Swipe arriba!", Toast.LENGTH_LONG).show();
                //TODO: ver si se puede acceder directamente al padre del evento, es decir el textView y no volverlo a buscar con findViewById
                TextView txt = findViewById(R.id.touchPanel);
                //lo cambio de color
                txt.setBackgroundColor(Color.BLUE);
                try {
                    //envío un mensaje al dispositivo Bluetooth al cual me emparejé
                    sendData("Movio arriba");
                }catch (IOException ex) {
                    //TODO implementar
                }
            }

            public void onSwipeRight() {
                Toast.makeText(MainActivity.this, "Swipe derecha!", Toast.LENGTH_LONG).show();
                //TODO: ver si se puede acceder directamente al padre del evento, es decir el textView y no volverlo a buscar con findViewById
                TextView txt = findViewById(R.id.touchPanel);
                //lo cambio de color
                txt.setBackgroundColor(Color.RED);
                try {
                    //envío un mensaje al dispositivo Bluetooth al cual me emparejé
                    sendData("Movio derecha");
                }catch (IOException ex) { }
            }

            public void onSwipeLeft() {
                Toast.makeText(MainActivity.this, "Swipe izquierda!", Toast.LENGTH_LONG).show();
                //TODO: ver si se puede acceder directamente al padre del evento, es decir el textView y no volverlo a buscar con findViewById
                TextView txt = findViewById(R.id.touchPanel);
                //lo cambio de color
                txt.setBackgroundColor(Color.BLACK);
                try {
                    //envío un mensaje al dispositivo Bluetooth al cual me emparejé
                    sendData("Movio izquierda");
                }catch (IOException ex) { }
            }

            public void onSwipeBottom() {
                Toast.makeText(MainActivity.this, "Swipe abajo!", Toast.LENGTH_LONG).show();
                //TODO: ver si se puede acceder directamente al padre del evento, es decir el textView y no volverlo a buscar con findViewById
                TextView txt = findViewById(R.id.touchPanel);
                //lo cambio de color
                txt.setBackgroundColor(Color.YELLOW);
                try {
                    //envío un mensaje al dispositivo Bluetooth al cual me emparejé
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

    /**
     Método que se ejecuta cuando el usuario realiza gestos de tocar pantalla
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            //presionar pantalla
            case (MotionEvent.ACTION_DOWN):
                Toast.makeText(getApplicationContext(), "Action was DOWN", Toast.LENGTH_LONG).show();
                return true;
            //mover el dedo en la pantalla
            case (MotionEvent.ACTION_MOVE):
                Toast.makeText(getApplicationContext(), "Action was MOVE", Toast.LENGTH_LONG).show();
                return true;
            //soltar pantalla
            case (MotionEvent.ACTION_UP):
                Toast.makeText(getApplicationContext(), "Action was UP", Toast.LENGTH_LONG).show();
                return true;
            //cancelar evento, por ejemplo: cuando se superpone un evento más importante
            case (MotionEvent.ACTION_CANCEL):
                Toast.makeText(getApplicationContext(), "Action was CANCEL", Toast.LENGTH_LONG).show();
                return true;
            //cuando se sale el dedo de la pantalla
            case (MotionEvent.ACTION_OUTSIDE):

                Toast.makeText(getApplicationContext(), "Salio afuera", Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onTouchEvent(event);

        }
    }

    /**
     * @Link
     * dedique: 3 hs sabado 28/9,   lunes 23/9  5hs,  domingo 22/9  5hs
     * Cómo conectar bluetooth?
     * https://stackoverflow.com/questions/32656510/register-broadcast-receiver-dynamically-does-not-work-bluetoothdevice-action-f
     * it is suggested to use context.getSystemService(Context.BLUETOOTH_SERVICE)
     * to get the BluetoothAdapter on API 18+, according to official doc.
     *
     *Problema con los permisos:
     *
     * https://stackoverflow.com/questions/37377260/android-bluetoothdevice-action-found-broadcast-never-received
     * The problem here was that permission had to be granted at runtime, and not only in the manifest,
     * since ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION are grouped in the DANGEROUS permissions group.
     * */
    void findBT()
    {

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        //Obtener un objeto que representa el dispositivo de bluetooth que posee el móvil

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(getApplicationContext(),"Bluetooth no disponible",Toast.LENGTH_LONG).show();
        }
        //Bluetooth está disponible, pero no habilitado
        if(!mBluetoothAdapter.isEnabled())
        {
            //intentar habilitarlo
            Toast.makeText(getApplicationContext(),"Encendiendo Bluetooth...",Toast.LENGTH_LONG).show();
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }


        /*BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                // BLE device was found, we can get its information now
                Toast.makeText(getApplicationContext(),"Se encontró el dispositivo Bluetooth" + device.getName(),Toast.LENGTH_LONG).show();
            }
        };*/

        // This callback is added to the start scan method as a parameter in this way
        //mBluetoothAdapter.startLeScan(mLeScanCallback);

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();


        Toast.makeText(getApplicationContext(),"Se inicia búsqueda Bluetooth",Toast.LENGTH_LONG).show();

        //obtengo los dipositivos emparejados con el móvil
        /*Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                //me conecto a uno específico
                if(device.getName().equals(DEVICE_NAME))
                {
                    mmDevice = device;
                    break;
                }
            }
        }*/
        //Toast.makeText(getApplicationContext(),"Se encontró el dispositivo Bluetooth",Toast.LENGTH_LONG).show();
    }


    /**
     Inicia un socket bluetooth de escucha para la comunicación
     */
    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Toast.makeText(getApplicationContext(),"tunel Bluetooth abierto",Toast.LENGTH_LONG).show();
    }

    /**
     Recibe desde el socket de bluetooth
     ver método openBT() donde inicializa la conexión
     */
    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //Utilizamos el punto (.) como último caracter para entender que el emisor termina de mandarnos datos

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            //Un hilo nuevo para correr en background
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        //tomo los bytes que vienen del socket
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            //voy leyendo los bytes disponibles
                            mmInputStream.read(packetBytes);
                            //por cada byte que leemos, iteramos
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                //hasta que el emisor no envie un caracter de fin de mensaje, seguimos leyendo (definimos que sea el punto , linea 215)
                                if(b == delimiter)
                                {
                                    //cuando manda un punto, mostramos el mensaje enviado, decodificandolo con US-ASCII
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    //delegar la presentación del mensaje en pantalla a otro thread para seguir leyendo mientras
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Toast toast = Toast.makeText(getApplicationContext(),data,Toast.LENGTH_LONG);
                                            LinearLayout toastLayout = (LinearLayout) toast.getView();
                                            TextView toastTV = (TextView) toastLayout.getChildAt(0);
                                            toastTV.setTextSize(30);
                                            toast.show();
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
        //inicia el thread que escucha desde un socket bluetooth y escribe en pantalla
        workerThread.start();
    }

    /**
     Enviar al socket bluetooth
     */
    void sendData(String text) throws IOException
    {
        if(mmOutputStream != null){
            String msg = text;
            msg += "\n";
            mmOutputStream.write(msg.getBytes());
            Toast.makeText(getApplicationContext(),"Se envió el mensaje: " + msg + " al dispositivo Bluetooth",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(),"No hay conexión con el dispositivo Bluetooth",Toast.LENGTH_LONG).show();
        }
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(getApplicationContext(),"Se cerró la conexión Bluetooth",Toast.LENGTH_LONG).show();
    }

}