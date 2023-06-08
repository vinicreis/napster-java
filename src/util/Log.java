package util;

public interface Log {
    void setDebug(boolean enable);
    void e(String msg);
    void e(String msg, Throwable e);
    void d(String msg);
    void w(String msg);
    void v(String msg);
}
