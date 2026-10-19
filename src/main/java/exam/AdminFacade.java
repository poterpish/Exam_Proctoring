package exam;
public class AdminFacade {
 private final Store db;private final ExamFacade workflow;private final AdmissionRepository a;private final MonitoringRepository b;private final FinalizationRepository c;
 public AdminFacade(Store db){this.db=db;workflow=new ExamFacade(db);a=new AdmissionRepository(db);b=new MonitoringRepository(db);c=new FinalizationRepository(db);}
 public String inspect(String id){String[] f=db.get("workflow",id);if(f==null)return "NOT FOUND";Admission x=a.find(id);Monitoring y=b.find(id);Finalization z=c.find(id);return "Exam="+id+" | workflow="+f[0]+" | admission="+(x==null?"NONE":x.status())+" | monitoring="+(y==null?"NONE":y.status())+" | transcript="+(z==null?"NONE":z.status()+" "+z.detail)+" | reason="+f[1];}
 public void cancel(String id){workflow.compensate(id,"Manual administrator rollback");}
}
