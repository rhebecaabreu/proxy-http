import java.io.*;
import java.util.*;
import java.net.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

public class Request {
  private Socket socketServer;
  private int defaultPort = 80;
  public String method = "GET", path = "/", version = "HTTP/1.1", host="";
  private HashMap<String, String> headerRequest = new HashMap<>();
  public BufferedReader reader;
  public BufferedWriter outputStream;
  public String addressHost="";

  public Request(BufferedReader reader) {
    this.reader = reader;
  }

  public void readRequestFromClient() {
    String requestString;

    try {
      requestString = reader.readLine();
      formatRequest(requestString);
    } catch (Exception e) {
      System.out.println("Error reading request from client");
      e.printStackTrace();
    }
  }

  public void formatRequest(String requestString) {

    try {
      String[] split = requestString.split(" ");

      method = split[0]; // GET
      path = split[1]; // host-url
      version = split[2]; // HTTP/1.1

      if (!path.substring(0, 4).equals("http")) {
        String temp = "http://";
        path = temp + path;
      }
      addressHost = path;

      URL url = new URL(path);
      host = url.getHost();
      path = url.getPath();

      headerRequest.put("Host", host);
    } catch (Exception e) {
      System.out.println("Error with URL path.");
      e.printStackTrace();
    } 
  }

  public BufferedReader sendRequestToServer(Socket socketClient) {
    try {
      if (!("GET".equals(method))) {
        System.out.println("We can only read GET requests. Sorry!");
        return null;
      } else {
        System.out.println("[REQUEST] "+ method + " " + path + " " + version);

        // Compute a logical file name as per schema
        // This allows the files on stored on disk to resemble that of the URL it was
        // taken from
        String fileExtension;

        // Get the type of file
        fileExtension = getExtension(new File(path));

        // Trailing / result in index.html of that directory being fetched
        if (fileExtension.contains("/")) {
          fileExtension = fileExtension.replace("/", "__");
          fileExtension = fileExtension.replace('.', '_');
          fileExtension += ".html";
        }

        // Check if file is an image
        if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
            fileExtension.contains(".jpeg") || fileExtension.contains(".gif") || fileExtension.contains(".bmp")) {
          
              System.out.println("IS AN IMAGE!");
          isImage(addressHost, socketClient);

          return null;

        } else {
          InetAddress address = InetAddress.getByName((headerRequest.get("Host")));
          System.out.println("[REQ]:>> Connecting to " + address);
          socketServer = new Socket(address, defaultPort);

          headerRequest.put("Connection", "close");
          headerRequest.put("Content-Type", "application/x-www-form-urlencoded");
          headerRequest.put("Content-Language", "en-US");

          outputStream = new BufferedWriter(
              new OutputStreamWriter(socketServer.getOutputStream(), StandardCharsets.UTF_8));
          reader = new BufferedReader(new InputStreamReader(socketServer.getInputStream(), StandardCharsets.UTF_8));

          writeRequest(method + " " + path + " " + version);

          for (String key : headerRequest.keySet()) {
            writeRequest(key + ": " + headerRequest.get(key));
          }
          writeRequest("");

          return reader;
        }    
      }
    } catch (IOException e) {
      System.out.println("Error trying to send request to server");
      e.printStackTrace();
      return null;
    }
  }

  private void writeRequest(String line) {
    try {
      System.out.println("[REQ]:> " + line);
      outputStream.write(line + "\r\n");
      // outputStream.write("\r\n");

      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // reads the image and sends response to client
  public void isImage(String imageUrlString, Socket socketClient) {
    try {
      URL imageURL = new URL(imageUrlString);
      
      BufferedImage bufferedImage = ImageIO.read(imageURL);
      final DataOutputStream outToClient = new DataOutputStream(socketClient.getOutputStream());

      if (bufferedImage == null) {
        System.out.println("Sending 404 to client as image wasn't received from server" + imageUrlString);
        String error = "HTTP/1.1 404 NOT FOUND\r\n" + "Server: Proxy Server/1.0\n" + "\r\n";
        outToClient.writeBytes(error);
        System.out.println(error);
        throw new IOException("Failed to download image");
      }
      
      String response = "";
      response += "HTTP/1.1 200 OK\r\n";
      response += "Server: Proxy Server/1.0\r\n";
      response += "Connection: Close\r\n";
      response += "Content-Type: image/png\r\n\r\n";

      System.out.println(response);
      outToClient.writeBytes(response);

      int lastIndexOfDot = imageUrlString.lastIndexOf(".");
      String extension = imageUrlString.substring(lastIndexOfDot + 1, imageUrlString.length());

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, extension, byteArrayOutputStream);
      ImageIO.write(bufferedImage, extension, socketClient.getOutputStream());

      outToClient.close();

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private static String getExtension(File file) {
    String name = file.getName();
    int lastIndexOf = name.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return ""; // empty extension
    }
    return name.substring(lastIndexOf);
  }

}