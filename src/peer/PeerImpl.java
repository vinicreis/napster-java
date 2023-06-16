package peer;

import log.ConsoleLog;
import log.Log;
import service.model.response.JoinResponse;
import service.model.response.UpdateResponse;
import service.Napster;
import service.model.enums.Operation;
import peer.thread.DownloadThread;
import peer.thread.ServerThread;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static util.AssertUtil.check;
import static util.IOUtil.readInput;

public class PeerImpl implements Peer {
    private static final String DEFAULT_FOLDER = "data";
    private static final String DEFAULT_FOLDER_FORMAT = "peer-%s-%d";
    private static final String TAG = "peer.PeerImpl";
    private final Napster napster;
    private final Log log = new ConsoleLog(TAG);
    private String ip;
    private Integer port;
    private File folder;
    private ServerSocket serverSocket;

    public PeerImpl(boolean debug) throws NotBoundException, IOException {
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
                Runtime.getRuntime().addShutdownHook(peer.onShutdown());

                peer.start();
            }

            System.out.println("peer.Peer encerrado!");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar peer!" );
        }
    }

    @Override
    public void start() {
        join();

        boolean running = true;

        while(running) {
            final Operation operation = Operation.read();

            switch (operation) {
                case UPDATE: update(); break;
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
                log.d("Interrupting server peer.thread...");
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
                System.out.println("peer.Peer já inicializado!");
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

            this.serverSocket = new ServerSocket(this.port);
            final ServerThread serverThread = new ServerThread(serverSocket, folder);
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
            log.e("Failed to start server peer.thread", e);
            System.out.println("Falha ao iniciar peer.thread do servidor");
        } catch (RuntimeException e) {
            log.e("Failed to run operation", e);
            System.out.println("Falha ao executar operação");
        } catch (Exception e) {
            System.out.println("Ocorreu um problema na sua operação");
        }
    }

    private void update() {
        final String filename = readInput("Enter the filename to be updated on server: ");

        update(filename);
    }

    @Override
    public void update(String filename) {
        try {
            check(isJoined(), "peer.Peer deve ser inicializado (função 1)!");

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
            check(isJoined(), "peer.Peer deve ser inicializado (função 1)!");

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
            check(isJoined(), "peer.Peer deve ser inicializado (função 1)!");

            final String ip = readInput("Enter peer IP: ");
            final int port = Integer.parseInt(readInput("Enter peer port: "));
            final String filename = readInput("Enter the filename: ");
            final DownloadThread.Callback callback = new DownloadThread.Callback() {
                @Override
                public void onSuccess(String filename) {
                    update(filename);
                }

                @Override
                public void onError(Exception e) {
                    throw new RuntimeException(e);
                }
            };

            final Socket socket = new Socket(ip, port);
            final DownloadThread thread = new DownloadThread(socket, folder, filename, callback);

            thread.start();
        } catch(IOException e) {
            log.e("server.Server failed!", e);
        } catch (RuntimeException e) {
            log.e("Failed to run operation", e);
            System.out.printf("Falha ao executar operação: %s\n", e.getMessage());
        }
    }

    @Override
    public Thread onShutdown() {
        return new Thread(() -> {
            try {
                log.d("Finishing peer gracefully...");
                close();
            } catch (Exception e) {
                log.e("Failed to finish peer gracefully!", e);
            }
        });
    }

    private boolean isJoined() {
        return ip != null && !ip.isEmpty() && port != null && folder != null;
    }
}
