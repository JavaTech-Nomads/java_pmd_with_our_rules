public class GodClass {
    //god class will represent an employee - their details and things you can do with them
    private Employee e;
    private Manager m;
    String empDetails;
    int managerDetails;

    public GodClass(Employee e, Manager m){
        this.e = e;
        this.m = m;
        System.out.println("in god class");
    }

    public void FetchEmployeeDetails() {
        this.e.getEmployeeName();
        this.e.getEmployeeId();
        this.e.getEmployeeSalaray();
        this.e.getEnclosingClass();
    }  
    public void SaveEmployeeDetails(Employee e) {
        empDetails = e.getEmployeeName;
    }  
    public void ValidateEmployeeDetails() {
        System.out.println("Validating emp details : " + empDetails);
    }  
    public void ExportEmpDetailsToCSV() {
        System.out.println("Exporting emp details : " + empDetails);
    }  
    public void ImportEmpDetailsForDb() {}  

    public void FetchManagerDetails() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }  
    public void SaveManagerDetails() {}  
    public void ValidateManagerDetails() {
        System.out.println("Validating manger details : " + managerDetails);
    }  
    public void ExportManagerDetailsToCSV() {}  
    public void ImportManagerDetailsForDb(Manager m) {
        System.out.println("doing something with m");
    }  
}