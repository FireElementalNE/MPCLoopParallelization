class Test14 {
    public static void main(String[] args) {
        int N = 5;
        int[] A = {1, 2, 3, 4, 5};
        int[] B = {1, 2, 3, 4, 5};
        int[] C = {1, 2, 3, 4, 5};
        int[] D = {1, 2, 3, 4, 5};
        int k = 0;
        int j = 0;
        int l = 0;
        int z = 0;
        for (int i=1; i <= N; i++) {
            z = i + 1;
            k = i + 1;
            j = k - 1;
            l = j - z;
            D[l] = 100;
            A[i] = B[i];
            B[i] = A[i] * D[l + 13];
            C[i] = A[i] * D[l];
            D[i] = B[i] * C[i];
        }
    }
}