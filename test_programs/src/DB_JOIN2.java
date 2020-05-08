class DB_JOIN2 {
    public static void main(String[] args) {
        int[] sum = new int[100];
        int[] db = new int[100];
        int att = 100;
        int len = 100;
        for(int i = 0; i < len; i++) {
            sum[i] = db[i*att+1] + db[i*att+2];
        }
    }
}