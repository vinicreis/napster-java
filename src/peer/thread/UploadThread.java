package peer.thread;

import log.ConsoleLog;
import log.Log;
import service.model.enums.Operation;
import view.ProgressBar;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import static peer.config.Config.BUFFER_SIZE;
import static util.AssertUtil.check;

public class UploadThread extends Thread {
    private static final String TAG = "UploadThread";
    private static final Log log = new ConsoleLog(TAG);
    private final File folder;
    private final Socket socket;
    private final BufferedReader reader;
    private final OutputStream writer;

    public UploadThread(Socket socket, File folder) throws IOException {
        this.folder = folder;
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

            final byte[] buffer = new byte[BUFFER_SIZE];
            long bytesSent = 0;
            int bytesCount;

            System.out.printf(
                    "\n\nEnviando arquivo %s ao peer %s:%d...\n",
                    file.getName(),
                    socket.getInetAddress().getHostName(),
                    socket.getPort()
            );
            log.d(String.format("Uploading file to peer %s", socket.getInetAddress().getHostName()));

            try(final FileInputStream fileReader = new FileInputStream(file)) {
                do {
                    bytesCount = fileReader.read(buffer);
                    bytesSent += bytesCount;

                    if(bytesCount > 0) {
                        writer.write(buffer, 0, bytesCount);
                        writer.flush();

                        progressBar.update(bytesSent);
//                    progressBar.print();
                    }
                } while (bytesCount > 0);
            }

            log.d("Upload finished! Closing connection...");
        } catch (SocketException e) {
            System.out.println("Falha ao enviar arquivo!");

            log.e(String.format("Failed to upload file to peer %s", socket.getInetAddress().getHostName()), e);
        } catch (Exception e) {
            log.e(String.format("Failed to upload file to peer %s", socket.getInetAddress().getHostName()), e);
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
