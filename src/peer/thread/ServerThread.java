package peer.thread;

import log.ConsoleLog;
import log.Log;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ServerThread extends Thread {
    private static final String TAG = "ServerThread";
    private static final Log log = new ConsoleLog(TAG);
    private final ServerSocket serverSocket;
    private final File folder;

    public ServerThread(ServerSocket serverSocket, File folder) {
        this.serverSocket = serverSocket;
        this.folder = folder;
    }

    @Override
    public void run() {
        try {
            log.d("Starting server...");

            //noinspection InfiniteLoopStatement
            while (true) {
                log.d("Listening download requests...");
                final Socket socket = serverSocket.accept();
                log.d(String.format("Connection established with peer %s", socket.getInetAddress().getHostName()));

                UploadThread thread = new UploadThread(socket, folder);
                thread.start();
            }
        } catch (SocketException e) {
            log.d("server.Server peer.thread interrupted...");
        } catch (Exception e) {
            System.out.printf("server.Server peer.thread failed: %s", e.getMessage());
            log.e("Failed to start server!", e);
        }
    }
}
