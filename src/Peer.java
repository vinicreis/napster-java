import interfaces.service.INapster;
import util.Log;
import util.ConsoleLog;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public class Peer {
    private static final Log log = new ConsoleLog("Peer");
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

    enum Operation {
        UPDATE(1),
        SEARCH(2),
        DOWNLOAD(3),
        EXIT(0);

        public final Integer code;

        Operation(int code) {
            this.code = code;
        }

        public static Operation valueOf(Integer code) throws IllegalArgumentException {
            switch (code) {
                case 1: return UPDATE;
                case 2: return SEARCH;
                case 3: return DOWNLOAD;
                case 0: return EXIT;
                default: throw new IllegalArgumentException("Invalid option selected!");
            }
        }

        public static Operation read() throws IllegalArgumentException {
            return valueOf(Integer.valueOf(readInput()));
        }

        public static void print() {
            for (Operation operation : Operation.values()) {
                System.out.printf("%s[%d]\n", operation.toString(), operation.code);
            }

            System.out.print("\n");
        }
    }

    static class UploadThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private OutputStream writer;
        private File folder;

        UploadThread(Socket socket, File folder) {
            try {
                this.folder = folder;
                this.socket = socket;
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                log.e("Failed to initialize UploadThread", e);
            }
        }

        @Override
        public void run() {
            try {
                String filename = reader.readLine();
                File file = new File(folder.getPath(), filename);

                assert file.exists() : String.format("File %s not found!", filename);

                log.i("Sending download size to peer...");
                writer.write(String.valueOf(file.length()).getBytes());

                log.i(String.format("Uploading file to peer %s", socket.getInetAddress().getHostName()));
                writer.write(Files.readAllBytes(file.toPath()));
                log.i("Upload finished! Closing connection...");

                socket.close();
            } catch (Exception e) {
                log.e(String.format("Failed to upload file to peer %s", socket.getInetAddress().getHostName()));
            }
        }
    }

    class DownloadThread extends Thread {
        private Peer peer;
        private Socket socket;
        private BufferedReader reader;
        private OutputStream writer;
        private String filename;

        DownloadThread(Peer peer, Socket socket, String filename) {
            try {
                this.peer = peer;
                this.socket = socket;
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new DataOutputStream(socket.getOutputStream());
                this.filename = filename;
            } catch (Exception e) {
                log.e("Failed to initialize UploadThread", e);
            }
        }

        @Override
        public void run() {
            try {
                File file = new File(folder, filename);

                if (file.createNewFile()) {
                    log.i(String.format("Created file %s to download...", filename));
                } else {
                    throw new RuntimeException(String.format("File %s already exists!", filename));
                }

                log.i("Sending wanted file's name...");
                writer.write(filename.getBytes());

                log.i("Reading file size to create buffer...");
                Long size = Long.decode(reader.readLine());
                char[] buffer = new char[Math.toIntExact(size)];

                log.i("Downloading file...");
                try(final FileWriter fileWriter = new FileWriter(file)) {
                    int count;

                    do {
                        count = (reader.read(buffer));

                        if(count > 0) {
                            fileWriter.write(buffer, 0, count);
                        }
                    } while (count > 0);
                }

                log.i("File download! Updating on server...");
                peer.update(filename);
                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ServerThread extends Thread {
        private final int port;
        private final File folder;

        ServerThread(int port, File folder) {
            this.port = port;
            this.folder = folder;
        }

        @Override
        public void run() {
            try(ServerSocket server = new ServerSocket(port)) {
                log.i("Starting server...");

                while (true) {
                    log.i("Listening download requests...");
                    Socket socket = server.accept();
                    log.i(String.format("Connection established with peer %s", socket.getInetAddress().getHostName()));

                    UploadThread thread = new UploadThread(socket, folder);
                    thread.start();
                }
            } catch (Exception e) {
                log.e("Failed to start server!", e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            assert args.length >= 1 : "IP and port argument are mandatory";

            String ip = args[0];
            int port = Integer.parseInt(args[1]);
            Peer peer = new Peer(ip, port);

            peer.join();

            ServerThread serverThread = new ServerThread(port, peer.folder);
            serverThread.start();

            boolean running = true;

            while(running) {
                System.out.print("Select an option:\n\n");
                Operation.print();
                Operation operation = Operation.read();

                switch (operation) {
                    case UPDATE: peer.update(); break;
                    case SEARCH: peer.search(); break;
                    case DOWNLOAD: peer.download(); break;
                    case EXIT:
                        serverThread.interrupt();

                        running = false;
                }
            }
        } catch (CancellationException e) {
            System.out.print("Peer finished!");
        } catch (NumberFormatException e) {
            log.e(String.format("Port %s is invalid", args[1]), e);
        } catch (Exception e) {
            log.e("Peer execution error", e);
        }
    }

    public void join() throws RuntimeException, RemoteException {
        File[] filesArray = folder.listFiles();

        assert filesArray != null : "Peer file list is null";

        List<File> files = Arrays.asList(filesArray);

        String result = napster.join(ip, port, files.stream().map(File::getName).collect(Collectors.toList()));

        // TODO: Extract string results to enum
        if(result.equals("JOIN_OK")) {
            log.i("Successfully joined to server!");
        } else {
            throw new RuntimeException("Failed to join to server");
        }
    }

    public void update() throws RemoteException {
        String filename = readInput("Enter the updated filename: ");

        update(filename);
    }

    public void update(String filename) throws RemoteException {
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

    public void search() throws RemoteException {
        String filename = readInput("Enter the filename to search: ");

        List<String> result = napster.search(filename);

        if(result.isEmpty()) {
            System.out.printf("No peers found with the file %s", filename);
        } else {
            System.out.print("File found on peers:");

            for (String peer : result) {
                System.out.print(peer);
            }
        }
    }

    public void download() {
        try {
            final String ip = readInput("Enter peer IP: ");
            // TODO: Check if IP is valid

            final int port = Integer.parseInt(readInput("Enter peer port: "));
            // TODO: Check if port is valid

            final String filename = readInput("Enter the filename: ");

            Socket socket = new Socket(ip, port);
            DownloadThread thread = new DownloadThread(this, socket, filename);

            thread.start();
        } catch(IOException e) {
            log.e("Server failed!", e);
        }
    }

    private static String readInput() {
        Scanner in = new Scanner(System.in);

        return in.nextLine();
    }

    private static String readInput(String message, Object... args) {
        System.out.printf(message, args);

        return readInput();
    }
}
