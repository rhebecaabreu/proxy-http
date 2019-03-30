import java.net.*;
import java.io.*;
import java.util.*;

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

      while (true) {
        ProxyThread proxythread = new ProxyThread(server.accept(), cacheSize);
        Thread thread = new Thread(proxythread);
        thread.start();
      }    
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.err.println("Usage: java Proxy " + "<localport> <cache size>");
    }
  }

 
}