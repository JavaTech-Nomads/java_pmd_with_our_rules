/**
 * This is a test project for the Godclass code smell
 * sdf
 * sd
 * fsdf
 * sdf
 * sdf
 * sdf
 * sdf
 * dfb
 * fb
 * sc
 * ad
 * a
 * 
 * czxc
 * czx
 * c
 * adfa
 * zsc
 * asd
 */
public class GodClass {
    /**
     * sample attribtes
     */
    private Employee e;
    private Manager m;
    private int test;


    public GodClass(Employee e, Manager m){
        this.e = e;
        this.m = m;
        this.test = 0;
    }

    /**
     * Returns an employee
     */
    public Employee getEmployee() {
        return this.e;
    }

    /**
     * Returns a manager
     */
    public Manager getManager() {
        return this.m;
    }

    /**
     * sets test variable
     */
    public void setTest(int number) {
        this.test = number;
    }

    /**
     * Fethces manager details
     */
    public void FetchManagerDetails() {
        this.m.getManagerName();
        this.m.getManagerAddress();
        this.m.getManagerSalaray();
    }

    /**
     * Fetches employee details
     */
    public void FetchEmployeeDetails() {
        this.e.getEmployeeName();
        this.e.getEmployeeAddress();
        this.test = 5;
    }}