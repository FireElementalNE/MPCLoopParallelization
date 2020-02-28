import java.util.Objects;

class Test4 {
    static double[] a = new double[5];
    //        double[] a1 = new double[a.length];
    static int i = 1;
    static int z = 10;
    static int p = 20;
    public static void main(String[] args) {
        for(int i = 0; i < a.length; i++) {
//            Utils.phi(a, a1);
//            a1 = (double[])Utils.arraycopy(a);
//            assert !Objects.equals(a1, null);
            z = z + i;
            a[i] = i;
            a[i] = i + a[i];
            if(a[i] > 3) {
                a[i] = p * i + z;
            } else {
                a[i] = p * i - z;
            }
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
