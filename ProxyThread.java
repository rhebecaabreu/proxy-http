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
    try {
      String requestString;

      proxyToClientBr = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
      proxyToClientBw = new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream()));

      requestString = proxyToClientBr.readLine();

      // Get the Request type
      String request = requestString.substring(0, requestString.indexOf(' '));
      // remove request type and space
      String urlString = requestString.substring(requestString.indexOf(' ') + 1);

      String http = urlString.substring(urlString.indexOf(' '));

      // Remove everything past next space
      urlString = urlString.substring(0, urlString.indexOf(' '));
      // Prepend http:// if necessary to create correct URL
      if (!urlString.substring(0, 4).equals("http")) {
        String temp = "http://";
        urlString = temp + urlString;
      }
      
      if (request.equals("GET")) {
        System.out.println("Request Received " + requestString); 
        // ProxyCache p = new ProxyCache(proxyToClientBw, proxyToClientBr, socketClient);
        // p.createCacheFile();
        // File file;
        // if ((file = p.getCachedPage(urlString)) != null) {
        //   System.out.println("Cached Copy found for : " + urlString + "\n");
        //   p.sendCachedPageToClient(file);
        // } else {
          sendData(urlString);
        
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    // } catch (ClassNotFoundException e) {
    //   // TODO Auto-generated catch block
    //   e.printStackTrace();
    // }
   
  }

  public void sendData(String urlString) {

    try {
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

      fileName = fileName + fileExtension;
      
      if(!checkIfIsImage(fileExtension, urlString, fileName)) {
        
        // Create the URL
        URL remoteURL = new URL(urlString);
        // Create a connection to remote server
        HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
        proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        proxyToServerCon.setRequestProperty("Content-Language", "en-US");
        proxyToServerCon.setUseCaches(false);
        proxyToServerCon.setDoOutput(true);

        // Create Buffered Reader from remote Server
        BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));

        // Send success code to client
        String line = "HTTP/1.1 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
        proxyToClientBw.write(line);

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
        String error = "HTTP/1.1 404 NOT FOUND\n" + "Proxy-agent: Proxy/1.0\n" + "\r\n";
        proxyToClientBw.write(error);
        proxyToClientBw.flush();
        return false; 
      }

      return true;
    } else {
      return false; 
    }
  }
}