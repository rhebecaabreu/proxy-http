import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ProxyServer implements Runnable {
  private Socket socketClient;
  private int cacheSize;

  public ProxyServer(Socket socket, int cacheSize){
    this.socketClient = socket;
    this.cacheSize = cacheSize;
  }

  @Override
  public void run() {
    // Cria um novo thread para transmitir mensagens do cliente para o servidor.
    new Thread() {
      @Override
      public void run() {
        startServer();
      }
    }.start();
  }

  public void startServer() {
    try {
      socketClient.setSoTimeout(2000);

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(socketClient.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter outputStream = new BufferedWriter(
          new OutputStreamWriter(socketClient.getOutputStream(), StandardCharsets.UTF_8));

      Request readRequest = new Request(reader);
      readRequest.readRequestFromClient();
      BufferedReader readerServer = readRequest.sendRequestToServer(socketClient);

      // was a image?
      if (readerServer != null) {
        Response response = new Response(readerServer);
        response.readResponseFromServer();
        response.sendResponseToClient(outputStream);
      }
    } catch (Exception e) {
      System.out.println("Error trying to start server");
      e.printStackTrace();
    }
  }
}