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
  GoogleCredentials credentials = GoogleCredentials
          .fromStream(new FileInputStream("./google_api_credentials.json"))
          .createScoped("https://www.googleapis.com/auth/firebase.messaging");

  public NotificationManager() throws IOException {
  }

  public HttpResponse<String> sendNotification(JsonObject body) throws InterruptedException, URISyntaxException, IOException {
    credentials.refreshIfExpired();
    AccessToken token = credentials.getAccessToken();
    HttpClient client = HttpClient.newHttpClient();
    URI url = new URI("https://statcord-notifications.arbee.workers.dev/?code=" + token.getTokenValue());
    HttpRequest request = HttpRequest.newBuilder()
            .uri(url)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
