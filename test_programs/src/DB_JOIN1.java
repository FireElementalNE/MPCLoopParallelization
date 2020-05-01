class DB_JOIN1 {
    public static void main(String[] args) {
        int[] a = new int[100];
        int[] b = new int[100];
        int[] OUTPUT_db = new int[100];
        int ATT_A = 10;
        int ATT_B = 11;
        int ATT = 12;
        int id_out  = 0;
        int LEN_B = 0;
        int i = 0;
        for(int j = 0; j < LEN_B; j++) {
            if(a[i*ATT_A] == b[j*ATT_B]) {
                OUTPUT_db[id_out*ATT] = a[i*ATT_A];
                OUTPUT_db[id_out*ATT+1] = a[i*ATT_A+1];
                OUTPUT_db[id_out*ATT+2] = b[j*ATT_B+1];
                id_out++;
            }
        }
    }
}