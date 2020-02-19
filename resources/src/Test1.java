import java.util.Objects;

class Test1 {
    public static void main(String[] args) {
        double[] a = new double[5];
//        double[] a1 = new double[a.length];
        int i = 1;
        while(i < 5) {
//            Utils.phi(a, a1);
//            a1 = (double[])Utils.arraycopy(a);
//            assert !Objects.equals(a1, null);
            a[i] = i;
            i++;
        }
    }
}
