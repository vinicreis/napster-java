import java.util.Arrays;

public interface Server extends AutoCloseable {
    void start();

    static void main(String[] args) {
        try (Server server = new ServerImpl(Arrays.asList(args).contains("--d"))) {
            server.start();

            System.out.println("\nPress any key to stop...");

            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        } catch (Exception e) {
            System.out.println("Failed to start server");
        }

        System.exit(0);
    }
}
