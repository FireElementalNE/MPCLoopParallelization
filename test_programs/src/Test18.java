class Test18 {
    public static void main(String[] args) {
        int[] A = new int[10];
        int[] B = new int[10];
        int[] C = new int[10];
        int[] D = new int[10];
        int[] E = new int[10];
        int sum = 0;
        for(int i = 0; i < 10; i++) {
            B[i] = A[i] * D[i-1];
            C[i] = A[i] * D[i-1];
            D[i] = B[i] * C[i];
            sum = sum + E[i];
        }
    }
}