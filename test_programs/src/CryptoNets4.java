class CryptoNets4 {
    public static void main(String[] args) {
        int[] a = new int[100];
        int[] b = new int[100];
        int common = Integer.parseInt(args[1]);
        int i = 0;
        int sum = 0;
        while(i+8<= common) {
            sum += a[i+0]*b[i+0] + a[i+1]*b[i+1] + a[i+2]*b[i+2] + a[i+3]*b[i+3] + a[i+4]*b[i+4] + a[i+5]*b[i+5] + a[i+6]*b[i+6] + a[i+7]*b[i+7];
            i+=8;
        }
    }
}

