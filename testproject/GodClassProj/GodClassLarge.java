public class GodClass {
    //god class will represent an employee - their details and things you can do with them
    private Employee e;
    private Manager m;
    String empDetails;
    int managerDetails;
    int test;
    String test2;
    String string;

    public GodClass(Employee e, Manager m){
        this.e = e;
        this.m = m;
        this.test = 0;
        this.test2 = "Working";
        this.string = "Hallelujiah";
        System.out.println("in god class");
    }

    public void setTest(int number) {
        this.test = number;
    }

    public int getTest() {
        return this.test;
    }

    public void setTest2(String string) {
        this.test2 = string;
    }

    public String getTest2() {
        return this.test2;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
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

    public Employee getEmployee() {
        return this.e;
    }

    public Manager getManager() {
        return this.m;
    }

    public void setEmployee(Employee e) {
        this.e = e;
    }

    public void setManager(Manager m) {
        this.m = m;
    }

    public void FetchManagerDetails1() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails2() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails3() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails4() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails5() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails6() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails7() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails8() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails9() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails10() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails11() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }

    public void FetchManagerDetails12() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }
    public void FetchManagerDetails13() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }
    public void FetchManagerDetails14() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }
    public void FetchManagerDetails15() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }
    public void FetchManagerDetails16() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }
    public void FetchManagerDetails17() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
        this.m.getManagerID();
    }
}