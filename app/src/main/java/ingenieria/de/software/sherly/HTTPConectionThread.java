package ingenieria.de.software.sherly;

import android.app.Activity;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ingenieria.de.software.sherly.model.Edge;
import ingenieria.de.software.sherly.model.Map;
import ingenieria.de.software.sherly.model.Node;

class HTTPConectionThread extends AsyncTask<String, String, String> {
    private MainActivity activity;


    public HTTPConectionThread(MainActivity activity){
        this.activity = activity;
    }
    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
     /*   HttpURLConnection conn = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(uri[0]);
            conn = (HttpURLConnection) url.openConnection();
            if(conn.getResponseCode() == HttpsURLConnection.HTTP_OK){
                // Do normal input or output stream reading
                inputStream = conn.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                responseString = bufferedReader.readLine();
            }
            else {
                responseString = "FAILED"; // See documentation for more info on response handling
            }
            inputStream.close();
        }  catch (IOException e) {
            Log.d("Estado SHERLY:","no se pudo conectar a " + uri);
            responseString = "No hay conexión";
        }
        finally {
            try{
                conn.disconnect();
            }catch(NullPointerException np){
                Log.d("Estado SHERLY:","no se pudo desconectar HTTP...");
            }
        }
*/
         responseString = "{\n" +
                "   \"nodes\":[\n" +
                "      {\n" +
                "         \"id\":2,\n" +
                "         \"title\":\"Nodo1:1\",\n" +
                "         \"x\":436,\n" +
                "         \"y\":158\n" +
                "      },\n" +
                "      {\n" +
                "         \"id\":3,\n" +
                "         \"title\":\"Nodo 2:2\",\n" +
                "         \"x\":769,\n" +
                "         \"y\":554\n" +
                "      },\n" +
                "      {\n" +
                "         \"id\":4,\n" +
                "         \"title\":\"Nodo 3:3\",\n" +
                "         \"x\":1021.2371215820312,\n" +
                "         \"y\":51.21833038330078\n" +
                "      },\n" +
                "      {\n" +
                "         \"id\":5,\n" +
                "         \"title\":\"Nodo 4:4\",\n" +
                "         \"x\":1126.8876953125,\n" +
                "         \"y\":433.29736328125\n" +
                "      }\n" +
                "   ],\n" +
                "   \"edges\":[\n" +
                "      {\n" +
                "         \"source\":1,\n" +
                "         \"target\":2,\n" +
                "         \"weight\":\"Camino de 1 a 2\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"source\":2,\n" +
                "         \"target\":3,\n" +
                "         \"weight\":\"Camino de 2 a 3\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"source\":3,\n" +
                "         \"target\":4,\n" +
                "         \"weight\":\"Camino de 3 a 4\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"source\":3,\n" +
                "         \"target\":1,\n" +
                "         \"weight\":\"Camino de 3 a 1\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try{
            Gson gson = new Gson();
            Map mapa = (Map)gson.fromJson(result, Map.class);
            Toast.makeText(activity,mapa.toString(),Toast.LENGTH_LONG).show();
            TextView text = activity.findViewById(R.id.mapa);
            text.setText(mapa.toString());
            activity.setMapa(mapa);
            //activity.t1.speak("Bienvenido a Guía,", TextToSpeech.QUEUE_FLUSH, null);
        }catch(Exception e){
            Log.d("Estado SHERLY:","No se pudo convertir a JSON la respusta");
        }
        //Do anything with response..
    }
}