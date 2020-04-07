class Test13 {
    public static void main(String[] args) {
        int N = 5;
        int[] A = {1, 2, 3, 4, 5};
        int[] B = {1, 2, 3, 4, 5};
        int[] C = {1, 2, 3, 4, 5};
        int[] D = {1, 2, 3, 4, 5};
        int k = 0;
        int j = 0;
        for (int i=1; i <= N; i++) {
            k = j;
            A[i] = B[k];
            B[i] = A[k] * D[i-1];
            C[i] = A[k] * D[i-1];
            D[i] = B[k] * C[i];
        }
    }
}