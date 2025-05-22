package es.upm.btb.microphone;

import android.util.Log;
import org.json.JSONObject;
import okhttp3.*;
import java.io.IOException;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
    private static final OkHttpClient client = new OkHttpClient();

    // Método para enviar telemetría de aceleración por HTTP POST
    public static String sendTelemetryData(JSONObject telemetryData, String targetUrl) {
        // Convertir el JSON a cadena y crear el cuerpo de la solicitud
        String json = telemetryData.toString();
        RequestBody body = RequestBody.create(
                MediaType.get("application/json; charset=utf-8"), json);

        // Crear la solicitud HTTP POST
        Request request = new Request.Builder()
                .url(targetUrl)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Verificar si la solicitud fue exitosa
            Log.d("telemetry", "Respuesta del servidor: " + response);
            if (response.isSuccessful()) {
                return response.body().string();  // Devolver la respuesta del servidor
            } else {
                Log.e(TAG, "HTTP request failed with code: " + response.code());
                return "Error: " + response.code();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error during HTTP POST request: "+e.getMessage(), e);
            return "Request failed: " + e.getMessage();
        }
    }
}
