package pl.kurs.threads;

/**
 * Page visit counter. visit() is called by many server threads at the same time.
 */
public class VisitCounter {

    // "volatile, so that all threads see the current value" (from code review)
    private volatile int visits;
    private volatile int uniqueUsers;

    public void visit(boolean newUser) {
        visits++;
        if (newUser) {
            uniqueUsers++;
        }
    }

    public int visits() {
        return visits;
    }

    public int uniqueUsers() {
        return uniqueUsers;
    }
}
