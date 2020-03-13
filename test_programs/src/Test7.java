class Test7 {
    public static void main(String[] args) {
        int[] a = {1, 2, 3, 4, 5};
        int[] b = {1, 2, 3, 4, 5};
        for(int i = 1; i < 5; i++) {
            a[i] = i;
            b[i] = a[i - 1];
        }
    }
}

//a1 = phi(a, a3)
//
//ac(a2, a1);
//a2[i] = i;
//ac(a3, a2);
//a3[i] = i + a2[i];
