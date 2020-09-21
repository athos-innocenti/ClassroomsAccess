import java.util.ArrayList;


public class Main {

    public static void main(String[] args) {
        final int N = 10;   // number of students
        final int M = 5;    // number of classrooms
        final int K = 2;    // seats per classroom
        final int T = 10000;    // class duration
        final int NUM_TURNSTILES = 4;   // number of turnstiles

        Classrooms classrooms = new Classrooms(T, M, K);

        Turnstiles ts = new Turnstiles(NUM_TURNSTILES);

        Student[] students = new Student[N];
        for (int j = 0; j < students.length; j++) {
            students[j] = new Student(ts, classrooms, M);
            students[j].setName(String.valueOf(j));
            students[j].start();
        }

        Display d = new Display(ts, classrooms, N);
        d.start();
    }
}


class Display extends Thread {
    private Turnstiles t;
    private Classrooms c;
    private int numStudents;

    Display(Turnstiles t, Classrooms c, int numStudents) {
        this.t = t;
        this.c = c;
        this.numStudents = numStudents;
    }

    @Override
    public void run() {
        try {
            int N = numStudents;
            while (N != 0) {
                int n1 = t.out();
                int n2 = c.out();
                N = n1 + n2;
                sleep(500);
            }
            this.interrupt();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}


class Student extends Thread {
    private Turnstiles t;
    private Classrooms classrooms;
    private int numClassrooms;

    Student(Turnstiles t, Classrooms classrooms, int numClassrooms) {
        this.t = t;
        this.classrooms = classrooms;
        this.numClassrooms = numClassrooms;
    }

    @Override
    public void run() {
        try {
            boolean finishedLesson = false;
            while (!finishedLesson) {
                int turnstileId = t.selectTurnstile(this);
                sleep(1000);
                t.releaseTurnstile(turnstileId);
                int classId = (int) (Math.random() * numClassrooms);
                classrooms.joinClassroom(this, classId);
                while (classrooms.getPresentStudents(classId).contains(this))
                    ; // wait for the end of the lesson
                finishedLesson = true;
            }
            System.out.println("Studente " + getStudentName() + " ha finito");
            this.interrupt();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    int getStudentName() {
        return Integer.parseInt(getName());
    }
}


class Turnstiles {
    private ArrayList<ArrayList<Student>> studWaitingPerTurnstile;
    private boolean[] passing;

    Turnstiles(int numTurnstiles) {
        passing = new boolean[numTurnstiles];
        studWaitingPerTurnstile = new ArrayList<>();
        for (int i = 0; i < numTurnstiles; i++)
            studWaitingPerTurnstile.add(new ArrayList<>());
    }

    private synchronized boolean allPassing() {
        boolean allPAssing = true;
        for (boolean b : passing) {
            if (!b) {
                allPAssing = false;
                break;
            }
        }
        return allPAssing;
    }

    private synchronized int shortestWait() {
        int minWait = 100000;
        int index = 0;
        for (int i = 0; i < studWaitingPerTurnstile.size(); i++) {
            if (!allPassing()) {
                if (!passing[i]) {
                    if (studWaitingPerTurnstile.get(i).size() < minWait) {
                        minWait = studWaitingPerTurnstile.get(i).size();
                        index = i;
                    }
                }
            } else {
                if (studWaitingPerTurnstile.get(i).size() < minWait) {
                    minWait = studWaitingPerTurnstile.get(i).size();
                    index = i;
                }
            }
        }
        return index;
    }

    synchronized int selectTurnstile(Student s) throws InterruptedException {
        int index = shortestWait();
        while (passing[index]) {
            if (!studWaitingPerTurnstile.get(index).contains(s))
                studWaitingPerTurnstile.get(index).add(s);
            wait();
        }
        passing[index] = true;
        if (studWaitingPerTurnstile.get(index).contains(s))
            studWaitingPerTurnstile.get(index).remove(s);
        return index;
    }

    synchronized void releaseTurnstile(int turnstileId) {
        passing[turnstileId] = false;
        notifyAll();
    }

    synchronized int out() {
        int sum = 0;
        System.out.println("Students waiting in each turnstile:");
        for (ArrayList<Student> s : studWaitingPerTurnstile) {
            System.out.print(s.size() + " ");
            sum += s.size();
        }
        return sum;
    }
}


class Classrooms {
    private int classDuration;
    private boolean[] lesson;
    private ArrayList<ArrayList<Student>> presentStudents;
    private ArrayList<ArrayList<Student>> waitingStudents;
    private int seatsPerClass;

    Classrooms(int time, int M, int K) {
        classDuration = time;
        seatsPerClass = K;
        lesson = new boolean[M];
        waitingStudents = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            waitingStudents.add(new ArrayList<>());
        }
        presentStudents = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            presentStudents.add(new ArrayList<>());
        }
    }

    synchronized void joinClassroom(Student s, int classId) throws InterruptedException {
        while (presentStudents.get(classId).size() == seatsPerClass) {
            if (!waitingStudents.get(classId).contains(s)) {
                System.out.println("\nStudente " + s.getStudentName() + " aspetta per la classe: " + classId);
                waitingStudents.get(classId).add(s);
            }
            wait();
        }
        incPresentStudents(s, classId);
        if (presentStudents.get(classId).size() == 1 && !lesson[classId]) {
            lesson[classId] = true;
            startNewLesson(classId);
        }
        if (presentStudents.get(classId).size() == 0 && waitingStudents.get(classId).size() > 0) {
            for (int i = 0; i < waitingStudents.get(classId).size(); i++) {
                Student stud = waitingStudents.get(classId).get(i);
                if (presentStudents.get(classId).size() < seatsPerClass) {
                    incPresentStudents(stud, classId);
                    waitingStudents.get(classId).remove(i);
                }
            }
            lesson[classId] = true;
            startNewLesson(classId);
        }
    }

    private void startNewLesson(int classId) {
        System.out.println("\nInizio lezione in aula " + classId);
        Lesson ls = new Lesson(classDuration, classId, this);
        ls.start();
    }

    private void incPresentStudents(Student s, int classId) {
        System.out.println("\nStudente " + s.getStudentName() + " entra nella aula " + classId);
        presentStudents.get(classId).add(s);
    }

    synchronized void restartLesson(int classId) throws InterruptedException {
        lesson[classId] = false;
        presentStudents.get(classId).clear();
        Thread.sleep(2000);
        notifyAll();
    }

    synchronized int out() {
        int sum = 0;
        System.out.println("Students in each classroom:");
        for (ArrayList<Student> s : presentStudents) {
            System.out.print(s.size() + " ");
            sum += s.size();
        }
        System.out.println("Students waiting for each classroom:");
        for (ArrayList<Student> s : waitingStudents) {
            System.out.print(s.size() + " ");
            sum += s.size();
        }
        System.out.println("Lesson in each classroom:");
        for (boolean s : lesson)
            System.out.print(s + " ");
        System.out.println();
        return sum;
    }

    ArrayList<Student> getPresentStudents(int classId) {
        return presentStudents.get(classId);
    }
}


class Lesson extends Thread {
    private int classDuration;
    private int classId;
    private Classrooms classrooms;

    Lesson(int classDuration, int classId, Classrooms classrooms) {
        this.classDuration = classDuration;
        this.classId = classId;
        this.classrooms = classrooms;
    }

    @Override
    public void run() {
        try {
            sleep(classDuration);
            classrooms.restartLesson(classId);
            System.out.println("\nFine lezione in aula " + classId);
            this.interrupt();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}