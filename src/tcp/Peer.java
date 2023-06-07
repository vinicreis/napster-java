package tcp;

import interfaces.network.IPeer;
import interfaces.service.INapster;
import util.ILog;
import util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

public class Peer implements IPeer {
    private static final ILog log = new Log("Peer");
    private static INapster napster;
    private String ip;
    private Integer port;

    static {
        try {
            Registry registry = LocateRegistry.getRegistry();
            napster = (INapster) registry.lookup("rmi://127.0.0.1/napster");
        } catch (Exception e) {
            log.e("Failed to initialize peer", e);
        }
    }

    private Peer(String ip, Integer port) {
        try {
            this.ip = ip;
            this.port = port;
        } catch (Exception e) {
            log.e("Failed to initialize peer", e);
        }
    }

    public static void main(String[] args) {
        try {
            if(args.length < 2) {
                throw new IllegalArgumentException("IP and port should be entered to start peer");
            }

            String ip = args[0];
            Integer port = Integer.valueOf(args[1]);

            while(true) {

            }
        } catch (NumberFormatException e) {
            log.e(String.format("Port %s is invalid", args[1]), e);
        } catch (Exception e) {
            log.e("Peer execution error", e);
        }
    }

    @Override
    public Boolean open(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new DataOutputStream(socket.getOutputStream());

            return true;
        } catch (UnknownHostException e) {
            log.e(String.format("Host %s:%s not known", ip, port), e); return false;
        } catch (Exception e) {
            log.e(String.format("Failed to open connection to host %s:%s", ip, port)); return false;
        }
    }

    public String read() throws IOException {
        return reader.readLine();
    }

    public void write(String msg) throws IOException {
        writer.writeBytes(msg);
    }

    @Override
    public Boolean close() {
        try {
            socket.close(); return true;
        } catch (Exception e) {
            log.e("Failed to close socket", e); return false;
        }
    }

    public void download() {
        try {
            while(true) {
                log.i("Waiting client connection...");
                Socket clientSocket = serverSocket.accept();

                log.i(String.format("Client connected on port %d", client.getPort()));
                DedicatedClientThread clientThread = new DedicatedClientThread(clientSocket);
                clientThread.start();
            }
        } catch(IOException e) {
            log.e("Server failed!", e);
        }
    }
}
