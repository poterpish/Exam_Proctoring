package exam;
import org.junit.jupiter.api.*;import java.nio.file.*;import static org.junit.jupiter.api.Assertions.*;
class ExamFacadeTest {
 private Path file;private Store db;private ExamFacade facade;private AdminFacade admin;
 @BeforeEach void fresh()throws Exception{file=Files.createTempFile("exam-test-",".sqlite");db=new Store(file.toString());facade=new ExamFacade(db);admin=new AdminFacade(db);}
 @AfterEach void clean()throws Exception{Files.deleteIfExists(file);}
 private ExamRequest anExam(String id,int similarity,boolean recorder){return new ExamRequest(id,"student001",60,similarity,true,8,10,recorder);}
 @Test void successfulExam(){assertEquals("COMPLETED",facade.start(anExam("happy",10,true)));assertTrue(admin.inspect("happy").contains("RECORDED score=80"));}
 @Test void whenBRejectsOnlyAIsCompensated(){assertTrue(facade.start(anExam("failureB",90,true)).startsWith("COMPENSATED"));assertEquals("REVOKED",db.get("admission","failureB")[0]);assertNull(db.get("monitoring","failureB"));}
 @Test void whenCFailsBThenAAreCompensated(){assertTrue(facade.start(anExam("failureC",10,false)).startsWith("COMPENSATED"));assertEquals("CANCELLED",db.get("monitoring","failureC")[0]);assertEquals("REVOKED",db.get("admission","failureC")[0]);assertNull(db.get("finalization","failureC"));}
 @Test void restartResumesPersistedPendingB(){db.put("admission","restartB","VERIFIED","student001");db.put("workflow","restartB","PENDING_B","");assertEquals("COMPLETED",new ExamFacade(new Store(file.toString())).resume(anExam("restartB",10,true)));}
 @Test void restartCompensatesPersistedPendingC(){db.put("admission","restartC","VERIFIED","student001");db.put("monitoring","restartC","CLEARED","similarity=10");db.put("workflow","restartC","PENDING_C","");assertTrue(new ExamFacade(new Store(file.toString())).resume(anExam("restartC",10,false)).startsWith("COMPENSATED"));assertEquals("CANCELLED",db.get("monitoring","restartC")[0]);}
 @Test void invalidAdmissionDoesNotCommit(){assertThrows(IllegalArgumentException.class,()->facade.start(new ExamRequest("bad","x",10,0,true,1,1,true)));assertNull(db.get("workflow","bad"));}
 @Test void duplicateExamRejected(){facade.start(anExam("duplicate",10,true));assertThrows(IllegalArgumentException.class,()->facade.start(anExam("duplicate",10,true)));}
 @Test void manualRollbackIsIdempotent(){db.put("admission","manual","VERIFIED","student001");db.put("workflow","manual","PENDING_B","");admin.cancel("manual");admin.cancel("manual");assertTrue(admin.inspect("manual").contains("COMPENSATED"));}
 @Test void completedCannotBeCancelled(){facade.start(anExam("done",10,true));assertThrows(IllegalStateException.class,()->admin.cancel("done"));}
 @Test void disconnectedScreenIsRejected(){assertTrue(facade.start(new ExamRequest("offline","student001",60,10,false,8,10,true)).startsWith("COMPENSATED"));}
 @Test void illegalEntityTransitionsAreRejected(){assertThrows(IllegalStateException.class,()->new Admission("x","s",Admission.Status.NEW).revoke());assertThrows(IllegalStateException.class,()->new Monitoring("x","",Monitoring.Status.READY).cancel());assertThrows(IllegalStateException.class,()->new Finalization("x","",Finalization.Status.DRAFT).voidRecord());}
 @Test void eachSubsystemCanBeUsedWithoutFacade(){new IdentityVerifier().check("student001",60);assertThrows(IllegalStateException.class,()->new PlagiarismChecker().check(80,true));assertEquals(75,new GradingEngine().grade(3,4,true));}
 @Test void administrativeInspectionHandlesMissing(){assertEquals("NOT FOUND",admin.inspect("missing"));}
 @Test void gradingValidatesAnswers(){assertThrows(IllegalArgumentException.class,()->new GradingEngine().grade(12,10,true));}
 @Test void screenSimilarityValidation(){assertThrows(IllegalArgumentException.class,()->new PlagiarismChecker().check(-1,true));}
 @Test void unknownExamCannotResume(){assertThrows(IllegalArgumentException.class,()->facade.resume(anExam("unknown",10,true)));}
}
