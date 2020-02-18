class Test1 {
    public static void main(String[] args) {
        double[] a = new double[5];
        int i = 1;
        int one = 1;
        while(i < 6) {
            double x = Math.pow(2.0, i);
            a[i - one] = x;
            i++;
        }
    }
}
