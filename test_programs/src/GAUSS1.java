class GAUSS1 {
    public static void main(String[] args) {
        int[] m = new int[100];
        int from = 77;
        int n = 100;
        int to = 66;
        for(int j = from; j < n; j++) {
            int tmp = m[from*n+j];
            m[from*n+j] = m[to*n+j];
            m[to*n+j] = tmp;
        }
    }
}


