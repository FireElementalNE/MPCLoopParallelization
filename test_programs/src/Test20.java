class Test20 {
    public static void main(String[] args) {
        int[] A = new int[10];
        int[] B = new int[10];
        int[] C = new int[10];
        for(int i = 0; i < 10; i++) {
            A[i] = B[i - 1];
            B[i] = C[i - 2];
        }
    }
}