class Test15 {
    public static void main(String[] args) {
        int[] A = new int[10];
        int[] B = new int[10];
        int sum = 0;
        for(int i = 0; i < 10; i++) {
            int t = A[i] * B[i];
            sum = sum + t;
        }
    }
}