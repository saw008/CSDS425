// package CSDS425Project1;

// Importing necessary libraries
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class myServer {
    public static void main(String[] args) throws IOException {

        // To read the config, parse the different fields.
        int portNum = Integer.parseInt(myReader("config.txt").split(",")[0]);
        final boolean ifPersistent = Boolean.parseBoolean(myReader("config.txt").split(",")[1]);
        final int connectionWaitTime = Integer.parseInt(myReader("config.txt").split(",")[2]);
        System.out.println("Port number: <" + portNum + ">\nIf persistent: <" + ifPersistent +
                ">\nWait time(in seconds): <" + connectionWaitTime + ">\n\n");

        final String myCaseID = "/hxl1033";
        // final String myCaseID = "";
        final String specialCookie = "Long-live-rock=10001";

        // Read all my html files, and save them as strings.
        final String contentTest1 = myReader("test1.html");
        final String contentTest2 = myReader("test2.html");
        final String contentVisits = myReader("visits.html");
        final String content404 = myReader("404.html");

        // Initialize the welcoming socket and a threadPool.
        ServerSocket welcomeSocket = new ServerSocket(portNum);
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

        // Infinite loop to receive streams from client and process them.
        while (true) {
            final Socket connectionSocket = welcomeSocket.accept();
            System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
            System.out.println("Connection received from " + connectionSocket.getInetAddress().getHostName()
                    + ":" + connectionSocket.getPort() + "\nSocket Hashcode: " + connectionSocket.hashCode());

            // A thread starts from here...
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // Now wait for incoming requests, block and listen.
                    BufferedReader fromClient;
                    OutputStream toClient;
                    try {
                        if (ifPersistent) {
                            // Set the wait time if the connection is persistent.
                            connectionSocket.setSoTimeout(connectionWaitTime * 1000);
                            System.err.println("Connection persistent timeout set");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    do {
                        try {
                            // Setting the input and output streams.
                            fromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                            toClient = connectionSocket.getOutputStream();

                            // Set a request variable to accept all the lines of a request.
                            StringBuilder request = new StringBuilder();
                            String line;
                            while ((line = fromClient.readLine()) != null) {
                                request.append(line).append("\r\n");
                                if (request.toString().endsWith("\r\n\r\n")) {
                                    // If the request ends with a "\r\n",
                                    // it means it has reached the end of request message.
                                    // So we break from accepting lines from input stream.
                                    break;
                                }
                            }
                            if (line == null) {
                                // If the incoming line is captured as null, we break from the loop and shutdown the socket.
                                System.out.println("Client has shutdown the connection. Socket will be closed now!");
                                connectionSocket.shutdownInput();
                                connectionSocket.shutdownOutput();
                                connectionSocket.close();
                                break;
                            }
                            // Un-comment the following line to see the actual content of each request.
//                            System.out.println("{" + request + "}");
                            String response;
                            String[] requestLines = request.toString().split("\r\n");

                            String[] firstLineOfRequest = requestLines[0].split(" ");
                            String requestURI = firstLineOfRequest[1];

                            int specialCookieValue;
                            int visitsCounter;
                            // Handling the cookie field.
                            if (request.toString().contains("Cookie")) {
                                String tmpCookieLine = request.substring(request.indexOf("Cookie"));
                                String cookieLine = tmpCookieLine.substring(0, tmpCookieLine.indexOf("\r\n")) + ";";
                                String specialCookieName = specialCookie.split("=")[0];
                                boolean specialCookieExistence = cookieLine.contains(specialCookieName);
                                String tmpKeyword = specialCookieName + "=[0-9]+";

                                if (specialCookieExistence) {
                                    // If the cookie field is existing, and the cookie in our is domain is also ..
                                    // .. existing, we extract that value and increment by one.
                                    //TODO: The way parsing cookie value is now too complicated, there must be a easier way.
                                    String tmpCookieValueString = (cookieLine).replaceAll(cookieLine.split(tmpKeyword)[0], "").replaceAll(cookieLine.split(tmpKeyword)[1], "").split("=")[1];
                                    System.out.println("<<<<<Special Cookie value: " + tmpCookieValueString + ">>>>>");
                                    specialCookieValue = Integer.parseInt(tmpCookieValueString) + 1;
                                    // Since the base of the cookie in our domain is 10000, we need to minus the ..
                                    // .. cookie value by 10000 to get the counter value.
                                    visitsCounter = specialCookieValue - 10000;
                                } else {
                                    // If the cookie field is existing, but the cookie in our domain is not ..
                                    // .. existing, we initialize it and add it into the cookie field.
                                    System.out.println("<<<<<Special Cookie does not exist.>>>>>");
                                    specialCookieValue = 10001;
                                    visitsCounter = specialCookieValue - 10000;
                                }
                            } else {
                                // If the cookie field is not existing at all, we create that field and add the ..
                                // .. cookie in our domain.
                                System.out.println("<<<<<Cookie does not exist.>>>>>");
                                specialCookieValue = 10001;
                                visitsCounter = specialCookieValue - 10000;
                            }

                            // Handling the different request contents.
                            switch (requestURI) {
                                case myCaseID + "/test1.html":
                                    response = generateResponse(contentTest1, false, visitsCounter, ifPersistent);
                                    break;
                                case myCaseID + "/test2.html":
                                    response = generateResponse(contentTest2, false, visitsCounter, ifPersistent);
                                    break;
                                case myCaseID + "/visits.html":
                                    String tmpResp = contentVisits.substring(0, contentVisits.indexOf("[") + 1) + visitsCounter + contentVisits.substring(contentVisits.indexOf("]"));
                                    response = generateResponse(tmpResp, false, visitsCounter, ifPersistent);
                                    break;
                                default:
                                    response = generateResponse(content404, true, -1, ifPersistent);
                                    break;
                            }

                            // Write the response to output stream and flush it. Say bye-bye!
                            toClient.write(response.getBytes());
                            toClient.flush();

                            if (!ifPersistent) {
                                // If the connection is non-persistent, shut down the connection right now.
                                connectionSocket.shutdownInput();
                                connectionSocket.shutdownOutput();
                                connectionSocket.close();
                                System.out.println("Non-persistent socket closed");
                                break;
                            }
                        } catch (SocketTimeoutException ex) {
                            // If the connection is persistent and the timer expires, a socket-timeout exception will ..
                            // .. be caught and shut down the connection.
                            System.out.println("Timeout!");
                            try {
                                connectionSocket.shutdownInput();
                                connectionSocket.shutdownOutput();
                                connectionSocket.close();
                                System.out.println("Persistent socket closed");
                            } catch (IOException er) {
                                er.printStackTrace();
                            }
                            break;
                        } catch (IOException e) {
                            System.out.println("IOException!");
                            e.printStackTrace();
                        }
                    } while (ifPersistent);
                }
            });
        }

    }

    // A helper function to handle file reading.
    private static String myReader(String yourFilePath) {
        BufferedReader tmpReader = null;
        try {
            tmpReader = new BufferedReader(new FileReader(yourFilePath));
        } catch (IOException ignored) {
        }
        String tmpLine;
        String data = "";
        try {
            while (null != (tmpLine = tmpReader.readLine())) {
                data += tmpLine;
            }
        } catch (IOException ignored) {
        }
        return data;
    }

    // A helper function to compose the response.
    private static String generateResponse(String fileContent, boolean isErr, int counterValue, boolean ifPersistent) {
        String response;
        String cookiePath = "path: /hxl1033";

        String toPersist = "close";
        if (ifPersistent)
            toPersist = "keep-alive";

        if (isErr) {
            // If the server does not own the expected resource, the program will be here and return the 404 Error msg.
            response = "HTTP/1.1 404 Not Found\r\n" +
                    "Connection: " + toPersist + "\r\n" +
                    "Content-Length: " + fileContent.getBytes().length + "\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "\r\n" +
                    fileContent + "\r\n";
        } else {
            // If the server have the expected resource, the program will be here and return corresponding content.
            String tmpSetCookie;
            if (counterValue > 1) {
                tmpSetCookie = "Set-cookie: Long-live-rock=" + (counterValue + 10000) + "; " + cookiePath + "\r\n";
            } else {
                tmpSetCookie = "Set-cookie: Long-live-rock=10001" + "; " + cookiePath + "\r\n";
            }
            response = "HTTP/1.1 200 OK\r\n" +
                    "Connection: " + toPersist + "\r\n" +
                    "Content-Length: " + fileContent.getBytes().length + "\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    tmpSetCookie +
                    "\r\n" +
                    fileContent + "\r\n";
        }
        return response;
    }
}