package util;

public interface Log {
    void e(String msg);
    void e(String msg, Throwable e);
    void i(String msg);
    void w(String msg);
    void v(String msg);
}
