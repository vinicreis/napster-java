package view;

public class ProgressBar {
    private static final int SIZE = 20;
    private static final String PROGRESS_TOKEN = "=";
    private static final String END_PROGRESS_TOKEN = ">";
    private final String name;
    private final String leftText;
    private final long total;
    private long current = 0;
    private double progress;

    public ProgressBar(String name, long total, String leftText){
        this.name = name;
        this.total = total;
        this.leftText = leftText;
    }

    public static void main(String[] args) {
        try {
            final ProgressBar progress = new ProgressBar("Test", 100, "Nothing...");

            for (int i = 0; i < 100; i++) {
                Thread.sleep(1500);

                progress.update(i);
                progress.print();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void update(long value) {
        current = value;
        progress = Math.min(1.0, (double) current / total);
    }

    public void print() {
        final long filledSize = Math.round(SIZE * progress);
        final long unfilledSize = SIZE - filledSize;

        if(name != null && !name.isEmpty())
            System.out.printf("[%s] ", name);

        if(leftText != null && !leftText.isEmpty())
            System.out.print(leftText + " ");

        System.out.print("|");

        for (int i = 0; i < filledSize; i++)
            System.out.print(PROGRESS_TOKEN);

        if (filledSize > 0)
            System.out.print(END_PROGRESS_TOKEN);

        for (int i = 0; i < unfilledSize; i++)
            System.out.print(" ");

        System.out.printf("| (%d, %d) %.2f %%\r", current, total, progress * 100);
    }
}
