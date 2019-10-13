package ingenieria.de.software.sherly;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;

public class MainActivity extends AppCompatActivity {
    //nombre del dispositivo emparejado (en nuestro caso va a ser dinámico)
    private static final  String DEVICE_NAME = "SHERLY";
    private final String DEBUG_TAG = "Estado de SHERLY: ";
    private final int POSITION_REQUEST_CODE = 1;
    private final int VOICE_REQUEST_CODE = 2;

    //Instancia del driver de bluetooth
    BluetoothAdapter mBluetoothAdapter;
    //Instancias de la interfaz del driver para entrada y salida
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    //Instancia de datos de entrada y salida de la interfaz
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    //Hilo para el manejo de datos desde la interfaz
    Thread workerThread;
    byte[] readBuffer;
    //posición del buffer que vamos leyendo
    int readBufferPosition;
    //flag que indica cuando dejar de leer del socket
    volatile boolean stopWorker;
    //imagen de "cargando"
    ProgressBar spinner;
    //TextView utilizado para las interacciones SWIPE (mide el tamaño de la pantalla)
    TextView touchPanel;
    //Interceptor para el evento de encontrar a SHERLY
    private static BroadcastReceiver  mReceiver;
    TextToSpeech t1;

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
        touchPanel = findViewById(R.id.touchPanel);
        mReceiver = getBroadcastReceiver();

        // Pido permisos para activar el Bluetooth
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, POSITION_REQUEST_CODE);
        }else{
            findBluetooth();
        }

        //Defino comportamiento Swipe
        touchPanel.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                try {
                    startActivityForResult(intent, VOICE_REQUEST_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Lo siento, no hay microfono",
                            Toast.LENGTH_SHORT).show();
                }
                notifyMovement("Movió arriba", Color.YELLOW);
            }
            public void onSwipeRight() {
                notifyMovement("Movió derecha", Color.RED);
            }
            public void onSwipeLeft() {
                notifyMovement("Movió izquierda", Color.BLUE);
            }
            public void onSwipeBottom() {
                notifyMovement("Movió abajo", Color.BLACK);
            }
        });


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(new Locale("es", "AR"));
                }
            }
        });
    }

    private void findBluetooth(){
        //Buscar los dispositivos bluetooth y conectarse a uno especificado
        //Para poder realizar la conexión con Bluetooth, se debió solicitar permisos al usuario para usarlo.
        //Para ello, en el archivo AndroidManifest.xml, se agregaron los permisos Bluetooth
        try
        {
            findBT();
        }
        catch (Exception ex) {
            Log.d(DEBUG_TAG, "No se pudo utilizar el Bluetooth" + ex.getMessage());
            Toast.makeText(MainActivity.this, "Bluetooth no disponible, reintente nuevamente" , Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case VOICE_REQUEST_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Toast.makeText(getApplicationContext(),result.get(0).toString(),Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
    /**
     * Cuando responde por los permisos el usuario
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Check which request we're responding to
        if (requestCode == POSITION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findBluetooth();
                // Camera permission has been granted, preview can be displayed
                Log.d(DEBUG_TAG, "Se obtuvieron permisos de posición.");
            } else {
                Log.d(DEBUG_TAG, "No se obtuvieron permisos de posición.");
                finish();
                System.exit(0);

            }
        }

    }

/**
 * Notifica al usuario y al bluetooth el movimiento de deslizar que hizo
 * */
    private void notifyMovement(String movement, int color){
        Toast.makeText(MainActivity.this, movement, Toast.LENGTH_LONG).show();
        //lo cambio de color
        touchPanel.setBackgroundColor(color);
        try {
            //envío un mensaje al dispositivo Bluetooth al cual me emparejé
            sendData(movement);
        }catch (IOException ex) {
            Log.d(DEBUG_TAG , "no se pudo notificar el movimiento");
        }
    }
/**
 * Devuelve un interceptor de eventos de Bluetooth
 * */
    private BroadcastReceiver getBroadcastReceiver(){
        return new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.d(DEBUG_TAG, "Buscando dispositivos Bluetooth...");
                    spinner.setVisibility(View.VISIBLE);
                    //discovery starts, we can show progress dialog or perform other tasks
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(DEBUG_TAG, "Búsqueda Bluetooth finalizada.");
                    spinner.setVisibility(View.GONE);
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    Log.d(DEBUG_TAG, "Se encontró un dispositivo Bluetooth");
                    spinner.setVisibility(View.GONE);
                    //bluetooth device found
                    mmDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = mmDevice.getName();
                    if(DEVICE_NAME.equals(deviceName)){
                        Log.d(DEBUG_TAG, "Se encontró a " + deviceName);
                        Toast.makeText(getApplicationContext(),"Se encontró a " + deviceName,Toast.LENGTH_LONG).show();
                        openBT();
                    }

                }
            }
        };
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
    void findBT () throws Exception
    {
        //Obtener una instancia que representa al Driver de Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d(DEBUG_TAG, "Bluetooth no disponible");
            Toast.makeText(getApplicationContext(),"Bluetooth no disponible",Toast.LENGTH_LONG).show();
            throw new Exception("Adaptador no disponible");
        }
        //Bluetooth está disponible, pero no habilitado
        if(!mBluetoothAdapter.isEnabled())
        {
            Log.d(DEBUG_TAG,"Solicitando al usuario activar Bluetooth...");
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        spinner.setVisibility(View.VISIBLE);
        //obtengo los dipositivos emparejados con el móvil
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                //me conecto a uno específico
                if(device.getName().equals(DEVICE_NAME))
                {
                    Log.d(DEBUG_TAG, "Se encontró a dispositivo " + device.getName());
                    mmDevice = device;
                    openBT();
                    spinner.setVisibility(View.GONE);
                    return;
                }
            }
        }

        if(mmDevice == null){
            Log.d(DEBUG_TAG, "No se encontró " + DEVICE_NAME + " vinculado. Se inicia búsqueda...");
            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();

            Toast.makeText(getApplicationContext(),DEVICE_NAME + " no vinculado, se inicia búsqueda...",Toast.LENGTH_LONG).show();
        }
    }


    /**
     Inicia un socket bluetooth de escucha para la comunicación
     */
    void openBT()
    {
        try {
            Toast.makeText(getApplicationContext(), "Conectando con " + mmDevice.getName() + "...", Toast.LENGTH_LONG).show();

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            beginListenForData();
            sendData("Conectado con " + mmDevice.getName());
            Log.d(DEBUG_TAG, "Túnel bluetooth abierto");
        }catch(IOException e){
            Log.d(DEBUG_TAG, "No se pudo conectar con " + mmDevice.getName());
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
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
                            Log.d(DEBUG_TAG,"Se recibió data de "+ mmDevice.getName() + ". Leyendo...");
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
                                    Log.d(DEBUG_TAG,"Fin del mensaje.");
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
                                            t1.speak(data, TextToSpeech.QUEUE_FLUSH, null);
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
                        Log.d(DEBUG_TAG, "Error en la comunicación: " +ex.getMessage());
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
            Log.d(DEBUG_TAG, " Se enviará " + text + " a " +mmDevice.getName());
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