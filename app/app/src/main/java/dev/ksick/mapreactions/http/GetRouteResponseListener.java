package dev.ksick.mapreactions.http;

import java.util.List;

import dev.ksick.mapreactions.Place;

public interface GetRouteResponseListener {

    void onSuccess(List<Place> route);

    void onError(int statusCode);
}