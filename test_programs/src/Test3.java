import java.util.Objects;

class Test3 {
    static int[] arraycpy(int[] a) {
        int[] b = new int[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }
    static int[] phi(Object a, Object a1) {
        int[] x = {1, 2, 3, 4, 5};
        return x;
    }
    public static void main(String[] args) {
        int N = 5;
        int[] C = {1, 2, 3, 4, 5};
        int[] X = {1, 2, 3, 4, 5};
        for (int i = 1; i <= N; i++) {
            if (C[i] > 10) {
                X[i] = i;
            } else {
                X[i] = i + 1;
            }
        }
    }
}

//a1 = phi(a, a3)
//
//ac(a2, a1);
//a2[i] = i;
//ac(a3, a2);
//a3[i] = i + a2[i];
