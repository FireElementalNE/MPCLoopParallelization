import java.util.Random;
class InputA {
    public int[] db;
    InputA() {
        db = new int[100];
    }
}
class DB_JOIN3 {
    private static Random rand = new Random();



    static int rand() {
        return rand.nextInt(1000);
    }

    public static void main(String[] args) {
        InputA a = new InputA();
        int ATT_A = 0;
        int LEN_A = a.db.length;
        int att = Integer.parseInt(args[1]);
        int len = Integer.parseInt(args[2]);
        for(int i = 0; i < LEN_A; i++) {
            //a.db[i*ATT_A] = i*10+rand()%8;
            a.db[i*ATT_A] = rand() % 20;
            a.db[i*ATT_A+1] = rand() % 100;
        }

    }
}