package ingenieria.de.software.sherly;

import android.app.Activity;
import android.os.AsyncTask;
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
        HttpURLConnection conn = null;
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
        }catch(Exception e){
            Log.d("Estado SHERLY:","No se pudo convertir a JSON la respusta");
        }
        //Do anything with response..
    }
}