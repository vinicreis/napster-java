package peer.thread;

import log.ConsoleLog;
import log.Log;
import service.model.enums.Operation;
import view.ProgressBar;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import static peer.config.Config.BUFFER_SIZE;

public class DownloadThread extends Thread {
    private static final String TAG = "DownloadThread";
    private static final Log log = new ConsoleLog(TAG);
    private final Socket socket;
    private final BufferedInputStream reader;
    private final PrintWriter writer;
    private final File file;
    private final Callback callback;

    public interface Callback {
        void onSuccess(String filename);
        void onError(Exception e);
    }

    public DownloadThread(Socket socket, File folder, String filename, Callback callback) throws IOException {
        this.setName(TAG + "-" + getId());
        this.socket = socket;
        this.reader = new BufferedInputStream(socket.getInputStream());
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.file = new File(folder, filename);
        this.callback = callback;
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
//                        progressBar.print();
                    }
                } while (count > 0);
            }

            log.d("File download! Updating on server...");
            System.out.printf(
                    "\n\nArquivo %s baixado com sucesso na pasta %s",
                    file.getName(),
                    file.getParentFile().getPath()
            );

            callback.onSuccess(file.getName());
        } catch (SocketException e) {
            System.out.println("Falha ao fazer download do arquivo!");

            if (file.exists() && file.delete()) {
                System.out.printf("Arquivo %s deletado!\n", file.getName());
            } else {
                System.out.println("Sem arquivos para deletar");
            }

            callback.onError(e);
        } catch (Exception e) {
            log.e("Failed to download file!", e);

            if (file.exists() && file.delete()) {
                System.out.printf("Arquivo %s deletado!\n", file.getName());
            } else {
                System.out.println("Sem arquivos para deletar");
            }

            callback.onError(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore close errors
            }

            Operation.reprint();
        }
    }
}
