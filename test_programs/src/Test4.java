class Test4 {

    public static void main(String[] args) {
        int[] a = new int[5];
        int z = 10;
        int p = 20;
        for(int i = 0; i < 5; i++) {
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
