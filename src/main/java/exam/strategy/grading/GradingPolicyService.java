package exam.strategy.grading;

import java.util.Objects;

/** Intentionally bad design for the first Git commit: one large switch. */
public final class GradingPolicyService {
    public int calculate(GradeInput input, ExamPurpose purpose) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(purpose, "purpose");
        return switch (purpose) {
            case STANDARD -> (int) Math.round(100.0 * input.correct() / input.total());
            case COMPETITIVE -> {
                int wrong = input.total() - input.correct();
                double points = Math.max(0, input.correct() - wrong * 0.25);
                yield (int) Math.round(100.0 * points / input.total());
            }
            case CERTIFICATION -> {
                double theory = (double) input.theoryCorrect() / input.theoryTotal();
                double practical = (double) input.practicalCorrect() / input.practicalTotal();
                yield (int) Math.round(100.0 * Math.min(theory, practical));
            }
        };
    }
}
