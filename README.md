# Assignment 4 — Exam Proctoring: Facade over a Three-Stage Chain

Java 17 / Maven / SQLite / JUnit 5 / JaCoCo. Individual domain: **Exam Proctoring** (variant 12).

## Run

1. Install **JDK 17+** and **Apache Maven 3.9+**.
2. Open the project folder in IntelliJ IDEA as a Maven project, or run:

```bash
mvn clean verify
mvn exec:java
```

### IntelliJ IDEA: `No suitable driver found for jdbc:sqlite:`

This error means the **SQLite JDBC dependency is missing from the runtime classpath**. A run command that contains only `-classpath ...\target\classes exam.Main` is NOT a Maven run and does not include SQLite JDBC.

**Fix in IntelliJ IDEA 2025:** Open the folder containing `pom.xml` as a **Maven project**, enable Maven import/sync (Maven tool window -> Reload All Maven Projects), then run **Maven -> Plugins -> exec -> exec:java**, or in IntelliJ Terminal run:

```powershell
mvn clean verify
mvn exec:java
```

Or create a self-contained runnable JAR and launch it with Java 17+ (Java 25 is fine):

```powershell
mvn clean package
java -jar target/exam-proctoring-facade-1.0-all.jar
```

If `mvn` is not recognized, install Maven, use IntelliJ's Maven tool window, or configure Maven in the IDE. When using the green Run button for `exam.Main`, ensure its module classpath includes Maven dependencies; if the command still lists only `target/classes`, reload Maven or use the above Maven commands instead.

The first command runs tests and generates `target/site/jacoco/index.html`. The second launches the **interactive CLI**. Maven downloads dependencies on the first run. The persistent database is `exam-proctoring.db` in the working directory. To reset the demo, **exit the program**, delete `exam-proctoring.db`, and launch it again.

To specify a different SQLite file: `mvn exec:java -Dexam.db=demo.db` (the system property can also be set in the IDE run configuration).

## Live defense script

1. Choose **1**, enter `exam001`: workflow `COMPLETED`, admission `VERIFIED`, monitoring `CLEARED`, transcript `RECORDED`.
2. Choose **2**, enter `exam002`: B rejects plagiarism similarity 90%; A becomes `REVOKED`, B and C are absent, workflow `COMPENSATED`.
3. Choose **3**, enter `exam003`: transcript recorder is offline; B becomes `CANCELLED`, then A becomes `REVOKED`, workflow `COMPENSATED`.
4. Choose **5** and inspect `exam001`, `exam002`, `exam003`. Exit, restart and inspect again to demonstrate disk persistence.
5. Choose **4** for manual input, **6** for manual compensation of an unfinished workflow, and **7** to resume a persisted pending workflow (useful for a seeded or interrupted state).

## Architecture

- **A / Admission**: `Admission` (entity and legal status transitions), `IdentityVerifier` (business rules, including exam timer duration), `AdmissionRepository` (direct SQLite gateway).
- **B / Monitoring**: `Monitoring` (entity), `PlagiarismChecker` (screen connection and similarity rule), `MonitoringRepository` (direct gateway).
- **C / Finalization**: `Finalization` (entity), `GradingEngine` (score calculation and transcript availability), `FinalizationRepository` (direct gateway).
- **ExamFacade**: coordinates A → B → C and compensates from durable records. **AdminFacade**: inspection and manual rollback.
- **Store**: embedded SQLite database with four durable tables: `admission`, `monitoring`, `finalization`, `workflow`.

No subsystem imports or calls another subsystem. Each subsystem can be instantiated independently; see `eachSubsystemCanBeUsedWithoutFacade` test. Entity transition guards work even without a facade.

### Durable saga / compensation

After A commits, `workflow=PENDING_B`; after B commits, `workflow=PENDING_C`; after C commits, `workflow=COMPLETED`. If B fails, A is revoked. If C fails, B is cancelled **before** A is revoked. `compensate` re-reads the persisted B and A rows and is idempotent for already compensated operations. Restart/recovery tests explicitly reopen SQLite. Completed workflows cannot be manually rolled back. This is a compensating saga, not a single cross-database SQL transaction.

### Principle of Least Knowledge: before / after

**Before (bad, direct coupling):**

```java
AdmissionRepository admissions = new AdmissionRepository(db);
MonitoringRepository monitors = new MonitoringRepository(db);
FinalizationRepository transcripts = new FinalizationRepository(db);
// Client would also invoke IdentityVerifier, PlagiarismChecker,
// GradingEngine, entity transitions and reverse operations itself.
```

**After (one friend):**

```java
ExamFacade exams = new ExamFacade(db);
String result = exams.start(request);
// Administration has its own narrower entry point:
String snapshot = new AdminFacade(db).inspect(request.id());
```

The client no longer needs to know the subsystem sequence, persistence gateways, or compensation order.

### Facade vs Adapter (reflection)

This design needs a **Facade**, not an Adapter, because the client's problem is *orchestration*, not an incompatible interface. The exam workflow combines independent identity/timer admission, screen/plagiarism monitoring, and grading/transcript recording into one understandable operation. An Adapter could translate the API of one monitoring vendor into another API, but it would not by itself decide when to reserve admission, when to commit a monitoring result, how to recover after restart, or how to compensate previously committed work when a later step fails. `ExamFacade` owns that cross-subsystem sequencing while the subsystems retain their own rules and persistence. `AdminFacade` provides a smaller interface for inspection and manual recovery. The two facades simplify what clients need to know without changing the underlying subsystem contracts.

## Testing and coverage

`mvn clean verify` runs JUnit tests, generates the JaCoCo HTML report, and supports verification of line coverage. Tests use independent temporary SQLite databases. Scenarios cover successful processing, both compensation depths, persisted restart at B and C, invalid input, duplicate IDs, direct subsystem usage, state transitions, and administrative inspection. Test helper `anExam(...)` reads as a small domain-specific testing DSL. **Check the generated JaCoCo percentage locally**: the 80% requirement cannot be claimed without an actual coverage report.

## UML

`docs/class-diagram.png` (rendered) and `docs/class-diagram.dot` (editable Graphviz source).

## Git / submission checklist

Commit code and tests together in meaningful Git commits (the instructor checks timely test history); publish to your own GitHub repository; run the CLI and `mvn clean verify`; review JaCoCo line coverage and improve tests if below 80%; submit **your actual repository URL** in Moodle. The ZIP is a local project package, not a substitute for a real Git history or URL.

## Simplifications / limitations

This is a teaching demo, not real webcam surveillance or plagiarism detection: screen availability and similarity are supplied as CLI input. The timer is a validated duration rather than a wall-clock scheduler. Crash recovery uses explicit `resume` with original request parameters; it does not automatically replay a request after a process kill. Database writes are durable but not an exactly-once distributed transaction; a hard crash between a domain write and a workflow update requires operator inspection. These limitations should be explained honestly during the defense.
