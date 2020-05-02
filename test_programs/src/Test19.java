class Test19 {
    public static void main(String[] args) {
        int[] A = new int[10];
        int[] B = new int[10];
        int z = -1;
        for(int i = 0; i < 10; i++) {
            int p = i * z;
            if(p > 10) {
                A[i - 2 + z] = 10; // j = i - 2 + z; j - i = d
            } else {
                A[i - 10] = 10;
            }
            B[i] = A[i - 1];
        }
    }
}