import java.net.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class ProxyThread implements Runnable {
  private Socket socketClient;
  private int cacheSize;
  BufferedWriter proxyToClientBw = null;
  BufferedReader proxyToClientBr = null;

  public ProxyThread(Socket socket, int cacheSize) {
    this.socketClient = socket;
    this.cacheSize = cacheSize;
  }

  @Override
  public void run() {
    new Thread() {
      @Override
      public void run() {
        recieveFromClient();
      }
    }.start();
  }

  private void recieveFromClient() {    
    StringTokenizer tokenizedLine;
    String requestString;

    try {
      proxyToClientBr = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
      proxyToClientBw = new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream()));

      requestString = proxyToClientBr.readLine();
     
      tokenizedLine = new StringTokenizer(requestString); //TODO arrumar isso aqui, tem hora que da null pointer 
      
      String request="", http="", urlString="";
      while(tokenizedLine.hasMoreTokens()) { 
        request = tokenizedLine.nextToken().toString();
        urlString = tokenizedLine.nextToken().toString();
        http = tokenizedLine.nextToken().toString();

        if (!urlString.substring(0, 4).equals("http")) {
          String temp = "http://";
          urlString = temp + urlString;
        }

        if (request.equals("GET")) {
          System.out.println("Request Received " + requestString);
          sendDataToServer(urlString, request, http);
        } else {
          System.out.println("Bad Request Message");
        }
        request = null;
        urlString = null;
        http = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  
  public String request(String urlString, String requestType, String httpVersion) {
    String line = requestType + " / " + httpVersion + "\r\n" +
                  "Host: "+ urlString + "\r\n" +
                  "Connection: close\n\r\n";
    System.out.println(line);

    return line;
  }


  public void sendDataToServer(String urlString, String requestType, String httpVersion) {
    try {
      String[]file = fileName(urlString);
      String fileName=file[0];
      String fileExtension=file[1];

      if(!checkIfIsImage(fileExtension, urlString, fileName)) {
        // Create the URL
        URL remoteURL = new URL(urlString);

        // Create a connection to remote server
        HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
        proxyToServerCon.setUseCaches(false);
        proxyToServerCon.setDoOutput(true);
        proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        proxyToServerCon.setRequestProperty("Content-Language", "en-US");

        BufferedWriter proxyToServerBw = new BufferedWriter(new OutputStreamWriter(proxyToServerCon.getOutputStream()));
        proxyToServerBw.write(request(urlString, requestType, httpVersion));
 
        // System.out.println(proxyToServerCon.getHeaderFields());
        BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));

        if(proxyToServerCon.getResponseCode()==200) {
          // Send success code to client
          String line = "HTTP/1.1 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
          proxyToClientBw.write(line);

          System.out.println("\nHTTP/1.1 " + proxyToServerCon.getResponseCode() + " OK");
          System.out.println("Content-Length: " + proxyToServerCon.getContentLength());
          System.out.println("Content-Type: " + proxyToServerCon.getContentType()+"\r\n");

          // Read from input stream between proxy and remote server
          while ((line = proxyToServerBR.readLine()) != null) {
            // Send on data to client
            proxyToClientBw.write(line);
          }

          // Ensure all data is sent by this point
          proxyToClientBw.flush();

          // Close Down Resources
          if (proxyToServerBR != null) {
            proxyToServerBR.close();
          }
        } else {

        }
      }
      if (proxyToClientBw != null) {
        proxyToClientBw.close();
      }
    }catch(IOException e) {
      e.printStackTrace();
    }
    
  }

  // Check if file is an image
  public boolean checkIfIsImage(String fileExtension, String urlString, String fileName) throws IOException { 
    if ((fileExtension.contains(".png")) || fileExtension.contains(".jpg") || fileExtension.contains(".jpeg")
        || fileExtension.contains(".gif")) {
      
      // Create the URL
      URL remoteURL = new URL(urlString);
      BufferedImage image = ImageIO.read(remoteURL);

      if (image != null) {
        // Send response code to client
        String line = "HTTP/1.1 200 OK\n" + "Proxy-agent: Proxy/1.0\n" + "\r\n";
        proxyToClientBw.write(line);
        proxyToClientBw.flush();

        // Send them the image data
        ImageIO.write(image, fileExtension.substring(1), socketClient.getOutputStream());

      } else {
        System.out.println("Sending 404 to client as image wasn't received from server" + fileName);
        String error = "HTTP/1.1 404 NOT FOUND\r\n" + "Proxy-agent: Proxy/1.0\n" + "\r\n";
        proxyToClientBw.write(error);
        proxyToClientBw.flush();
        return false; 
      }
      return true;
    } else {
      return false; 
    }
  }

  public String[] fileName(String urlString) {
    // Compute a logical file name as per schema
    // This allows the files on stored on disk to resemble that of the URL it was
    // taken from
    int fileExtensionIndex = urlString.lastIndexOf(".");
    String fileExtension;

    // Get the type of file
    fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

    // Get the initial file name
    String fileName = urlString.substring(0, fileExtensionIndex);

    // Trim off http://www. as no need for it in file name
    fileName = fileName.substring(fileName.indexOf('.') + 1);

    // Remove any illegal characters from file name
    fileName = fileName.replace("/", "__");
    fileName = fileName.replace('.', '_');

    // Trailing / result in index.html of that directory being fetched
    if (fileExtension.contains("/")) {
      fileExtension = fileExtension.replace("/", "__");
      fileExtension = fileExtension.replace('.', '_');
      fileExtension += ".html";
    }
    String[] file = new String[2];
    file[0]=fileName = fileName + fileExtension;; 
    file[1]=fileExtension;

    return file;
  }
}