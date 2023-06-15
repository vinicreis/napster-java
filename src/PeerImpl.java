import model.response.JoinResponse;
import model.response.UpdateResponse;
import service.Napster;
import log.ConsoleLog;
import log.Log;
import view.ProgressBar;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
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
    private static final String DEFAULT_FOLDER = "data";
    private static final String DEFAULT_FOLDER_FORMAT = "peer-%s-%d";
    private static final String TAG = "PeerImpl";
    private final Napster napster;
    private final Log log = new ConsoleLog(TAG);
    private String ip;
    private Integer port;
    private File folder;
    private ServerSocket serverSocket;

    PeerImpl(boolean debug) throws NotBoundException, IOException {
        try {
            log.setDebug(debug);

            final Registry registry = LocateRegistry.getRegistry();
            this.napster = (Napster) registry.lookup("rmi://localhost/napster");

            check(napster != null, "Serviço remoto não disponível");
        } catch (Exception e) {
            log.e("Failed to initialize peer", e);

            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            final boolean debug = Arrays.asList(args).contains("--d");

            try (Peer peer = new PeerImpl(debug)) {
                Runtime.getRuntime().addShutdownHook(peer.getShutdownHook());

                peer.start();
            }

            System.out.println("Peer encerrado!");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar peer!" );
        }
    }

    enum Operation {
        JOIN(1, "Inicializar"),
        SEARCH(2, "Procurar"),
        DOWNLOAD(3, "Baixar"),
        EXIT(0, "Sair");

        private final Integer code;
        private final String formattedName;

        Operation(int code, String formattedName) {
            this.code = code;
            this.formattedName = formattedName;
        }

        public static Operation valueOf(Integer code) throws IllegalArgumentException {
            switch (code) {
                case 1: return JOIN;
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
            System.out.print("\r");

            for (Operation operation : Operation.values()) {
                System.out.printf("%d - %s\n", operation.getCode(), operation.getFormattedName());
            }

            System.out.print("\n");
        }

        public int getCode() {
            return code;
        }

        public String getFormattedName() {
            return formattedName;
        }
    }

    class UploadThread extends Thread {
        private static final String TAG = "UploadThread";
        private final Socket socket;
        private final BufferedReader reader;
        private final OutputStream writer;

        UploadThread(Socket socket) throws IOException {
            this.setName(TAG + "-" + getId());
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = socket.getOutputStream();
        }

        @Override
        public void run() {
            try {
                log.d("Upload started! Reading desired file from client...");
                final File file = new File(folder.getPath(), reader.readLine());

                check(file.exists(), String.format("Arquivo %s não encontrado!", file.getName()));

                final ProgressBar progressBar = new ProgressBar(getName(), file.length(), "Uploading...");
                final DataOutputStream dataWriter = new DataOutputStream(socket.getOutputStream());

                log.d("Sending file size to peer");
                dataWriter.writeLong(file.length());

                final FileInputStream fileReader = new FileInputStream(file);
                final byte[] buffer = new byte[BUFFER_SIZE];
                long bytesSent = 0;
                int bytesCount;

                System.out.printf(
                        "\n\n[%s] Enviando arquivo %s ao peer %s:%d...\n",
                        getName(),
                        file.getName(),
                        socket.getInetAddress().getHostName(),
                        socket.getPort()
                );
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
            } catch (SocketException e) {
                System.out.printf("%s: Falha ao enviar arquivo!\n", getName());

                log.e(String.format("Failed to upload file to peer %s", socket.getInetAddress().getHostName()));
            } catch (Exception e) {
                log.e(String.format("Failed to upload file to peer %s", socket.getInetAddress().getHostName()));
            }
        }
    }

    class DownloadThread extends Thread {
        private static final String TAG = "DownloadThread";
        private final Socket socket;
        private final BufferedInputStream reader;
        private final PrintWriter writer;
        private final File file;

        DownloadThread(Socket socket, String filename) throws IOException {
            this.setName(TAG + "-" + getId());
            this.socket = socket;
            this.reader = new BufferedInputStream(socket.getInputStream());
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.file = new File(folder, filename);
        }

        @Override
        public void run() {
            try {
                if (file.createNewFile()) {
                    log.d(String.format("Created file %s to download...", file.getName()));
                } else {
                    throw new RuntimeException(String.format("File %s already exists!", file.getName()));
                }

                log.d("Sending wanted file's name...");
                writer.println(file.getName());

                final DataInputStream dataReader = new DataInputStream(socket.getInputStream());
                final long fileSize = dataReader.readLong();
                final ProgressBar progressBar = new ProgressBar(getName(), fileSize, "Downloading...");

                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesReceived = 0;

                log.d("Downloading file...");
                try (final FileOutputStream fileWriter = new FileOutputStream(file)) {
                    int count;

                    do {
                        count = (reader.read(buffer));
                        bytesReceived += count;

                        if(count > 0) {
                            fileWriter.write(buffer, 0, count);

                            progressBar.update(bytesReceived);
                            progressBar.print();
                        }
                    } while (count > 0);
                }

                log.d("File download! Updating on server...");
                System.out.printf(
                        "[%s] Arquivo %s baixado com sucesso na pasta %s",
                        getName(),
                        file.getName(),
                        folder.getPath()
                );
                update(file.getName());
            } catch (SocketException e) {
                System.out.println("Falha ao fazer download do arquivo!");

                if (file.exists() && file.delete()) {
                    System.out.printf("[%s] Arquivo %s deletado!\n", getName(), file.getName());
                } else {
                    System.out.printf("[%s] Sem arquivos para deletar\n", getName());
                }
            } catch (Exception e) {
                log.e("Failed to download file!", e);

                if (file.exists() && file.delete()) {
                    System.out.printf("[%s] Arquivo %s deletado!\n", getName(), file.getName());
                } else {
                    System.out.printf("[%s] Sem arquivos para deletar\n", getName());
                }

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
                    final Socket socket = serverSocket.accept();
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

    class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                log.d("Finishing peer gracefully...");
                close();
            } catch (Exception e) {
                log.e("Failed to finish peer gracefully!", e);
            }
        }
    }

    @Override
    public void start() {
        boolean running = true;

        while(running) {
            final PeerImpl.Operation operation = PeerImpl.Operation.read();

            switch (operation) {
                case JOIN: join(); break;
                case SEARCH: search(); break;
                case DOWNLOAD: download(); break;
                case EXIT:
                    running = false;
                    break;
            }
        }
    }

    @Override
    public void close() {
        try {
            if (!isJoined()) return;

            log.d("Leaving Napster...");
            napster.leave(ip, port);
            if(serverSocket != null) {
                log.d("Interrupting server thread...");
                serverSocket.close();
            }
        } catch (Exception e) {
            log.e("Failed to stop peer!", e);
        }
    }

    @Override
    public void join() {
        try {
            if (isJoined()) {
                System.out.println("Peer já inicializado!");
                return;
            }

            ip = readInput("Digite o IP do peer: ");
            check(ip != null && !ip.isEmpty(), "IP não pode ser nulo");

            port = Integer.parseInt(readInput("Digite a porta do peer: "));
            check(port > 0, "Port deve ser maior que zero");

            final URI defaultFolder = Paths.get(
                    System.getProperty("user.dir"),
                    DEFAULT_FOLDER,
                    String.format(DEFAULT_FOLDER_FORMAT, ip.replace(".", "-"), port)
            ).toUri();

            final String enteredFolder = readInput("Digite o caminho da pasta do peer\n[%s]\nPasta:", defaultFolder.getPath());

            if(enteredFolder != null && !enteredFolder.isEmpty()){
                folder = new File(enteredFolder);
            } else {
                folder = new File(defaultFolder);
            }

            if(!this.folder.exists() && !this.folder.mkdirs()) {
                throw new RuntimeException("Falha ao criar pasta do peer");
            }

            final File[] filesArray = folder.listFiles();

            check(filesArray != null, "Lista de arquivos do peer é nula");

            final List<File> files = Arrays.asList(filesArray);
            final List<String> fileNames = files.stream().map(File::getName).collect(Collectors.toList());
            final String result = napster.join(ip, port, fileNames);

            check(result.equals(JoinResponse.OK.getCode()), "Falha do serviço remoto para inicializar o peer");

            final ServerThread serverThread = new ServerThread();
            this.serverSocket = new ServerSocket(this.port);
            serverThread.start();

            log.d("Successfully joined to server!");
            System.out.printf(
                    "Sou peer %s:%d com os arquivos %s\n\n",
                    ip,
                    port,
                    String.join(", ", fileNames)
            );
        } catch (RemoteException e) {
            log.e("Failed to run operation on remote service", e);
            System.out.println("Falha na execução da operação no serviço remoto");
        } catch (IOException e) {
            log.e("Failed to start server thread", e);
            System.out.println("Falha ao iniciar thread do servidor");
        } catch (RuntimeException e) {
            log.e("Failed to run operation", e);
            System.out.println("Falha ao executar operação");
        } catch (Exception e) {
            System.out.println("Ocorreu um problema na sua operação");
        }
    }

    @Override
    public void update(String filename) {
        try {
            check(isJoined(), "Peer deve ser inicializado (função 1)!");

            final File file = new File(folder.getAbsolutePath(), filename);

            check(file.exists(), String.format("Arquivo %s não existe", filename));

            final String result = napster.update(ip, port, filename);

            if(result.equals(UpdateResponse.OK.getCode())) {
                log.d(String.format("Updated server to serve file %s", filename));
            } else {
                throw new RuntimeException(String.format("Failed to update file %s on server", filename));
            }
        } catch (RemoteException e) {
            log.e("Failed to run operation on remote service", e);
            System.out.println("Falha na execução da operação no serviço remoto");
        } catch (RuntimeException e) {
            log.e("Failed to run operation", e);
            System.out.printf("Falha ao executar operação: %s\n", e.getMessage());
        } catch (Exception e) {
            System.out.println("Ocorreu um problema na sua operação");
        }
    }

    @Override
    public void search() {
        try {
            check(isJoined(), "Peer deve ser inicializado (função 1)!");

            final String filename = readInput("Enter the filename to search: ");

            final List<String> result = napster.search(ip, port, filename);

            if (result.isEmpty()) {
                System.out.printf("\nNenhum peer possui o arquivo %s\n", filename);
            } else {
                System.out.println("\nPeers com arquivos solicitados:");

                for (String peer : result) {
                    System.out.println(peer);
                }

                System.out.println();
            }
        } catch (RemoteException e) {
            log.e("Failed to run operation on remote service", e);
            System.out.println("Falha na execução da operação no serviço remoto");
        } catch (RuntimeException e) {
            log.e("Failed to run operation", e);
            System.out.printf("Falha ao executar operação: %s\n", e.getMessage());
        } catch (Exception e) {
            System.out.println("Ocorreu um problema na sua operação");
        }
    }

    @Override
    public void download() {
        try {
            check(isJoined(), "Peer deve ser inicializado (função 1)!");

            final String ip = readInput("Enter peer IP: ");
            final int port = Integer.parseInt(readInput("Enter peer port: "));
            final String filename = readInput("Enter the filename: ");

            final Socket socket = new Socket(ip, port);
            final DownloadThread thread = new DownloadThread(socket, filename);

            thread.start();
        } catch(IOException e) {
            log.e("Server failed!", e);
        } catch (RuntimeException e) {
            log.e("Failed to run operation", e);
            System.out.printf("Falha ao executar operação: %s\n", e.getMessage());
        }
    }

    @Override
    public Thread getShutdownHook() {
        return new ShutdownHook();
    }

    private void check(boolean rule, String message) throws RuntimeException {
        if(!rule) throw new RuntimeException(message);
    }

    private boolean isJoined() {
        return ip != null && !ip.isEmpty() && port != null && folder != null;
    }

    private static String readInput() {
        final Scanner in = new Scanner(System.in);

        return in.nextLine();
    }

    private static String readInput(String message, Object... args) {
        System.out.printf(message, args);

        return readInput();
    }
}
