package ingenieria.de.software.sherly;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ingenieria.de.software.sherly.model.Camino;
import ingenieria.de.software.sherly.model.Nodo;

class HTTPConectionThread extends AsyncTask<String, String, String> {
    private Activity activity;


    public HTTPConectionThread(Activity activity){
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
            List<Nodo> nodos = (List<Nodo>) gson.fromJson(result,Nodo.class);
            Toast.makeText(activity,result,Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Log.d("Estado SHERLY:","No se pudo convertir a JSON la respusta");
        }
        //Do anything with response..
    }
}