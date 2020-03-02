class Test0 {
    static int f1(int a) {
        return Math.abs(a);
    }
    static int f2(int a, int b) {
        return Math.abs(a - b);
    }
    static int f3(int a, int b) {
        return Math.abs(a * b);
    }
    static int f4(int a, int b) {
        return Math.abs(a ^ b);
    }
    static int[] make_arr() {
        int a[] = new int[5];
        a[0] = 1;
        a[1] = 2;
        a[2] = 3;
        a[3] = 4;
        a[4] = 5;
        return a;
    }
    public static void main(String[] args) {
        int N = 5;
        int[] A = {1, 2, 3, 4, 5};
        int[] B = {1, 2, 3, 4, 5};
        int[] C = {1, 2, 3, 4, 5};
        int[] D = {1, 2, 3, 4, 5};
        for (int i=1; i <= N; i++) {
            A[i] = f1(B[i]);
            B[i] = f2(A[i], D[i-1]);
            C[i] = f3(A[i], D[i-1]);
            D[i] = f4(B[i], C[i]);
        }
    }
}
