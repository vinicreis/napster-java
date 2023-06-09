package util;

public class ProgressBar {
    private static final int SIZE = 20;
    private static final String PROGRESS_TOKEN = "=";
    private static final String END_PROGRESS_TOKEN = ">";
    private final String leftText;
    private final long total;
    private double progress;

    public ProgressBar(long total, String leftText){
        this.total = total;
        this.leftText = leftText;
    }

    public void update(int value) {
        progress = (double) total / value;
    }

    public void print() {
        final long filledSize = Math.round(SIZE * progress);
        final long unfilledSize = Math.round(1 - SIZE * progress);

        if(leftText != null && leftText.isEmpty())
            System.out.print(leftText + " ");

        System.out.print("|");

        for (int i = 0; i < filledSize; i++)
            System.out.print(PROGRESS_TOKEN);

        if (filledSize > 0)
            System.out.print(END_PROGRESS_TOKEN);

        for (int i = 0; i < unfilledSize; i++)
            System.out.print(" ");

        System.out.printf("| %.2f %%\r", progress * 100);
    }
}
