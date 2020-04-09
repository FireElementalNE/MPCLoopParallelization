import java.util.Random;

class DB_JOIN_TWO2 {
    private static Random rand = new Random();

    private static int sqr(int val, int exp) {
        int dist = (val - exp) * (val - exp);
        return dist;
    }


    static int rand() {
        return rand.nextInt(1000);
    }

    public static void main(String[] args) {
        int[] db = new int[100];
        int[] var = new int[100];
        int att = Integer.parseInt(args[1]);
        int att_out = Integer.parseInt(args[2]);
        int len = db.length;
        int exp = Integer.parseInt(args[3]);
        for(int i = 0; i < len; i++) {
            if(db[i]!=0) {
                var[i] = sqr(db[i], exp);
            } else {
                var[i] = 0;
            }
        }
    }
}
