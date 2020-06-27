package dev.ksick.mapreactions.http;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import dev.ksick.mapreactions.Place;

public class GetRouteTask extends AsyncTask<String, Void, GetRouteResponse> {

    private GetRouteResponseListener responseListener = null;

    public GetRouteTask(GetRouteResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    @Override
    protected GetRouteResponse doInBackground(String... params) {
        try {
            URL url = new URL("https://api.map-reactions.ksick.dev/v0-1/route?phrase=" + params[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                return new GetRouteResponse(responseCode, null);
            }

            InputStreamReader responseStreamReader = new InputStreamReader(connection.getInputStream());

            List<Place> places = new Gson().fromJson(
                    responseStreamReader,
                    new TypeToken<ArrayList<Place>>() {
                    }.getType()
            );
            responseStreamReader.close();

            return new GetRouteResponse(responseCode, places);
        } catch (Exception e) {
            return new GetRouteResponse(-1, null);
        }
    }

    @Override
    protected void onPostExecute(GetRouteResponse getRouteResponse) {
        super.onPostExecute(getRouteResponse);
        if (getRouteResponse.statusCode == 200) {
            responseListener.onSuccess(getRouteResponse.route);
        } else {
            responseListener.onError(getRouteResponse.statusCode);
        }
    }
}

