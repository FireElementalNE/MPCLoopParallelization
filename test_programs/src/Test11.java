class Test11 {
    public static void main(String[] args) {
        int N = 5;
        int[] A = {1, 2, 3, 4, 5};
        int[] B = {1, 2, 3, 4, 5};
        int[] C = {1, 2, 3, 4, 5};
        int[] D = {1, 2, 3, 4, 5};
        for (int i=1; i <= N; i++) {
            A[i] = B[i];
            B[i] = A[i] * D[i-1];
            C[i] = A[i] * D[i+1];
            D[i] = B[i] * C[i*1];
        }
    }
}
