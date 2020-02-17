class Test2 {
    static int f1(int a, int b) {
        return a + b;
    }
    public static void main(String[] args) {
        int[] a = {1, 2, 3, 4, 5};
        int[] b = {1, 2, 3, 4, 5};
        int[] c = {1, 2, 3, 4, 5};
        int[] d = {1, 2, 3, 4, 5};
        int i = 1;
        while(i < 5) {
            a[i] = f1(b[i - 1], c[i - 1]);
            b[i] = d[i] + 1;
            c[i] = d[i] + 1;
        }
    }
}
