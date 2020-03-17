class Test1 {
    public static void main(String[] args) {
        int[] a = new int[5];
//        double[] a1 = new double[a.length];
        int i = 1;
        int z = 10;
        int p = 20;
        int q = 0;
        while(i < 5) {
//            Utils.phi(a, a1);
//            a1 = (double[])Utils.arraycopy(a);
//            assert !Objects.equals(a1, null);
            z = z + i;
            a[i] = i;
            a[i] = i + a[i];
            if(a[i] > 3) {
                a[i] = p * i + z;
                a[i] = z * i + p;
            } else {
                a[i] = p * i - z;
            }
            q = q + a[i];
            i++;
        }
    }
}

//a1 = phi(a, a3)
//
//ac(a2, a1);
//a2[i] = i;
//ac(a3, a2);
//a3[i] = i + a2[i];
