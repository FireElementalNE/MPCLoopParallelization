import java.util.Objects;

class Test1 {
    public static void main(String[] args) {
        double[] a = new double[5];
//        double[] a1 = new double[a.length];
        int i = 1;
        int z = 10;
        while(i < 5) {
//            Utils.phi(a, a1);
//            a1 = (double[])Utils.arraycopy(a);
//            assert !Objects.equals(a1, null);
            a[i] = i;
            a[i] = i + a[i];
            if(a[i] > 3) {
                z = z + 10;
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
