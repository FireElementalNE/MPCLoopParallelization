class DB_JOIN_TWO1 {
    public static void main(String[] args) {
        int[] db = new int[100];
        int[] row = new int[2];
        int[] OUTPUT_db = new int[100];
        int att = Integer.parseInt(args[1]);
        int att_out = Integer.parseInt(args[2]);
        int id_out = 0;
        int len = db.length;
        for(int i = 0; i < len; i++) {
            if(db[i*att] == row[0]) {
                id_out++;
                OUTPUT_db[i*att_out] = db[i*att];
                OUTPUT_db[i*att_out+1] = db[i*att+1];
                OUTPUT_db[i*att_out+2] = row[1];
            }
        }
    }
}
