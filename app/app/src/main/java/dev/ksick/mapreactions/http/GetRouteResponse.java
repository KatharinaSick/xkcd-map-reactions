package dev.ksick.mapreactions.http;

import java.util.List;

import dev.ksick.mapreactions.Place;

public class GetRouteResponse {
    int statusCode;
    List<Place> route;

    public GetRouteResponse(int statusCode, List<Place> route) {
        this.statusCode = statusCode;
        this.route = route;
    }
}
