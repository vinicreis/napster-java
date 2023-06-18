package service.model.repository;

import com.sun.istack.internal.NotNull;
import log.ConsoleLog;
import log.Log;
import util.AssertUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface SimpleFileEntity {
    String toFile();
}

public abstract class SimpleFileRepository<K, T extends SimpleFileEntity> {
    // TODO: Make it concurrent
    private static final String TAG = "SimpleFileRepository";
    private static final Log log = new ConsoleLog(TAG);
    private final File file;

    public SimpleFileRepository(String path) throws RuntimeException {
        file = new File(path);

        AssertUtil.check(file.exists(), String.format("File %s not found", file.getPath()));
    }

    public abstract T fromFile(String encoded);

    public T findOne(K id) {
        try(Scanner scanner = new Scanner(file)) {
            if(scanner.hasNext(String.format("%s:", id.toString()))) {
                return fromFile(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            log.e(String.format("File %s not found", file.getPath()));
        } catch (Exception e) {
            log.e("Failed to find object", e);
        }

        return null;
    }

    @NotNull
    public List<T> findMany(K id) {
        final List<T> result = new ArrayList<>();

        try(Scanner scanner = new Scanner(file)) {
            while(scanner.hasNext(String.format("%s:", id.toString()))) {
                result.add(fromFile(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            log.e(String.format("File %s not found", file.getPath()));
        } catch (Exception e) {
            log.e("Failed to find object", e);
        }

        return result;
    }

    public boolean insert(K id, T obj) {
        try(FileWriter writer = new FileWriter(file)) {
            writer.write(String.format("%s:%s", id.toString(), obj.toFile()));

            return true;
        } catch (IOException e) {
            log.e("Failed to insert object", e);

            return false;
        }
    }

    public int remove(K id) {
        // TODO: Not implemented yet
        return -1;
    }
}
