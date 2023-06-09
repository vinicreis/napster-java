import model.response.JoinResponse;
import model.response.UpdateResponse;
import service.Napster;
import log.ConsoleLog;
import log.Log;
import util.ProgressBar;

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

        private final Integer code;

        Operation(int code) {
            this.code = code;
        }

        public static Operation valueOf(Integer code) throws IllegalArgumentException {
            switch (code) {
                case 1: return UPDATE;
                case 2: return SEARCH;
                case 3: return DOWNLOAD;
                case 0: return EXIT;
                default: throw new IllegalArgumentException("Opção inválida selecionada!");
            }
        }

        public static Operation read() throws IllegalArgumentException {
            print();

            return valueOf(Integer.valueOf(readInput("Selecione uma opção: ")));
        }

        public static void print() {
            for (Operation operation : Operation.values()) {
                System.out.printf("%d - %s\n", operation.getCode(), operation);
            }

            System.out.print("\n");
        }

        public int getCode() {
            return code;
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

                ProgressBar progressBar = new ProgressBar(file.length(), "Uploading...");
                FileInputStream fileReader = new FileInputStream(file);
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesSent = 0;
                int bytesCount;

                log.d(String.format("Uploading file to peer %s", socket.getInetAddress().getHostName()));
                do {
                    bytesCount = fileReader.read(buffer);
                    bytesSent += bytesCount;

                    if(bytesCount > 0) {
                        writer.write(buffer, 0, bytesCount);
                        writer.flush();

                        progressBar.update(bytesSent);
                        progressBar.print();
                    }
                } while (bytesCount > 0);

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

                // TODO: Receive file size to show download progress

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
                System.out.printf(
                        "Arquivo %s baixado com sucesso na pasta %s",
                        file.getName(),
                        folder.getPath()
                );
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
                log.d("Server thread interrupted...");
            } catch (Exception e) {
                System.out.printf("Server thread failed: %s", e.getMessage());
                log.e("Failed to start server!", e);
            }
        }
    }

    static class ShutdownHook extends Thread {
        private final Peer peer;

        ShutdownHook(Peer peer) {
            this.peer = peer;
        }

        @Override
        public void run() {
            try {
                System.out.println("\n\nEncerrando peer graciosamente...");
                peer.close();
            } catch (Exception e) {
                System.out.printf("Erro ao finalizar o peer graciosamente: %s\n", e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            assert args.length >= 1 : "IP and port argument are mandatory";

            String ip = args[0];
            int port = Integer.parseInt(args[1]);
            boolean debug = Arrays.asList(args).contains("--d");

            try (Peer peer = new PeerImpl(ip, port, debug)) {
                Runtime.getRuntime().addShutdownHook(new ShutdownHook(peer));

                peer.start();
                peer.join();

                boolean running = true;

                while(running) {
                    Operation operation = Operation.read();

                    switch (operation) {
                        case UPDATE: peer.update(); break;
                        case SEARCH: peer.search(); break;
                        case DOWNLOAD: peer.download(); break;
                        case EXIT:
                            running = false;
                            break;
                    }
                }
            }

            System.out.println("Peer finished!");
        } catch (NumberFormatException e) {
            System.out.printf("Invalid port: %s\n", args[1]);
        } catch (Exception e) {
            System.out.printf("Failed to initialize peer: %s\n", e.getMessage());
        }
    }

    @Override
    public void start() {
        serverThread.start();
    }

    @Override
    public void close() {
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
        List<String> fileNames = files.stream().map(File::getName).collect(Collectors.toList());

        String result = napster.join(ip, port, fileNames);

        if(result.equals(JoinResponse.OK.getCode())) {
            log.d("Successfully joined to server!");
            System.out.printf(
                    "Sou peer %s:%d com os arquivos %s\n\n",
                    ip,
                    port,
                    String.join(", ", fileNames)
            );
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
            System.out.printf("Nenhum peer possui o arquivo %s\n", filename);
        } else {
            System.out.println("Peers com arquivos solicitados:");

            for (String peer : result) {
                System.out.println(peer);
            }
        }
    }

    @Override
    public void download() {
        try {
            final String ip = readInput("Enter peer IP: ");
            final int port = Integer.parseInt(readInput("Enter peer port: "));
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
