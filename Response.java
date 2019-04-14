
import java.io.*;
import java.util.*;
import java.net.*;

public class Response { 
  BufferedReader reader;
  ArrayList<String> linesResponseFromServer = new ArrayList<>();

  public Response(BufferedReader reader) {
    this.reader = reader;
  }

  public void readResponseFromServer() {
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        linesResponseFromServer.add(line);
      }

      // Close Down Resources
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      System.out.println("Error trying to get response from server");
      e.printStackTrace();
    }
  }

  public void sendResponseToClient(BufferedWriter outputStream) {
    try {
      for (String line : linesResponseFromServer) {
        System.out.println("[RES]:> " + line);
        outputStream.write(line + "\r\n");
        // outputStream.flush();
      }
      outputStream.write("\r\n");
      // outputStream.flush();

      // Close down resources
      if (outputStream != null) {
        outputStream.close();
      }
    } catch (IOException e) {
      System.out.println("Error trying to send response to client");
      e.printStackTrace();
    }
  }
}