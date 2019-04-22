import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ProxyServer implements Runnable {
  private Socket socketClient;
  private int cacheSize;
  private ProxyCacheLRU proxyCacheLRU; 

  public ProxyServer(Socket socket, int cacheSize){
    this.socketClient = socket;
    this.cacheSize = cacheSize;
    this.proxyCacheLRU = new ProxyCacheLRU(cacheSize);
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
      String cachedData="";
      boolean isCached = false; 
      if (readerServer != null) {
        Response response = new Response(readerServer);

        synchronized (proxyCacheLRU) {
          System.out.println(readRequest.addressHost);
          if (proxyCacheLRU.containsKey(readRequest.addressHost)) {
            cachedData = proxyCacheLRU.get(readRequest.addressHost);
            System.out.println("Cache hit........" + readRequest.addressHost);
            System.out.println(cachedData);
            System.out.println("paozinhho");
            isCached = true;
          } else {
            cachedData = response.readResponseFromServer();
            response.sendResponseToClient(outputStream);

            if (cachedData.length() > cacheSize) {
              System.out.println("File size larger than cache....");
            } else {
              String cache_miss = proxyCacheLRU.put(readRequest.addressHost, cachedData);
              System.out.println("Cache missed.........." + cache_miss);
            }
          }
        }
        if(isCached) {
          outputStream.write(cachedData);
        }
      }
    } catch (Exception e) {
      System.out.println("Error trying to start server");
      e.printStackTrace();
    }
  }
}