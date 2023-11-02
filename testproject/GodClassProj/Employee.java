public class Employee {
    private int employeeID;
    private String employeeName;
    private String employeeAddress;
    private double employeeSalaray;

    public int getEmployeeID(){
        return employeeID;
    }
    
    public void setEmployeeID(int id){
        employeeID = id;
    } 
    public String getEmployeeName() {  
        return employeeName; 
    }  
    public void setEmployeeName(String name){
        employeeName = name;
    }

    public String getEmployeeAddress() {
        return employeeAddress;
    }
    public void setEmployeeAddress(String employeeAddress) {
        this.employeeAddress = employeeAddress;
    }
    public double getEmployeeSalaray() {
        return employeeSalaray;
    }
    public void setEmployeeSalaray(double employeeSalaray) {
        this.employeeSalaray = employeeSalaray;
    }
    
}
