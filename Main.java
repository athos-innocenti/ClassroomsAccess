import java.util.ArrayList;


public class Main {

    public static void main(String[] args) {
        final int N = 35;              // number of students
        final int M = 5;                // number of classrooms
        final int K = 10;               // seats per classroom
        final int T = 10000;            // class duration
        final int NUM_TURNSTILES = 4;

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
    private int numStudents;
    private Turnstiles turnstiles;
    private Classrooms classrooms;

    Display(Turnstiles turnstiles, Classrooms classrooms, int numStudents) {
        this.numStudents = numStudents;
        this.turnstiles = turnstiles;
        this.classrooms = classrooms;
    }

    @Override
    public void run() {
        try {
            int N = numStudents;
            while (N != 0) {
                sleep(500);
                int n1 = turnstiles.out();
                int n2 = classrooms.out();
                N = n1 + n2;
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}


class Student extends Thread {
    private int numClassrooms;
    private Turnstiles turnstiles;
    private Classrooms classrooms;

    Student(Turnstiles turnstiles, Classrooms classrooms, int numClassrooms) {
        this.numClassrooms = numClassrooms;
        this.turnstiles = turnstiles;
        this.classrooms = classrooms;
    }

    @Override
    public void run() {
        try {
            int turnstileId = turnstiles.selectTurnstile(this);
            sleep(1000);
            turnstiles.releaseTurnstile(turnstileId);
            int classId = (int) (Math.random() * numClassrooms);
            classrooms.joinClassroom(this, classId);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}


class Turnstiles {
    private boolean[] passing;
    private ArrayList<ArrayList<Student>> studWaitingPerTurnstile;

    Turnstiles(int numTurnstiles) {
        passing = new boolean[numTurnstiles];
        studWaitingPerTurnstile = new ArrayList<>();
        for (int i = 0; i < numTurnstiles; i++)
            studWaitingPerTurnstile.add(new ArrayList<>());
    }

    private synchronized int shortestWait() {
        int minWait = 100000;
        int index = 0;
        for (int i = 0; i < studWaitingPerTurnstile.size(); i++) {
            if (studWaitingPerTurnstile.get(i).size() < minWait) {
                minWait = studWaitingPerTurnstile.get(i).size();
                index = i;
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
    private int seatsPerClass;
    private int classDuration;
    private boolean[] lesson;
    private ArrayList<ArrayList<Student>> presentStudents;
    private ArrayList<ArrayList<Student>> waitingStudents;

    Classrooms(int time, int M, int K) {
        seatsPerClass = K;
        classDuration = time;
        lesson = new boolean[M];
        presentStudents = new ArrayList<>();
        for (int i = 0; i < M; i++)
            presentStudents.add(new ArrayList<>());
        waitingStudents = new ArrayList<>();
        for (int i = 0; i < M; i++)
            waitingStudents.add(new ArrayList<>());
    }

    private void startNewLesson(int classId) {
        Lesson ls = new Lesson(classDuration, classId, this);
        ls.start();
    }

    synchronized void joinClassroom(Student s, int classId) throws InterruptedException {
        while (presentStudents.get(classId).size() == seatsPerClass) {
            if (!waitingStudents.get(classId).contains(s))
                waitingStudents.get(classId).add(s);
            wait();
        }
        if (waitingStudents.get(classId).contains(s))
            waitingStudents.get(classId).remove(s);
        presentStudents.get(classId).add(s);
        if (presentStudents.get(classId).size() == 1 && !lesson[classId]) {
            lesson[classId] = true;
            startNewLesson(classId);
        }
    }

    synchronized void restartLesson(int classId) {
        for (Student s : presentStudents.get(classId))
            s.interrupt();
        presentStudents.get(classId).clear();
        lesson[classId] = false;
        notifyAll();
    }

    synchronized int out() {
        int sum = 0;
        System.out.println("\nStudents in each classroom:");
        for (ArrayList<Student> s : presentStudents) {
            System.out.print(s.size() + " ");
            sum += s.size();
        }
        System.out.println("\nStudents waiting for each classroom:");
        for (ArrayList<Student> s : waitingStudents) {
            System.out.print(s.size() + " ");
            sum += s.size();
        }
        System.out.println("\nLesson in each classroom:");
        for (boolean s : lesson)
            System.out.print(s + " ");
        System.out.println("\n");
        return sum;
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
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}