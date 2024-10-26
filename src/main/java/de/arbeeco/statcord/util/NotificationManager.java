package de.arbeeco.statcord.util;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NotificationManager {
  String device_token = null;

  public NotificationManager(String device_token) {
    this.device_token = device_token;
  }

  public HttpResponse<String> sendNotification(JsonObject body) throws IOException, URISyntaxException, InterruptedException {
    GoogleCredentials credentials = GoogleCredentials
            .fromStream(new FileInputStream("./google_api_credentials.json"))
            .createScoped("https://www.googleapis.com/auth/firebase.messaging");
    credentials.refreshIfExpired();
    AccessToken token = credentials.getAccessToken();
    HttpClient client = HttpClient.newHttpClient();
    URI url = new URI("https://statcord-notifications.arbee.workers.dev/?code=" + token.getTokenValue());
    String bodyString = body.toString();
    bodyString = bodyString.replace("$token", device_token);
    HttpRequest request = HttpRequest.newBuilder()
            .uri(url)
            .POST(HttpRequest.BodyPublishers.ofString(bodyString))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
