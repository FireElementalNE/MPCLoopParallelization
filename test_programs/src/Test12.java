class Test12 {
    public static void main(String[] args) {
        int[] a = new int[5];
        int i = 1;
        int z = 10;
        int p = 20;
        int q = 0;
        int k = 0;
        while(i < 5) {
            k = i;
            z = z + i;
            a[i] = i;
            a[k] = i + a[i];
            int j = i - 1;
            if(a[i] > 3) {
                a[k] = p * 10;
                if(p > 20) {
                    a[i] = p * 20;
                    if(p > 100) {
                        a[i] = p * 5;
                    } else {
                        a[i-1] = p - 23;
                    }
                    z = z + a[i - z];
                } else {
                    a[k] = p * 30;
                }
                z = z + a[i-3];
            } else {
                a[i] = p * 40;
            }
            q = q + a[i];
            i++;
        }
    }
}
