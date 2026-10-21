package exam.strategy.grading;

/** One exam, including two separately assessed sections. */
public record GradeInput(
        int correct, int total,
        int theoryCorrect, int theoryTotal,
        int practicalCorrect, int practicalTotal) {
    public GradeInput {
        if (theoryTotal < 1 || practicalTotal < 1 || total < 1
                || theoryCorrect < 0 || theoryCorrect > theoryTotal
                || practicalCorrect < 0 || practicalCorrect > practicalTotal
                || correct < 0 || correct > total
                || (long) theoryTotal + practicalTotal != total
                || (long) theoryCorrect + practicalCorrect != correct) {
            throw new IllegalArgumentException("Inconsistent exam section totals");
        }
    }
}
