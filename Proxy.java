import java.io.*;
import java.util.*;
import java.net.*;

public class Proxy {
  public static void main(String[] args) {
    try {
      if (args.length != 2) {
        throw new IllegalArgumentException("Wrong number of arguments.");
      }

      int localport = Integer.parseInt(args[0]);
      int cacheSize = Integer.parseInt(args[1]);

      ServerSocket server = new ServerSocket(localport);
      System.out.println("Starting proxy on port " + server.getLocalPort()); // And start running the server
      System.out.println("Waiting client connection...");

      while (true) {
        ProxyServer proxyServer = new ProxyServer(server.accept(), cacheSize);
        Thread thread = new Thread(proxyServer);
        thread.start();
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.err.println("Usage: java Proxy " + "<localport> <cache size in MB>");
    }
  }

}