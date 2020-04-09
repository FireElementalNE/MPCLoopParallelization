import java.util.Random;

class DB_JOIN3_FIXED {
    private static Random rand = new Random();

    static int rand() {
        return rand.nextInt(1000);
    }

    public static void main(String[] args) {
        int[] db = new int[100];
        int ATT_A = 0;
        int LEN_A = db.length;
        int att = Integer.parseInt(args[1]);
        int len = Integer.parseInt(args[2]);
        for(int i = 0; i < LEN_A; i++) {
            //a.db[i*ATT_A] = i*10+rand()%8;
            db[i*ATT_A] = rand() % 20;
            db[i*ATT_A+1] = rand() % 100;
        }
    }
}