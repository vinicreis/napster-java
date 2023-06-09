import model.response.JoinResponse;
import model.response.UpdateResponse;
import service.Napster;
import log.ConsoleLog;
import log.Log;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class PeerImpl implements Peer {
    private static final int BUFFER_SIZE = 4096;
    private static final String FOLDER_NAME = "data";
    private static final String TAG = "PeerImpl";
    private final Napster napster;
    private final Log log = new ConsoleLog(TAG);
    private final String ip;
    private final Integer port;
    private final File folder;
    private final ServerThread serverThread;
    private final ServerSocket serverSocket;

    private PeerImpl(String ip, Integer port, boolean debug) throws NotBoundException, IOException {
        try {
            log.setDebug(debug);

            Registry registry = LocateRegistry.getRegistry();
            this.napster = (Napster) registry.lookup("rmi://localhost/napster");

            assert napster != null : "Napster server is not available";
            assert ip != null && !ip.isEmpty() : "IP cannot be null or empty";
            // TODO: Check if IP address is valid
            assert port != null : "Port cannot be null";
            assert port > 0 : "Port must be greater than 0";

            this.ip = ip;
            this.port = port;
            this.folder = new File(
                    Paths.get(
                            System.getProperty("user.dir"),
                            FOLDER_NAME,
                            String.format("peer-%s-%d", ip.replace(".", "-"), port)
                    ).toUri()
            );

            if(!this.folder.exists() && !this.folder.mkdirs()) {
                throw new RuntimeException("Failed to create peer folder");
            }

            this.serverThread = new ServerThread();
            this.serverSocket = new ServerSocket(this.port);
        } catch (Exception e) {
            log.e("Failed to initialize peer", e);

            throw e;
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

    class UploadThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private OutputStream writer;

        UploadThread(Socket socket) {
            try {
                this.socket = socket;
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = socket.getOutputStream();
            } catch (Exception e) {
                log.e("Failed to initialize UploadThread", e);
            }
        }

        @Override
        public void run() {
            try {
                log.d("Upload started! Reading desired file from client...");
                String filename = reader.readLine();
                File file = new File(folder.getPath(), filename);

                assert file.exists() : String.format("File %s not found!", filename);

                FileInputStream fileReader = new FileInputStream(file);
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;

                log.d(String.format("Uploading file to peer %s", socket.getInetAddress().getHostName()));
                // TODO: Add progress print
                do {
                    read = fileReader.read(buffer);

                    if(read > 0) {
                        writer.write(buffer, 0, read);
                        writer.flush();
                    }
                } while (read > 0);

                log.d("Upload finished! Closing connection...");

                socket.close();
            } catch (Exception e) {
                log.e(String.format("Failed to upload file to peer %s", socket.getInetAddress().getHostName()));
            }
        }
    }

    class DownloadThread extends Thread {
        private Socket socket;
        private BufferedInputStream reader;
        private PrintWriter writer;
        private String filename;

        DownloadThread(Socket socket, String filename) {
            try {
                this.socket = socket;
                this.reader = new BufferedInputStream(socket.getInputStream());
                this.writer = new PrintWriter(socket.getOutputStream(), true);
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
                    log.d(String.format("Created file %s to download...", filename));
                } else {
                    throw new RuntimeException(String.format("File %s already exists!", filename));
                }

                log.d("Sending wanted file's name...");
                writer.println(filename);

                byte[] buffer = new byte[BUFFER_SIZE];

                log.d("Downloading file...");
                try(final FileOutputStream fileWriter = new FileOutputStream(file)) {
                    int count;

                    do {
                        count = (reader.read(buffer));

                        if(count > 0) {
                            fileWriter.write(buffer, 0, count);
                        }
                    } while (count > 0);
                }

                log.d("File download! Updating on server...");
                update(filename);
                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ServerThread extends Thread {
        @Override
        public void run() {
            try {
                log.d("Starting server...");

                //noinspection InfiniteLoopStatement
                while (true) {
                    log.d("Listening download requests...");
                    Socket socket = serverSocket.accept();
                    log.d(String.format("Connection established with peer %s", socket.getInetAddress().getHostName()));

                    UploadThread thread = new UploadThread(socket);
                    thread.start();
                }
            } catch (SocketException e) {
                System.out.println("Server thread interrupted...");
            } catch (Exception e) {
                System.out.printf("Server thread failed: %s", e.getMessage());
                log.e("Failed to start server!", e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            assert args.length >= 1 : "IP and port argument are mandatory";

            String ip = args[0];
            int port = Integer.parseInt(args[1]);
            Peer peer = new PeerImpl(ip, port, Arrays.asList(args).contains("--d"));

            peer.start();
            peer.join();

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
                        peer.stop();

                        running = false;
                }
            }

            System.out.println("Peer finished!");
        } catch (NumberFormatException e) {
            System.out.printf("invalid port: %s", args[1]);
        } catch (Exception e) {
            System.out.printf("Failed to initialize peer: %s", e.getMessage());
        }
    }

    @Override
    public void start() {
        serverThread.start();
    }

    @Override
    public void stop() {
        try {
            log.d("Leaving Napster...");
            napster.leave(ip, port);
            log.d("Interrupting server thread...");
            serverSocket.close();
        } catch (Exception e) {
            log.e("Failed to stop peer!", e);
        }
    }

    @Override
    public void join() throws RuntimeException, RemoteException {
        File[] filesArray = folder.listFiles();

        assert filesArray != null : "Peer file list is null";

        List<File> files = Arrays.asList(filesArray);

        String result = napster.join(ip, port, files.stream().map(File::getName).collect(Collectors.toList()));

        if(result.equals(JoinResponse.OK.getCode())) {
            log.d("Successfully joined to server!");
        } else {
            throw new RuntimeException("Failed to join to server");
        }
    }

    @Override
    public void update() throws RemoteException {
        String filename = readInput("Enter the updated filename: ");

        update(filename);
    }

    private void update(String filename) throws RemoteException {
        File file = new File(folder.getAbsolutePath(), filename);

        assert file.exists() : String.format("File %s do not exists", filename);

        String result = napster.update(ip, port, filename);

        if(result.equals(UpdateResponse.OK.getCode())) {
            log.d(String.format("Updated server to serve file %s", filename));
        } else {
            throw new RuntimeException(String.format("Failed to update file %s on server", filename));
        }
    }

    @Override
    public void search() throws RemoteException {
        String filename = readInput("Enter the filename to search: ");

        List<String> result = napster.search(filename);

        if(result.isEmpty()) {
            System.out.printf("No peers found with the file %s\n", filename);
        } else {
            System.out.println("File found on peers:");

            for (String peer : result) {
                System.out.println(peer);
            }
        }
    }

    @Override
    public void download() {
        try {
            final String ip = readInput("Enter peer IP: ");
            // TODO: Check if IP is valid

            final int port = Integer.parseInt(readInput("Enter peer port: "));
            // TODO: Check if port is valid

            final String filename = readInput("Enter the filename: ");

            Socket socket = new Socket(ip, port);
            DownloadThread thread = new DownloadThread(socket, filename);

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
