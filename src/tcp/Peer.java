package tcp;

import interfaces.network.IPeer;
import interfaces.service.INapster;
import model.response.JoinResponse;
import util.Assert;
import util.ILog;
import util.Log;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Peer implements IPeer {
    private static final ILog log = new Log("Peer");
    private static INapster napster;
    private String ip;
    private Integer port;
    private File folder;

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
            assert ip != null && !ip.isEmpty() : "IP cannot be null or empty";
            // TODO: Check if IP address is valid
            assert port != null : "Port cannot be null";
            assert port > 0 : "Port must be greater than 0";

            this.ip = ip;
            this.port = port;
            this.folder = new File(
                    System.getProperty("user.dir"),
                    String.format("peer-%s-%d", ip.replace(".", "-"), port)
            );

            if(!this.folder.exists() && !this.folder.mkdir()) {
                throw new RuntimeException("Failed to create peer folder");
            }
        } catch (Exception e) {
            log.e("Failed to initialize peer", e);
        }
    }

    enum Option {
        UPDATE(1),
        SEARCH(2),
        DOWNLOAD(3);

        public final Integer code;

        Option(int code) {
            this.code = code;
        }

        public static Option valueOf(Integer code) throws IllegalArgumentException {
            switch (code) {
                case 1: return UPDATE;
                case 2: return SEARCH;
                case 3: return DOWNLOAD;
                default: throw new IllegalArgumentException("Invalid option selected!");
            }
        }

        public static Option readOption() throws IllegalArgumentException {
            return valueOf(Integer.valueOf(System.console().readLine()));
        }
    }

    class PeerThread extends Thread {
        private final Peer peer;
        private final Option option;

        PeerThread(Peer peer, Option option) {
            this.peer = peer;
            this.option = option;
        }

        @Override
        public void run() {

        }
    }

    public static void main(String[] args) {
        try {
            assert args.length >= 1 : "IP and port argument are mandatory";

            String ip = args[0];
            Integer port = Integer.valueOf(args[1]);
            Peer peer = new Peer(ip, port);

            peer.join();

            while(true) {
                Option option = Option.readOption();

                switch (option) {
                    case UPDATE: peer.update(); break;
                    case SEARCH:
                    case DOWNLOAD:
                }
            }
        } catch (NumberFormatException e) {
            log.e(String.format("Port %s is invalid", args[1]), e);
        } catch (Exception e) {
            log.e("Peer execution error", e);
        }
    }

    public void join() throws RuntimeException {
        File[] filesArray = folder.listFiles();

        assert filesArray != null : "Peer file list is null";

        List<File> files = Arrays.asList(filesArray);

        String result = napster.join(ip, port, files.stream().map(File::getName).collect(Collectors.toList()));

        if(result.equals(JoinResponse.OK.getCode())) {
            log.i("Successfully joined to server!");
        } else {
            throw new RuntimeException("Failed to join to server");
        }
    }

    public void update() {
        System.out.print("Enter the updated filename: ");
        String filename = System.console().readLine();

        update(filename);
    }

    public void update(String filename) {
        File file = new File(folder.getAbsolutePath(), filename);

        assert file.exists() : String.format("File %s do not exists", filename);

        String result = napster.update(ip, port, filename);

        // TODO: Extract result string
        if(result.equals("UPDATE_OK")) {
            log.i(String.format("Updated server to serve file %s", filename));
        } else {
            throw new RuntimeException(String.format("Failed to update file %s on server", filename));
        }
    }

    public void search() {
        String filename = System.console().readLine();

        List<String> result = napster.search(filename);

        if(result.isEmpty()) {
            System.console().printf("No peers found with the file %s", filename);
        } else {
            System.console().printf("File found on peers:");

            for (String peer : result) {
                System.console().printf(peer);
            }
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
